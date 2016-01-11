package net.bpiwowar.mg4j.extensions.query;

import it.unimi.di.big.mg4j.index.TermProcessor;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import org.apache.commons.lang.mutable.MutableInt;

import java.util.TreeMap;

/**
 * Transform topics into MG4J query language
 */
public interface TopicProcessor {
    /** Process a topic */
    String process(Tokenizer tokenizer, TermProcessor processor, IndexedField index, Topic topic);

    TreeMap<String, MutableInt> getPositiveTerms(Tokenizer tokenizer, TermProcessor termProcessor, IndexedField index, Topic topic);
}
