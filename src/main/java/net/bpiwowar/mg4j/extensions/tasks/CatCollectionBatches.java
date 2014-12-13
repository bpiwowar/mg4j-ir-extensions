package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.document.Document;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.document.DocumentIterator;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.experimaestro.tasks.AbstractTask;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentCollection;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import net.bpiwowar.mg4j.extensions.utils.TextToolChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.util.ArrayList;

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

    @JsonArgument(required = true)
    Collection collection;

    @JsonArgument(name = "fields", help = "The fields to output", required = true)
    ArrayList<String> fieldNames = new ArrayList<>();

    @JsonArgument(required = true)
    TextToolChain toolchain;

    @JsonArgument(name = "batch_size", required = true, help = "Number of elements in each batch")
    long batchSize;

    @JsonArgument(name = "batches", required = true, help = "Number of batches")
    long batches = 0;

    transient boolean stop;

    @Override
    public JsonObject execute(JsonObject r) throws Throwable {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size should be greater than 0");
        }
        // -*- Loads the document collection
        DocumentCollection documents = collection.get();
        DocumentFactory factory = documents.factory();

        LOGGER.info(format("Term processor class is %s", toolchain.termProcessor.getClass()));

        // -*- Get the fields
        int[] fields = new int[fieldNames.size()];
        DocumentFactory.FieldType[] types = new DocumentFactory.FieldType[fields.length];
        for (int i = fields.length; --i >= 0; ) {
            fields[i] = factory.fieldIndex(fieldNames.get(i));
            types[i] = factory.fieldType(i);
        }


        final long numberOfDocuments = documents.size();
        MutableString word = new MutableString();
        MutableString delimiter = new MutableString();

        // Handles broken pipe
        stop = false;
        Signal pipeSignal = new Signal("PIPE");
        Signal.handle(pipeSignal, signal -> stop = true);

        long batch = 0;
        while (!stop && batch < batches) {
            batch++;
            LOGGER.debug(format("Batch %d / %d", batch, batches));
            // Select a starting document
            final int start = (int) (Math.random() * (numberOfDocuments - batchSize));
            try (final DocumentIterator iterator =
                         documents instanceof SegmentedDocumentCollection ?
                                 ((SegmentedDocumentCollection) documents).iterator(start)
                                 : null) {

                for (int docid = start; docid < start + batchSize && !stop; docid++) {
                    try (final Document document =
                                 iterator == null ?
                                         documents.document(docid)
                                         : iterator.nextDocument()) {
                        System.out.format("%d\t%s", docid, document.uri());
                        for (int i = 0; i < fields.length; i++) {
                            final Object content = document.content(0);
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
            }
        }

        return r;
    }
}
