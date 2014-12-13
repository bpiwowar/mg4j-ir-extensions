package net.bpiwowar.mg4j.extensions.query;

import it.unimi.di.big.mg4j.index.TermProcessor;
import net.bpiwowar.experimaestro.tasks.ClassChooserInstance;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.utils.Output;
import net.bpiwowar.mg4j.extensions.utils.TermUtil;
import org.apache.commons.lang.mutable.MutableInt;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static net.bpiwowar.mg4j.extensions.utils.Output.Formatter;

/**
 * Transform topics into MG4J query language
 */
@ClassChooserInstance(name = "part")
public class PartTopicProcessor implements TopicProcessor {
    @JsonArgument(name = "parts", help = "The parts of the query that we use", required = true)
    private Set<String> queryTypes = new TreeSet<>();

    /**
     * A formatter that adds a MG4J weight to each word
     */
    public static Formatter<Map.Entry<String, MutableInt>> weightedWordFormatter =
            t -> String.format("%s{%d}", t.getKey(), t.getValue().intValue());



    @Override
    public String process(TermProcessor processor, IndexedField index, Topic topic) {
        // --- Get the MG4J topic
        TreeMap<String, MutableInt> terms = new TreeMap<>();

        for (String queryType : queryTypes) {
            Query query = topic.getTopicPart(queryType);
            // Take all the words from the topic and construct the query for
            // MG4J
            TermUtil.getPositiveTerms(query, terms, processor, index);
        }

        return Output.toString(" | ", terms.entrySet(), weightedWordFormatter);

    }
}
