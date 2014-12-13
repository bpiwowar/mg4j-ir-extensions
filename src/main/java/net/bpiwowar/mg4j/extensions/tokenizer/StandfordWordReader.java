package net.bpiwowar.mg4j.extensions.tokenizer;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.experimaestro.tasks.ClassChooserInstance;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

/**
 * A tokenizer using core NLP utils from Standford
 */
@ClassChooserInstance(name = "standford")
public class StandfordWordReader implements WordReader {
    private StandfordTokenStream stream;

    @Override
    public boolean next(MutableString word, MutableString delimiter) throws IOException {
        if (stream == null || !stream.hasNext()) {
            return false;
        }

        word.replace(stream.next());
        delimiter.replace(" ");
        return true;
    }

    @Override
    public WordReader setReader(Reader reader) {
        stream = new StandfordTokenStream(reader);
        return this;
    }

    @Override
    public WordReader copy() {
        return new StandfordWordReader();
    }

    static class StandfordTokenStream implements Iterator<String> {

        private final Iterator<List<HasWord>> sentenceIterator;
        private Iterator<HasWord> tokenIterator;
        boolean eos = false;

        public StandfordTokenStream(Reader reader) {
            DocumentPreprocessor dp = new DocumentPreprocessor(reader);
            sentenceIterator = dp.iterator();
        }

        public boolean hasNext() {
            if (eos) return true;

            if (sentenceIterator == null)
                return false;

            while (tokenIterator == null || !tokenIterator.hasNext()) {
                if (!sentenceIterator.hasNext())
                    return false;
                List<HasWord> words = sentenceIterator.next();
                tokenIterator = words.iterator();
            }

            return tokenIterator.hasNext();
        }

        public String next() {
            if (eos) {
                eos = false;
                return "__EOS__";
            }

            String s = tokenIterator.next().toString();
            if (!tokenIterator.hasNext()) {
                eos = true;
            }
            return s;
        }
    }

}
