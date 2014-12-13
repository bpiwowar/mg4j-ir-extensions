package net.bpiwowar.mg4j.extensions.query;

import java.util.Set;


public class Term extends Text {
    /**
     * A term
     */
    public String word;

    public Term(SimpleQuery.Operator operator, String word) {
        super(operator);
        this.word = word;
    }


    public Term(String word) {
        super();
        this.word = word;
    }

    public Term() {
    }

    public Term(SimpleQuery.Term term) {
        super(term.getOperator());
        word = term.getTerm();
    }

    @Override
    public String toString() {
        return super.toString() + word;
    }

    @Override
    public void addAllTerms(Set<String> set) {
        set.add(word);
    }
}
