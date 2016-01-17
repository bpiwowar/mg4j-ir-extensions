package net.bpiwowar.mg4j.extensions.adhoc.lm;

import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.search.DocumentIterator;
import net.bpiwowar.mg4j.extensions.adhoc.LMModel;
import net.bpiwowar.xpm.manager.tasks.ClassChooserInstance;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;

import java.io.IOException;

import static java.lang.String.format;

/**
 * A LM with Jelinek Mercer smoothing
 */
@ClassChooserInstance(name = "dirichlet")
public class DirichletScorer extends LMScorer implements LMModel {
    @JsonArgument
    double mu = 1000;

    /**
     * An array indexed by offsets that caches the inverse document-frequency part of the formula, multiplied by the index weight.
     */
    private double[] indexWeights;
    private double[] prColl;

    public DirichletScorer() {
    }

    public DirichletScorer(DirichletScorer other) {
        super(other);
        this.mu = other.mu;
    }

    public synchronized LMScorer copy() {
        return new DirichletScorer(this);
    }


    public double score() throws IOException {
        setupVisitor.clear();
        documentIterator.acceptOnTruePaths(counterCollectionVisitor);
        final long document = documentIterator.document();
        final int[] count = setupVisitor.count;
        final int[] indexNumber = setupVisitor.indexNumber;
        final int[] size = this.size;

        for (int i = size.length; i-- != 0; ) size[i] = sizes[i].getInt(document);

        int k;
        double score = 0;
        for (int i = count.length; i-- != 0; ) {
            k = indexNumber[i];
            score += indexWeights[i] * Math.log(
                    (((double) count[i] / size[k]) + mu * prColl[i])
                    /
                    ((double) size[k] + mu)
            );
        }
        return score;

    }

    @Override
    public void wrap(DocumentIterator d) throws IOException {
        super.wrap(d);

        final Index[] index = termVisitor.indices();
        final long[] occurrences = setupVisitor.occurrences;
        final int[] indexNumber = setupVisitor.indexNumber;

        indexWeights = new double[occurrences.length];
        prColl = new double[occurrences.length];

        for (int i = indexWeights.length; i-- != 0; ) {
            final Index termIndex = index[indexNumber[i]];
            prColl[i] = (double) occurrences[i] / (double) termIndex.numberOfOccurrences;
            indexWeights[i] = index2Weight.getDouble(index[indexNumber[i]]);
        }

//        System.err.format("Setup visitor = %s%n", setupVisitor);
    }

    @Override
    public String toString() {
        return format("Dirichlet(%.3g)", mu);
    }


}
