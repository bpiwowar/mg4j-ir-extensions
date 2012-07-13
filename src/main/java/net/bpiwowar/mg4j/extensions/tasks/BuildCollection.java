package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import it.unimi.di.big.mg4j.document.PropertyBasedDocumentFactory;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.util.Properties;
import net.bpiwowar.mg4j.extensions.Compression;
import net.bpiwowar.mg4j.extensions.trec.TRECDocumentCollection;
import net.bpiwowar.mg4j.extensions.trec.TRECDocumentFactory;
import net.bpiwowar.mg4j.extensions.warc.WARCDocumentCollection;
import net.bpiwowar.mg4j.extensions.warc.WARCDocumentFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * Builds a collection
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(name = "build-collection", project = {"mg4j", "extensions"})
public class BuildCollection extends AbstractTask {
    final static private Logger LOGGER = Logger.getLogger(BuildCollection.class);

    @Argument(name = "out", help = "The output file", required = true)
    File output;

    @Argument(name = "configuration", checkers = IOChecker.Readable.class, help = "XML configuration file from ir.collections")
    File configuration;

    @Override
    public int execute() throws Throwable {
        // Create the file now
        OutputStream out = new FileOutputStream(output);

        // Read the IR collection file
        LOGGER.info("Reading the IR collections file");
        final InputStream stream = configuration != null ? new FileInputStream(configuration) : System.in;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(stream);

        LOGGER.info(String.format("Root is %s", document.getDocumentElement().getNamespaceURI()));
        // Get the document definition
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new IRCNamespaces());
        Element documents = (Element) xpath.evaluate("//irc:documents", document, XPathConstants.NODE);
        if (documents == null) {
            LOGGER.error("Could not find the document collection information");
            return 1;
        }

        // Get the compression
        Compression compression = Compression.fromString(documents.getAttribute("compression"));

        // Get the files
        LOGGER.info("Reading the file list");
        final String path = documents.getAttribute("path");
        System.out.format("Document path: %s%n", path);
        ArrayList<String> list = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        String s;
        while ((s = reader.readLine()) != null)
            list.add(s);
        String[] files = (String[]) list.toArray(new String[list.size()]);
        Arrays.sort(files);


        // Choose the right document factory
        final String docType = documents.getAttribute("type");

        final TRECDocumentCollection collection;
        if (docType.equals("trec")) {
            Properties properties = new Properties();
            properties.setProperty(PropertyBasedDocumentFactory.MetadataKeys.ENCODING, "UTF-8");
            final TRECDocumentFactory documentFactory = new TRECDocumentFactory(properties);

            collection = new TRECDocumentCollection(files,
                    documentFactory, TRECDocumentCollection.DEFAULT_BUFFER_SIZE, compression);

        } else if (docType.equals("warc/0.18")) {
            collection = new WARCDocumentCollection(files,
                    new WARCDocumentFactory(), WARCDocumentCollection.DEFAULT_BUFFER_SIZE, compression);
        } else {
            LOGGER.error(String.format("Unkown document type [%s]", docType));
            System.exit(-1);
            throw new AssertionError();
        }

        // Store the collection
        BinIO.storeObject(collection, out);

        return 0;
    }

    private static class IRCNamespaces implements NamespaceContext {
        @Override
        public String getNamespaceURI(String s) {
            if (s.equals("irc"))
                return "http://ircollections.sourceforge.net";
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
