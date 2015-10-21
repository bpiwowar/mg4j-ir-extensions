package net.bpiwowar.mg4j.extensions.adhoc;

import bpiwowar.argparser.GenericHelper;
import bpiwowar.argparser.utils.ReadLineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses a TREC run
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TRECRun {
    final static private Logger LOGGER = LoggerFactory.getLogger(TRECRun.class);

    public Map<String, List<ScoreInfo>> runs() {
        return runs;
    }

    static public class ScoreInfo {
        public String docno;
        public final long iter;
        int rank;
        double score;

        public ScoreInfo(String docno, long iter, int rank, double score) {
            this.docno = docno;
            this.iter = iter;
            this.rank = rank;
            this.score = score;
        }
    }
    /**
     * Results per topic
     *
     * topic -> list)
     */
    Map<String, List<ScoreInfo>> runs = GenericHelper.newTreeMap();


    public TRECRun(Reader in) {
        for (String line : new ReadLineIterator(in)) {
            String[] fields = line.split("\\s+");
            if (fields.length != Fields.values().length)
                throw new RuntimeException(String.format(
                        "Non TREC qrels format for line: %s", line));

            String qid = fields[Fields.QID.ordinal()];
            String docno = fields[Fields.DOCNO.ordinal()];
            int rank = Integer.parseInt(fields[Fields.RANK.ordinal()]);
            long iter= Long.parseLong(fields[Fields.ITER.ordinal()]);
            double score  = Double.parseDouble(fields[Fields.SIM.ordinal()]);

            final ScoreInfo scoreInfo = new ScoreInfo(docno, iter, rank, score);
            List<ScoreInfo> list = runs.get(qid);
            if (list == null) {
                runs.put(qid, list = new ArrayList<>());
            }
            list.add(scoreInfo);
        }
    }

    public TRECRun(File file) throws FileNotFoundException {
        this(new FileReader(file));
    }

    public TRECRun(String filename) throws FileNotFoundException {
        this(new File(filename));
    }

    public enum Fields {
        QID, ITER, DOCNO, RANK, SIM, RUN_ID
    }


}
