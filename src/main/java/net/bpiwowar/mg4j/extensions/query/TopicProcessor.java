package net.bpiwowar.mg4j.extensions.query;

import it.unimi.di.big.mg4j.index.TermProcessor;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;

/**
 * Transform topics into MG4J query language
 */
public interface TopicProcessor {
    /** Process a topic */
    String process(TermProcessor processor, IndexedField index, Topic topic);
}
