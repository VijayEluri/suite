package suite.lp.doer;

import java.util.HashMap;
import java.util.Map;

import suite.lp.kb.Rule;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.util.IdHashKey;

public class Cloner {

	private Map<IdHashKey, Node> clonedNodes = new HashMap<>();

	public Rule clone(Rule rule) {
		return new Rule(clone(rule.getHead()), clone(rule.getTail()));
	}

	public Node clone(Node node) {
		return clonedNodes.computeIfAbsent(new IdHashKey(node), key -> {
			Tree tree = Tree.of(null, null, node);
			cloneRight(tree);
			return tree.getRight();
		});
	}

	private void cloneRight(Tree tree) {
		while (tree != null) {
			Tree nextTree = null;
			Node right = tree.getRight().finalNode();
			IdHashKey key = new IdHashKey(right);
			Node right1 = clonedNodes.get(key);

			if (right1 == null) {
				if (right instanceof Reference)
					right1 = new Reference();
				else if (right instanceof Tree) {
					nextTree = (Tree) right;
					right1 = nextTree = Tree.of(nextTree.getOperator(), clone(nextTree.getLeft()), nextTree.getRight());
				} else
					right1 = right;

				clonedNodes.put(key, right1);
			}

			Tree.forceSetRight(tree, right1);
			tree = nextTree;
		}
	}

	public Node cloneOld(Node node) {
		node = node.finalNode();
		IdHashKey key = new IdHashKey(node);
		Node node1 = clonedNodes.get(key);

		if (node1 == null) {
			if (node instanceof Reference)
				node1 = new Reference();
			else if (node instanceof Tree) {
				Tree tree = (Tree) node;
				Node left = tree.getLeft(), right = tree.getRight();
				Node left1 = clone(left), right1 = clone(right);
				if (left != left1 || right != right1)
					node1 = Tree.of(tree.getOperator(), left1, right1);
				else
					node1 = node;
			} else
				node1 = node;

			clonedNodes.put(key, node1);
		}

		return node1;
	}

}
