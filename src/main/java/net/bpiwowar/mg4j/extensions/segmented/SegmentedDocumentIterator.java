package net.bpiwowar.mg4j.extensions.segmented;

import it.unimi.di.big.mg4j.document.AbstractDocumentIterator;
import it.unimi.di.big.mg4j.document.Document;
import it.unimi.dsi.fastutil.objects.ObjectBigListIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.io.SegmentedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Iterator over documents in a segmented file
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class SegmentedDocumentIterator extends AbstractDocumentIterator {
    static final private Logger LOGGER = LoggerFactory.getLogger(SegmentedDocumentIterator.class);

    /**
     * An iterator returning the descriptors of the documents in the
     * enveloping collection.
     */
    private final ObjectBigListIterator<SegmentedDocumentDescriptor> descriptorIterator;
    /**
     * The current stream.
     */
    private SegmentedInputStream siStream;
    /**
     * The current document.
     */
    private long currentDocument = 0;
    /**
     * The last returned document.
     */
    private Document last;
    /**
     * The first descriptor of the next files, if any, or
     * <code>null</code> if nextFile() has never been called.
     */
    private SegmentedDocumentDescriptor firstNextDescriptor;
    private SegmentedDocumentCollection collection;

    /**
     * Initialiaze a new document iterator on a segmented document collection
     *
     * @param collection
     */
    public SegmentedDocumentIterator(SegmentedDocumentCollection collection) {
        this.collection = collection;
        descriptorIterator = collection.descriptors.iterator();
    }

    /**
     * Initialize a new document iterator on a segmented document collection
     *
     * @param collection The document collection
     * @param start The starting point in the
     */
    public SegmentedDocumentIterator(SegmentedDocumentCollection collection, long start) {
        this.collection = collection;
        this.currentDocument = start;
        descriptorIterator = collection.descriptors.subList(start, collection.descriptors.size64()).iterator();
    }

    /**
     * Initialize a new document iterator on a segmented document collection
     *
     * @param collection The document collection
     * @param start The starting point in the
     */
    public SegmentedDocumentIterator(SegmentedDocumentCollection collection, long start, long end) {
        this.collection = collection;
        this.currentDocument = start;
        descriptorIterator = collection.descriptors.subList(start, end).iterator();
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (siStream != null) {
            if (last != null)
                last.close();
            while (siStream.hasMoreBlocks()) {
                LOGGER.debug("Skipping one block [to close properly]");
                siStream.nextBlock();
            }
            LOGGER.debug("Closing");
            siStream.close();
            siStream = null;
        }
    }

    @Override
    public Document nextDocument() throws IOException {
        /*
        * If necessary, skip to the next segment, else, try skipping to
        * the next files.
        */
        LOGGER.debug("nextDocument() has been called ");

        if (last != null) {
            last.close();
            if (!siStream.hasMoreBlocks()) {
                if (!nextFile())
                    return last = null;
            } else
                siStream.nextBlock();
        } else if (!nextFile()) {
            return null; // First call
        }

        return last = collection.factory.getDocument(siStream,
                collection.metadata(currentDocument++));
    }

    private boolean nextFile() throws IOException {
        if (collection.size() == 0)
            return false;
        if (siStream != null)
            siStream.close();

        if (!descriptorIterator.hasNext())
            return false;

        // We assume documents contained in the same files are
        // contiguous so we collect all of them until we find a different
        // files index.
        SegmentedDocumentDescriptor currentDescriptor = descriptorIterator.next();
        int currentFileIndex = currentDescriptor.fileIndex;

        // We create the segmented input stream with all just collected descriptors
        siStream = new SegmentedInputStream(collection.openFileStream(collection.files[currentFileIndex]));

        // Add all the blocks from this file
        do {
            siStream.addBlock(currentDescriptor.toSegments());
            if (!descriptorIterator.hasNext())
                break;
            currentDescriptor = descriptorIterator.next();
        } while (currentDescriptor.fileIndex == currentFileIndex);

        descriptorIterator.previous();
        // will be
        // meaningless, but
        // it won't be used
        // anyway
        return true;
    }
}
