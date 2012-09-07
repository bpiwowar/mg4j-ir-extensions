package net.bpiwowar.mg4j.extensions.query;

import java.util.Set;


public class LogicalPredicate extends Predicate {
	public LogicalPredicate(Predicate left, Operator operator, Predicate right) {
		super();
		this.right = right;
		this.left = left;
		this.operator = operator;
	}

	public static enum Operator { AND, OR };
	
	public Operator operator;
	public Predicate left, right;
	
	@Override
	public String toString() {
		return String.format("%s %s %s", left, operator, right);
	}

	@Override
	public void addAllTerms(Set<String> set) {
		if (left != null)
			left.addAllTerms(set);
		if (right != null)
			right.addAllTerms(set);
	}
}
