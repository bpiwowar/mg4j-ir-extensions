package net.bpiwowar.mg4j.extensions.adhoc;

import bpiwowar.argparser.GenericHelper;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.JsonType;

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

    @JsonType(type = "irc:qrels")
    static public class MG4JAssessments {
        @JsonArgument
        File path;
        @JsonArgument
        String format;
        @JsonArgument
        String id;

        public Judgments get() throws FileNotFoundException {
            switch(format) {
                case "trec":
                    return new TRECJudgments(path);
                case "trec.diversity":
                    return new TRECJudgments(path, true);
                default:
                    throw new IllegalArgumentException("Cannot handle QRELS with format " + format);
            }
        }
    };

}
