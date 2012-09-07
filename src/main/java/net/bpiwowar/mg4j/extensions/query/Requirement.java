package net.bpiwowar.mg4j.extensions.query;

import bpiwowar.argparser.utils.Output;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;
import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Set;

/**
 * A requirement (conditions are separated by commas in the query)
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Requirement extends Query {
	private static final long serialVersionUID = 1L;

	public ArrayList<Text> terms = new ArrayList<Text>();

	/**
	 * Build from a SE query
	 * 
	 * @param query
	 *            The query to build from
	 */
	public Requirement(SimpleQuery seQuery) {
		for (SimpleQuery.Component c : seQuery.getSequence()) {
			if (c instanceof SimpleQuery.Term) {
				// FIXME: uses the same default reader as MG4J, but should be
				// adapted!
				FastBufferedReader reader = new FastBufferedReader();
				SimpleQuery.Term term = (SimpleQuery.Term) c;
				reader.setReader(new StringReader(term.getTerm()));
				MutableString word = new MutableString();
				MutableString nonWord = new MutableString();
				try {
					while (reader.next(word, nonWord)) {
						if (word.length() > 0)
							terms.add(new Term(term.getOperator(), word
									.toString()));
					}
				} catch (IOException e) {
					// Should not happen!
					throw new RuntimeException(e);
				}
			} else if (c instanceof SimpleQuery.Phrase)
				terms.add(new Phrase((SimpleQuery.Phrase) c));
			else
				throw new NotImplementedException();
		}
	}

	public Requirement() {
	}

	@Override
	public String toString() {
		return Output.toString(" ", terms);
	}

	@Override
	public void addTerms(Set<String> set) {
		for (Text text : terms)
			text.addAllTerms(set);
	}
}
