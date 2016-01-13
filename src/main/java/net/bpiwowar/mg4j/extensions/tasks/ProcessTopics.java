package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.xpm.manager.tasks.AbstractTask;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.ProgressListener;
import net.bpiwowar.xpm.manager.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import net.bpiwowar.mg4j.extensions.utils.TextToolChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;

import static java.lang.String.format;

/**
 * Process queries
 */
@TaskDescription(id = "mg4j:process-topics", output = "mg4j:processed-topics",
        registry = Registry.class,
        description = "Get the queries as list of tokens")
public class ProcessTopics extends AbstractTask {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ProcessTopics.class);

    @JsonArgument(name = "toolchain", required = true)
    TextToolChain toolchain;

    @JsonArgument(name = "queries", required = true)
    ArrayList<Query> queries;

    @Override
    public JsonObject execute(JsonObject r, ProgressListener progress) throws Throwable {
        MutableString word = new MutableString();
        MutableString delimiter = new MutableString();

        int count = 0;

        for (Query query : queries) {
            count++;
            if (query.query == null) {
                throw new RuntimeException(format("Query has no 'query' field: %s", query));
            }
            if (query.id == null) {
                throw new RuntimeException(format("Query has no 'id' field: %s", query));
            }
            toolchain.wordReader.setReader(new StringReader(query.query));
            System.out.print(count);
            System.out.print("\t");
            System.out.print(query.id);
            while (toolchain.wordReader.next(word, delimiter) && toolchain.termProcessor.processTerm(word)) {
                System.out.print('\t');
                System.out.print(word);
            }
            System.out.println();
        }


        return r;
    }

}
