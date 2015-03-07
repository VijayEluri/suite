package suite.lp.doer;

import java.util.HashMap;
import java.util.Map;

import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Rewriter;

public class Generalizer {

	private Map<Node, Reference> variables = new HashMap<>();

	public Node generalize(Node node) {
		Tree tree = Tree.of(null, null, node);
		generalizeRight(tree);
		return tree.getRight();
	}

	private void generalizeRight(Tree tree) {
		while (tree != null) {
			Tree nextTree = null;
			Node right = tree.getRight().finalNode();
			Tree rt;

			if (right instanceof Atom) {
				String name = ((Atom) right).name;
				if (name.startsWith(SewingGeneralizer.wildcardPrefix))
					right = new Reference();
				if (name.startsWith(SewingGeneralizer.variablePrefix))
					right = getVariable(right);
			} else if ((rt = Tree.decompose(right)) != null)
				right = nextTree = Tree.of(rt.getOperator(), generalize(rt.getLeft()), rt.getRight());
			else
				right = Rewriter.transform(right, this::generalize);

			Tree.forceSetRight(tree, right);
			tree = nextTree;
		}
	}

	public Reference getVariable(Node variable) {
		return variables.computeIfAbsent(variable, any -> new Reference());
	}

}
