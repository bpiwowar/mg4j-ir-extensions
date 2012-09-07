package net.bpiwowar.mg4j.extensions.segmented;

import bpiwowar.argparser.Logger;
import it.unimi.di.big.mg4j.document.AbstractDocumentIterator;
import it.unimi.di.big.mg4j.document.Document;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.io.SegmentedInputStream;

import java.io.IOException;

/**
 * Iterator over documents
* @author B. Piwowarski <benjamin@bpiwowar.net>
* @date 20/6/12
*/
public class SegmentedDocumentIterator extends AbstractDocumentIterator {
    static final private Logger LOGGER = Logger.getLogger(SegmentedDocumentIterator.class);

    /**
     * An iterator returning the descriptors of the documents in the
     * enveloping collection.
     */
    private final ObjectIterator<SegmentedDocumentDescriptor> descriptorIterator;
    /** The current stream. */
    private SegmentedInputStream siStream;
    /** The current document. */
    private int currentDocument = 0;
    /** The last returned document. */
    private Document last;
    /**
     * The first descriptor of the next files, if any, or
     * <code>null</code> if nextFile() has never been called.
     */
    private SegmentedDocumentDescriptor firstNextDescriptor;
    private SegmentedDocumentCollection collection;

    /**
     * Initialiaze a new document iterator on a segmented document collection
     * @param collection
     */
    public SegmentedDocumentIterator(SegmentedDocumentCollection collection) {
        this.collection = collection;
        descriptorIterator = collection.descriptors
                .iterator();
    }

    @Override
    public void close() throws IOException {
        if (siStream != null) {
            if (last != null)
                last.close();
            super.close();
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
        } else if (!nextFile())
            return null; // First call

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
        SegmentedDocumentDescriptor currentDescriptor = firstNextDescriptor != null ? firstNextDescriptor
                : descriptorIterator.next();
        int currentFileIndex = currentDescriptor.fileIndex;


        // We create the segmented input stream with all just collected descriptors
        siStream = new SegmentedInputStream(collection.openFileStream(collection.files[currentFileIndex]));

        do {
            siStream.addBlock(currentDescriptor.toSegments());
            if (!descriptorIterator.hasNext())
                break;
            currentDescriptor = descriptorIterator.next();
        } while (currentDescriptor.fileIndex == currentFileIndex);

        firstNextDescriptor = currentDescriptor; // The last assignment
        // will be
        // meaningless, but
        // it won't be used
        // anyway
        return true;
    }
}
