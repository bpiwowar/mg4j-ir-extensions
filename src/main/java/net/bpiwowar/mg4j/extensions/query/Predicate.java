package net.bpiwowar.mg4j.extensions.query;

import java.util.Set;

/**
 * A predicate in NEXI, i.e. represents what is between brackets
 * <code>[A and B]</code>
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Predicate {
	abstract public void addAllTerms(Set<String> set);
}
