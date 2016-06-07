package net.bpiwowar.mg4j.extensions.tasks;

import it.unimi.di.big.mg4j.document.DocumentCollection;
import net.bpiwowar.mg4j.extensions.Utils;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.JsonType;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * A collection resource
 */
@JsonType(type = "mg4j:collection", resource = true)
public class Collection {
    @JsonArgument()
    File path;

    transient DocumentCollection self = null;

    synchronized public DocumentCollection get() throws IllegalAccessException, IOException, InstantiationException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        if (self == null) {
            self = Utils.getDocumentCollection(path);
        }
        return self;

    }
}
