package net.bpiwowar.mg4j.extensions.segmented;

import bpiwowar.argparser.Logger;
import it.unimi.di.big.mg4j.document.*;
import it.unimi.dsi.fastutil.objects.ObjectBigArrayBigList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.SegmentedInputStream;
import it.unimi.dsi.logging.ProgressLogger;
import net.bpiwowar.mg4j.extensions.Compression;
import net.sf.samtools.util.BlockCompressedInputStream;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * A collection of documents that are concatenated in files.
 *
 * It is assumed that while the number of documents is big, and may not fit easily in memory,
 * the number of files is low.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 17/7/12
 */
public abstract class SegmentedDocumentCollection extends AbstractDocumentCollection implements Serializable {
    /** The serialization ID */
    private static final long serialVersionUID = 2;

    static final private Logger LOGGER = Logger.getLogger(SegmentedDocumentCollection.class);
    /**
     * Default buffer size, set up after some experiments.
     */
    public static final int DEFAULT_BUFFER_SIZE = 64000;

    /**
     * The list of the files containing the documents.
     * @todo Use a trie data structure?
     */
    protected String[] files;

    /**
     * Whether the files in {@link #files} are gzipped.
     */
    protected Compression compression = Compression.NONE;

    /**
     * The document factory.
     */
    protected DocumentFactory factory;

    /**
     * The buffer size.
     */
    protected final int bufferSize;

    /**
     * The list of document descriptors. We assume that descriptors within the
     * same files are contiguous - descriptors are saved separately, that's why
     * they are transient
     */
    public transient ObjectBigArrayBigList<SegmentedDocumentDescriptor> descriptors;

    /**
     * The last returned stream.
     */
    transient protected SegmentedInputStream lastStream;

    /**
     * Creates a new TREC collection by parsing the given files.
     *
     * @param files       an array of files names containing documents in TREC
     *                    format.
     * @param factory     the document factory (usually, a composite one).
     * @param bufferSize  the buffer size.
     * @param compression true if the files are gzipped.
     */
    public SegmentedDocumentCollection(String[] files, DocumentFactory factory,
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
    protected SegmentedDocumentCollection(String[] files, DocumentFactory factory,
                                     ObjectBigArrayBigList<SegmentedDocumentDescriptor> descriptors,
                                     int bufferSize, Compression compression) {
        this.compression = compression;
        this.files = files;
        this.bufferSize = bufferSize;
        this.factory = factory;
        this.descriptors = descriptors;
    }

    @Override
    public long size() {
        return descriptors.size64();
    }

    /** Parse the content of a given file */
    abstract protected void parseContent(int fileIndex, InputStream is) throws IOException;

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

        final long size = s.readLong();
        final ObjectBigArrayBigList<SegmentedDocumentDescriptor> descriptors = new ObjectBigArrayBigList<SegmentedDocumentDescriptor>();
        descriptors.ensureCapacity(size);
        for (int i = 0; i < size; i++)
            descriptors.add(new SegmentedDocumentDescriptor(s.readInt(), s.readLong(), s.readInt()));
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
        s.writeLong(descriptors.size64());

        for (SegmentedDocumentDescriptor descriptor : descriptors) {
            s.writeInt(descriptor.fileIndex);
            s.writeLong(descriptor.startMarker);
            s.writeInt(descriptor.stopMarkerDiff);
        }
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

    @Override
    public Reference2ObjectMap<Enum<?>, Object> metadata(final long index) {
        ensureDocumentIndex(index);
        final Reference2ObjectArrayMap<Enum<?>, Object> metadata
                = new Reference2ObjectArrayMap<Enum<?>, Object>(4);

        SegmentedDocumentDescriptor trecDocumentDescriptor = descriptors.get(index);
        metadata.put(PropertyBasedDocumentFactory.MetadataKeys.URI, "Document #" + index);
//        metadata.put(Metadata.DOCID, trecDocumentDescriptor.docid);
        return metadata;
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
        return factory().getDocument(stream(n), metadata);
    }


    public DocumentFactory getFactory() {
        return factory;
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
        return new SegmentedDocumentIterator(this);
    }
}