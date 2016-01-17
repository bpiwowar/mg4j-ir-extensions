package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.mg4j.extensions.utils.*;
import net.bpiwowar.xpm.manager.tasks.AbstractTask;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.ProgressListener;
import net.bpiwowar.xpm.manager.tasks.TaskDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.String.format;

/**
 * Wrapper for Index in MG4J
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(id = "mg4j:cat-batches", output = "xp:null", description = "Outputs a document collection as a stream",
        registry = Registry.class)
public class CatCollectionBatches extends AbstractTask {
    static final Logger LOGGER = LoggerFactory.getLogger(CatCollectionBatches.class);
    // Just for debug
    final static boolean noOutput = false;

    @JsonArgument(name = "collections", required = true)
    ArrayList<Collection> collectionPaths;
    @JsonArgument(name = "fields", help = "The fields to output", required = true)
    ArrayList<String> fieldNames = new ArrayList<>();
    @JsonArgument(required = true)
    TextToolChain toolchain;
    @JsonArgument(name = "batch_size", required = true, help = "Number of elements in each batch")
    long batchSize;
    @JsonArgument(required = false, help = "Whether the document number and ID should be printed (default true)")
    boolean header = true;
    @JsonArgument(name = "batches", required = true, help = "Number of batches (positive number) or " +
            "number of iterations (negative number)")
    long batches = 0;
    transient boolean stop;

    /**
     * Iterator for a range
     */
    public CloseableIterator<CollectionInformation.DocumentInformation> range(CollectionInformation[] collections, long start, long end) throws IOException {
        if (end - start == 0) {
            return new CloseableIterator<CollectionInformation.DocumentInformation>() {
                @Override
                protected CollectionInformation.DocumentInformation computeNext() {
                    return endOfData();
                }
            };
        }

        return new CloseableIterator<CollectionInformation.DocumentInformation>() {
            public CloseableIterator<CollectionInformation.DocumentInformation> iterator;
            // The starting position within the next collection
            long position = start;

            // The offset for the next collection
            long offset = start;

            // Remaining number of documents to output
            long remaining = end - start;

            // Number of remaining documents for the current iterator (debug)
            long iteratorRemaining;

            // Current index
            int ix = 0;

            {
                while (position >= collections[ix].size) {
                    position -= collections[ix].size;
                    ix++;
                }
                // Adjust offset, so that it matches the beginning of the collection
                offset -= position;
            }

            @Override
            protected CollectionInformation.DocumentInformation computeNext() {
                if (iterator != null) {
                    if (iterator.hasNext()) {
                        --iteratorRemaining;
                        return iterator.next();
                    }
                    assert iteratorRemaining == 0;
                    Exceptions.propagateStatement(() -> iterator.close());
                }

                if (remaining == 0) {
//                    System.err.format("%n");
                    return endOfData();
                }

                // Number of documents that we take from this iterator
                final long size = Long.min(collections[ix].size - position, remaining);
                iteratorRemaining = size - 1;
                iterator = Exceptions.propagate(() -> collections[ix].range(offset, position, position + size));
                LOGGER.info(format("Range[ix=%d{size=%d}, offset=%d, position=%d, size=%d, remaining=%d]",
                        ix, collections[ix].size,
                        offset, position, size, remaining - size));

                position = 0;
                remaining -= size;
                offset += collections[ix].size;
                ix++;

                return iterator.next();
            }

            @Override
            public void close() throws Exception {
                if (iterator != null) iterator.close();
            }


        };
    }

    @Override
    public JsonObject execute(JsonObject r, ProgressListener progress) throws Throwable {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size should be greater than 0");
        }
        // -*- Loads the document collection
        CollectionInformation[] collections = this.collectionPaths.stream()
                .map(Streams.propagateFunction(c -> new CollectionInformation(c, fieldNames)))
                .toArray(n -> new CollectionInformation[n]);

        LOGGER.info(format("Term processor class is %s", toolchain.termProcessor.getClass()));

        // Compute the total number of documents
        final long numberOfDocuments = Arrays.stream(collections).mapToLong(ci -> ci.size).sum();


        // Handles broken pipe
        stop = false;
        Signals.PIPE.handle(signal -> stop = true);

        long batch = 0;
        MutableString word = new MutableString();
        MutableString delimiter = new MutableString();

        if (batches < 0) {
            long totalSize = Arrays.stream(collections).mapToLong(CollectionInformation::getSize).sum();
            long old = batches;
            batches = -totalSize / batchSize * batches;
            LOGGER.info(format("Total size is %d: outputing %d batches of size %d (%d times)",
                    totalSize, batchSize, batches, -old));
        }

        while (!stop && (batch < batches || batches == 0)) {
            batch++;
            // Select a starting document
            final int start = (int) (Math.random() * (numberOfDocuments - batchSize));
            LOGGER.debug(format("Batch %d / %d [start = %d]", batch, batches, start));
            try (final CloseableIterator<CollectionInformation.DocumentInformation> iterator = range(collections, start, start + batchSize)) {
                int count = 0;
                while (iterator.hasNext()) {

                    try (final CollectionInformation.DocumentInformation info = iterator.next()) {
                        ++count;
                        if (!noOutput) {
//                            System.out.format("%d\t", count);
                            if (header) {
                                System.out.format("%d\t%s\t", info.docid, info.document.uri());
                            }
                        }

                        final int[] fields = info.fields();
                        DocumentFactory.FieldType[] types = info.types();
                        boolean first = true;
                        for (int i = 0; i < fields.length; i++) {
                            final Object content = info.document.content(0);
                            switch (types[i]) {
                                case TEXT: {
                                    toolchain.wordReader.setReader((FastBufferedReader) content);
                                    while (toolchain.wordReader.next(word, delimiter) && toolchain.termProcessor.processTerm(word)) {
                                        if (word != null && !word.isEmpty()) {
                                            if (!noOutput) {
                                                if (!first) System.out.print('\t');
                                                else first = false;
                                                System.out.print(word);
                                            }
                                        }
                                    }
                                    break;
                                }
                                default:
                                    throw new RuntimeException("Cannot handle type " + types[i]);
                            }

                        }

                        if (!noOutput) System.out.println();
                    }
                }

                if (count != batchSize) {
                    throw new AssertionError(format("Batch size was not respected (%d required, %d given / start %d)",
                            batchSize, count, start));
                }
            } catch (Throwable t) {
                LOGGER.error(format("Error outputing batch %d / %d [start = %d]", batch, batches, start));
                throw t;
            }
        }

        // no output
//        throw new AssertionError("This should not have been committed... I told you (above)!!!");
        return r;

    }


}
