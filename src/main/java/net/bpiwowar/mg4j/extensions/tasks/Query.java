package net.bpiwowar.mg4j.extensions.tasks;

import net.bpiwowar.experimaestro.tasks.JsonArgument;

import static java.lang.String.format;

/**
* Created by bpiwowar on 14/10/14.
*/
public class Query {
    @JsonArgument
    String id;

    @JsonArgument
    String query;

    @Override
    public String toString() {
        return format("Query %s [%s]", id, query);
    }
}
