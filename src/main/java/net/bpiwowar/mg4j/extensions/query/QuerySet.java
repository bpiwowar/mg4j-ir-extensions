package net.bpiwowar.mg4j.extensions.query;

import java.util.Map;

/**
 * A set of topics indexed by ID
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface QuerySet {
    /**
     * Returns a set of queries as a map of topic ID and {@link Topic}.
     */
    Map<String, ? extends Topic> queries();

}
