package net.bpiwowar.mg4j.extensions;


import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.index.IndexIterator;
import it.unimi.di.big.mg4j.search.AbstractIntersectionDocumentIterator;
import it.unimi.di.big.mg4j.search.DocumentIterator;
import it.unimi.di.big.mg4j.search.score.AbstractWeightedScorer;
import it.unimi.di.big.mg4j.search.score.DelegatingScorer;
import it.unimi.di.big.mg4j.search.visitor.AbstractDocumentIteratorVisitor;
import it.unimi.di.big.mg4j.search.visitor.CounterSetupVisitor;
import it.unimi.di.big.mg4j.search.visitor.TermCollectionVisitor;
import it.unimi.dsi.fastutil.ints.IntBigList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author B. Piwowarski <benjamin
 *         Based on the {@linkplain it.unimi.di.big.mg4j.search.score.BM25Scorer} class
 */
public class RelevanceModelScorer extends AbstractWeightedScorer implements DelegatingScorer {
    public static final Logger LOGGER = LoggerFactory.getLogger(RelevanceModelScorer.class);
    public static final boolean DEBUG = true;

    private static final class Visitor extends AbstractDocumentIteratorVisitor {
        /**
         * Offset-indexed precomputed values.
         */
        private final double[] k1Plus1TimesWeightedIdfPart;
        /**
         * Offset-indexed precomputed values.
         */
        private final double k1Times1MinusB;
        /**
         * An array (parallel to {@link TermCollectionVisitor#indices()} that caches average document sizes.
         */
        private final double k1TimesBDividedByAverageDocumentSize[];
        /**
         * An array (parallel to {@link TermCollectionVisitor#indices()} that caches size lists.
         */
        private final IntBigList sizes[];
        /**
         * Cached from {@link RelevanceModelScorer}.
         */
        private final double[] sizeComponent;
        /**
         * Cached from {@link CounterSetupVisitor}.
         */
        private final int[] indexNumber;
        /**
         * The length of {@link TermCollectionVisitor#indices()} cached.
         */
        private final int numberOfIndices;
        /**
         * An array parallel to {@link #indexNumber} keeping track of whether we already accumulated the score for a specific term/index pair.
         */
        private final boolean[] seen;
        /**
         * An array accumulating the indices in {@link #seen} that have been set to true, so to accelerate {@link #reset(int)}.
         */
        private final int[] seenList;
        /**
         * The accumulated score.
         */
        public double score;
        /**
         * The number of valid entries in {@link #seenList}.
         */
        private int numberOfSeen;

        public Visitor(final double k1Times1Minusb, final double[] k1Plus1TimesWeightedIdfPart, final double[] k1TimesBDividedByAverageDocumentSize, final int numberOfIndices, final int[] indexNumber, final IntBigList[] sizes) {
            this.k1Times1MinusB = k1Times1Minusb;
            this.k1Plus1TimesWeightedIdfPart = k1Plus1TimesWeightedIdfPart;
            this.k1TimesBDividedByAverageDocumentSize = k1TimesBDividedByAverageDocumentSize;
            this.sizeComponent = new double[numberOfIndices];
            this.numberOfIndices = numberOfIndices;
            this.indexNumber = indexNumber;
            this.seen = new boolean[indexNumber.length];
            this.seenList = new int[indexNumber.length];
            this.sizes = sizes;
        }

        public Boolean visit(final IndexIterator indexIterator) throws IOException {
            final int offset = indexIterator.id();
            if (!seen[offset]) {
                seen[seenList[numberOfSeen++] = offset] = true;
                final int count = indexIterator.count();
                score += (count * k1Plus1TimesWeightedIdfPart[offset]) / (count + sizeComponent[indexNumber[offset]]);
            }
            return Boolean.TRUE;
        }

        public void reset(final long document) {
            score = 0;
            // Clear seen information (on the first invocation does nothing as numberOfSeen == 0 ).
            while (numberOfSeen-- != 0) seen[seenList[numberOfSeen]] = false;
            numberOfSeen = 0;

            for (int i = numberOfIndices; i-- != 0; )
                sizeComponent[i] = k1Times1MinusB + k1TimesBDividedByAverageDocumentSize[i] * sizes[i].getInt(document);
        }
    }


