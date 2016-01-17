package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import it.unimi.di.big.mg4j.document.Document;
import it.unimi.di.big.mg4j.document.DocumentFactory;
import it.unimi.di.big.mg4j.document.DocumentIterator;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.mg4j.extensions.utils.CollectionInformation;
import net.bpiwowar.mg4j.extensions.utils.Registry;
import net.bpiwowar.mg4j.extensions.utils.Streams;
import net.bpiwowar.mg4j.extensions.utils.TextToolChain;
import net.bpiwowar.xpm.manager.tasks.AbstractTask;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.ProgressListener;
import net.bpiwowar.xpm.manager.tasks.TaskDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Output a set of documents
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(id = "mg4j:cat", output = "xp:file", description = "Outputs one or more document collections as a stream",
        registry = Registry.class)
public class CatCollection extends AbstractTask {
    static final Logger LOGGER = LoggerFactory.getLogger(CatCollection.class);

    @JsonArgument(name = "collections", help = "Collections to output", required = true)
    ArrayList<Collection> collectionPaths;

    @JsonArgument(required = true)
    TextToolChain toolchain;

    @JsonArgument(name = "fields", help = "The fields to output", required = true)
    ArrayList<String> fieldNames = new ArrayList<>();

    @JsonArgument(help = "File with one document ID per line (empty = output all documents)")
    File documents;

    @JsonArgument(help = "JSON array of documents to output")
    ArrayList<String> ids = new ArrayList<>();

    @Override
    public JsonObject execute(JsonObject r, ProgressListener progress) throws Throwable {
        // Get the list of collections
        CollectionInformation[] collections = this.collectionPaths.stream()
                .map(Streams.propagateFunction(c -> new CollectionInformation(c, fieldNames)))
                .toArray(n -> new CollectionInformation[n]);


        LOGGER.info(String.format("Term processor class is %s", toolchain.termProcessor.getClass()));

        if (documents == null && ids.isEmpty()) {
            // We just output everything
            for (CollectionInformation collection : collections) {
                try (final DocumentIterator iterator = collection.iterator()) {
                    long docid = 0;
                    for (Document document = iterator.nextDocument(); document != null; document = iterator.nextDocument()) {
                        outputDocument(collection.fields, collection.types, docid, document);
                        docid++;
                    }
                }
            }

        } else {
            if (collections.length != 1) {
                throw new IllegalArgumentException("Outputing specific documents requires having only one collection");
            }

            CollectionInformation collection = collections[0];
            // Loop over document IDs

            ArrayList<Stream<String>> streams = new ArrayList<>();

            if (!documents.getName().equals("")) {
                streams.add(Files.lines(documents.toPath()));
            }

            if (!ids.isEmpty()) {
                streams.add(ids.stream());
            }

            final Stream<String> docIdStream = streams.stream().reduce(Stream::concat)
                    .orElse(new BufferedReader(new InputStreamReader(System.in)).lines());

            try (Stream<String> lines = docIdStream) {
                lines.map(Long::parseLong)
                        .sorted() // Sort in order to minimize random seeking
                        .forEach(
                                index -> {
                                    try {
                                        outputDocument(collection.fields, collection.types, index, collection.document(index));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        );

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
