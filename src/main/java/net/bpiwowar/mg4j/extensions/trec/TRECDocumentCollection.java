package net.bpiwowar.mg4j.extensions.trec;

/*		 
 * MG4J: Managing Gigabytes for Java
 *
 * Copyright (C) 2006-2007 Sebastiano Vigna
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 2.1 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

import bpiwowar.argparser.Logger;
import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.document.DocumentIterator;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.io.SegmentedInputStream;
import net.bpiwowar.mg4j.extensions.Compression;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentCollection;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentDescriptor;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentIterator;
import net.bpiwowar.mg4j.extensions.utils.Match;

import java.io.IOException;
import java.io.InputStream;

/**
 * A collection for the TREC data set.
 * <p/>
 * <p/>
 * The documents are stored as a set of descriptors, representing the (possibly
 * gzipped) files they are contained in and the start and stop position in that
 * files. To manage descriptors later we rely on {@link SegmentedInputStream}.
 * <p/>
 * <p/>
 * To interpret a files, we read up to <samp>&lt;DOC&gt;</samp> and place a start
 * marker there, we advance to the header and store the URI. An intermediate
 * marker is placed at the end of the doc header tag and a stop marker just
 * before <samp>&lt;/DOC&gt;</samp>.
 * <p/>
 * <p/>
 * The collection provides both sequential access to all documents via the
 * iterator and random access to a given document. However, the two operations
 * are performed very differently as the sequential operation is much more
 * performant than calling {@link #document(long)} repeatedly.
 *
 * @author Alessio Orlandi
 * @author Luca Natali
 * @author Benjamin Piwowarski
 */
public class TRECDocumentCollection extends SegmentedDocumentCollection {
    /** Our static logger */
    static final private Logger LOGGER = Logger.getLogger(TRECDocumentCollection.class);

    /** Serialization ID */
    private static final long serialVersionUID = 1;

    transient private static final boolean DEBUG = false;


    transient byte buffer[] = new byte[8 * 1024];

    transient byte docnoBuffer[] = new byte[1024];

    public TRECDocumentCollection(String[] files, DocumentFactory factory, int bufferSize, Compression compression) throws IOException {
        super(files, factory, bufferSize, compression);
    }

    public TRECDocumentCollection(String[] files, DocumentFactory factory, ObjectBigArrayBigList<SegmentedDocumentDescriptor> descriptors, int bufferSize, Compression compression) {
        super(files, factory, descriptors, bufferSize, compression);
    }


    transient static final Match DOC_OPEN = Match.create("<DOC>"),
            DOC_CLOSE = Match.create("</DOC>"), DOCNO_OPEN = Match.create("<DOCNO>"),
            DOCNO_CLOSE = Match.create("</DOCNO>");


    protected static boolean equals(byte[] a, int len, byte[] b) {
        if (len != b.length)
            return false;
        while (len-- != 0)
            if (a[len] != b[len])
                return false;
        return true;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (lastStream != null)
            lastStream.close();
        descriptors = null;
    }

    @Override
    public TRECDocumentCollection copy() {
        return new TRECDocumentCollection(files, factory().copy(), descriptors,
                bufferSize, compression);
    }



    /**
     * Merges a new collection in this one, by rebuilding the gzFile array and
     * appending the other object one, concatenating the descriptors while
     * rebuilding all.
     * <p/>
     * It is supposed that the passed object contains no duplicates for the
     * local collection.
     */
    public void merge(TRECDocumentCollection other) {
        int oldLength = this.files.length;

        this.files = ObjectArrays.ensureCapacity(this.files, this.files.length
                + other.files.length);
        System
                .arraycopy(other.files, 0, this.files, oldLength,
                        other.files.length);

        ObjectIterator<SegmentedDocumentDescriptor> it = other.descriptors
                .iterator();
        while (it.hasNext()) {
            final SegmentedDocumentDescriptor tdd = (SegmentedDocumentDescriptor) it
                    .next().clone();
            tdd.fileIndex += oldLength;
            this.descriptors.add(tdd);
        }
    }

    /**
     * Process one of the files in order to find the blocks. This identifies
     * for each document its exact position and length in the set of files (see
     * also {@link SegmentedDocumentDescriptor} in this class).
     *
     * @param fileIndex The index in the files array
     * @param is        The input stream for this files
     * @throws java.io.IOException
     */
    protected void parseContent(int fileIndex, InputStream is) throws IOException {

        long currStart = 0, currStop = 0, position;

        // Are we within a document
        boolean startedBlock = false;
        boolean startedDocNo = false;

        LOGGER.debug("Processing files %d (%s)", fileIndex, files[fileIndex]);

        FastBufferedInputStream fbis = new FastBufferedInputStream(is,
                bufferSize);

        // Process the document
        int read;
        DOC_OPEN.reset();
        position = 0;
        int docnoCount = 0;

        final boolean debugEnabled = LOGGER.isDebugEnabled();
        while ((read = fbis.read(buffer, 0, buffer.length)) > 0) {
            for (int offset = 0; offset < read; offset++) {
                final byte b = buffer[offset];

                if (!startedBlock) {
                    if (DOC_OPEN.match(b)) {
                        // we matched <DOC>, now getting ready for
                        // <DOCNO> and </DOC>. A new document block starts
                        startedBlock = true;
                        currStart = position + offset + 1 - DOC_OPEN.size();
                        DOC_CLOSE.reset();
                        DOCNO_OPEN.reset();
                    }
                } else {
                    if (startedDocNo) {
                        // read and store document number byte
                        docnoBuffer = ByteArrays.grow(docnoBuffer,
                                docnoCount + 1);
                        docnoBuffer[docnoCount++] = b;

                        // check if we already read </DOCNO>
                        if (DOCNO_CLOSE.match(b)) {
                            startedDocNo = false;
                            docnoCount -= DOCNO_CLOSE.size(); // cut </DOCNO>
                        }
                    } else if (DOCNO_OPEN.match(b)) {
                        // we matched <DOCNO>. Prepare to read its content
                        startedDocNo = true;
                        docnoCount = 0;
                        DOCNO_CLOSE.reset();
                    }

                    // Handle separately the </DOC> tag
                    if (DOC_CLOSE.match(b)) {
                        // matched </DOCNO>
                        startedDocNo = startedBlock = false;
                        currStop = position + offset + 1;
                        DOC_OPEN.reset();

                        // get the document number
                        final String docno = new String(docnoBuffer, 0,
                                docnoCount, "ASCII").trim();

                        // Store document pointers in a document descriptor,
                        // add to descriptors. This stores all required
                        // information to identify the document among the set
                        // of input files.
                        if (debugEnabled)
                            LOGGER.debug("Setting markers {%s, %d, %d}", docno,
                                    currStart, currStop);
                        descriptors.add(new SegmentedDocumentDescriptor(fileIndex, currStart, currStop));
                        startedBlock = false;

                    }
                }
            }

            // Update the number of read bytes
            position += read;
        }

        fbis.close();
    }


}
