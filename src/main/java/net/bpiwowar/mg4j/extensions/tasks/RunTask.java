package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentClass;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import net.bpiwowar.mg4j.extensions.utils.LazyString;
import net.bpiwowar.mg4j.extensions.utils.timer.TaskTimer;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

@TaskDescription(name = "run-task", project = { "ir", "renaissance", "iqir" },
        description = "Runs a task described by an XML document and an (optional) javascript specification")
public class RunTask extends AbstractTask {
	final static Logger LOGGER = Logger.getLogger(RunTask.class);

    @ArgumentClass(help = "The timer configuration")
    TaskTimer timer = new TaskTimer(LOGGER);

    @Argument(name = "task", checkers = IOChecker.Readable.class, help = "XML configuration file from ir.collections")
    File configuration;

    @Argument(name = "js-file", checkers = IOChecker.Readable.class,
            help = "JavaScript file specification for task handling")
    File jsFile;

    @Argument(name = "js", help = "JavaScript string specification for task handling")
    String jsString;

//    @ArgumentClass(prefix = "index")
//	Index index;
//    @ArgumentClass(prefix = "collection")
//    DocumentCollection collection;

	@Override
	public int execute() throws Throwable {

        // --- Configure with javascript

        // create a script engine manager
        ScriptEngineManager jsFactory = new ScriptEngineManager();

        // create a JavaScript engine
        ScriptEngine engine = jsFactory.getEngineByName("JavaScript");

        try {
        // Evaluate the default javascript
        final URL defaultJS = this.getClass().getResource("/META-INF/run-task.js");
        LOGGER.info(LazyString.format("Reading default javascript [%s]", defaultJS));
        engine.eval(new InputStreamReader(this.getClass().getResourceAsStream("/META-INF/run-task.js")));

        // Evaluate the file
        if (jsFile != null)
            engine.eval(new java.io.FileReader(jsFile));

        // Evaluate the string
        if (jsString != null)
            engine.eval(jsString);
        } catch(ScriptException e) {
            LOGGER.error(e.getMessage());
            return 1;
        }

        // --- Read the XML task definition
        LOGGER.info("Reading the IR collections file");
        final InputStream stream = configuration != null ? new FileInputStream(configuration) : System.in;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(stream);


        //




//		collection.init();
//		index.init();
//
//		// Dicarded documents
//		TRECJudgments discarded = discardedQRELFile == null ? null
//				: new TRECJudgments(discardedQRELFile);
//
//		// Queries
//		Set<String> topicIds = GenericHelper.newHashSet();
//		Map<String, ? extends Topic> topics = querySet.queries();
//		for (String id : topics.keySet()) {
//			logger.debug("Considering topic %s (%b/%b/%b)", id, topics.keySet()
//					.contains(id), onlyTopics.isEmpty(), onlyTopics
//					.contains(id));
//			if (topics.keySet().contains(id)
//					&& (onlyTopics.isEmpty() || onlyTopics.contains(id))) {
//				topicIds.add(id);
//			}
//		}
//
//		if (topicIds.isEmpty()) {
//			logger.error("No topics to be answered");
//			return 1;
//		}
//
//		// Iterates on topics
//		timer.start();
//		Task task = timer.new Task("Answering topics", "topics",
//				topicIds.size());
//		PrintStream output = System.out;
//
//		model.init(collection, index);
//		int totalRetrieved = 0;
//		for (String topicId : topicIds) {
//			logger.info("Answering topic %s", topicId);
//
//			Topic topic = topics.get(topicId);
//			ObjectArrayList<DocumentScoreInfo> results = new ObjectArrayList<DocumentScoreInfo>();
//
//			Set<String> discardedDocuments = null;
//			if (discarded != null) {
//				Map<String, Integer> map = discarded.get(topicId);
//				if (map != null)
//					discardedDocuments = map.keySet();
//
//			}
//
//			// Ask for results (add some documents in case we discard some
//			// after)
//			model.process(topic, results,
//					capacity
//							+ (discardedDocuments == null ? 0
//									: discardedDocuments.size()), timer);
//
//			final int retrieved = results.size();
//			totalRetrieved += retrieved;
//			logger.info("Returned %d results", retrieved);
//			int added = 0;
//			for (int i = 0; i < retrieved && added < capacity; i++) {
//				DocumentScoreInfo dsi = results.get(i);
//				Document document = collection.document(dsi.getDocumentId());
//				final String docno = (String) collection.getMetadata(dsi.getDocumentId(), Metadata.DOCID);
//
//				// If it was not a discarded document
//				if (discardedDocuments == null
//						|| !discardedDocuments.contains(docno)) {
//					output.format("%s Q0 %s %d %g %s%n", topicId, docno, i,
//							dsi.getScore(), runId);
//					added++;
//				}
//				document.close();
//			}
//			task.progress();
//		}
		return 0;
	}
}
