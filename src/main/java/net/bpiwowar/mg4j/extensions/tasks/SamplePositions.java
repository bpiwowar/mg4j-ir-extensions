package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.positions.Sampler;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import net.bpiwowar.mg4j.extensions.utils.Signals;
import net.bpiwowar.xpm.manager.tasks.AbstractTask;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.ProgressListener;
import net.bpiwowar.xpm.manager.tasks.TaskDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Sample term positions
 */
@TaskDescription(id = "mg4j:sample-positions", output = "mg4j:positions-stream",
        description = "Outputs a tab-separated value stream with" +
                "<pre>TERM TID DID FIELD DOC_LENGHT POSITION_1 POSITION_2 ...</pre>",
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

        Signals.PIPE.handle(signal -> stop = true);

        while (!stop) {
            sampler.next();
            sampler.state.print(out);
        }

        LOGGER.info("Finished outputing samples");
        return null;
    }
}
