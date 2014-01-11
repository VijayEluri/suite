package suite.immutable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;

import suite.util.FunUtil.Source;

/**
 * Immutable Red-Black tree implementation. However, node deletion is not
 * implemented.
 * 
 * @author ywsing
 */
public class RbTree<T> implements ImmutableTree<T> {

	private Node root;
	private Comparator<T> comparator;

	private class Node {
		private boolean isBlack;
		private T pivot;
		private Node left, right;

		private Node(boolean isBlack, T pivot, Node left, Node right) {
			this.isBlack = isBlack;
			this.pivot = pivot;
			this.left = left;
			this.right = right;
		}
	}

	public RbTree(Comparator<T> comparator) {
		this(comparator, null);
	}

	private RbTree(Comparator<T> comparator, Node root) {
		this.root = root;
		this.comparator = comparator;
	}

	@Override
	public Source<T> source() {
		return new Source<T>() {
			private Deque<Node> nodes = new ArrayDeque<>();

			{
				pushLefts(root);
			}

			public T source() {
				T result;

				if (!nodes.isEmpty()) {
					Node node = nodes.pop();
					pushLefts(node.right);
					result = node.pivot;
				} else
					result = null;

				return result;
			}

			private void pushLefts(Node node) {
				while (node != null) {
					nodes.push(node);
					node = node.left;
				}
			}
		};
	}

	public T find(T t) {
		Node node = root;

		while (node != null) {
			int c = comparator.compare(node.pivot, t);
			if (c < 0)
				node = node.left;
			else if (c > 0)
				node = node.right;
			else
				return node.pivot;
		}

		return null;
	}

	public RbTree<T> add(T t) {
		return add(t, false);
	}

	/**
	 * Replaces a value with another. Mainly for dictionary cases to replace
	 * stored value for the same key.
	 * 
	 * Asserts comparator.compare(<original-value>, t) == 0.
	 */
	public RbTree<T> replace(T t) {
		return add(t, true);
	}

	public RbTree<T> remove(T t) {
		throw new UnsupportedOperationException();
	}

	private RbTree<T> add(T t, boolean isReplace) {
		Node node = root;

		if (node != null && !node.isBlack) // Turns red node into black
			node = new Node(true, node.pivot, node.left, node.right);

		return new RbTree<>(comparator, add(node, t, isReplace));
	}

	private Node add(Node node, T t, boolean isReplace) {
		Node node1;

		if (node != null) {
			int c = comparator.compare(node.pivot, t);

			if (c < 0)
				node1 = new Node(node.isBlack, node.pivot, add(node.left, t, isReplace), node.right);
			else if (c > 0)
				node1 = new Node(node.isBlack, node.pivot, node.left, add(node.right, t, isReplace));
			else if (isReplace)
				node1 = new Node(node.isBlack, t, node.left, node.right);
			else
				throw new RuntimeException("Duplicate node " + t);
		} else
			node1 = new Node(false, t, null, null);

		return balance(node1);
	}

	private Node balance(Node n) {
		if (n.isBlack) {
			Node ln = n.left, rn = n.right;

			if (ln != null && !ln.isBlack) {
				Node lln = ln.left, lrn = ln.right;
				if (lln != null && !lln.isBlack)
					n = createBalancedNode(lln.left, lln.pivot, lln.right, ln.pivot, lrn, n.pivot, rn);
				else if (lrn != null && !lrn.isBlack)
					n = createBalancedNode(lln, ln.pivot, lrn.left, lrn.pivot, lrn.right, n.pivot, rn);
			} else if (rn != null && !rn.isBlack) {
				Node rln = rn.left, rrn = rn.right;
				if (rln != null && !rln.isBlack)
					n = createBalancedNode(ln, n.pivot, rln.left, rln.pivot, rln.right, rn.pivot, rrn);
				else if (rrn != null && !rrn.isBlack)
					n = createBalancedNode(ln, n.pivot, rln, rn.pivot, rrn.left, rrn.pivot, rrn.right);
			}
		}

		return n;
	}

	private Node createBalancedNode(Node n0, T p0, Node n1, T p1, Node n2, T p2, Node n3) {
		return new Node(false, p1, new Node(true, p0, n0, n1), new Node(true, p2, n2, n3));
	}

}
