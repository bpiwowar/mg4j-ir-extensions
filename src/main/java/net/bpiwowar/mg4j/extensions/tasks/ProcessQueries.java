package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;
import org.apache.log4j.Logger;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Created by bpiwowar on 8/9/14.
 */
@TaskDescription(name = "process-queries", project = {"ir"}, description = "Get the queries")
public class ProcessQueries extends AbstractTask {
    protected static final Logger LOGGER = Logger.getLogger(ProcessQueries.class);

    @Argument(name = "word-reader", required = true)
    WordReaderType wordReaderType;

    static public class Query {
        String id;
        String query;
    }

    @Override
    public int execute() throws Throwable {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<Collection<Query>>() {
        }.getType();
        WordReader wordReader = wordReaderType.getWordReader();

        final Collection<Query> queries = gson.fromJson(new InputStreamReader(System.in), collectionType);
        MutableString word = new MutableString();
        MutableString delimiter = new MutableString();

        for (Query query : queries) {
            if (query.query == null) throw new RuntimeException("A query has no 'query' field");
            if (query.id == null) throw new RuntimeException("A query has no 'id' field");
            wordReader.setReader(new StringReader(query.query));
            System.out.print(query.id);
            while (wordReader.next(word, delimiter)) {
                System.out.print('\t');
                System.out.print(word);
            }
            System.out.println();
        }


        return 0;
    }
}
