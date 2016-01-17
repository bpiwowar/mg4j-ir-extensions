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
@ClassChooserInstance(name = "hiemstra")
public class HiemstraScorer extends LMScorer implements LMModel {
    @JsonArgument
    double c = 0.15;

    /**
     * An array indexed by offsets that caches the inverse document-frequency part of the formula, multiplied by the index weight.
     */
    private double[] cOverOneMinusCTimesFrequency;
    private double[] indexWeights;

    public HiemstraScorer() {
    }

    public HiemstraScorer(HiemstraScorer other) {
        super(other);
        this.c = other.c;
    }

    public synchronized LMScorer copy() {
        return new HiemstraScorer(this);
    }


    public double score() throws IOException {
        setupVisitor.clear();
        documentIterator.acceptOnTruePaths(counterCollectionVisitor);
        final long document = documentIterator.document();
        final int[] count = setupVisitor.count;
        final int[] indexNumber = setupVisitor.indexNumber;
        final double[] cOverOneMinusCTimesFrequency = this.cOverOneMinusCTimesFrequency;
        final int[] size = this.size;

        for (int i = size.length; i-- != 0; ) size[i] = sizes[i].getInt(document);

        int k;
        double score = 0;
        for (int i = count.length; i-- != 0; ) {
            k = indexNumber[i];
            if (count[i] != 0) {
                score += indexWeights[i] * Math.log(
                        1. + cOverOneMinusCTimesFrequency[i] / (double) size[k] * (double) count[i]
                );
            }
        }
        return score;

    }

    @Override
    public void wrap(DocumentIterator d) throws IOException {
        super.wrap(d);

        final Index[] index = termVisitor.indices();
        final long[] occurrences = setupVisitor.occurrences;
        final int[] indexNumber = setupVisitor.indexNumber;

        cOverOneMinusCTimesFrequency = new double[occurrences.length];
        indexWeights = new double[occurrences.length];

        for (int i = cOverOneMinusCTimesFrequency.length; i-- != 0; ) {
            final Index termIndex = index[indexNumber[i]];

            cOverOneMinusCTimesFrequency[i] = c * (double) termIndex.numberOfOccurrences / (1. - c) * (double) occurrences[i];
            indexWeights[i] = index2Weight.getDouble(index[indexNumber[i]]);
        }

//        System.err.format("Setup visitor = %s%n", setupVisitor);
    }

    @Override
    public String toString() {
        return format("Hiemstra(%.3g)", c);
    }


}
