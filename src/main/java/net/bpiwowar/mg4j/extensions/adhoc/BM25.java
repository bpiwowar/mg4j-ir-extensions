package net.bpiwowar.mg4j.extensions.adhoc;

import it.unimi.di.big.mg4j.search.score.BM25Scorer;
import it.unimi.di.big.mg4j.search.score.Scorer;
import net.bpiwowar.experimaestro.tasks.ClassChooserInstance;
import net.bpiwowar.experimaestro.tasks.JsonArgument;

/**
 * Represent a BM25 model
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@ClassChooserInstance(name = "bm25")
public class BM25 extends MG4JScorer {
    @JsonArgument
    double b = 0.75;

    @JsonArgument
    double k1 = 1.2;

    @Override
    public String toString() {
        return String.format("BM25_b=%.3f_k1=%.3f", b, k1);
    }

    @Override
    Scorer getScorer() {
        return new BM25Scorer(k1, b);
    }
}
