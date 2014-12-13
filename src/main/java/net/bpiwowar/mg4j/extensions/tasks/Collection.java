package net.bpiwowar.mg4j.extensions.tasks;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import it.unimi.di.big.mg4j.document.IdentityDocumentFactory;
import it.unimi.di.big.mg4j.tool.Scan;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import sf.net.experimaestro.tasks.Type;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * A collection resource
 */
@Type(type = "mg4j:collection", resource = true)
public class Collection {
    @JsonArgument()
    File path;

    DocumentCollection get() throws IllegalAccessException, IOException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        return (DocumentCollection) Scan.getSequence(path.getAbsolutePath(),
                IdentityDocumentFactory.class, new String[]{},
                Scan.DEFAULT_DELIMITER, org.slf4j.LoggerFactory.getLogger(Collection.class));

    }
}
