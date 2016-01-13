package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.GenericHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.bpiwowar.xpm.manager.tasks.AbstractTask;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.ProgressListener;
import net.bpiwowar.xpm.manager.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.adhoc.RetrievalModel;
import net.bpiwowar.mg4j.extensions.adhoc.Run;
import net.bpiwowar.mg4j.extensions.adhoc.TRECJudgments;
import net.bpiwowar.mg4j.extensions.adhoc.TRECRun;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.query.QuerySet;
import net.bpiwowar.mg4j.extensions.query.Tokenizer;
import net.bpiwowar.mg4j.extensions.query.Topic;
import net.bpiwowar.mg4j.extensions.query.TopicProcessor;
import net.bpiwowar.mg4j.extensions.query.Topics;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import net.bpiwowar.mg4j.extensions.utils.timer.TaskTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.bpiwowar.xpm.manager.tasks.Path;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

@TaskDescription(id = "mg4j:adhoc",
        output = "irc:run",
        description = "Runs an ad-hoc task",
        registry = Registry.class)
public class Adhoc extends AbstractTask {
    final private static Logger LOGGER = LoggerFactory.getLogger(Adhoc.class);

    @JsonArgument(help = "The timer configuration")
    TaskTimer $timer = new TaskTimer(LOGGER);

    @JsonArgument(name = "top_k")
    int capacity = 1500;

    @JsonArgument()
    IndexedCollection index;

    @JsonArgument
    RetrievalModel model;

    @JsonArgument()
    String $run_id = null;

    @JsonArgument(required = true)
    Topics topics;

    @JsonArgument(name = "only_topics", help = "Restrict topics to this list (comma separated)")
    Set<String> onlyTopics = new HashSet<>();

    @JsonArgument(name = "field", help = "Field to use (by default \"text\")")
    public String field = "text";

    @JsonArgument()
    TopicProcessor topic_processor;

    @JsonArgument(name = "baserun", help = "Use a base run to re-rank results instead of looking at all documents")
    Run baseRun;

    @Path(copy = "path")
    File run;

    @Override
    public JsonElement execute(JsonObject r, ProgressListener progress) throws Throwable {
        // Get collection + index
        DocumentCollection collection = index.getCollection().get();
        IndexedField _index = index.get(field);

        if ($run_id == null) {
            $run_id = model.toString();
        }

        // Dicarded documents (not used)
        File discardedQRELFile = null;
        TRECJudgments discarded = discardedQRELFile == null ? null : new TRECJudgments(discardedQRELFile);

        QuerySet querySet = topics.getQuerySet();

        // Do something
        Set<String> topicIds = GenericHelper.newHashSet();
        Map<String, ? extends Topic> topics = querySet.queries();
        for (String id : topics.keySet()) {
            LOGGER.debug("Considering topic %s (%b/%b/%b)", id, topics.keySet()
                    .contains(id), onlyTopics.isEmpty(), onlyTopics
                    .contains(id));
            if (topics.keySet().contains(id)
                    && (onlyTopics.isEmpty() || onlyTopics.contains(id))) {
                topicIds.add(id);
            }
        }

        if (topicIds.isEmpty()) {
            throw new RuntimeException("No topics to be answered");
        }


        // Handles base run
        Object2ObjectLinkedOpenHashMap<String, LongSet> baseRunMap = null;
        if (baseRun != null) {
            baseRunMap = new Object2ObjectLinkedOpenHashMap<>();
            TRECRun trecRun = new TRECRun(baseRun.path);
            for (Map.Entry<String, List<TRECRun.ScoreInfo>> entry : trecRun.runs().entrySet()) {
                final String qid = entry.getKey();
                LongSet set = new LongOpenHashSet();

                for (TRECRun.ScoreInfo info : entry.getValue()) {
                    // FIXME: HACK HACK HACK (but protected)
                    final long docid = info.iter;
                    final String docno = (String) collection.metadata(docid).get(PropertyBasedDocumentFactory.MetadataKeys.URI);
                    if (!docno.equals(info.docno)) {
                        throw new AssertionError(format("Docnos don't match (mg4j=%s vs run=%s) for document ID %d", docno, info.docno, docid));
                    }
                    set.add(docid);
                }
                baseRunMap.put(qid, set);
            }

        }

        // Use default token
        final Tokenizer tokenizer = new Tokenizer(index.getTextToolchain().wordReader);

        // Iterates on topics
        $timer.start();
        try (PrintStream output = new PrintStream(new FileOutputStream(run))) {
            TaskTimer.Task task = $timer.new Task("Answering topics", "topics", topicIds.size());

            model.init(collection, _index);

            // Loop over topics
            for (String topicId : topicIds) {
                LOGGER.info(format("Answering topic %s", topicId));

                final LongSet documentRestriction = baseRunMap != null ? baseRunMap.get(topicId) : null;

                Topic topic = topics.get(topicId);
                ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results = new ObjectArrayList<>();

                Set<String> discardedDocuments = null;
                if (discarded != null) {
                    Map<String, Integer> map = discarded.get(topicId);
                    if (map != null)
                        discardedDocuments = map.keySet();

                }

                // Ask for results (add some documents in case we discard some
                // after)
                model.init(collection, _index);
                model.process(topicId,
                        topic_processor.process(tokenizer, index.getTermProcessor(), _index, topic),
                        capacity + (discardedDocuments == null ? 0 : discardedDocuments.size()),
                        $timer, results, documentRestriction
                );

                final int retrieved = results.size();
                LOGGER.info(format("Returned %d results", retrieved));
                int added = 0;
                for (int i = 0; i < retrieved && added < capacity; i++) {
                    DocumentScoreInfo dsi = results.get(i);
//                Document document = collection.document(dsi.document);
//                System.err.println("URI: " + document.uri());
//                System.err.println("URI[" + dsi.document + "]: " + collection.metadata(dsi.document).get(PropertyBasedDocumentFactory.MetadataKeys.URI));
                    final String docno = (String) collection.metadata(dsi.document).get(PropertyBasedDocumentFactory.MetadataKeys.URI);
                    // If it was not a discarded document
                    if (discardedDocuments == null
                            || !discardedDocuments.contains(docno)) {
                        // Use second field as document ID
                        output.format("%s %d %s %d %g %s%n", topicId, dsi.document, docno, i,
                                dsi.score, $run_id);
                        added++;
                    }
//                document.close();
                }
                task.progress();
            }

            // Write document map
            LOGGER.info("Finished");
        } finally {
            $timer.close();
        }
        return JsonNull.INSTANCE;
    }

}
