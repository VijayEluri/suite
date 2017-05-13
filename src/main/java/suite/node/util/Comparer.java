package suite.node.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.tree.TreeAnd;
import suite.node.tree.TreeOp;
import suite.node.tree.TreeOr;
import suite.node.tree.TreeTuple;
import suite.streamlet.Read;
import suite.util.Object_;

public class Comparer implements Comparator<Node> {

	public static Comparer comparer = new Comparer();

	private static Map<Class<? extends Node>, Integer> order = new HashMap<>();
	static {
		order.put(Reference.class, 0);
		order.put(Int.class, 10);
		order.put(Atom.class, 20);
		order.put(Str.class, 30);
		order.put(TreeAnd.class, 40);
		order.put(TreeOp.class, 40);
		order.put(TreeOr.class, 40);
		order.put(TreeTuple.class, 40);
		order.put(Tuple.class, 50);
		order.put(Dict.class, 60);
	}

	@Override
	public int compare(Node n0, Node n1) {
		n0 = n0.finalNode();
		n1 = n1.finalNode();
		Class<? extends Node> clazz0 = n0.getClass();
		Class<? extends Node> clazz1 = n1.getClass();
		int c = Integer.compare(order.get(clazz0), order.get(clazz1));

		if (c == 0)
			if (clazz0 == Atom.class)
				return ((Atom) n0).name.compareTo(((Atom) n1).name);
			else if (clazz0 == Dict.class) {
				Map<Node, Reference> m0 = ((Dict) n0).map;
				Map<Node, Reference> m1 = ((Dict) n1).map;
				Set<Node> keys = new HashSet<>();
				keys.addAll(m0.keySet());
				keys.addAll(m1.keySet());
				for (Node key : Read.from(keys).sort(this::compare))
					c = c != 0 ? c : Object_.compare(m0.get(key), m1.get(key));
				return c;
			} else if (clazz0 == Int.class)
				return Integer.compare(((Int) n0).number, ((Int) n1).number);
			else if (clazz0 == Reference.class)
				return Integer.compare(((Reference) n0).getId(), ((Reference) n1).getId());
			else if (clazz0 == Str.class)
				return ((Str) n0).value.compareTo(((Str) n1).value);
			else if (Tree.class.isAssignableFrom(clazz0)) {
				Tree t0 = (Tree) n0;
				Tree t1 = (Tree) n1;
				c = t0.getOperator().getPrecedence() - t1.getOperator().getPrecedence();
				c = c != 0 ? c : compare(t0.getLeft(), t1.getLeft());
				c = c != 0 ? c : compare(t0.getRight(), t1.getRight());
				return c;
			} else if (clazz0 == Tuple.class) {
				Node[] nodes0 = ((Tuple) n0).nodes;
				Node[] nodes1 = ((Tuple) n1).nodes;
				int i = 0, l = Math.min(nodes0.length, nodes1.length);
				while (c == 0 && i < l)
					c = compare(nodes0[i], nodes1[i]);
				if (c == 0)
					c = Integer.compare(nodes0.length, nodes1.length);
				return c;

			} else
				return Integer.compare(n0.hashCode(), n1.hashCode());
		else
			return c;
	}

}
