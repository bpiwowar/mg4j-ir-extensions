package net.bpiwowar.mg4j.extensions.rf;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Relevance Feedback handling with MG4J (translates back and forth between
 * internal and external ID)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class MG4JRelevanceFeedback {

    /**
     * A document contains both the docno (external) & docid (internal)
     * information. The docno may be the TREC id, while the docid is the
     * internal MG4J id of the document.
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    static public class MG4JDocument implements Document {
        public long docid = -1;
        public String docno;

        /**
         * @param docid the internal MG4J doc ID
         * @param docno the external doc number (e.g. TREC id)
         */
        public MG4JDocument(long docid, String docno) {
            this.docid = docid;
            this.docno = docno;
        }
    }

    /**
     * Get relevance feedback using MG4J. This methods turns the output of a
     * scorer into a collection of documents returned after relevance feedback.
     * It supports different relevance feedback strategies.
     *
     * @param collection the document collection
     * @param method     the relevance feedback strategy
     * @param topicId    the topic ID
     * @param results    the results as returned by the base scorer
     * @return the documents selected applying the relevance feedback
     * strategy
     */
    static final public Collection<ScoredDocument> get(
            final DocumentCollection collection,
            RelevanceFeedbackMethod method,
            final String topicId,
            final ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results) {
        // Our factory
        MG4JFactory factory = new MG4JFactory(collection);

        // Adaptator for the list of retrieved documents
        final AbstractList<MG4JDocument> retrieved = new AbstractList<MG4JDocument>() {
            @Override
            public MG4JDocument get(int index) {
                final DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>> dsi = results
                        .get(index);
                return new MG4JDocument(dsi.document, null);
            }

            @Override
            public int size() {
                return results.size();
            }
        };

        Collection<ScoredDocument> list = method.process(topicId,
                retrieved, factory);

        Collection<ScoredDocument> feedback = new ArrayList<>(list.size());
        for (ScoredDocument x : list) {
            feedback.add(x);
        }
        return feedback;
    }
}
