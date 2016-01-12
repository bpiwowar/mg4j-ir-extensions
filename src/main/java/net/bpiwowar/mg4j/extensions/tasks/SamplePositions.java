package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.index.IndexIterator;
import it.unimi.di.big.mg4j.index.IndexReader;
import it.unimi.di.big.mg4j.search.DocumentIterator;
import it.unimi.dsi.fastutil.ints.IntBigList;
import net.bpiwowar.experimaestro.tasks.AbstractTask;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.ProgressListener;
import net.bpiwowar.experimaestro.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Sample term positions
 */
@TaskDescription(id = "mg4j:sample-positions", output = "mg4j:positions-stream",
        description = "Outputs a tab-separated value stream with" +
                "<pre>TERM TID DID DOC_LENGHT POSITION_1 POSITION_2 ...</pre>",
        registry = Registry.class)
public class SamplePositions extends AbstractTask {
    public static final Logger LOGGER = LoggerFactory.getLogger(SamplePositions.class);

    @JsonArgument(required = true)
    IndexedCollection index;

    @JsonArgument(name = "fields", help = "The fields to output, associated to a weight (for sampling)." +
            "If the weight is < 0, then it is initialized to the number of values", required = true)
    Map<String, Double> fieldNames = new HashMap<>();

    @JsonArgument(help = "Maximum (approximate through sampling) number of documents to output for each sampled term",
            required = false)
    long maxdocuments = Long.MAX_VALUE;

    @JsonArgument(required = false)
    long seed = new Random().nextLong();

    // Used to detect a PIPE broken event
    transient boolean stop = false;

    static public class Source {
        IndexedField indexedField;
        double weight;
        IndexReader indexReader;

        public Source(IndexedField indexedField, double weight, IndexReader indexReader) {
            this.indexedField = indexedField;
            this.weight = weight;
            this.indexReader = indexReader;
        }
    }

    public JsonObject execute(JsonObject json, ProgressListener progress) throws Exception {
        if (fieldNames.isEmpty()) {
            throw new RuntimeException("At least one field name should be given");
        }

        // Get all the indices
        final Source[] sources = new Source[fieldNames.size()];

        int i = 0;
        for (Map.Entry<String, Double> entry : fieldNames.entrySet()) {
            String fieldName = entry.getKey();
            IndexedField _index = index.get(fieldName);
            double v = entry.getValue();
            if (v <= 0) v = _index.getNumberOfPostings();
            if (i > 0) v += sources[i - 1].weight;
            sources[i] = new Source(_index, v, _index.getReader());
            if (!_index.index.hasPositions) {
                throw new RuntimeException("No positions for index " + fieldName + "!");
            }
            ++i;
        }

        double totalWeight = sources[sources.length - 1].weight;
        for (Source source : sources) {
            source.weight /= totalWeight;
        }

        // Infinite stream
        final Random random = new Random(seed);
        LOGGER.info("Random seed is {}", seed);

        long docid;
        int position;
        final PrintStream out = System.out;

        // Finish
        Signal pipeSignal = new Signal("PIPE");
        Signal.handle(pipeSignal, signal -> stop = true);

        while (!stop) {
            // Choose index and term
            final double v = random.nextDouble();
            int ix = 0;
            for (; ix < sources.length; ++ix) {
                if (sources[ix].weight >= v) {
                    break;
                }
            }
            assert ix < sources.length;

            final Source source = sources[ix];
            long termId = (long) (random.nextFloat() * source.indexedField.index.numberOfTerms);
            final IntBigList sizes = source.indexedField.index.sizes;

            // Outputs
            final IndexIterator documents = source.indexReader.documents(termId);

            String prefix = String.format("%s\t%d\t", source.indexedField.getTerm(termId), termId);
            final double samplingRate = maxdocuments / (double) documents.frequency();

            while ((docid = documents.nextDocument()) != DocumentIterator.END_OF_LIST) {
                if (samplingRate >= 1. || random.nextDouble() < samplingRate) {
                    out.print(prefix);
                    out.print(docid);
                    out.print('\t');
                    out.print(sizes.get(docid));
                    while ((position = documents.nextPosition()) != IndexIterator.END_OF_POSITIONS) {
                        out.print('\t');
                        out.print(position);
                    }
                    out.println();
                }
            }
        }

        LOGGER.info("Finished outputing samples");
        return null;
    }
}
