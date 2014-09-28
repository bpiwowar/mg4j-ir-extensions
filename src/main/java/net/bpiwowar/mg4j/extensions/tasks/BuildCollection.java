package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.util.Properties;
import net.bpiwowar.mg4j.extensions.Compression;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentCollection;
import net.bpiwowar.mg4j.extensions.trec.TRECDocumentCollection;
import net.bpiwowar.mg4j.extensions.trec.TRECDocumentFactory;
import net.bpiwowar.mg4j.extensions.warc.WARCDocumentCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@TaskDescription(name = "build-collection", project = {"mg4j", "extensions"})
public class BuildCollection extends AbstractTask {
    final static private Logger LOGGER = LoggerFactory.getLogger(BuildCollection.class);
    public static final String IRCOLLECTIONS_NS = "http://ircollections.sourceforge.net";

    @Argument(name = "out", help = "The output file", required = true)
    File output;

    @Argument(name = "configuration", checkers = IOChecker.Readable.class, help = "XML configuration file from ir.collections. If not given, uses the standard input.")
    File configuration;


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
                        if (!key.equals("$$value")) {
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
        @SerializedName("$compression")
        String compression;

        @SerializedName("$format")
        String format;

        @SerializedName("path")
        @JsonAdapter(XPMPathAdapter.class)
        public String path;
    }

    @Override
    public int execute() throws Throwable {
        // Create the file now
        OutputStream out = new FileOutputStream(output);

        // Read the IR collection file
        LOGGER.info("Reading the IR collections file");
        final InputStream stream = configuration != null ? new FileInputStream(configuration) : System.in;

        final Gson gson = new GsonBuilder()
                .create();
        final DocumentsConfiguration conf = gson.fromJson(new InputStreamReader(stream), DocumentsConfiguration.class);


        final Compression compression = Compression.fromString(conf.compression);
        LOGGER.info(String.format("Format is %s, compression  is %s", conf.format, compression));

        // Get the files
        LOGGER.info("Reading the file list");
        System.out.format("Document path: %s%n", conf.path);
        ArrayList<String> list = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(conf.path)));
        String s;
        while ((s = reader.readLine()) != null)
            list.add(s);
        String[] files = list.toArray(new String[list.size()]);
        Arrays.sort(files);


        final DocumentCollection collection;
        String docType = conf.format;
        File metadataFile = new File(output.getAbsolutePath() + ".metadata");

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
        return 0;
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
