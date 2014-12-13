package net.bpiwowar.mg4j.extensions.query;

import bpiwowar.argparser.ListAdaptator;

import java.util.List;

/**
 * This implements a simple topic which is just a string
 *
 * @author <a mailto="ingo@dcs.gla.ac.uk">Ingo Frommholz</a>
 */
public class SimpleTopic implements Topic {

    /**
     * The topic id
     */
    private String id = "NN";

    /**
     * The query
     */
    private final String query;

    /**
     * The list of types in the topic description
     */
    final static List<String> typeList = ListAdaptator.create(new String[]{
            "simple"});

    /**
     * Provide query, take default id
     *
     * @param query the query
     */
    public SimpleTopic(String query) {
        this.query = query;
    }

    /**
     * Provide query and id
     *
     * @param query
     * @param id
     */
    public SimpleTopic(String query, String id) {
        this(query);
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Query getTopicPart(String type) {
        // we ignore the type
        return new StringQuery(query);
    }

    @Override
    public List<String> getTypes() {
        return typeList;
    }

}
