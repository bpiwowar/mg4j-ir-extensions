package net.bpiwowar.mg4j.extensions.adhoc;

import bpiwowar.argparser.GenericHelper;
import bpiwowar.argparser.utils.ReadLineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A set of TREC judgments as contained in one TREC file
 * <p/>
 * Format is supposed to be: <div><code>qid  iter  docno  rel</code></div>
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TRECJudgments extends Judgments {
    final static private Logger LOGGER = LoggerFactory.getLogger(TRECJudgments.class);

    public TRECJudgments(Reader in) {
        for (String line : new ReadLineIterator(in)) {
            String[] fields = line.split("\\s+");
            if (fields.length != Fields.values().length)
                throw new RuntimeException(String.format(
                        "Non TREC qrels format for line: %s", line));

            String qid = fields[Fields.QID.ordinal()];
            String docno = fields[Fields.DOCNO.ordinal()];
            Integer rel = new Integer(fields[Fields.REL.ordinal()]);

            Map<String, Integer> map = judgments.get(qid);
            if (map == null)
                judgments.put(qid, map = GenericHelper.newTreeMap());

            Integer put = map.put(docno, rel);
            if (put != null)
                LOGGER.warn("Duplicate judgment for document {} and topic {}", docno, qid);
        }
    }

    public TRECJudgments(File file) throws FileNotFoundException {
        this(new FileReader(file));
    }


    public void write(File file) throws IOException {
        PrintStream out = new PrintStream(file);
        for (Entry<String, Map<String, Integer>> rels : judgments.entrySet())
            for (Entry<String, Integer> x : rels.getValue().entrySet()) {
                writeQrel(out, rels.getKey(), x.getKey(), x.getValue());
            }
        out.close();
    }
    static final public void writeQrel(PrintStream out, String qid,
                                       String docno, int rel) {
        out.format("%s 0 %s %d%n", qid, docno, rel);
    }
}
