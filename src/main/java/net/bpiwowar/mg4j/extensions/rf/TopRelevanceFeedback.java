package net.bpiwowar.mg4j.extensions.rf;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.handlers.ClassChooser.Choice;
import net.bpiwowar.mg4j.extensions.adhoc.TRECJudgments;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.lang.Math.min;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Choice(name = "top", help = "Uses the top retrieved documents with a true relevance value")
public class TopRelevanceFeedback implements RelevanceFeedbackMethod {
    @Argument(name = "top-k", help = "The number of documents to be considered as relevant")
    int topK;

    @Argument(name = "qrels", help = "The qrels file", required = true)
    File qrelsFile;

    @Argument(name = "ignore-missing", help = "Ignore missing relevance judgments for feedback (default: consider not relevant)")
    boolean ignoreMissing;

    transient private TRECJudgments judgments;

    transient private boolean initialised = false;

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public void init() throws Exception {
        judgments = new TRECJudgments(qrelsFile);
        initialised = true;
    }

    @Override
    public <T extends Document> Collection<ScoredDocument> process(
            String topicid, List<T> retrieved, DocumentFactory<T> factory) {
        // Get the judgments
        Map<String, Integer> topicQrels = judgments.get(topicid);
        if (topicQrels == null && !ignoreMissing)
            return null;

        // Return what has been judged
        ArrayList<ScoredDocument> list = new ArrayList<>();
        final int top = min(retrieved.size(), topK);

        for (int i = 0; i < top; i++) {
            String docid = factory.getDocNo(retrieved.get(i));
            float rel = topicQrels == null ? 0 : convert(topicQrels.get(docid));
            list.add(new ScoredDocument(retrieved.get(i), rel));
        }

        return list;
    }

    static final private float convert(Integer rel) {
        if (rel == null)
            return 0f;
        return rel > 0 ? 1f : 0f;
    }
}
