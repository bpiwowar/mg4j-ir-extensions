package net.bpiwowar.mg4j.extensions.adhoc;

import it.unimi.di.big.mg4j.search.score.Scorer;
import net.bpiwowar.mg4j.extensions.adhoc.lm.LMScorer;
import net.bpiwowar.xpm.manager.tasks.ClassChooser;
import net.bpiwowar.xpm.manager.tasks.ClassChooserInstance;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;

/**
 * Represent a language model
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@ClassChooserInstance(name = "lm")
public class LM extends MG4JScorer {
    @JsonArgument(required = true)
    @ClassChooser(classesOfPackage = LMScorer.class)
    LMModel smoothing;

    @Override
    Scorer getScorer() {
        return smoothing;
    }

    @Override
    public String toString() {
        return "LM/" + smoothing.toString();
    }
}
