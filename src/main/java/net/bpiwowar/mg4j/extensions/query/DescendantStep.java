package net.bpiwowar.mg4j.extensions.query;

import bpiwowar.argparser.ListAdaptator;
import bpiwowar.argparser.utils.Output;

import java.util.HashSet;
import java.util.Set;

public class DescendantStep {
	/**
	 * Tag set (null if no constraint)
	 */
	public Set<String> tags;
	/**
	 * Predicate for this query (null if no predicate)
	 */
	public Predicate predicate;

	public DescendantStep() {
	}

	public DescendantStep(String... tags) {
		if (tags.length == 0) this.tags = null;
		else {
			this.tags = new HashSet<String>();
			this.tags.addAll(new ListAdaptator<String>(tags));
		}
	}

	@Override
	public String toString() {
		String s;
		if (tags == null)
			s = "*";
		if (tags.size() == 1)
			s = tags.iterator().next().toString();
		else
			s = String.format("(%s)", Output.toString("|", tags));

		if (predicate != null)
			return String.format("//%s[%s]", s, predicate
					.toString());
		return s;
	}
}
