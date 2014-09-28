package net.bpiwowar.mg4j.extensions.rf;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.handlers.ClassChooser.Choice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.min;


@Choice(name="blind")
public class BlindRelevanceFeedback implements RelevanceFeedbackMethod {
	@Argument(name = "top-k", help = "The number of documents to be considered as relevant")
	int topK;

	@Override
	public boolean isConstant() {
		return false;
	}

	/**
	 * Constructor
	 * @param topK return the topK documents
	 */
	public BlindRelevanceFeedback(int topK) {
		this.topK = topK;
	}
	
	public BlindRelevanceFeedback(){}
	
	@Override
	public <T extends Document> Collection<ScoredDocument> process(
			String topicid, List<T> retrieved, DocumentFactory<T> factory) {
		ArrayList<ScoredDocument> list = new ArrayList<>();
		final int top = min(retrieved.size(), topK);

		for (int i = 0; i < top; i++)
			list.add(new ScoredDocument(retrieved.get(i), 1f));

		return list;
	}

	@Override
	public void init() throws Exception {
	}

}
