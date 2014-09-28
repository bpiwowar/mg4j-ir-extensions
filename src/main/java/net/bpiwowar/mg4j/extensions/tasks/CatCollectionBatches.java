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
import it.unimi.dsi.io.WordReader;
import it.unimi.dsi.lang.MutableString;
import net.bpiwowar.mg4j.extensions.segmented.SegmentedDocumentCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static java.lang.StrictMath.random;

/**
 * Wrapper for Index in MG4J
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/7/12
 */
@TaskDescription(name = "cat-batch", project = {"ir", "mg4j"}, description = "Outputs a document collection as a stream")
public class CatCollectionBatches extends AbstractTask {
    static final Logger LOGGER = LoggerFactory.getLogger(CatCollectionBatches.class);

    @Argument(name = "collection", help = "Sequence to index", required = true)
    String sequence;

    @Argument(name = "term-processor", help = "An XML serialized form of the term processor (default: no processor)", handler = XStreamHandler.class, group = "Options")
    TermProcessor termProcessor = NullTermProcessor.getInstance();

    @Argument(name = "fields", help = "The fields to output", required = true)
    ArrayList<String> fieldNames = new ArrayList<>();

    @Argument(name = "word-reader")
    WordReaderType wordReaderType = WordReaderType.DEFAULT;

    @Argument(name = "batch-size", required = true, help = "Number of elements in each batch")
    long batchSize;

    @Override
    public int execute() throws Throwable {
        // -*- Loads the document collection
        DocumentCollection collection = (DocumentCollection) Scan.getSequence(this.sequence,
                IdentityDocumentFactory.class, new String[]{},
                Scan.DEFAULT_DELIMITER, LOGGER);
        DocumentFactory factory = collection.factory();

        final WordReader wordReader = wordReaderType.getWordReader();
        LOGGER.info(String.format("Term processor class is %s", termProcessor.getClass()));

        // -*- Get the fields
        int[] fields = new int[fieldNames.size()];
        DocumentFactory.FieldType[] types = new DocumentFactory.FieldType[fields.length];
        for (int i = fields.length; --i >= 0; ) {
            fields[i] = factory.fieldIndex(fieldNames.get(i));
            types[i] = factory.fieldType(i);
        }


        final long numberOfDocuments = collection.size();
        MutableString word = new MutableString();
        MutableString delimiter = new MutableString();

        while (true) {
            // Select a starting document
            final int start = (int) (Math.random() * (numberOfDocuments - batchSize));
            try(final DocumentIterator iterator =
                    collection instanceof SegmentedDocumentCollection ?
                            ((SegmentedDocumentCollection) collection).iterator(start)
                            : null) {

                for (int docid = start; docid < start + batchSize; docid++) {
                    final Document document =
                            iterator == null ?
                                    collection.document(docid)
                                    : iterator.nextDocument();

                    System.out.format("%d\t%s", docid, document.uri());
                    for (int i = 0; i < fields.length; i++) {
                        final Object content = document.content(0);
                        switch (types[i]) {
                            case TEXT: {
                                wordReader.setReader((FastBufferedReader) content);
                                while (wordReader.next(word, delimiter)) {
                                    System.out.print('\t');
                                    System.out.print(word);
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
        }

    }
}
