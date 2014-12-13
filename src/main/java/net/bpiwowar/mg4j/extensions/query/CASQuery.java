package net.bpiwowar.mg4j.extensions.query;

import bpiwowar.argparser.utils.Output;

import java.util.ArrayList;
import java.util.Set;

/**
 * A content and structure query (only contains descendant steps for the moment)
 *
 * @author bpiwowar
 */
public class CASQuery extends Query {
    public ArrayList<DescendantStep> steps = new ArrayList<DescendantStep>();

    @Override
    public String toString() {
        return Output.toString("//", steps);
    }

    @Override
    public void addTerms(Set<String> set) {
        for (DescendantStep ds : steps)
            if (ds.predicate != null)
                ds.predicate.addAllTerms(set);
    }
}
