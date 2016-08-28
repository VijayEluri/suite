package suite.node;

import java.util.List;

import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.tree.TreeAnd;
import suite.node.tree.TreeOp;
import suite.node.tree.TreeOr;
import suite.node.tree.TreeTuple;
import suite.streamlet.Outlet;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

public abstract class Tree extends Node {

	private Node left, right;

	public static Tree decompose(Node node) {
		return node instanceof Tree ? (Tree) node : null;
	}

	public static Tree decompose(Node node, Operator operator) {
		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			return tree.getOperator() == operator ? tree : null;
		}
		return null;
	}

	public static Streamlet<Node> iter(Node node) {
		return iter(node, TermOp.AND___);
	}

	public static Streamlet<Node> iter(Node node0, Operator operator) {
		return new Streamlet<>(() -> Outlet.from(new Source<Node>() {
			private Node node = node0;

			public Node source() {
				Tree tree = Tree.decompose(node, operator);
				if (tree != null) {
					node = tree.getRight();
					return tree.getLeft();
				} else
					return null;
			}
		}));
	}

	public static Node of(Operator operator, List<Node> nodes) {
		Node result = Atom.NIL;
		int i = nodes.size();
		while (0 <= --i)
			result = of(operator, nodes.get(i), result);
		return result;
	}

	public static Tree of(Operator operator, Node left, Node right) {
		if (operator == TermOp.AND___)
			return new TreeAnd(left, right);
		else if (operator == TermOp.OR____)
			return new TreeOr(left, right);
		else if (operator == TermOp.TUPLE_)
			return new TreeTuple(left, right);
		else
			return new TreeOp(operator, left, right);
	}

	protected Tree(Node left, Node right) {
		this.left = left;
		this.right = right;
	}

	// These methods violate the immutable property of the tree. Should only
	// used by cloner for performance purpose.
	public static void forceSetLeft(Tree tree, Node left) {
		tree.left = left;
	}

	public static void forceSetRight(Tree tree, Node right) {
		tree.right = right;
	}

	public abstract Operator getOperator();

	public Node getLeft() {
		return left.finalNode();
	}

	public Node getRight() {
		return right.finalNode();
	}

}
