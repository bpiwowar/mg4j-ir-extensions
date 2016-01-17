package net.bpiwowar.mg4j.extensions.query;

import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Tokenizer {
    WordReader reader;
    MutableString sep = new MutableString();
    MutableString word = new MutableString();

    public Tokenizer(WordReader reader) {
        this.reader = reader;
    }

    public Tokenizer() {
        this.reader = new FastBufferedReader();
    }

    List<String> tokenize(String s) {
        ArrayList<String> tokens = new ArrayList<>();
        reader.setReader(new StringReader(s));
        try {
            while (reader.next(word, sep)) {
                tokens.add(word.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tokens;
    }
}
