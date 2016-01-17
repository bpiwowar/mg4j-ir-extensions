package net.bpiwowar.mg4j.extensions.adhoc;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.unimi.di.big.mg4j.document.Document;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;
import net.bpiwowar.mg4j.extensions.query.Query;
import net.bpiwowar.mg4j.extensions.rf.DocumentFactory;
import net.bpiwowar.mg4j.extensions.rf.MG4JFactory;
import net.bpiwowar.mg4j.extensions.rf.MG4JRelevanceFeedback;
import net.bpiwowar.mg4j.extensions.rf.RelevanceFeedbackMethod;
import net.bpiwowar.mg4j.extensions.utils.timer.TaskTimer;
import net.bpiwowar.xpm.manager.tasks.ClassChooserInstance;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the relevance model
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 15/10/12
 */
@ClassChooserInstance(name = "RM", description = "Relevance model from Lavrenko")
public class RelevanceModel implements RetrievalModel {

    public enum Method {
        IID,
        CONDITIONAL
    }

    @JsonArgument
    double lambda;

    @JsonArgument
    Method method;

    @JsonArgument
    RetrievalModel baseModel;

    @JsonArgument
    RelevanceFeedbackMethod relevanceFeedbackMethod;

    transient private DocumentFactory factory;
    transient private DocumentCollection collection;
    transient private IndexedField index;
    transient private LongBigList frequencies;

    @Override
    public String toString() {
        return String.format("RelevanceModel(rf=%s,base=%s)", relevanceFeedbackMethod, baseModel);
    }

    @Override
    public void init(DocumentCollection collection, IndexedField index) throws Exception {
        baseModel.init(collection, index);
        relevanceFeedbackMethod.init();
        factory = new MG4JFactory(collection);
        this.collection = collection;

        this.index = index;
        this.frequencies = index.getTermFrequency();

    }

    @Override
    public void close() {
    }

    /**
     * Holds the probabilities
     */
    public class Result {
        /**
         * Ratio p(w|R) / p(w|N)
         */
        final Long2DoubleLinkedOpenHashMap p_ratio;

        /**
         * Creates the object from probabilities computed up to an unknown scaling constant K
         *
         * @param alpha holds K p(w, q) / p(w|G)
         * @param c_w_q holds K p(w, q)
         */
        public Result(double alpha, Long2DoubleLinkedOpenHashMap c_w_q) {
            this.p_ratio = c_w_q;

            final long numberOfPostings = index.getNumberOfPostings();

            // Holds the sum of p(w, q) over w
            double sum = 0;

            // Holds  (1 - \sum_{w \not\in S} p(w|G)), i.e. the sum of probabilities
            // of unseen terms occurring in an "empty" document
            double x = 1;

            for (Long2DoubleMap.Entry e : p_ratio.long2DoubleEntrySet()) {
                final double v = e.getDoubleValue();
                final double p_w__G = (double) frequencies.getLong(e.getLongKey()) / numberOfPostings;

                sum += v;
                e.setValue(v / p_w__G);
                x -= p_w__G;
            }

            // Adds contribution of unseen terms
            assert x > 0;
            sum += x * alpha;

            // Normalize
            for (Long2DoubleMap.Entry e : p_ratio.long2DoubleEntrySet()) {
                e.setValue(e.getDoubleValue() / sum);
            }

            // Sets the unseen term ratio
            p_ratio.defaultReturnValue(alpha / sum);
        }

        /**
         * Returns p(w|R) / p(w|N)
         *
         * @param termId
         * @return
         */
        public double getTermPRatio(long termId) {
            return p_ratio.get(termId);
        }


    }

    @Override
    public void process(String topicId, String topic, int capacity, TaskTimer timer, ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results, LongSet onlyDocuments) throws Exception {
        // TODO: capacity should depend on the relevance feedback method
        baseModel.process(topicId, topic, capacity, timer, results, onlyDocuments);

        final Collection<MG4JRelevanceFeedback.MG4JDocument> feedback =
                relevanceFeedbackMethod.process(topicId, null /* FIXME: should not be null */, factory);

        // FIXME: should be a parameter
        int[] contents = {0};
        long unknown = index.getUnknownTermId();

        // --- Get the query terms
        if (1 == 1) throw new NotImplementedException("topic parts below");
        final Query query = null; //topic.getTopicPart(null);
        Set<String> queryTermStrings = new HashSet<>();
        LongArraySet queryTermSet = new LongArraySet();
        query.addTerms(queryTermStrings);
        for (String term : queryTermStrings) {
            final long termId = index.getTermId(term);
            if (termId != unknown) {
                queryTermSet.add(termId);
            }
        }

        long queryTerms[] = new long[queryTermSet.size()];
        queryTerms = queryTermSet.toArray(queryTerms);


        final Result result = null;
        switch (method) {
            case IID:
//                result = method1(feedback, topic, contents, queryTerms);
                break;
            case CONDITIONAL:
//                result = method2(feedback, topic, contents, queryTerms);
                break;
            default:
                throw new AssertionError();
        }


        // Now we can compute

        // Computes the final score of documents prod_w p(w|R) / p(w|N)
        for (DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>> dsi : results) {
            Multiset<Long> words = readDocument(contents, dsi.document);
            final int docLength = words.size();

            double p_rel = 1;
            for (Multiset.Entry<Long> e : words.entrySet()) {
                final long wordId = e.getElement().longValue();
                p_rel *= Math.exp(Math.log(result.getTermPRatio(wordId)) * (double) e.getCount());
            }

            dsi.score = p_rel;

        }

    }

