package net.bpiwowar.mg4j.extensions.query;

import bpiwowar.argparser.utils.Output;

import java.util.ArrayList;
import java.util.Set;

public class COQuery extends Query {
    private static final long serialVersionUID = 1L;

    public ArrayList<Requirement> requirements = new ArrayList<Requirement>();

    public void add(Requirement req) {
        requirements.add(req);
    }

    @Override
    public String toString() {
        return Output.toString(", ", requirements);
    }

    @Override
    public void addTerms(Set<String> set) {
        for (Requirement requirement : requirements)
            requirement.addTerms(set);
    }

    public void add(COQuery coQuery) {
        for (Requirement req : coQuery.requirements)
            requirements.add(req);
    }
}
