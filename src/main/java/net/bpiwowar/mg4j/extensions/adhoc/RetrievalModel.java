package net.bpiwowar.mg4j.extensions.adhoc;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.bpiwowar.mg4j.extensions.conf.IndexConfiguration;
import net.bpiwowar.mg4j.extensions.query.Topic;
import net.bpiwowar.mg4j.extensions.utils.timer.TaskTimer;

public interface RetrievalModel {

	public void init(DocumentCollection collection, IndexConfiguration index) throws Exception;

	/**
	 * Process a topic and return a set of results
	 *
     * @param topic
     * @param results
     * @param capacity
     * @param timer A timer
     */
	public void process(
            Topic topic,
            ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results,
            int capacity, TaskTimer timer) throws Exception;

	void close();
}
