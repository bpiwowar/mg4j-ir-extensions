package net.bpiwowar.mg4j.extensions;

import com.google.gson.*;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.IdentityDocumentFactory;
import it.unimi.di.big.mg4j.tool.Scan;
import net.bpiwowar.mg4j.extensions.query.QuerySet;
import net.bpiwowar.mg4j.extensions.query.Tokenizer;
import net.bpiwowar.mg4j.extensions.query.TopicProcessor;
import net.bpiwowar.mg4j.extensions.tasks.Collection;
import net.bpiwowar.mg4j.extensions.utils.TextToolChain;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.XPMTypeAdapterFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 */
public class Utils {
    /**
     * Returns a document collection serialized in a path
     * @param path
     * @return A document collection
     * @throws IllegalAccessException
     * @throws IOException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws ClassNotFoundException
     */
    static public DocumentCollection getDocumentCollection(File path) throws IllegalAccessException, IOException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
            return (DocumentCollection) Scan.getSequence(path.getAbsolutePath(),
                    IdentityDocumentFactory.class, new String[]{},
                    Scan.DEFAULT_DELIMITER, LoggerFactory.getLogger(Collection.class));
    }
    static public DocumentCollection getDocumentCollection(String path) throws IllegalAccessException, IOException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        return getDocumentCollection(new File(path));
    }

    public static void getQueryTerms(QuerySet querySet, HashSet<String> terms,
                                     HashMap<String, HashSet<String>> query2terms,
                                     TopicProcessor topic_processor, TextToolChain toolchain) {
        querySet.queries().forEach((qid, topic) -> {
            final HashSet<String> queryTerms = new HashSet<>();
            query2terms.put(qid, queryTerms);
            topic_processor.getPositiveTerms(new Tokenizer(toolchain.wordReader),
                    toolchain.termProcessor, null, topic)
                    .keySet()
                    .forEach((e) -> {
                        terms.add(e);
                        queryTerms.add(e);
                    });
        });
    }

    public static <T> T get(String json, Class<T> classT) {
        XPMTypeAdapterFactory factory = new XPMTypeAdapterFactory();
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setExclusionStrategies(new XPMExclusionStrategy())
                .setFieldNamingStrategy(new XPMNamingStrategy())
                .registerTypeAdapterFactory(factory);
        final Gson gson = gsonBuilder
                .create();
        return (T)gson.fromJson(json, classT);
    }

    private static class XPMExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(JsonArgument.class) == null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

    private static class XPMNamingStrategy implements FieldNamingStrategy {
        @Override
        public String translateName(Field f) {
            // Get the name from the annotation
            final JsonArgument annotation = f.getAnnotation(JsonArgument.class);
            if (annotation != null && !annotation.name().equals("")) {
                return annotation.name();
            }

            // Otherwise, use the field name
            return f.getName();
        }
    }
}
