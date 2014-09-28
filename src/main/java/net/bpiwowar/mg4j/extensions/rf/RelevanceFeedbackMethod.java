package net.bpiwowar.mg4j.extensions.rf;

import java.util.Collection;
import java.util.List;

/**
 * Given a set of retrieved documents, returns a set of judged documents to be
 * used for feedback
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface RelevanceFeedbackMethod {
	/**
	 * Returns true if the RF does not depend on a list of results
	 */
	boolean isConstant();

	/**
	 * Return the list of documents to be used for RF
	 */
	<T extends Document> Collection<ScoredDocument> process(String topicid,
			List<T> retrieved, DocumentFactory<T> factory);

	/**
	 * Initialise
	 * 
	 * @throws Exception
	 */
	void init() throws Exception;

}