    /**
     * The default value used for the parameter <var>k</var><sub>1</sub>.
     */
    public final static double DEFAULT_K1 = 1.2;
    /**
     * The default value used for the parameter <var>b</var>.
     */
    public final static double DEFAULT_B = 0.5;
    /**
     * The value of the document-frequency part for terms appearing in more than half of the documents.
     */
    public final static double EPSILON_SCORE = 1.0E-6;
    /**
     * Disjunctive queries on {@linkplain IndexIterator index iterators} are handled using the flat evaluator only if they contain less than
     * this number of disjuncts. The generic evaluator is more efficient if there are several disjuncts, as it
     * invokes {@link IndexIterator#count()} only on the terms that are part of the front. This value is largely architecture, query,
     * term-distribution, and whatever else dependent.
     */
    public static final int MAX_FLAT_DISJUNCTS = 16;

    /**
     * The counter setup visitor used to estimate counts.
     */
    private final CounterSetupVisitor setupVisitor;
    /**
     * The term collection visitor used to estimate counts.
     */
    private final TermCollectionVisitor termVisitor;

    /**
     * The parameter <var>k</var><sub>1</sub>.
     */
    private final double k1;
    /**
     * The parameter <var>b</var>.
     */
    private final double b;

    /**
     * The parameter {@link #k1} multiplied by one minus {@link #b}, precomputed.
     */
    private final double k1Times1MinusB;
    /**
     * A value precomputed for flat evaluation.
     */
    private double k1TimesBDividedByAverageDocumentSize;
    /**
     * The list of sizes, cached for flat evaluation.
     */
    private IntBigList sizes;
    /**
     * An array indexed by offsets that caches the inverse document-frequency part of the formula, multiplied by the index weight, cached for flat evaluation.
     */
    private double[] k1Plus1TimesWeightedIdfPart;
    /**
     * The value of {@link TermCollectionVisitor#numberOfPairs()} cached, if {@link #indexIterator} is <code>null</code>.
     */
    private int numberOfPairs;
    /**
     * An array of nonzero-frequency index iterators, all on the same index, used by the flat evaluator, or <code>null</code> for generic evaluation.
     */
    private IndexIterator[] flatIndexIterator;
    /**
     * A visitor used by the generic evaluator.
     */
    private Visitor visitor;

    /**
     * Creates a BM25 adhoc using {@link #DEFAULT_K1} and {@link #DEFAULT_B} as parameters.
     */
    public RelevanceModelScorer() {
        this(DEFAULT_K1, DEFAULT_B);
    }

    /**
     * Creates a BM25 adhoc using specified <var>k</var><sub>1</sub> and <var>b</var> parameters.
     *
     * @param k1 the <var>k</var><sub>1</sub> parameter.
     * @param b  the <var>b</var> parameter.
     */
    public RelevanceModelScorer(final double k1, final double b) {
        termVisitor = new TermCollectionVisitor();
        setupVisitor = new CounterSetupVisitor(termVisitor);
        this.k1 = k1;
        this.b = b;
        k1Times1MinusB = k1 * (1 - b);
    }

    /**
     * Creates a BM25 adhoc using specified <var>k</var><sub>1</sub> and <var>b</var> parameters specified by strings.
     *
     * @param k1 the <var>k</var><sub>1</sub> parameter.
     * @param b  the <var>b</var> parameter.
     */
    public RelevanceModelScorer(final String k1, final String b) {
        this(Double.parseDouble(k1), Double.parseDouble(b));
    }

    public synchronized RelevanceModelScorer copy() {
        final RelevanceModelScorer scorer = new RelevanceModelScorer(k1, b);
        scorer.setWeights(index2Weight);
        return scorer;
    }

    public double score() throws IOException {

        final long document = documentIterator.document();

        if (flatIndexIterator == null) {
            visitor.reset(document);
            documentIterator.acceptOnTruePaths(visitor);
            return visitor.score;
        } else {
            final double sizeComponent = k1Times1MinusB + k1TimesBDividedByAverageDocumentSize * sizes.getInt(document);
            double score = 0;
            final double[] k1Plus1TimesWeightedIdfPart = this.k1Plus1TimesWeightedIdfPart;
            final IndexIterator[] actualIndexIterator = this.flatIndexIterator;

            for (int i = numberOfPairs; i-- != 0; )
                if (actualIndexIterator[i].document() == document) {
                    final int c = actualIndexIterator[i].count();
                    score += (c * k1Plus1TimesWeightedIdfPart[i]) / (c + sizeComponent);
                }
            return score;
        }
    }

    public double score(final Index index) {
        throw new UnsupportedOperationException();
    }


