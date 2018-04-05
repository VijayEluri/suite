package suite.lp.search;

import suite.lp.Configuration.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.ProverBuilder.Finder;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.lp.sewing.impl.SewingProverImpl;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public class SewingProverBuilder implements Builder {

	private ProverConfig proverConfig;

	public SewingProverBuilder() {
		this(new ProverConfig());
	}

	public SewingProverBuilder(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

	@Override
	public Fun<Node, Finder> build(RuleSet ruleSet) {
		var sewingProver = new SewingProverImpl(ruleSet);

		return goal -> {
			var goal1 = SewingGeneralizerImpl.generalize(goal);
			var pred = sewingProver.prover(goal1);

			return (source, sink) -> {
				var proverConfig1 = new ProverConfig(ruleSet, proverConfig);
				proverConfig1.setSource(source);
				proverConfig1.setSink(sink);
				pred.test(proverConfig1);
			};
		};
	}

}
