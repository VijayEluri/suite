package org.suite.kb;

import java.util.ArrayList;
import java.util.List;

import org.suite.doer.Cloner;
import org.suite.node.Node;

public class LinearRuleSet implements RuleSet {

	private List<Rule> rules = new ArrayList<>();

	protected LinearRuleSet() {
	}

	public static RuleSet create() {
		return new IndexedRuleSet();
	}

	@Override
	public void clear() {
		rules.clear();
	}

	@Override
	public void addRule(Rule rule) {
		rule = new Cloner().clone(rule);
		rules.add(rule);
	}

	@Override
	public void addRuleToFront(Rule rule) {
		rule = new Cloner().clone(rule);
		rules.add(0, rule);
	}

	@Override
	public void removeRule(Rule rule) {
		rules.remove(rule);
	}

	@Override
	public List<Rule> searchRule(Node node) {
		return rules;
	}

	@Override
	public List<Rule> getRules() {
		return rules;
	}

}
