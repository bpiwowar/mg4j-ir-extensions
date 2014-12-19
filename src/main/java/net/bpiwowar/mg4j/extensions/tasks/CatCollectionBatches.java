package net.bpiwowar.mg4j.extensions.tasks;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.document.*;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.experimaestro.tasks.AbstractTask;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.String.format;

/**
 * Wrapper for Index in MG4J
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/7/12
 */
@TaskDescription(id = "mg4j:cat-batches", output = "xp:null", description = "Outputs a document collection as a stream",
        registry = Registry.class)
public class CatCollectionBatches extends AbstractTask {
    static final Logger LOGGER = LoggerFactory.getLogger(CatCollectionBatches.class);

    @JsonArgument(name = "collections", required = true)
    ArrayList<Collection> collectionPaths;

    @JsonArgument(name = "fields", help = "The fields to output", required = true)
    ArrayList<String> fieldNames = new ArrayList<>();

    @JsonArgument(required = true)
    TextToolChain toolchain;

    @JsonArgument(name = "batch_size", required = true, help = "Number of elements in each batch")
    long batchSize;

    @JsonArgument(name = "batches", required = true, help = "Number of batches")
    long batches = 0;

    transient boolean stop;

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

            // Remaining documents
            long remaining = end - start;

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
                    if (iterator.hasNext()) return iterator.next();
                    Exceptions.propagateStatement(() -> iterator.close());
                }

                if (remaining == 0) {
                    System.err.format("%n");
                    return endOfData();
                }

                final long size = Long.min(collections[ix].size - 1 - position, remaining);
                iterator = Exceptions.propagate(() -> collections[ix].range(offset, position, position + size));
                //System.err.format("Range[ix=%d{size=%d}, offset=%d, position=%d, size=%d, remaining=%d]%n",
                //        ix,collections[ix].size,
                //        offset, position, size, remaining-size);

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
    public JsonObject execute(JsonObject r) throws Throwable {
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
        Signal pipeSignal = new Signal("PIPE");
        Signal.handle(pipeSignal, signal -> stop = true);

        long batch = 0;
        MutableString word = new MutableString();
        MutableString delimiter = new MutableString();

        while (!stop && batch < batches) {
            batch++;
            LOGGER.debug(format("Batch %d / %d", batch, batches));
            // Select a starting document
            final int start = (int) (Math.random() * (numberOfDocuments - batchSize));
            try (final CloseableIterator<CollectionInformation.DocumentInformation> iterator = range(collections, start, start + batchSize)) {
                int count = 0;
                while (iterator.hasNext()) {
                    try (final CollectionInformation.DocumentInformation info = iterator.next()) {
                        ++count;
                        System.out.format("%d\t%s", info.docid, info.document.uri());
                        final int[] fields = info.fields();
                        DocumentFactory.FieldType[] types = info.types();
                        for (int i = 0; i < fields.length; i++) {
                            final Object content = info.document.content(0);
                            switch (types[i]) {
                                case TEXT: {
                                    toolchain.wordReader.setReader((FastBufferedReader) content);
                                    while (toolchain.wordReader.next(word, delimiter) && toolchain.termProcessor.processTerm(word)) {
                                        if (word != null && !word.isEmpty()) {
                                            System.out.print('\t');
                                            System.out.print(word);
                                        }
                                    }
                                    break;
                                }
                                default:
                                    throw new RuntimeException("Cannot handle type " + types[i]);
                            }

                        }

                        System.out.println();
                    }
                }

                if (count != batchSize) {
                    throw new AssertionError(format("Batch size was not respected (%d required, %d given)",
                            batchSize, count));
                }
            }
        }

        return r;
    }


}
