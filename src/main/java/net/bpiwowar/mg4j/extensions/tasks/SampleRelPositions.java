package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.document.AbstractDocumentCollection;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.index.IndexIterator;
import it.unimi.di.big.mg4j.search.DocumentIterator;
import it.unimi.dsi.fastutil.ints.IntBigList;
import net.bpiwowar.experimaestro.tasks.AbstractTask;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.adhoc.Judgments;
import net.bpiwowar.mg4j.extensions.conf.IndexedCollection;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.query.QuerySet;
import net.bpiwowar.mg4j.extensions.query.Tokenizer;
import net.bpiwowar.mg4j.extensions.query.Topic;
import net.bpiwowar.mg4j.extensions.query.TopicProcessor;
import net.bpiwowar.mg4j.extensions.query.Topics;
import net.bpiwowar.mg4j.extensions.tasks.SamplePositions.Source;
import net.bpiwowar.mg4j.extensions.trec.IdentifiableCollection;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import net.bpiwowar.mg4j.extensions.utils.TextToolChain;
import org.apache.commons.lang.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * Sample term positions
 */
@TaskDescription(id = "mg4j:document-positions", output = "mg4j:positions-stream",
        description = "Outputs a tab-separated value stream with" +
                "<pre>QID TID DID DOC_LENGHT POSITION_1 POSITION_2 ...</pre>",
        registry = Registry.class)
public class SampleRelPositions extends AbstractTask {
    public static final Logger LOGGER = LoggerFactory.getLogger(SampleRelPositions.class);

    @JsonArgument(required = true)
    IndexedCollection index;

    @JsonArgument(name = "fields", help = "The fields to get the positions from", required = true)
    Set<String> fieldNames = new HashSet<>();

    @JsonArgument(name = "toolchain", required = true)
    TextToolChain toolchain;

    @JsonArgument(required = true)
    Topics topics;

    @JsonArgument(required = true)
    TopicProcessor topic_processor;

    @JsonArgument(required = true)
    Judgments.MG4JAssessments qrels;

    public JsonObject execute(JsonObject json) throws Exception {
        if (fieldNames.isEmpty()) {
            throw new RuntimeException("At least one field name should be given");
        }

        // Get all the indices
        final Source[] sources = new Source[fieldNames.size()];

        int i = 0;
        for (String fieldName : fieldNames) {
            IndexedField _index = index.get(fieldName);
            sources[i] = new Source(_index, 1., _index.getReader());
            if (!_index.index.hasPositions) {
                throw new RuntimeException("No positions for index " + fieldName + "!");
            }
            ++i;
        }

        double totalWeight = sources[sources.length - 1].weight;
        for (Source source : sources) {
            source.weight /= totalWeight;
        }

        // Get the queries
        QuerySet querySet = topics.getQuerySet();
        for (Topic topic : querySet.queries().values()) {
            final Set<String> terms = topic_processor.getPositiveTerms(new Tokenizer(toolchain.wordReader), toolchain.termProcessor, null, topic).keySet();

        }

        // Get the qrels
        final Judgments judgments = qrels.get();


        long docid;
        int position;
        final PrintStream out = System.out;

        // Finish

        final DocumentCollection collection = index.getCollection().get();
        IdentifiableCollection uriCollection = (IdentifiableCollection) collection;

//        while (!stop) {
//            // Choose index and term
//            final double v = random.nextDouble();
//            int ix = 0;
//            for (; ix < sources.length; ++ix) {
//                if (sources[ix].weight >= v) {
//                    break;
//                }
//            }
//            assert ix < sources.length;
//
//            final Source source = sources[ix];
//            long termId = (long) (random.nextFloat() * source.indexedField.index.numberOfTerms);
//            final IntBigList sizes = source.indexedField.index.sizes;
//
//            // Outputs
//            final IndexIterator documents = source.indexReader.documents(termId);
//
//            String prefix = String.format("%s\t%d\t", source.indexedField.getTerm(termId), termId);
//            final double samplingRate = maxdocuments / (double) documents.frequency();
//
//            while ((docid = documents.nextDocument()) != DocumentIterator.END_OF_LIST) {
//                if (samplingRate >= 1. || random.nextDouble() < samplingRate) {
//                    out.print(prefix);
//                    out.print(docid);
//                    out.print('\t');
//                    out.print(sizes.get(docid));
//                    while ((position = documents.nextPosition()) != IndexIterator.END_OF_POSITIONS) {
//                        out.print('\t');
//                        out.print(position);
//                    }
//                    out.println();
//                }
//            }
//        }

        LOGGER.info("Finished outputing samples");
        return null;
    }
}
