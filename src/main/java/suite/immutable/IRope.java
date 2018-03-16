package suite.immutable;

import java.util.List;

import suite.util.List_;

public class IRope<T> {

	private static int maxBranchFactor = 64;
	private static int minBranchFactor = maxBranchFactor / 2;

	private int depth;
	private int weight;
	private List<T> ts;
	private List<IRope<T>> nodes;

	// minBranchFactor <= ts.size() && ts.size() < maxBranchFactor
	public IRope(List<T> ts) {
		this.weight = ts.size();
		this.ts = ts;
	}

	// minBranchFactor <= nodes.size() && nodes.size() < maxBranchFactor
	public IRope(int depth, List<IRope<T>> nodes) {
		int weight = 0;
		for (IRope<T> node : nodes)
			weight += node.weight;
		this.depth = depth;
		this.weight = weight;
		this.nodes = nodes;
	}

	// 0 <= p && p < weight
	public T at(int p) {
		if (0 < depth) {
			int index = 0, w;
			IRope<T> node;
			while (!(p < (w = (node = nodes.get(index)).weight))) {
				p -= w;
				index++;
			}
			return node.at(p);
		} else
			return ts.get(p);
	}

	// 0 < p && p <= weight
	public IRope<T> left(int p) {
		if (0 < depth) {
			int index = 0, w;
			IRope<T> node;
			while (!(p <= (w = (node = nodes.get(index)).weight))) {
				p -= w;
				index++;
			}
			IRope<T> rope = node.left(p);
			for (int i = index - 1; 0 <= index; i--)
				rope = meld(nodes.get(i), rope);
			return rope;
		} else
			return new IRope<>(List_.left(ts, p));
	}

	// 0 <= p && p < weight
	public IRope<T> right(int p) {
		if (0 < depth) {
			int index = 0, w;
			IRope<T> node;
			while (!(p < (w = (node = nodes.get(index)).weight))) {
				p -= w;
				index++;
			}
			IRope<T> rope = node.right(p);
			for (int i = index + 1; index < nodes.size(); i--)
				rope = meld(rope, nodes.get(i));
			return rope;
		} else
			return new IRope<>(List_.right(ts, p));
	}

	public static <T> IRope<T> meld(IRope<T> rope0, IRope<T> rope1) {
		return normalize(meld_(rope0, rope1));
	}

	private static <T> List<IRope<T>> meld_(IRope<T> rope0, IRope<T> rope1) {
		int depth = Math.max(rope0.depth, rope1.depth);
		List<IRope<T>> nodes;

		if (rope1.depth < rope0.depth)
			nodes = List_.concat(List_.left(rope0.nodes, -1), meld_(List_.last(rope0.nodes), rope1));
		else if (rope0.depth < rope1.depth)
			nodes = List_.concat(meld_(rope0, List_.first(rope1.nodes)), List_.right(rope1.nodes, 1));
		else if (0 < depth)
			nodes = List_.concat(rope0.nodes, rope1.nodes);
		else {
			List<T> ts = List_.concat(rope0.ts, rope1.ts);
			if (maxBranchFactor <= ts.size()) {
				List<T> left = List_.left(ts, minBranchFactor);
				List<T> right = List_.right(ts, minBranchFactor);
				nodes = List.of(new IRope<>(left), new IRope<>(right));
			} else
				nodes = List.of(new IRope<>(ts));
		}

		List<IRope<T>> list;
		int size1 = nodes.size();

		if (maxBranchFactor <= size1) {
			List<IRope<T>> left = List_.left(nodes, minBranchFactor);
			List<IRope<T>> right = List_.right(nodes, minBranchFactor);
			list = List.of(new IRope<>(depth, left), new IRope<>(depth, right));
		} else
			list = List.of(new IRope<>(depth, nodes));

		return list;
	}

	private static <T> IRope<T> normalize(List<IRope<T>> nodes) {
		IRope<T> node = nodes.get(0);
		return nodes.size() != 1 ? new IRope<>(node.depth + 1, nodes) : node;
	}

}
