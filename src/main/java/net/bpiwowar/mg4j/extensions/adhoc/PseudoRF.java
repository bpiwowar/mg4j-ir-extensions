package net.bpiwowar.mg4j.extensions.adhoc;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.handlers.ClassChooser;
import it.unimi.di.big.mg4j.document.Document;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.query.SelectedInterval;
import it.unimi.di.big.mg4j.search.score.DocumentScoreInfo;
import it.unimi.dsi.fastutil.ints.IntBigList;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongRBTreeMap;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;

import static java.lang.Math.log;
import static java.lang.Math.min;

/**
* Created by bpiwowar on 15/12/14.
*/ // ---- Relevance feedback parameters ----
// ---------------------------------------
public abstract class PseudoRF {
    @Argument(name = "top-k", help = "Top results to consider (default: top 10)")
    int k = 10;

    /**
     * @param results The set of results
     * @throws java.io.IOException
     */
    abstract Collection<CharSequence> process(
            ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results)
            throws IOException;

    abstract void init() throws IOException;

    @ClassChooser.Choice(name = "trec-8")
    static public class TopRanked extends PseudoRF {

        @Argument(name = "threshold", help = "Threshold")
        double c = 0;

        // log of vocabularySize log(V)
        private double logV;

        private long vocabularySize;

        private IntBigList frequencies;

        private double logN;

        /**
         * The ith entry stores log(Comb(k; i+1))
         */
        double[] combinaisons;

        private IndexedField index;

        private DocumentCollection collection;

        private int fieldIndex;

        private TermProcessor processor;

        @Override
        void init() throws IOException {
            vocabularySize = index.index.numberOfTerms;
            this.frequencies = index.getFrequencies();
            logN = log(index.index.numberOfDocuments);
            logV = log(vocabularySize);

            // TODO: Annalina - create a transformer that stem & stop using
            // processor
            // queryEngine.transformer(transformer);

            // Compute log(Comb(k; i+1)) for i = 0 to k-1
            combinaisons = new double[k];
            // log(Comb(k; 1)) is log(k)
            combinaisons[0] = log(k);
            for (int i = 1; i < k; i++) {
                // C(k; i+1) = C(k; i) * (k-i) / (i+1)
                // [don't forget that combinaisons[i] is C(k; i+1)
                combinaisons[i] = combinaisons[i - 1] + log(k - i)
                        - log(i + 1);
            }

            fieldIndex = collection.factory().fieldIndex(index.field);
            if (fieldIndex < 0)
                throw new RuntimeException(String.format(
                        "Could not find field %s in index", index.field));

        }

        @Override
        public Collection<CharSequence> process(
                ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results)
                throws IOException {
            // Creates a new collection of terms
            Collection<CharSequence> newTerms = new HashSet<>();

            // Get a subspace representation of the K first documents
            if (k > results.size()) {
                MG4JScorer.logger
                        .warn(String.format(
                                "The number of returned documents (%d) is inferior to the number of documents (%d) for blind RF",
                                results.size(), k));
            }

            // --- Compute the frequencies of terms
            Long2LongRBTreeMap relFreq = computeTermFrequencies(results);

            // --- Select the candidates
            for (Long2LongMap.Entry entry : relFreq.long2LongEntrySet()) {
                long rt2 = entry.getLongValue();
                int nt = frequencies.getInt(entry.getLongKey());
                final double termScore = rt2 * (logN - log(nt))
                        - combinaisons[(int) (rt2 - 1)] - logV;
                if (termScore > c) {
                    // Add term to set
                    final CharSequence term = index.getTerm(entry
                            .getLongKey());
                    newTerms.add(term);
                    MG4JScorer.logger.info(String.format("Adding term %s", term));
                } else {
                    if (MG4JScorer.logger.isDebugEnabled())
                        MG4JScorer.logger.debug(String.format("Not adding term %s (%g <= %g)", index
                                .getTerm(entry.getLongKey()), termScore, c));
                }

            }

            return newTerms;
        }

        /**
         * Computes the term frequency in a set of results
         * @param results
         * @return
         * @throws java.io.IOException
         */
        private Long2LongRBTreeMap computeTermFrequencies(
                ObjectArrayList<DocumentScoreInfo<Reference2ObjectMap<Index, SelectedInterval[]>>> results)
                throws IOException {
            Long2LongRBTreeMap relFreq = new Long2LongRBTreeMap();
            relFreq.defaultReturnValue(0);

            for (int i = 0; i < min(k, results.size()); i++) {
                Document document = collection
                        .document(results.get(i).document);
                Reader reader = (Reader) document.content(fieldIndex);
                WordReader wordReader = document.wordReader(fieldIndex);
                wordReader.setReader(reader);

                MutableString word = new MutableString();
                MutableString nonWord = new MutableString();
                final LongRBTreeSet set = new LongRBTreeSet();

                while (wordReader.next(word, nonWord)) {
                    if (processor.processTerm(word)) {
                        long termId = index.getTermId(word);
                        if (termId >= 0)
                            if (set.add(termId))
                                relFreq
                                        .put(termId,
                                                relFreq.get(termId) + 1);
                    }
                }

                document.close();

            }
            return relFreq;
        }
    }

}
