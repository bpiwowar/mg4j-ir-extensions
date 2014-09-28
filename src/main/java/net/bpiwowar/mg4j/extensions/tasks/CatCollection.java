package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.handlers.XStreamHandler;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import it.unimi.di.big.mg4j.document.*;
import it.unimi.di.big.mg4j.index.NullTermProcessor;
import it.unimi.di.big.mg4j.index.TermProcessor;
import it.unimi.di.big.mg4j.tool.Scan;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Wrapper for Index in MG4J
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/7/12
 */
@TaskDescription(name = "cat", project = {"ir", "mg4j"}, description = "Outputs a document collection as a stream")
public class CatCollection extends AbstractTask {
    static final Logger LOGGER = LoggerFactory.getLogger(CatCollection.class);

    @Argument(name = "collection-sequence", help = "Sequence to index", required = true)
    String sequence;

    @Argument(name = "term-processor", help = "An XML serialized form of the term processor (default: no processor)", handler = XStreamHandler.class, group = "Options")
    TermProcessor termProcessor = NullTermProcessor.getInstance();

    @Argument(name = "field-names", help="The fields to output", required = true)
    ArrayList<String> fieldNames = new ArrayList<>();

    @Override
    public int execute() throws Throwable {
        DocumentCollection collection = (DocumentCollection) Scan.getSequence(this.sequence,
                IdentityDocumentFactory.class, new String[]{},
                Scan.DEFAULT_DELIMITER, LOGGER);
        DocumentFactory factory = collection.factory();

        LOGGER.info(String.format("Term processor class is %s", termProcessor.getClass()));

        int[] fields = new int[fieldNames.size()];
        DocumentFactory.FieldType[] types = new DocumentFactory.FieldType[fields.length];
        for(int i = fields.length; --i >= 0; ) {
            fields[i] = factory.fieldIndex(fieldNames.get(i));
            types[i] = factory.fieldType(i);
        }

        MutableString s = new MutableString();
        final DocumentIterator iterator = collection.iterator();
        for(Document document = iterator.nextDocument(); document != null; document = iterator.nextDocument()) {
            System.out.format("Document %s", document.uri());

            for(int i = 0; i < fields.length; i++) {
                final Object content = document.content(0);
                switch(types[i]) {
                    case TEXT:
                        final FastBufferedReader reader = (FastBufferedReader) content;
                        while ((s = reader.readLine(s)) != null) {
                            System.out.println(s);
                        }
                        break;
                    default:
                        throw new RuntimeException("Cannot handle type " + types[i]);
                }

            }

        }

        return 0;
    }
}
