package net.bpiwowar.mg4j.extensions.rf;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.handlers.ClassChooser.Choice;
import net.bpiwowar.mg4j.extensions.adhoc.TRECJudgments;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Choice(name = "constant")
public class ConstantRelevanceFeedback implements RelevanceFeedbackMethod {
	@Argument(name = "qrels", help = "The qrels file", required = true)
	File qrelsFile;

	transient private TRECJudgments judgments;

	@Override
	public boolean isConstant() {
		return true;
	}

	@Override
	public void init() throws Exception {
		judgments = new TRECJudgments(qrelsFile);

	}

	@Override
	public <T extends Document> Collection<ScoredDocument> process(
			String topicid, List<T> retrieved, DocumentFactory<T> factory) {
		// Get the judgments
		Map<String, Integer> topicQrels = judgments.get(topicid);
		if (topicQrels == null)
			return null;

		// Return what has been judged
		ArrayList<ScoredDocument> list = new ArrayList<>();
		for (Entry<String, Integer> docrel : topicQrels.entrySet())
			list.add(new ScoredDocument(factory.getDocument(docrel.getKey()), (float) docrel.getValue()));

		return list;
	}

}
