package net.bpiwowar.mg4j.extensions.query;

/**
 * Query type
 */
public enum QueryType {
    /** The main query */
    TITLE,

    /** The description */
    DESCRIPTION;

    static QueryType from(String type) {
        switch(type) {
            case "title":
            case "query":
                return TITLE;

            case "desc":
            case "description":
                return DESCRIPTION;
        }
        return null;
    }
}
