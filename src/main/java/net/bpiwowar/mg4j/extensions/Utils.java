package net.bpiwowar.mg4j.extensions;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.IdentityDocumentFactory;
import it.unimi.di.big.mg4j.tool.Scan;
import net.bpiwowar.mg4j.extensions.tasks.Collection;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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

}
