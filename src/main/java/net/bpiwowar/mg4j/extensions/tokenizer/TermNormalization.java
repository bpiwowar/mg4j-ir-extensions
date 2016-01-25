package net.bpiwowar.mg4j.extensions.tokenizer;

import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.query.nodes.Term;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.xpm.manager.tasks.ClassChooserInstance;

/**
 * Term normalization:
 * - handle numbers by changing all the digits to 9
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

        if (term.equals('.') || term.equals("``") || term.equals(",")) {
            return false;
        }

        term.toLowerCase();
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TermProcessor;
    }
}
