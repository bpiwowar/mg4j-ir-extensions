package net.bpiwowar.mg4j.extensions.adhoc;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.handlers.ClassChooser;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.index.NullTermProcessor;
import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.query.Query;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.query.parser.SimpleParser;
import it.unimi.di.big.mg4j.search.DocumentIteratorBuilderVisitor;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.di.big.mg4j.search.score.Scorer;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.*;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.utils.timer.TaskTimer;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * A wrapper for all the MG4j models
 */
public abstract class MG4JScorer implements RetrievalModel {
    final static Logger logger = Logger.getLogger(MG4JScorer.class);

    @Argument(name = "term-processor", help = "The file that contains the term processor description")
    TermProcessor processor = NullTermProcessor.getInstance();

    @Argument(name = "rf", prefix = "pseudo-rf", help = "How to handle pseudo-relevance feedback", handler = ClassChooser.class)
    PseudoRF pseudoRF;

    abstract Scorer getScorer();

    /**
     * The MG4J topic. Made public to be reusable later. Invoke process()
     * before using this!
     */
    public String mg4jTopic;

    transient private IndexedField index;

    private QueryEngine queryEngine;

    @Override
    public void init(DocumentCollection collection, IndexedField index) throws Exception {
        this.index = index;

        final Object2ReferenceLinkedOpenHashMap<String, Index> indexMap = new Object2ReferenceLinkedOpenHashMap<>(
                Hash.DEFAULT_INITIAL_SIZE, .5f);
        indexMap.put(index.field, index.index);
        final Reference2DoubleOpenHashMap<Index> index2Weight = new Reference2DoubleOpenHashMap<>();
        index2Weight.put(index.index, 1.);

        final SimpleParser queryParser = new SimpleParser(indexMap.keySet(),
                indexMap.firstKey(), null);


        final Reference2ReferenceMap<Index, Object> index2Parser = new Reference2ReferenceOpenHashMap<>();

        queryEngine = new QueryEngine(queryParser, new DocumentIteratorBuilderVisitor(indexMap,
                index2Parser, indexMap.get(indexMap.firstKey()),
                Query.MAX_STEMMING), indexMap);

        queryEngine.score(new Scorer[]{getScorer()}, new double[]{1});

        queryEngine.multiplex = true;
        queryEngine.setWeights(index2Weight);

    }


    @Override
    public void close() {
    }

    @Override
    public void process(String topicId, String topic, int capacity, TaskTimer timer,
                        ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results,
                        LongSet onlyDocuments) throws Exception {
        logger.info(String.format("Topic %s", mg4jTopic));
        queryEngine.process(topic, 0, pseudoRF == null ? capacity : pseudoRF.k, results, onlyDocuments);

        // --- Handling relevance feedback
        if (pseudoRF != null) {
            throw new NotImplementedException();
//            for (CharSequence term : pseudoRF.process(topic))
//                update(terms, term.toString());
//
//            mg4jTopic = Output.toString(" | ", terms.entrySet(),
//                    weightedWordFormatter);
//            logger.info(String.format("After blind RF, topic is %s", mg4jTopic));
//
//            topic.clear();
//            queryEngine.process(mg4jTopic, 0, capacity, topic);
        }
    }



    // --- Get sets of terms

    static public void update(Map<String, MutableInt> terms, String word) {
        MutableInt v = terms.get(word);
        if (v == null)
            v = terms.put(word, new MutableInt(1));
        else
            v.add(1);


    }

}
