package suite.lp.doer;

import suite.lp.Configuration.ProverConfig;
import suite.node.Node;

public interface ProverFactory {

	public Prove_ prover(Node node);

	public interface Prove_ {
		public boolean test(ProverConfig pc);
	}

}
