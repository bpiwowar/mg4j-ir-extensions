package net.bpiwowar.mg4j.extensions.utils;

import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.dsi.io.WordReader;
import net.bpiwowar.mg4j.extensions.Utils;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;

public class TextToolChain {
    @JsonArgument(name = "tokenizer", required = true)
    public WordReader wordReader;

    @JsonArgument(name = "term_processor", required = true)
    public TermProcessor termProcessor;

    static public TextToolChain fromJSON(String json) {
        return Utils.get(json, TextToolChain.class);
    }

    public WordReader getWordReader() {
        return wordReader;
    }

    public TermProcessor getTermProcessor() {
        return termProcessor;
    }
}
