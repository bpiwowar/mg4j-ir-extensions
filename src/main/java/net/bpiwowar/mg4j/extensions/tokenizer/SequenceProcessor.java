package net.bpiwowar.mg4j.extensions.tokenizer;

import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.xpm.manager.tasks.ClassChooserInstance;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;

import java.util.ArrayList;

/**
 * Term normalization:
 * - handle numbers by changing all the digits to 9
 */
@ClassChooserInstance(name = "sequence")
public class SequenceProcessor implements TermProcessor {
    @JsonArgument
    ArrayList<TermProcessor> processors;

    @Override
    public boolean processTerm(MutableString term) {
        for (int i = 0; i < term.length(); i++) {
            if (Character.isDigit(term.charAt(i))) {
                term.setCharAt(i, '9');
            }
        }

        if (term.equals('.') || term. equals("``") || term.equals(",")) {
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
        return new SequenceProcessor();
    }
}
