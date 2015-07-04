package suite.lp.doer;

import java.util.HashMap;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.lp.kb.Rule;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Rewriter;

public class Cloner {

	private Map<IdentityKey<Node>, Node> clonedNodes = new HashMap<>();

	public Rule clone(Rule rule) {
		return new Rule(clone(rule.head), clone(rule.tail));
	}

	public Node clone(Node node) {
		Tree tree = Tree.of(null, null, node);
		cloneRight(tree);
		return tree.getRight();
	}

	private void cloneRight(Tree tree) {
		while (tree != null) {
			Tree nextTree = null;
			Node right = tree.getRight();
			IdentityKey<Node> key = IdentityKey.of(right);
			Node right1 = clonedNodes.get(key);
			Tree rt;

			if (right1 == null) {
				if (right instanceof Reference)
					right1 = new Reference();
				else if ((rt = Tree.decompose(right)) != null)
					right1 = nextTree = Tree.of(rt.getOperator(), clone(rt.getLeft()), rt.getRight());
				else
					right1 = Rewriter.transform(right, this::clone);

				clonedNodes.put(key, right1);
			}

			Tree.forceSetRight(tree, right1);
			tree = nextTree;
		}
	}

	public Node cloneOld(Node node) {
		return clonedNodes.computeIfAbsent(IdentityKey.of(node), key -> {
			Node node_ = key.key;

			if (node_ instanceof Reference)
				node_ = new Reference();
			else if (node_ instanceof Tree) {
				Tree tree = (Tree) node_;
				Node left = tree.getLeft(), right = tree.getRight();
				Node left1 = cloneOld(left), right1 = cloneOld(right);
				if (left != left1 || right != right1)
					node_ = Tree.of(tree.getOperator(), left1, right1);
			} else
				node_ = Rewriter.transform(node, this::cloneOld);

			return node_;
		});
	}

}
