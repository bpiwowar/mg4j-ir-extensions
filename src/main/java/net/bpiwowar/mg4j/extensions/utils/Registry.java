package net.bpiwowar.mg4j.extensions.utils;

import bpiwowar.argparser.Argument;
import it.unimi.di.big.mg4j.index.NullTermProcessor;
import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.index.snowball.EnglishStemmer;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.WordReader;
import net.bpiwowar.experimaestro.tasks.ClassChooser;
import net.bpiwowar.experimaestro.tasks.ClassChooserInstance;
import net.bpiwowar.mg4j.extensions.adhoc.BM25;
import net.bpiwowar.mg4j.extensions.adhoc.RetrievalModel;
import net.bpiwowar.mg4j.extensions.query.TopicProcessor;
import net.bpiwowar.mg4j.extensions.tokenizer.StandfordWordReader;
import net.bpiwowar.mg4j.extensions.tokenizer.TermNormalization;

/**
 * Registry for the different types of objects that can be found in the tasks
 */
public class Registry {
    @ClassChooser(classesOfPackage = StandfordWordReader.class, instances = {
            @ClassChooserInstance(name = "standard", instance = FastBufferedReader.class)
    })
    WordReader wordReader;

    @Argument(help = "The term processor (default: no processor)")
    @ClassChooser(classesOfPackage = TermNormalization.class, instances = {
            @ClassChooserInstance(name = "null", instance = NullTermProcessor.class),
            @ClassChooserInstance(name = "english", instance = EnglishStemmer.class)
    })
    TermProcessor termProcessor = NullTermProcessor.getInstance();

    @Argument(help = "Retrieval model")
    @ClassChooser(classesOfPackage = BM25.class)
    RetrievalModel retrievalModel;

    @Argument(help = "Process queries into MG4J format")
    @ClassChooser(classesOfPackage = TopicProcessor.class)
    TopicProcessor topicProcessor;

}