    private Result method1(Collection<MG4JRelevanceFeedback.MG4JDocument> feedback, ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results, int[] contents, long[] queryTerms) throws IOException {
        // Collect all words from documents and estimate P(w|R) and P(w|N)

        // Compute P(w q1 .. qn)
        MutableString separator = new MutableString();
        MutableString token = new MutableString();

        // Probability of P(w q1 ... qn) - Eq. 9 (model iid) and 12 (conditional)
        Long2DoubleLinkedOpenHashMap p_w_q = new Long2DoubleLinkedOpenHashMap();
        p_w_q.defaultReturnValue(0.);

        // Probability of picking one "relevant" document is uniform
        double p_m = 1. / (double) feedback.size();

        double sum_prod_p_q__M = 0;

        for (MG4JRelevanceFeedback.MG4JDocument document : feedback) {
            Multiset<Long> words = readDocument(contents, document.docid);
            final int docLength = words.size();

            // Computes Prod. P(q|M)
            double p_q__M = 1;
            for (long qi : queryTerms) {
                p_q__M *= getSmoothedPr(qi, words.count(qi), docLength);
            }

            sum_prod_p_q__M += p_q__M;

            // Updates P(w, q1 ... qn)
            for (Multiset.Entry<Long> w : words.entrySet()) {
                final long wordId = w.getElement().longValue();
                final double new_p = p_w_q.get(wordId) +
                        p_m * p_q__M + getSmoothedPr(w.getElement(), w.getCount(), docLength);
                p_w_q.put(wordId, new_p);
            }


        } // Loop over "relevant" documents

        return new Result((1. - lambda) * sum_prod_p_q__M, p_w_q);
    }

    private Result method2(Collection<MG4JRelevanceFeedback.MG4JDocument> feedback, ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results, int[] contents, long[] queryTerms) throws IOException {

        // We use the fact that
        // P(w, q) \propto (\prod_i \sum_M p(q/M)p(w/M)) / \sum_M p(w|M)

        // for terms not in the feedback documents, this simplifies to
        // (1-lambda) * p(w|C) * \prod_i \sum_M p(q/M)

        long unknown = index.getUnknownTermId();

        // Collect all words from documents and estimate P(w|R) and P(w|N)

        // Compute P(w q1 .. qn)

        // Probability of P(w q1 ... qn) - Eq. 12 (conditional)

        // Stores the sum
        Long2ObjectLinkedOpenHashMap<double[]> pr_w__qi = new Long2ObjectLinkedOpenHashMap<>();
        pr_w__qi.defaultReturnValue(null);

        // Probability of picking one "relevant" document is uniform
        double p_m = 1. / (double) feedback.size();

        double[] sum_p_qi__M = new double[queryTerms.length];
        for (int i = sum_p_qi__M.length; --i >= 0; )
            sum_p_qi__M[i] = 1;

        for (MG4JRelevanceFeedback.MG4JDocument document : feedback) {
            Multiset<Long> words = readDocument(contents, document.docid);
            final int docLength = words.size();

            // Computes P(q_i|M)
            double[] p_qi__M = new double[queryTerms.length];
            for (int i = p_qi__M.length; --i >= 0; ) {
                p_qi__M[i] = getSmoothedPr(queryTerms[i], words.count(queryTerms[i]), docLength);
                sum_p_qi__M[i] += p_qi__M[i];
            }


            // Computes a value proportional to P(w q1...qn)
            for (Multiset.Entry<Long> w : words.entrySet()) {
                final long wordId = w.getElement().longValue();
                double[] p = pr_w__qi.get(wordId);
                if (p == null)
                    pr_w__qi.put(wordId, p = new double[queryTerms.length]);
                for (int i = p_qi__M.length; --i >= 0; ) {
                    p[i] += getSmoothedPr(w.getElement(), w.getCount(), docLength) * p_qi__M[i];
                }
            }

        } // Loop over "relevant" documents

        // Now, computes p(w|R) for each word in the feedback document
        Long2DoubleLinkedOpenHashMap result = new Long2DoubleLinkedOpenHashMap();
        for (Map.Entry<Long, double[]> entry : pr_w__qi.entrySet()) {
            double p = 1;
            for (double x : entry.getValue()) p *= x;
            result.put(entry.getKey().longValue(), p);
        }


        // Compute the value for the unknown terms
        double p = 1. - lambda;
        for (int i = sum_p_qi__M.length; --i >= 0; )
            p *= sum_p_qi__M[i];

        return new Result(p, result);
    }


    /**
     * Read a document
     *
     * @param contents Contents to read
     * @param document The document ID to read
     * @return A multiset containing the frequencies of the contained terms
     * @throws IOException
     */
    private Multiset<Long> readDocument(int[] contents, long document) throws IOException {
        MutableString separator = new MutableString();
        MutableString token = new MutableString();

        final Document doc = collection.document(document);
        long unknown = index.getUnknownTermId();

        Multiset<Long> words = HashMultiset.create();

        for (int contentId : contents) {
            final WordReader reader = doc.wordReader(0);

            // Loop over terms
            while (reader.next(token, separator)) {
                final Long termId = index.getTermId(token);
                if (termId == unknown) continue;
                words.add(termId);
            }
        } // loop over content
        return words;
    }


    /**
     * Get a smoothed version for the relevance model
     *
     * @param termId
     * @param termFreq
     * @param docLength
     * @return
     */
    private double getSmoothedPr(long termId, int termFreq, int docLength) {
        // Linear smoothing (eq. 15 p. 123)
        return
                lambda * (double) termFreq / (double) docLength +
                        (1. - lambda) * ((double) frequencies.getLong(termId) / (double) index.getNumberOfPostings());
    }


}
