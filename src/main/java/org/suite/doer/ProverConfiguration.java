package org.suite.doer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.suite.SuiteUtil;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.RuleSetUtil;

public class ProverConfiguration {

	private RuleSet ruleSet;
	private boolean isTrace;
	private Set<String> noTracePredicates;

	public ProverConfiguration() {
		this(RuleSetUtil.create());
	}

	public ProverConfiguration(RuleSet ruleSet) {
		this(ruleSet //
				, SuiteUtil.isTrace //
				, new HashSet<>(Arrays.asList("member", "replace")));
	}

	public ProverConfiguration(ProverConfiguration pc) {
		this(pc.ruleSet, pc);
	}

	public ProverConfiguration(RuleSet ruleSet, ProverConfiguration pc) {
		this(ruleSet, pc.isTrace, pc.noTracePredicates);
	}

	public ProverConfiguration(RuleSet ruleSet //
			, boolean isTrace //
			, Set<String> noTracePredicates) {
		this.ruleSet = ruleSet;
		this.isTrace = isTrace;
		this.noTracePredicates = noTracePredicates;
	}

	public RuleSet ruleSet() {
		return ruleSet;
	}

	public void setRuleSet(RuleSet ruleSet) {
		this.ruleSet = ruleSet;
	}

	public boolean isTrace() {
		return isTrace;
	}

	public void setTrace(boolean isTrace) {
		this.isTrace = isTrace;
	}

	public Set<String> getNoTracePredicates() {
		return noTracePredicates;
	}

	public void setNoTracePredicates(Set<String> noTracePredicates) {
		this.noTracePredicates = noTracePredicates;
	}

}
