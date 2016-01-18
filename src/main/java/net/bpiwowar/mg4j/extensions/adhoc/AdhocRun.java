package net.bpiwowar.mg4j.extensions.adhoc;

import bpiwowar.argparser.GenericHelper;
import bpiwowar.argparser.utils.ReadLineIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.bpiwowar.mg4j.extensions.trec.IdentifiableCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses a TREC run
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class AdhocRun {
    final static private Logger LOGGER = LoggerFactory.getLogger(AdhocRun.class);

    public Map<String, List<ScoreInfo>> runs() {
        return runs;
    }


    public enum RunType {
        TREC
    }

    static public class ScoreInfo {
        public String docno;
        public final long iter;
        int rank;
        double score;
        public String runId;

        public ScoreInfo(String docno, long iter, int rank, double score, String runId) {
            this.docno = docno;
            this.iter = iter;
            this.rank = rank;
            this.score = score;
            this.runId = runId;
        }
    }

    /**
     * Results per topic
     * <p>
     * topic -> list)
     */
    Map<String, List<ScoreInfo>> runs = GenericHelper.newTreeMap();

    private AdhocRun() {
    }

    static public AdhocRun loadTREC(Reader in) {
        AdhocRun run = new AdhocRun();
        for (String line : new ReadLineIterator(in)) {
            String[] fields = line.split("\\s+");
            if (fields.length != Fields.values().length)
                throw new RuntimeException(String.format(
                        "Non TREC qrels format for line: %s", line));

            String qid = fields[Fields.QID.ordinal()];
            String docno = fields[Fields.DOCNO.ordinal()];
            int rank = Integer.parseInt(fields[Fields.RANK.ordinal()]);
            long iter = Long.parseLong(fields[Fields.ITER.ordinal()]);
            double score = Double.parseDouble(fields[Fields.SIM.ordinal()]);
            String runId = fields[Fields.RUN_ID.ordinal()];

            final ScoreInfo scoreInfo = new ScoreInfo(docno, iter, rank, score, runId);
            List<ScoreInfo> list = run.runs.get(qid);
            if (list == null) {
                run.runs.put(qid, list = new ArrayList<>());
            }
            list.add(scoreInfo);
        }

        return run;
    }


    static public AdhocRun load(Reader in, RunType type) {
        switch (type) {
            case TREC:
                return loadTREC(in);
        }
        throw new AssertionError("Unknown type: " + type);
    }

    public static AdhocRun load(File file, RunType type) throws FileNotFoundException {
        return load(new FileReader(file), type);
    }

    public static AdhocRun load(String filename, RunType type) throws FileNotFoundException {
        return load(new FileReader(new File(filename)), type);
    }


    public void save(File outFile, RunType type) throws IOException {
        try (final PrintStream stream = new PrintStream(outFile)) {
            save(stream, type);
        }
    }

    public void save(PrintStream out, RunType type) throws IOException {
        switch (type) {
            case TREC:
                saveTREC(out);
                break;
            default:
                throw new AssertionError("Unknown type: " + type);
        }
    }

    private void saveTREC(PrintStream out) {
        runs.forEach((qid, list) -> {
            list.forEach(si -> {
                out.format("%s %d %s %d %g %s",
                        qid, "Q0", si.docno, si.rank, si.score, si.runId
                        );
            });
        });
    }

    public enum Fields {
        QID, ITER, DOCNO, RANK, SIM, RUN_ID
    }


    /**
     * @param collection
     * @return
     * @throws IOException
     */
    public Object2ObjectLinkedOpenHashMap<String, LongSet> retrieveDocno(IdentifiableCollection collection) throws IOException {
        Object2ObjectLinkedOpenHashMap<String, LongSet> run = new Object2ObjectLinkedOpenHashMap<>();

        for (Map.Entry<String, List<AdhocRun.ScoreInfo>> entry : runs().entrySet()) {
            final String qid = entry.getKey();
            LongSet set = new LongOpenHashSet();

            for (AdhocRun.ScoreInfo info : entry.getValue()) {
                final long docid = collection.getDocumentFromURI(info.docno);

                set.add(docid);
            }
            run.put(qid, set);
        }
        return run;
    }

}
