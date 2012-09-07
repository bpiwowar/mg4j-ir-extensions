package net.bpiwowar.mg4j.extensions.query;

import bpiwowar.argparser.utils.Output;

import java.util.ArrayList;
import java.util.Set;

public class Phrase extends Text {
	public ArrayList<Term> terms = new ArrayList<Term>();
	
	public Phrase() {
		
	}

	public Phrase(ArrayList<Term> terms) {
		this.terms = terms;
	}

	public Phrase(SimpleQuery.Phrase c) {
		super(c.getOperator());
		for(SimpleQuery.Term t: c.getTerms()) {
			terms.add(new Term(t));
		}
	}

	@Override
	public String toString() {
		return String.format("%s\"%s\"", super.toString(), Output.toString(" ", terms));
	}

	@Override
	public void addAllTerms(Set<String> set) {
		for(Term term: terms)
			term.addAllTerms(set);
	}
}
