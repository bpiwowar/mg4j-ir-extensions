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

import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.SegmentedInputStream;
import net.bpiwowar.mg4j.extensions.Compression;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentCollection;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentDescriptor;
import net.bpiwowar.mg4j.extensions.utils.ByteMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
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
    /**
     * Our static logger
     */
    static final private Logger LOGGER = LoggerFactory.getLogger(TRECDocumentCollection.class);

    /**
     * Serialization ID
     */
    private static final long serialVersionUID = 1;

    transient byte buffer[];

    transient byte docnoBuffer[];

    public TRECDocumentCollection(String[] files, DocumentFactory factory, int bufferSize, Compression compression, File metadataFile) throws IOException {
        super(files, factory, bufferSize, compression, metadataFile);
    }

    public TRECDocumentCollection(String[] files, DocumentFactory factory, ObjectBigArrayBigList<SegmentedDocumentDescriptor> descriptors, int bufferSize, Compression compression, File metadataFile) {
        super(files, factory, descriptors, bufferSize, compression, metadataFile);
    }

    public void checkBuffers() {
        if (buffer == null) {
            buffer = new byte[8 * 1024];
            docnoBuffer = new byte[1024];
        }
    }


    transient static final ByteMatch DOC_OPEN = ByteMatch.create("<DOC>"),
            DOC_CLOSE = ByteMatch.create("</DOC>"), DOCNO_OPEN = ByteMatch.create("<DOCNO>"),
            DOCNO_CLOSE = ByteMatch.create("</DOCNO>");


    protected static boolean equals(byte[] a, int len, byte[] b) {
        if (len != b.length)
            return false;
        while (len-- != 0)
            if (a[len] != b[len])
                return false;
        return true;
    }

    @Override
    public TRECDocumentCollection copy() {
        return new TRECDocumentCollection(files, factory().copy(), descriptors,
                bufferSize, compression, metadataFile);
    }

    @Override
    public Reference2ObjectMap<Enum<?>, Object> metadata(long index) {
        ensureDocumentIndex(index);
        final Reference2ObjectArrayMap<Enum<?>, Object> metadata
                = new Reference2ObjectArrayMap<>(4);

        try {
            metadataRandomAccess.seek(descriptors.get(index).metadataPosition);
            String docno = metadataRandomAccess.readUTF();
            metadata.put(PropertyBasedDocumentFactory.MetadataKeys.URI, docno);
        } catch (IOException e) {
            LOGGER.error(String.format("Could not retrieve metadata for file %d [%s]", index, e));
        }
        return metadata;
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
    @Override
    protected void parseContent(final int fileIndex, InputStream is, final DataOutputStream metadataStream) throws IOException {
        LOGGER.debug("Processing files %d (%s)", fileIndex, files[fileIndex]);
        checkBuffers();
        FastBufferedInputStream fbis = new FastBufferedInputStream(is,
                bufferSize);
        final boolean debugEnabled = LOGGER.isDebugEnabled();

        EventHandler handler = new EventHandler() {
            @Override
            public void endDocument(String docno, long currStart, long currStop) throws IOException {
                long metadataPosition = metadataStream.size();
                metadataStream.writeUTF(docno);

                // Store document pointers in a document descriptor,
                // add to descriptors. This stores all required
                // information to identify the document among the set
                // of input files.
                if (debugEnabled)
                    LOGGER.debug("Setting markers {%s, %d, %d}", docno,
                            currStart, currStop);
                descriptors.add(SegmentedDocumentDescriptor.create(fileIndex, currStart, currStop, metadataPosition));

            }
        };
        parseContent(fbis, buffer, docnoBuffer, handler);
    }

    static public class EventHandler {
        public void startDocument() {
        }

        public void endDocument(String docno, long currStart, long currStop) throws IOException {
        }

        public void write(byte[] bytes, int offset, int length) {
        }
    }

    static public void parseContent(InputStream fbis, byte[] buffer, byte[] docnoBuffer, EventHandler handler) throws IOException {
        long currStart = 0, currStop = 0, position;

        // Are we within a document?
        boolean startedBlock = false;
        boolean startedDocNo = false;

        // Process the document
        int read;
        DOC_OPEN.reset();
        position = 0;
        int docnoCount = 0;

        while ((read = fbis.read(buffer, 0, buffer.length)) > 0) {
            int docOffset = 0;

            for (int offset = 0; offset < read; offset++) {
                final byte b = buffer[offset];

                if (startedBlock) {
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

                        // New document
                        handler.write(buffer, docOffset, offset - docOffset + 1);
                        handler.endDocument(docno, currStart, currStop);
                    }
                } else {
                    if (DOC_OPEN.match(b)) {
                        // we matched <DOC>, now getting ready for
                        // <DOCNO> and </DOC>. A new document block starts
                        startedBlock = true;
                        currStart = position + offset + 1 - DOC_OPEN.size();
                        DOC_CLOSE.reset();
                        DOCNO_OPEN.reset();
                        handler.startDocument();
                        handler.write(DOC_OPEN.getBytes(), 0, DOC_OPEN.getBytes().length);
                        docOffset = offset + 1;
                    }
                }
            } // Loop over bytes of the buffer

            if (startedBlock) {
                assert read - docOffset < buffer.length;
                handler.write(buffer, docOffset, read - docOffset);
            }

            // Update the number of read bytes
            position += read;
        }

        fbis.close();
    }


}
