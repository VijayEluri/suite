package suite.lp.kb;

import java.util.List;

import suite.node.Node;
import suite.node.util.Singleton;
import suite.util.Util;

public class CompositeRuleSet implements RuleSet {

	private RuleSet first;
	private RuleSet second;

	public CompositeRuleSet(RuleSet first, RuleSet second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public void addRule(Rule rule) {
		second.addRule(rule);
	}

	@Override
	public void addRuleToFront(Rule rule) {
		second.addRuleToFront(rule);
	}

	@Override
	public void removeRule(Rule rule) {
		second.removeRule(rule);
	}

	/**
	 * If the override rule set contain rules for specified head, return them.
	 *
	 * Otherwise return what we got from parent rule set.
	 */
	@Override
	public List<Rule> searchRule(Node head) {
		List<Rule> rules = second.searchRule(head);
		if (rules.isEmpty())
			rules = first.searchRule(head);
		return rules;
	}

	/**
	 * Returns everything we have anyway: override rule set and parent rule set.
	 */
	@Override
	public List<Rule> getRules() {
		return Util.add(first.getRules(), second.getRules());
	}

	@Override
	public boolean equals(Object object) {
		return Singleton.get().getInspectUtil().equals(this, object);
	}

	@Override
	public int hashCode() {
		return Singleton.get().getInspectUtil().hashCode(this);
	}

}
