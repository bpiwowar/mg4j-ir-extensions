package net.bpiwowar.mg4j.extensions.adhoc;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.search.score.Scorer;
import net.bpiwowar.experimaestro.tasks.ClassChooserInstance;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.mg4j.extensions.conf.IndexedField;

/**
 * Represent a language model
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@ClassChooserInstance(name = "lm")
public class LM extends MG4JScorer {
    @JsonArgument
    LMModel smoothing;

    @Override
    Scorer getScorer() {
        return smoothing;
    }
}
