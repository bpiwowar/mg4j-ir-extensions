package net.bpiwowar.mg4j.extensions.adhoc.lm;

import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.search.DocumentIterator;
import net.bpiwowar.experimaestro.tasks.ClassChooserInstance;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.mg4j.extensions.adhoc.LMModel;

import java.io.IOException;

import static java.lang.String.format;

/**
 * A LM with Jelinek Mercer smoothing
 */
@ClassChooserInstance(name = "jelinek-mercer")
public class JelinekMercerScorer extends LMScorer implements LMModel {
    @JsonArgument
    double lambda = 0.05;

    /**
     * An array indexed by offsets that caches the inverse document-frequency part of the formula, multiplied by the index weight.
     */
    private double[] lambdaTimesPrColl;
    private double[] indexWeights;

    public JelinekMercerScorer() {
    }

    public JelinekMercerScorer(JelinekMercerScorer other) {
        super(other);
        this.lambda = other.lambda;
    }

    public synchronized LMScorer copy() {
        return new JelinekMercerScorer(this);
    }


    public double score() throws IOException {
        setupVisitor.clear();
        documentIterator.acceptOnTruePaths(counterCollectionVisitor);
        final long document = documentIterator.document();
        final int[] count = setupVisitor.count;
        final int[] indexNumber = setupVisitor.indexNumber;
        final double[] lambdaTimesPrColl = this.lambdaTimesPrColl;
        final int[] size = this.size;

        for (int i = size.length; i-- != 0; ) size[i] = sizes[i].getInt(document);

        int k;
        double score = 0;
        for (int i = count.length; i-- != 0; ) {
            k = indexNumber[i];
            score += indexWeights[i] * Math.log(
                    (1. - lambda) * ((double) count[i] / size[k])
                            + lambdaTimesPrColl[i]);
        }
        return score;

    }

    @Override
    public void wrap(DocumentIterator d) throws IOException {
        super.wrap(d);

        final Index[] index = termVisitor.indices();
        final long[] occurrences = setupVisitor.occurrences;
        final int[] indexNumber = setupVisitor.indexNumber;

        lambdaTimesPrColl = new double[occurrences.length];
        indexWeights = new double[occurrences.length];

        for (int i = lambdaTimesPrColl.length; i-- != 0; ) {
            final Index termIndex = index[indexNumber[i]];

            lambdaTimesPrColl[i] = lambda * (double) occurrences[i] / (double) termIndex.numberOfOccurrences;
            indexWeights[i] = index2Weight.getDouble(index[indexNumber[i]]);
        }

//        System.err.format("Setup visitor = %s%n", setupVisitor);
    }

    @Override
    public String toString() {
        return format("Jelinek-Mercer(%.3g)", lambda);
    }


}
