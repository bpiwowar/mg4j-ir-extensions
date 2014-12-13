package net.bpiwowar.mg4j.extensions.tokenizer;

import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.experimaestro.tasks.ClassChooserInstance;

/**
 * Term normalization: handle numbers
 */
@ClassChooserInstance(name = "normalization")
public class TermNormalization implements TermProcessor {
    @Override
    public boolean processTerm(MutableString term) {
        for (int i = 0; i < term.length(); i++) {
            if (Character.isDigit(term.charAt(i))) {
                term.setCharAt(i, '9');
            }
        }
        return true;
    }

    @Override
    public boolean processPrefix(MutableString term) {
        return processTerm(term);
    }

    @Override
    public TermProcessor copy() {
        return new TermNormalization();
    }
}
