package net.bpiwowar.mg4j.extensions.query;

import it.unimi.di.big.mg4j.index.TermProcessor;
import net.bpiwowar.xpm.manager.tasks.ClassChooserInstance;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.utils.Output;
import net.bpiwowar.mg4j.extensions.utils.TermUtil;
import org.apache.commons.lang.mutable.MutableInt;

import java.util.*;

import static net.bpiwowar.mg4j.extensions.utils.Output.Formatter;

/**
 * Transform topics into MG4J query language
 */
@ClassChooserInstance(name = "part")
public class PartTopicProcessor implements TopicProcessor {
    final private static org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(PartTopicProcessor.class);

    @JsonArgument(name = "parts", help = "The parts of the query that we use", required = true)
    private ArrayList<Set<String>> queryTypes = new ArrayList<>();

    /**
     * A formatter that adds a MG4J weight to each word
     */
    public static Formatter<Map.Entry<String, MutableInt>> weightedWordFormatter =
            t -> String.format("%s{%d}", t.getKey(), t.getValue().intValue());


    @Override
    public String process(Tokenizer tokenizer, TermProcessor processor, IndexedField index, Topic topic) {
        TreeMap<String, MutableInt> terms = getPositiveTerms(tokenizer, processor, index, topic);

        return Output.toString(" | ", terms.entrySet(), weightedWordFormatter);

    }

    @Override
    public TreeMap<String, MutableInt> getPositiveTerms(Tokenizer tokenizer, TermProcessor processor, IndexedField index, Topic topic) {
        TreeMap<String, MutableInt> terms = new TreeMap<>();

        // Iterates over query types until we get something
        Iterator<Set<String>> fallbackIterator = queryTypes.iterator();
        while (terms.isEmpty() && fallbackIterator.hasNext()) {
            getPositiveTerms(tokenizer, processor, index, topic, terms, fallbackIterator.next());
        }

        return terms;
    }

    static private void getPositiveTerms(Tokenizer tokenizer, TermProcessor processor, IndexedField index, Topic topic, TreeMap<String, MutableInt> terms, Set<String> queryTypes) {
        for (String queryType : queryTypes) {
            // Retrieve the query part
            Query query = topic.getTopicPart(queryType);

            if (query == null) continue;

            // Take all the words from the topic and construct the query for
            // MG4J
            TermUtil.getPositiveTerms(tokenizer, query, terms, processor, index);
        }
    }
}
