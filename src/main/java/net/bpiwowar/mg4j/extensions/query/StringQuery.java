package net.bpiwowar.mg4j.extensions.query;

import org.apache.commons.lang.NotImplementedException;

import java.util.Set;

public class StringQuery extends Query {
	/**
	 * Our string query
	 */
	private final String query;

	public StringQuery(String query) {
		this.query = query;
	}
	
	public String getQuery() {
		return query;
	}

	@Override
	public void addTerms(Set<String> set) {
		throw new NotImplementedException();
	}
}
