package net.bpiwowar.mg4j.extensions.utils;

import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.dsi.io.WordReader;
import net.bpiwowar.experimaestro.tasks.JsonArgument;

public class TextToolChain {
    @JsonArgument(name = "tokenizer")
    public WordReader wordReader;

    @JsonArgument(name = "term_processor")
    public TermProcessor termProcessor;

}
