package net.bpiwowar.mg4j.extensions;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.IdentityDocumentFactory;
import it.unimi.di.big.mg4j.index.Index;
import it.unimi.di.big.mg4j.index.IndexIterator;
import it.unimi.di.big.mg4j.index.IndexReader;
import it.unimi.di.big.mg4j.search.DocumentIterator;
import it.unimi.di.big.mg4j.tool.Scan;
import it.unimi.dsi.fastutil.objects.ObjectBigList;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import static java.lang.String.format;

/**
 * Created by bpiwowar on 30/12/15.
 */
public class Test {
    static public void main(String[] args) throws IllegalAccessException, IOException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException, URISyntaxException, ConfigurationException {
        File collectionPath = new File("/Users/bpiwowar/projects/learning-term-weights/out/mg4j.collection/c84d8afccb0419b61cd1b934398a1ee8");
        File indexPath = new File("/Users/bpiwowar/projects/learning-term-weights/out/mg4j.index/eeb0731a24a934ab10cb7b6479eb418a/");
        String field = "text";

        final DocumentCollection collection = (DocumentCollection) Scan.getSequence(new File(collectionPath, "collection").getAbsolutePath(),
                IdentityDocumentFactory.class, new String[]{},
                Scan.DEFAULT_DELIMITER, LoggerFactory.getLogger(Test.class));

        String basename = format("%s-%s", "index", field);
        Index index = Index.getInstance(new File(indexPath, basename).getAbsolutePath(), true, true);


        final IndexReader reader = index.getReader(10000);

        long docid;
        int position;

        final ObjectBigList<? extends CharSequence> list = index.termMap.list();

        final int termId = 3932;
        if (!index.hasPositions) {
            throw new RuntimeException("No positions!");
        }
        System.err.format("Term %d: %s%n", termId, list.get(termId));
        final IndexIterator documents = reader.documents(termId);
        int nb = 0;
        while ((docid = documents.nextDocument()) != DocumentIterator.END_OF_LIST) {
            System.err.format("--- Document %d (size %d)%n", docid, index.sizes.get(docid));
            while ((position = documents.nextPosition()) != IndexIterator.END_OF_POSITIONS) {
                System.err.format("Position %d%n", position);
            }
            if (++nb >= 50) break;
        }

    }
}
