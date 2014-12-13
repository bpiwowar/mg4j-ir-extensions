package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.document.Document;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.document.DocumentIterator;
import it.unimi.di.big.mg4j.document.IdentityDocumentFactory;
import it.unimi.di.big.mg4j.index.NullTermProcessor;
import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.tool.Scan;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.experimaestro.tasks.AbstractTask;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import net.bpiwowar.mg4j.extensions.utils.TextToolChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Output a set of documents
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(id = "mg4j:cat", output = "xp:file", description = "Outputs a document collection as a stream",
        registry = Registry.class)
public class CatCollection extends AbstractTask {
    static final Logger LOGGER = LoggerFactory.getLogger(CatCollection.class);

    @JsonArgument(help = "Sequence to index", required = true)
    Collection collection;

    @JsonArgument(required = true)
    TextToolChain toolchain;

    @JsonArgument(name = "fields", help = "The fields to output", required = true)
    ArrayList<String> fieldNames = new ArrayList<>();

    @JsonArgument(help = "File with one document ID per line (empty = standard input)")
    File documents;

    @Override
    public JsonObject execute(JsonObject r) throws Throwable {
        DocumentCollection collection = (DocumentCollection) Scan.getSequence(this.collection.path.getAbsolutePath(),
                IdentityDocumentFactory.class, new String[]{},
                Scan.DEFAULT_DELIMITER, LOGGER);
        DocumentFactory factory = collection.factory();


        LOGGER.info(String.format("Term processor class is %s", toolchain.termProcessor.getClass()));

        int[] fields = new int[fieldNames.size()];
        DocumentFactory.FieldType[] types = new DocumentFactory.FieldType[fields.length];
        for (int i = fields.length; --i >= 0; ) {
            fields[i] = factory.fieldIndex(fieldNames.get(i));
            types[i] = factory.fieldType(i);
        }

        if (documents == null) {
            final DocumentIterator iterator = collection.iterator();
            long docid = 0;
            for (Document document = iterator.nextDocument(); document != null; document = iterator.nextDocument()) {
                outputDocument(fields, types, docid, document);
                docid++;
            }
        } else {
            try (Stream<String> lines = documents.getName().equals("") ?
                 new BufferedReader(new InputStreamReader(System.in)).lines() : Files.lines(documents.toPath())) {
                lines.map(Long::parseLong).sorted().forEach(
                        index -> {
                            try {
                                outputDocument(fields, types, index, collection.document(index));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

            }
        }
        return r;
    }

    private void outputDocument(int[] fields, DocumentFactory.FieldType[] types, long docid, Document document) throws IOException {
        MutableString word = new MutableString();
        MutableString delimiter = new MutableString();
        System.out.format("%d\t%s", docid, document.uri());
        for (int i = 0; i < fields.length; i++) {
            final Object content = document.content(0);
            switch (types[i]) {
                case TEXT: {
                    toolchain.wordReader.setReader((FastBufferedReader) content);
                    while (toolchain.wordReader.next(word, delimiter) && toolchain.termProcessor.processTerm(word)) {
                        if (word != null && !word.isEmpty()) {
                            System.out.print('\t');
                            System.out.print(word);
                        }
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Cannot handle type " + types[i]);
            }
        }
        System.out.println();
    }
}
