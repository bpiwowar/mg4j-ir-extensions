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

import bpiwowar.argparser.*;
import it.unimi.di.big.mg4j.document.AbstractDocumentCollection;
import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.document.*;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.io.SegmentedInputStream;
import it.unimi.dsi.logging.ProgressLogger;
import net.bpiwowar.mg4j.extensions.CollectionBuilderOptions;
import net.bpiwowar.mg4j.extensions.Compression;
import net.bpiwowar.mg4j.extensions.Metadata;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentDescriptor;
import net.bpiwowar.mg4j.extensions.utils.Match;
import net.sf.samtools.util.BlockCompressedInputStream;
import org.apache.commons.configuration.ConfigurationException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

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
public class TRECDocumentCollection extends AbstractDocumentCollection
        implements Serializable {

    static final private Logger LOGGER = Logger.getLogger(TRECDocumentCollection.class);

    private static final long serialVersionUID = 1;

    transient private static final boolean DEBUG = false;
    /**
     * Default buffer size, set up after some experiments.
     */
    transient public static final int DEFAULT_BUFFER_SIZE = 64000;

    protected static boolean equals(byte[] a, int len, byte[] b) {
        if (len != b.length)
            return false;
        while (len-- != 0)
            if (a[len] != b[len])
                return false;
        return true;
    }

    public static void main(String[] arg) throws IOException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException,
            InvalidHolderException, ConfigurationException,
            ArgParserException {

        CollectionBuilderOptions options = new CollectionBuilderOptions();
        ArgParser argParser = new ArgParser("TRECDocumentCollection");
        argParser.addOptions(options);
        String[] file = argParser.matchAllArgs(arg, 0,
                ArgParserOption.STOP_FIRST_UNMATCHED);

        run(options, file);
    }

    /**
     * Parses the document collection and finally stores the
     * TRECDocumentCollection in a files
     *
     * @param options the set of options
     * @param files   the list of document files to parse
     * @throws java.io.IOException
     */
    public static void run(CollectionBuilderOptions options, String[] files) throws IOException,
           ConfigurationException {

        // Get an array of files from standard input if we don't have one in the arguments
        if (files.length == 0) {
            final ObjectArrayList<String> list = new ObjectArrayList<String>();
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(System.in));
            String s;
            while ((s = bufferedReader.readLine()) != null)
                list.add(s);
            files = list.toArray(new String[0]);
        }

        // To avoid problems with find and similar utilities, we sort the files
        // names
        if (!options.unsorted)
            Arrays.sort(files);

        final DocumentFactory documentFactory = new TRECDocumentFactory(
                options.properties
                        .toArray(new String[options.properties.size()]));

        if (files.length == 0)
            System.err.println("WARNING: empty files set.");
        TRECDocumentCollection coll = new TRECDocumentCollection(files,
                documentFactory, options.bufferSize, options.compression);
        BinIO.storeObject(coll, options.collection);
    }

    /**
     * The list of the files containing the documents.
     */
    String[] files;

    /**
     * Whether the files in {@link #files} are gzipped.
     */
    private Compression compression = Compression.NONE;

    /**
     * The document factory.
     */
    protected DocumentFactory factory;

    /**
     * The list of document descriptors. We assume that descriptors within the
     * same files are contiguous - descriptors are saved separately, that's why
     * they are transient
     */
    protected transient ObjectBigArrayBigList<SegmentedDocumentDescriptor> descriptors;

    /**
     * The buffer size.
     */
    private final int bufferSize;

    /**
     * The last returned stream.
     */
    transient private SegmentedInputStream lastStream;

    transient byte buffer[] = new byte[8 * 1024];

    transient byte docnoBuffer[] = new byte[1024];

    transient static final Match DOC_OPEN = Match.create("<DOC>"),
            DOC_CLOSE = Match.create("</DOC>"), DOCNO_OPEN = Match.create("<DOCNO>"),
            DOCNO_CLOSE = Match.create("</DOCNO>");

    /**
     * Creates a new TREC collection by parsing the given files.
     *
     * @param files       an array of files names containing documents in TREC
     *                    format.
     * @param factory     the document factory (usually, a composite one).
     * @param bufferSize  the buffer size.
     * @param compression true if the files are gzipped.
     */
    public TRECDocumentCollection(String[] files, DocumentFactory factory,
                                  int bufferSize, Compression compression) throws IOException {
        this.files = files;
        this.factory = factory;
        this.bufferSize = bufferSize;
        this.descriptors = new ObjectBigArrayBigList<SegmentedDocumentDescriptor>();
        this.compression = compression;

        final ProgressLogger progressLogger = new ProgressLogger(LOGGER);
        progressLogger.expectedUpdates = files.length;
        progressLogger.itemsName = "files";

        progressLogger.start("Parsing files with compression \"" + compression
                + "\"");

        for (int i = 0; i < files.length; i++) {
            parseContent(i, openFileStream(files[i]));
            progressLogger.update();
        }

        progressLogger.done();
    }

    /**
     * Copy constructor (that is, the one used by {@link #copy()}. Just
     * initializes final fields
     */
    protected TRECDocumentCollection(String[] files, DocumentFactory factory,
                                     ObjectBigArrayBigList<SegmentedDocumentDescriptor> descriptors,
                                     int bufferSize, Compression compression) {
        this.compression = compression;
        this.files = files;
        this.bufferSize = bufferSize;
        this.factory = factory;
        this.descriptors = descriptors;
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
        return new TRECDocumentCollection(files, factory.copy(), descriptors,
                bufferSize, compression);
    }

    /**
     * Returns a document
     *
     * @param n number of the document to return
     * @return the document
     */
    @Override
    public Document document(long n) throws IOException {
        Reference2ObjectMap<Enum<?>, Object> metadata = metadata(n);
        return factory.getDocument(stream(n), metadata);
    }

    /**
     * Returns the underlying document factory
     *
     * @return the document factory
     */
    @Override
    public DocumentFactory factory() {
        return this.factory;
    }

    /**
     * Returns the iterator over the documents. Use this method if you want
     * sequential access to the documents.
     *
     * @return the document iterator
     * @throws java.io.IOException
     */
    @Override
    public DocumentIterator iterator() throws IOException {
        return new TRECDocumentIterator(this);
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

    @Override
    public Reference2ObjectMap<Enum<?>, Object> metadata(final long index) {
        ensureDocumentIndex(index);
        final Reference2ObjectArrayMap<Enum<?>, Object> metadata
                = new Reference2ObjectArrayMap<Enum<?>, Object>(4);

        SegmentedDocumentDescriptor trecDocumentDescriptor = descriptors.get(index);
        metadata.put(PropertyBasedDocumentFactory.MetadataKeys.URI, "Document #" + index);
        metadata.put(Metadata.DOCID, trecDocumentDescriptor.docid);
        return metadata;
    }

    /**
     * Opens the files stream, supporting certain kinds of compression
     *
     * @param fileName the files name
     * @return the files stream
     * @throws java.io.IOException if something went wrong or compression is not
     *                             supported
     */
    final InputStream openFileStream(String fileName)
            throws IOException {
        if (compression == null) compression = Compression.NONE;

        switch (compression) {
            case BLOCK_GZIP:
                return new BlockCompressedInputStream(new File(fileName));
            case GZIP:
                return new GZIPInputStream(new FileInputStream(fileName));
            case NONE:
                return new FileInputStream(fileName);
        }

        throw new AssertionError(
                "Missing case in the switch for handling compression in files");
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
                        descriptors.add(new SegmentedDocumentDescriptor(docno,
                                fileIndex, currStart, currStop));
                        startedBlock = false;

                    }
                }
            }

            // Update the number of read bytes
            position += read;
        }

        fbis.close();
    }


    @Override
    public long size() {
        return descriptors.size();
    }

    @Override
    public InputStream stream(final long n) throws IOException {
        // Creates a Segmented Input Stream with only one segment in (the
        // requested one).
        ensureDocumentIndex(n);
        if (lastStream != null)
            lastStream.close();
        final SegmentedDocumentDescriptor descr = descriptors.get(n);
        return lastStream = new SegmentedInputStream(
                openFileStream(files[descr.fileIndex]), descr.toSegments());
    }


    /**
     * Deserialization
     *
     * @param s The object stream
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     */
    private void readObject(final ObjectInputStream s) throws IOException,
            ClassNotFoundException {
        s.defaultReadObject();

        final int size = s.readInt();
        final ObjectBigArrayBigList<SegmentedDocumentDescriptor> descriptors = new ObjectBigArrayBigList<SegmentedDocumentDescriptor>();
        descriptors.ensureCapacity(size);
        for (int i = 0; i < size; i++)
            descriptors.add(new SegmentedDocumentDescriptor(s.readUTF(),
                    s.readInt(), s.readLong(), s.readInt()));
        this.descriptors = descriptors;
    }

    /**
     * Write the object to disk
     *
     * @param s The serialisation step
     * @throws java.io.IOException
     */
    private void writeObject(final ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(descriptors.size());

        for (SegmentedDocumentDescriptor descriptor : descriptors) {
            s.writeUTF(descriptor.docid);
            s.writeInt(descriptor.fileIndex);
            s.writeLong(descriptor.startMarker);
            s.writeInt(descriptor.stopMarkerDiff);
        }
    }

}
