package net.bpiwowar.mg4j.extensions.adhoc.lm;

import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.search.DocumentIterator;
import it.unimi.dsi.fastutil.longs.LongBigList;
import net.bpiwowar.experimaestro.tasks.ClassChooserInstance;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.mg4j.extensions.adhoc.LMModel;
import net.bpiwowar.mg4j.extensions.utils.IndexUtils;

import java.io.IOException;

/**
 * A LM with Jelinek Mercer smoothing
 */
@ClassChooserInstance(name = "jelinek-mercer")
public class JelinekMercerScorer extends LMScorer implements LMModel {
    @JsonArgument
    double lambda = 0.9;

    /**
     * An array indexed by offsets that caches the inverse document-frequency part of the formula, multiplied by the index weight.
     */
    private double[] lambdaTimesPrColl;

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
            if (count[i] != 0) score += index2Weight.getDouble(k) * Math.log(
                    (1. - lambda) * ((double) count[i] / size[k])
                            + lambdaTimesPrColl[i]);
        }
        return score;

    }

    @Override
    public void wrap(DocumentIterator d) throws IOException {
        super.wrap(d);

        final Index[] index = termVisitor.indices();
        final long[] frequency = setupVisitor.frequency;
        final int[] indexNumber = setupVisitor.indexNumber;
        final int[] offset2TermId = setupVisitor.offset2TermId;

        // We do all logs here, and multiply by the weight
        lambdaTimesPrColl = new double[frequency.length];
        for (int i = lambdaTimesPrColl.length; i-- != 0; ) {
            final Index termIndex = index[indexNumber[i]];
            final LongBigList frequencies = IndexUtils.getTermFrequency(termIndex);

            lambdaTimesPrColl[i] =
                    lambda * (double) frequencies.get(offset2TermId[i]) / (double) termIndex.numberOfOccurrences;
        }

    }

}
