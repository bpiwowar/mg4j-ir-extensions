package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentClass;
import bpiwowar.argparser.GenericHelper;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import it.unimi.di.big.mg4j.document.Document;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.bpiwowar.mg4j.extensions.adhoc.BM25;
import net.bpiwowar.mg4j.extensions.adhoc.RetrievalModel;
import net.bpiwowar.mg4j.extensions.adhoc.TRECJudgments;
import net.bpiwowar.mg4j.extensions.adhoc.TRECTopic;
import net.bpiwowar.mg4j.extensions.conf.DocumentCollectionConfiguration;
import net.bpiwowar.mg4j.extensions.conf.IndexConfiguration;
import net.bpiwowar.mg4j.extensions.query.QuerySet;
import net.bpiwowar.mg4j.extensions.query.Topic;
import net.bpiwowar.mg4j.extensions.utils.LazyString;
import net.bpiwowar.mg4j.extensions.utils.XMLUtils;
import net.bpiwowar.mg4j.extensions.utils.timer.TaskTimer;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@TaskDescription(name = "adhoc", project = { "ir", "renaissance", "iqir" },
        description = "Runs an ad-hoc task")
public class Adhoc extends AbstractTask {
	final static Logger logger = Logger.getLogger(Adhoc.class);

    @ArgumentClass(help = "The timer configuration")
    TaskTimer timer = new TaskTimer(logger);


    @Argument(name = "task")
    File taskFile;

    @Argument(name = "run-id")
    String runId = "null";

    @Argument(name = "top-k")
    int capacity = 1500;

    @ArgumentClass(prefix = "index")
    IndexConfiguration index;

    @ArgumentClass(prefix = "collection")
    DocumentCollectionConfiguration collectionCf;

    @Argument(name = "topics", help = "Restrict topics to this list (comma separated)")
    Set<String> onlyTopics = new HashSet<>();

    File discardedQRELFile = null;

    public static final String MG4J_NAMESPACE = "net.bpiwowar.mg4j-ir-extensions";
    private static final QName ADHOC_MODEL = new QName(MG4J_NAMESPACE, "adhoc.model");

    public static final String IRC_NAMESPACE = "http://ircollections.sourceforge.net";
    private static final QName TOPICS = new QName(IRC_NAMESPACE, "topics");

    @XmlType
    static public class Configuration {

    }

	@Override
	public int execute() throws Throwable {
        final DocumentCollection collection = collectionCf.init();
        index.init();

        // Read model & topics
        logger.info("Reading model");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        org.w3c.dom.Document xml = taskFile == null  ? dBuilder.parse(System.in) : dBuilder.parse(new FileInputStream(taskFile));

        JAXBContext context = JAXBContext.newInstance(BM25.class);
        Unmarshaller um = context.createUnmarshaller();

        RetrievalModel model = null;
        QuerySet querySet = null;

        for(Element child: XMLUtils.elements(xml.getDocumentElement().getChildNodes())) {
            if (XMLUtils.is(child, ADHOC_MODEL)) {
                for(Element grandchild: XMLUtils.elements(child.getChildNodes())) {
                    model = (RetrievalModel) um.unmarshal(grandchild);
                    break;
                }
            }
            if (XMLUtils.is(child, TOPICS)) {
                final String type = child.getAttribute("type");
                switch(type) {
                    case "trec":
                        try(BufferedReader reader = new BufferedReader(new FileReader(child.getAttribute("path")))) {
                            querySet = TRECTopic.readTopics(reader, false);
                        }
                        break;
                    default:
                        throw new RuntimeException(String.format("Cannot handle topics of type %s", type));
                }
                // Do something
            }
        }

        if (model == null)
            throw new IllegalArgumentException("No model was present in the XML description file");
        if (querySet == null)
            throw new IllegalArgumentException("No topics were present in the XML description file");

        logger.info(String.format("Starting with model [%s] and %d topics", model, querySet.queries().size()));

        // Dicarded documents
		TRECJudgments discarded = discardedQRELFile == null ? null
				: new TRECJudgments(discardedQRELFile);



        // Queries
		Set<String> topicIds = GenericHelper.newHashSet();
		Map<String, ? extends Topic> topics = querySet.queries();
		for (String id : topics.keySet()) {
			logger.debug(new LazyString("Considering topic %s (%b/%b/%b)", id, topics.keySet()
					.contains(id), onlyTopics.isEmpty(), onlyTopics
					.contains(id)));
			if (topics.keySet().contains(id)
					&& (onlyTopics.isEmpty() || onlyTopics.contains(id))) {
				topicIds.add(id);
			}
		}

		if (topicIds.isEmpty()) {
			logger.error("No topics to be answered");
			return 1;
		}

		// Iterates on topics
		timer.start();
		TaskTimer.Task task = timer.new Task("Answering topics", "topics",
				topicIds.size());
		PrintStream output = System.out;

		model.init(collection, index);
		int totalRetrieved = 0;
		for (String topicId : topicIds) {
			logger.info(String.format("Answering topic %s", topicId));

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
			model.process(topic, results,
					capacity
							+ (discardedDocuments == null ? 0
									: discardedDocuments.size()), timer);

			final int retrieved = results.size();
			totalRetrieved += retrieved;
			logger.info(String.format("Returned %d results", retrieved));
			int added = 0;
			for (int i = 0; i < retrieved && added < capacity; i++) {
				DocumentScoreInfo dsi = results.get(i);
				Document document = collection.document(dsi.document);
                System.err.println("URI: " + document.uri());
                System.err.println("URI["+dsi.document+"]: " + collection.metadata(dsi.document).get(PropertyBasedDocumentFactory.MetadataKeys.URI));
				final String docno = (String) collection.metadata(dsi.document).get(PropertyBasedDocumentFactory.MetadataKeys.URI);

				// If it was not a discarded document
				if (discardedDocuments == null
						|| !discardedDocuments.contains(docno)) {
					output.format("%s Q0 %s %d %g %s%n", topicId, docno, i,
							dsi.score, runId);
					added++;
				}
				document.close();
			}
			task.progress();
		}
		return 0;
	}
}
