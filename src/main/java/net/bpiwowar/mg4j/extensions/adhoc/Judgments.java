package net.bpiwowar.mg4j.extensions.adhoc;

import bpiwowar.argparser.GenericHelper;
import sf.net.experimaestro.tasks.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Set;

/**
 * Relevance assessments between topics and documents
 */
public abstract class Judgments {
    /**
     * our judgments topic -> (document -> judgment)
     */
    Map<String, Map<String, Integer>> judgments = GenericHelper.newTreeMap();

    /**
     * Get a set of topics (or null)
     *
     * @param qid The query ID
     * @return
     */
    public Map<String, Integer> get(String qid) {
        return judgments.get(qid);
    }

    public enum Fields {
        QID, ITER, DOCNO, REL
    }

    public Set<String> getTopics() {
        return judgments.keySet();
    }

    @Type(type = "mg4j:qrels")
    static public class MG4JAssessments {
        File path;
        String format;
        String id;

        public Judgments get() throws FileNotFoundException {
            switch(format) {
                case "TREC":
                    return new TRECJudgments(path);
                default:
                    throw new IllegalArgumentException("Cannot handle QRELS with format " + format);
            }
        }
    };

}
