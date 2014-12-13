/**
 *
 */
package net.bpiwowar.mg4j.extensions.conf;

import it.unimi.di.big.mg4j.index.TermProcessor;
import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.mg4j.extensions.tasks.Collection;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

final public class IndexedCollection {
    final static private Logger LOGGER = Logger.getLogger(IndexedCollection.class);

    @JsonArgument(name = "path", help = "Index basename", required = true)
    public File path;

    @JsonArgument(required = true)
    TermProcessor term_processor;

    @JsonArgument(name = "collection")
    Collection collection;

    transient public Map<String, IndexedField> indices = new HashMap<>();

    public IndexedCollection() {
    }


    /**
     * Initialise the index
     */
    public IndexedField get(String field) throws Exception {
        IndexedField index = indices.get(field);
        if (index != null)
            return index;

        index = new IndexedField(path, field);
        indices.put(field, index);
        return index;
    }


    public Collection getCollection() {
        return collection;
    }

    public TermProcessor getTerm_processor() {
        return term_processor;
    }
}