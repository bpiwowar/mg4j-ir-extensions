package net.bpiwowar.mg4j.extensions.query;

import it.unimi.di.big.mg4j.index.TermProcessor;
import net.bpiwowar.mg4j.extensions.Utils;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import org.apache.commons.lang.mutable.MutableInt;

import java.util.TreeMap;

/**
 * Transform topics into MG4J query language
 */
public interface TopicProcessor {
    /**
     * Process a topic
     */
    String process(Tokenizer tokenizer, TermProcessor processor, IndexedField index, Topic topic);

    /**
     * Returns a bag of word representation of the topics
     *
     * @param tokenizer     The string tokenizer
     * @param termProcessor The term processor used to pre-process query terms
     * @param index         The index to be used (or null if no index should be used) to filter out non existent terms
     * @param topic         The topic to process
     * @return A map between positive query terms and their number of occurrences
     */
    TreeMap<String, MutableInt> getPositiveTerms(Tokenizer tokenizer, TermProcessor termProcessor, IndexedField index, Topic topic);

    static TopicProcessor fromJSON(String json) {
        return Utils.get(json, TopicProcessor.class);
    }
}
