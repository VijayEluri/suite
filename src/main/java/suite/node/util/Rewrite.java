package suite.node.util;

import java.util.function.Predicate;

import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Rewrite_;
import suite.node.io.Rewrite_.NodeRead;
import suite.streamlet.Read;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;

public class Rewrite {

	public boolean contains(Node from, Node node) {
		return new Predicate<Node>() {
			public boolean test(Node node) {
				boolean b;
				if (!eq(node, from)) {
					NodeRead nr = NodeRead.of(node);
					b = Read.from(nr.children).isAny(p -> test(p.t1));
				} else
					b = true;
				return b;
			}
		}.test(node);
	}

	public Node replace(Node from, Node to, Node node0) {
		return rewrite(n -> !eq(n, from) ? n : to, node0);
	}

	public Node rewrite(Node from, Node to, Node node0) {
		return rewrite(() -> {
			Cloner cloner = new Cloner();
			return new Node[] { cloner.clone(from), cloner.clone(to), };
		}, node0);
	}

	public Node rewrite(Source<Node[]> source, Node node) {
		Trail trail = new Trail();

		return rewrite(node0 -> {
			Node node1;
			if (!(node0 instanceof Reference)) {
				int pit = trail.getPointInTime();
				Node[] ft = source.source();

				if (Binder.bind(node0, ft[0], trail))
					node1 = ft[1];
				else {
					trail.unwind(pit);
					node1 = node0;
				}
			} else
				node1 = node0;
			return node1;
		}, node);
	}

	public Node rewrite(Iterate<Node> fun, Node node0) {
		return fun.apply(Rewrite_.map(node0, n -> rewrite(fun, n)));
	}

	private boolean eq(Node n0, Node n1) {
		return Comparer.comparer.compare(n0, n1) == 0;
	}

}