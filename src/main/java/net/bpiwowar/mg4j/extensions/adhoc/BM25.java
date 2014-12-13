package net.bpiwowar.mg4j.extensions.adhoc;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.search.score.BM25Scorer;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.ClassChooserInstance;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;

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
    public void init(DocumentCollection collection, IndexedField index) throws Exception {
        scorer = new BM25Scorer(k1, b);
        super.init(collection, index);
    }

}
