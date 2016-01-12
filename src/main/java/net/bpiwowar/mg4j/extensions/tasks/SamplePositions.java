package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.index.IndexIterator;
import net.bpiwowar.experimaestro.tasks.AbstractTask;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.ProgressListener;
import net.bpiwowar.experimaestro.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.positions.Sampler;
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

    public JsonObject execute(JsonObject json, ProgressListener progress) throws Exception {
        Sampler sampler = new Sampler(seed, fieldNames, index, maxdocuments);

        PrintStream out = System.out;

        // Finish
        Signal pipeSignal = new Signal("PIPE");
        Signal.handle(pipeSignal, signal -> stop = true);

        while (!stop) {
            sampler.next();
            out.format("%s\t%d\t", sampler.getTerm(), sampler.getTermId());
            out.print(sampler.getDocumentId());
            out.print('\t');
            out.print(sampler.getDocumentSize());
            int position;
            while ((position = sampler.nextPosition()) != IndexIterator.END_OF_POSITIONS) {
                out.print('\t');
                out.print(position);
            }
            out.println();
        }

        LOGGER.info("Finished outputing samples");
        return null;
    }
}
