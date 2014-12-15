package net.bpiwowar.mg4j.extensions.adhoc;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.utils.timer.TaskTimer;

/**
 * Provides a MG4J scorer
 */
public interface RetrievalModel {

    /**
     * Initialize the retrieval model from a document collection and index configuration
     */
    public void init(DocumentCollection collection, IndexedField index) throws Exception;

    /**
     * Close all resources
     */
    void close();

    /**
     * Process a topic and return a set of results
     *
     * @param topicId
     * @param topic
     * @param capacity
     * @param timer    A timer
     * @param results
     */
    public void process(String topicId, String topic, int capacity, TaskTimer timer, ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results) throws Exception;
}
