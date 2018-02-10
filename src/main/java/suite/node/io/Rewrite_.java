package suite.node.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.util.Comparer;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;

public class Rewrite_ {

	private static Node LEFT_ = Atom.of("l");
	private static Node RIGHT = Atom.of("r");

	public enum ReadType {
		DICT(0), TERM(1), TREE(2), TUPLE(3),;

		public byte value;

		public static ReadType of(byte value) {
			return Read.from(ReadType.values()).filter(rt -> rt.value == value).uniqueResult();
		}

		private ReadType(int value) {
			this.value = (byte) value;
		}
	};

	public static class NodeHead {
		public final ReadType type;
		public final Node terminal;
		public final Operator op;

		public NodeHead(ReadType type, Node terminal, Operator op) {
			this.type = type;
			this.terminal = terminal;
			this.op = op;
		}
	}

	public static class NodeRead extends NodeHead {
		public final List<Pair<Node, Node>> children;

		public static NodeRead of(Node node) {
			ReadType type;
			Node terminal;
			Operator op;
			List<Pair<Node, Node>> children;
			Tree tree;

			if (node instanceof Dict) {
				Map<Node, Reference> map = ((Dict) node).map;
				type = ReadType.DICT;
				terminal = null;
				op = null;
				children = Read.from2(map) //
						.sort((p0, p1) -> Comparer.comparer.compare(p0.t0, p1.t0)) //
						.mapValue(Node::finalNode) //
						.toList();
			} else if ((tree = Tree.decompose(node)) != null) {
				Pair<Node, Node> p0 = Pair.of(LEFT_, tree.getLeft());
				Pair<Node, Node> p1 = Pair.of(RIGHT, tree.getRight());
				type = ReadType.TREE;
				terminal = null;
				op = tree.getOperator();
				children = List.of(p0, p1);
			} else if (node instanceof Tuple) {
				Node[] nodes = ((Tuple) node).nodes;
				type = ReadType.TUPLE;
				terminal = null;
				op = null;
				children = Read.from(nodes).map(n -> Pair.<Node, Node> of(Atom.NIL, n.finalNode())).toList();
			} else {
				type = ReadType.TERM;
				terminal = node;
				op = null;
				children = List.of();
			}

			return new NodeRead(type, terminal, op, children);
		}

		private NodeRead(ReadType type, Node terminal, Operator op, List<Pair<Node, Node>> children) {
			super(type, terminal, op);
			this.children = children;
		}
	}

	public static class NodeWrite {
		public final Node node;

		public NodeWrite(ReadType type, Node terminal, Operator op, List<Pair<Node, Node>> children) {
			switch (type) {
			case DICT:
				node = new Dict(Read.from2(children).mapValue(Reference::of).collect(As::map));
				break;
			case TERM:
				node = terminal;
				break;
			case TREE:
				node = Tree.of(op, children.get(0).t1, children.get(1).t1);
				break;
			case TUPLE:
				node = Tuple.of(Read.from(children).map(p -> p.t1).toArray(Node.class));
				break;
			default:
				node = Fail.t();
			}
		}
	}

	public static Node map(Node node, Iterate<Node> fun) {
		NodeRead nr = NodeRead.of(node);
		List<Pair<Node, Node>> children1 = new ArrayList<>();
		boolean isSame = true;

		for (Pair<Node, Node> pair : nr.children) {
			Node child0 = pair.t1;
			Node childx = fun.apply(child0);
			if (child0 != childx) {
				isSame = false;
				children1.add(Pair.of(pair.t0, childx));
			} else
				children1.add(pair);
		}

		if (isSame)
			return node;
		else
			return new NodeWrite(nr.type, nr.terminal, nr.op, children1).node;
	}

}
