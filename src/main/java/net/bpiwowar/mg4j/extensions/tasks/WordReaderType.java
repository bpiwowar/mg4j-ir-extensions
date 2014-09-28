package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.EnumValue;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.WordReader;
import net.bpiwowar.mg4j.extensions.tokenizer.StandfordWordReader;

/**
* Created by bpiwowar on 28/9/14.
*/
public enum WordReaderType {
    @EnumValue("default")
    DEFAULT,

    @EnumValue("standford")
    STANDFORD;

    public WordReader getWordReader() {
        switch(this) {
            case DEFAULT:
                return new FastBufferedReader();
            case STANDFORD:
                return new StandfordWordReader();
            default:
                throw new AssertionError("Not all the cases have been implemented [" + this + "]");
        }
    }


}
