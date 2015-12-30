package net.bpiwowar.mg4j.extensions.tasks;

import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.util.Properties;
import net.bpiwowar.experimaestro.tasks.AbstractTask;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.TaskDescription;
import net.bpiwowar.mg4j.extensions.Compression;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentCollection;
import net.bpiwowar.mg4j.extensions.trec.TRECDocumentCollection;
import net.bpiwowar.mg4j.extensions.trec.TRECDocumentFactory;
import net.bpiwowar.mg4j.extensions.warc.WARCDocumentCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sf.net.experimaestro.tasks.Path;

import javax.xml.namespace.NamespaceContext;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Builds a collection
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(id = "mg4j:collection", output = "mg4j:collection")
public class BuildCollection extends AbstractTask {
    public static final String IRCOLLECTIONS_NS = "http://ircollections.sourceforge.net";
    final static private Logger LOGGER = LoggerFactory.getLogger(BuildCollection.class);

    @JsonArgument(type = "irc:documents")
    DocumentsConfiguration documents;

    @Path(copy = "path")
    File collection;

    @Override
    public JsonObject execute(JsonObject r) throws Throwable {
        // Create the file now
        OutputStream out = new FileOutputStream(collection);

        // Read the IR collection file
        final Compression compression = Compression.fromString(documents.compression);
        LOGGER.info(String.format("Format is %s, compression  is %s", documents.format, compression));

        // Get the files
        LOGGER.info("Reading the file list");
        System.out.format("Document path: %s%n", documents.path);
        ArrayList<String> list = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(documents.path)));
        String s;
        while ((s = reader.readLine()) != null)
            list.add(s);
        String[] files = list.toArray(new String[list.size()]);
        Arrays.sort(files);


        final DocumentCollection collection;
        String docType = documents.format;
        File metadataFile = new File(this.collection.getAbsolutePath() + ".metadata");

        switch (docType) {
            case "trec":
                Properties properties = new Properties();
                properties.setProperty(PropertyBasedDocumentFactory.MetadataKeys.ENCODING, "UTF-8");
                final TRECDocumentFactory documentFactory = new TRECDocumentFactory(properties);

                collection = new TRECDocumentCollection(files,
                        documentFactory, SegmentedDocumentCollection.DEFAULT_BUFFER_SIZE, compression, metadataFile);
                break;

            case "warc/0.18":
                collection = new WARCDocumentCollection(files, SegmentedDocumentCollection.DEFAULT_BUFFER_SIZE, compression, metadataFile);
                break;
            default:
                LOGGER.error(String.format("Unknown document type [%s]", docType));
                System.exit(-1);
                throw new AssertionError();
        }

        // Store the collection
        BinIO.storeObject(collection, out);

        LOGGER.info("Found {} documents in the collection", collection.size());
        return null;
    }

    public static class XPMPathAdapter extends TypeAdapter<String> {
        @Override
        public void write(JsonWriter jsonWriter, String s) throws IOException {

        }

        @Override
        public String read(JsonReader jsonReader) throws IOException {
            final JsonToken token = jsonReader.peek();
            String s = null;
            switch (token) {
                case BEGIN_OBJECT:
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        final String key = jsonReader.nextName();
                        if (!key.equals("$value")) {
                            jsonReader.skipValue();
                        } else {
                            s = jsonReader.nextString();
                        }
                    }
                    jsonReader.endObject();
                    return s;
                case STRING:
                    return jsonReader.nextString();
                default:
                    throw new RuntimeException("cannot read");
            }

        }
    }

    static public class DocumentsConfiguration {
        @JsonArgument(name = "path")
        @JsonAdapter(XPMPathAdapter.class)
        public String path;

        @JsonArgument(name = "$compression")
        String compression;

        @JsonArgument(name = "$format")
        String format;
    }

    private static class IRCNamespaces implements NamespaceContext {
        @Override
        public String getNamespaceURI(String s) {
            if (s.equals("irc"))
                return IRCOLLECTIONS_NS;
            return null;
        }

        @Override
        public String getPrefix(String s) {
            return null;
        }

        @Override
        public Iterator getPrefixes(String s) {
            return null;
        }
    }

}
