package net.bpiwowar.mg4j.extensions.query;

import bpiwowar.argparser.utils.Output;

import java.util.ArrayList;
import java.util.Set;

public class AboutPredicate extends Predicate {
    /**
     * List of steps (null if current element)
     */
    public ArrayList<DescendantStep> steps = new ArrayList<DescendantStep>();

    /**
     * Text condition
     */
    public COQuery text;

    public AboutPredicate(ArrayList<DescendantStep> steps, COQuery text) {
        super();
        this.steps = steps;
        this.text = text;
    }


    @Override
    public String toString() {
        return String.format("about(%s%s, %s)", steps == null ? "." : ".//", steps == null ? "" : Output.toString("//", steps), text);
    }


    @Override
    public void addAllTerms(Set<String> set) {
        if (text != null)
            text.addTerms(set);
    }


}
