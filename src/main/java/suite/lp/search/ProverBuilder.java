package suite.lp.search;

import static suite.util.Friends.fail;

import java.util.ArrayList;
import java.util.List;

import suite.lp.doer.Cloner;
import suite.lp.kb.RuleSet;
import suite.node.Node;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.util.To;

public class ProverBuilder {

	public interface Builder {
		public Fun<Node, Finder> build(RuleSet ruleSet);
	}

	public interface Finder {
		public void find(Source<Node> source, Sink<Node> sink);

		public default Node collectSingle(Node in) {
			var list = collectList(in);
			var size = list.size();
			if (size == 1)
				return list.get(0);
			else
				return fail(0 < size ? "too many results" : "no result");
		}

		public default List<Node> collectList(Node in) {
			var nodes = new ArrayList<Node>();
			find(To.source(in), node -> nodes.add(new Cloner().clone(node)));
			return nodes;
		}

		public default Source<Node> collect(Node in) {
			var source = To.source(in);
			return FunUtil.suck(sink0 -> find(source, node -> sink0.f(new Cloner().clone(node))));
		}
	}

}
