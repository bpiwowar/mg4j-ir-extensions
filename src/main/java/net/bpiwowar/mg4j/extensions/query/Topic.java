package net.bpiwowar.mg4j.extensions.query;

/**
 * An interface for a topic. Topics may consist of several parts
 * describing the information need. These parts may for instance be a query
 * or query reformulation as well as a narrative.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface Topic {
    /**
     * Gets the topic ID
     *
     * @return the topic ID
     */
    String getId();

    /**
     * Gets a CO representation of the given topic part/type which can be
     * used as the actual query
     *
     * @param type the topic part/type
     * @return a {@link Query} object representing the topic part, or null if no such object exists
     */
    Query getTopicPart(QueryType type);
}
