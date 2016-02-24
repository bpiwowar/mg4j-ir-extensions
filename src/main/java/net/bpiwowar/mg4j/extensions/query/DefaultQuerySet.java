package net.bpiwowar.mg4j.extensions.query;


import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Default query set implementation.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class DefaultQuerySet implements QuerySet, Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Topic> queries = new TreeMap<String, Topic>();

    public DefaultQuerySet() {
    }

    @Override
    public Map<String, ? extends Topic> queries() {
        return queries;
    }

    /**
     * Add a new {@link Topic} to the query set
     *
     * @param id    the topic ID
     * @param topic the {@link Topic} object to add
     */
    public void put(String id, Topic topic) {
        queries.put(id, topic);
    }

    public void add(Topic topic) {
        put(topic.getId(), topic);
    }
}