    public void wrap(DocumentIterator d) throws IOException {
        super.wrap(d);

		/* Note that we use the index array provided by the weight function, *not* by the visitor or by the iterator.
         * If the function has an empty domain, this call is equivalent to prepare(). */
        termVisitor.prepare(index2Weight.keySet());

        d.accept(termVisitor);

        if (DEBUG) LOGGER.debug("Term Visitor found " + termVisitor.numberOfPairs() + " leaves");

        // Note that we use the index array provided by the visitor, *not* by the iterator.
        final Index[] index = termVisitor.indices();

        if (DEBUG) LOGGER.debug("Indices: " + Arrays.toString(index));

        flatIndexIterator = null;
		
		/* We use the flat evaluator only for single-index, term-only queries that are either quite small, and
		 * then either conjunctive, or disjunctive with a reasonable number of terms. */

        if (indexIterator != null && index.length == 1 && (documentIterator instanceof AbstractIntersectionDocumentIterator || indexIterator.length < MAX_FLAT_DISJUNCTS)) {
			/* This code is a flat, simplified duplication of what a CounterSetupVisitor would do. It is here just for efficiency. */
            numberOfPairs = 0;
			/* Find duplicate terms. We score unique pairs term/index with nonzero frequency, as the standard method would do. */
            final LongOpenHashSet alreadySeen = new LongOpenHashSet();

            for (int i = indexIterator.length; i-- != 0; )
                if (indexIterator[i].frequency() != 0 && alreadySeen.add(indexIterator[i].termNumber()))
                    numberOfPairs++;

            if (numberOfPairs == indexIterator.length) flatIndexIterator = indexIterator;
            else {
				/* We must compact the array, eliminating zero-frequency iterators. */
                flatIndexIterator = new IndexIterator[numberOfPairs];
                alreadySeen.clear();
                for (int i = 0, p = 0; i != indexIterator.length; i++)
                    if (indexIterator[i].frequency() != 0 && alreadySeen.add(indexIterator[i].termNumber()))
                        flatIndexIterator[p++] = indexIterator[i];
            }

            if (flatIndexIterator.length != 0) {
                // Some caching of frequently-used values
                k1TimesBDividedByAverageDocumentSize = k1 * b * flatIndexIterator[0].index().numberOfDocuments / flatIndexIterator[0].index().numberOfOccurrences;
                if ((this.sizes = flatIndexIterator[0].index().sizes) == null)
                    throw new IllegalStateException("A BM25 adhoc requires document sizes");

                // We do all logs here, and multiply by the weight
                k1Plus1TimesWeightedIdfPart = new double[numberOfPairs];
                for (int i = 0; i < numberOfPairs; i++) {
                    final long frequency = flatIndexIterator[i].frequency();
                    k1Plus1TimesWeightedIdfPart[i] = (k1 + 1) * Math.max(EPSILON_SCORE,
                            Math.log((flatIndexIterator[i].index().numberOfDocuments - frequency + 0.5) / (frequency + 0.5))) * index2Weight.getDouble(flatIndexIterator[i].index());
                }
            }
        } else {
            // Some caching of frequently-used values
            final double[] k1TimesBDividedByAverageDocumentSize = new double[index.length];
            for (int i = index.length; i-- != 0; )
                k1TimesBDividedByAverageDocumentSize[i] = k1 * b * index[i].numberOfDocuments / index[i].numberOfOccurrences;

            if (DEBUG) LOGGER.debug("Average document sizes: " + Arrays.toString(k1TimesBDividedByAverageDocumentSize));
            final IntBigList[] sizes = new IntBigList[index.length];
            for (int i = index.length; i-- != 0; )
                if ((sizes[i] = index[i].sizes) == null)
                    throw new IllegalStateException("A BM25 adhoc requires document sizes");

            setupVisitor.prepare();
            d.accept(setupVisitor);
            numberOfPairs = termVisitor.numberOfPairs();
            final long[] frequency = setupVisitor.frequency;
            final int[] indexNumber = setupVisitor.indexNumber;

            // We do all logs here, and multiply by the weight
            k1Plus1TimesWeightedIdfPart = new double[frequency.length];
            for (int i = k1Plus1TimesWeightedIdfPart.length; i-- != 0; )
                k1Plus1TimesWeightedIdfPart[i] = (k1 + 1) * Math.max(EPSILON_SCORE,
                        Math.log((index[indexNumber[i]].numberOfDocuments - frequency[i] + 0.5) / (frequency[i] + 0.5))) * index2Weight.getDouble(index[indexNumber[i]]);

            visitor = new Visitor(k1Times1MinusB, k1Plus1TimesWeightedIdfPart, k1TimesBDividedByAverageDocumentSize, termVisitor.indices().length, indexNumber, sizes);
        }

    }

    public boolean usesIntervals() {
        return false;
    }

}
