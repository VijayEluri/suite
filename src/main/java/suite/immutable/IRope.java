package suite.immutable;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import suite.primitive.IntInt_Obj;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;
import suite.util.List_;

public class IRope<T> {

	private static int maxBranchFactor = 64;
	private static int minBranchFactor = maxBranchFactor / 2;

	public final int depth;
	public final int weight;
	public final IRopeList<T> ts;
	public final List<IRope<T>> ropes;

	public static class IRopeList<T> {
		public int size;
		public Int_Obj<T> get;
		public IntInt_Obj<IRopeList<T>> subList;
		public Iterate<IRopeList<T>> concat;

		public static IRopeList<Character> of(String s) {
			return ropeList(IRope.of(ropeList(s)));
		}

		public IRopeList(int size, Int_Obj<T> get, IntInt_Obj<IRopeList<T>> subList, Iterate<IRopeList<T>> concat) {
			this.size = size;
			this.get = get;
			this.subList = subList;
			this.concat = concat;
		}

		public IRopeList<T> left(int p) {
			return subList.apply(0, p);
		}

		public IRopeList<T> right(int p) {
			return subList.apply(p, size);
		}
	}

	public static <T> IRope<T> of(IRopeList<T> ts) {
		var rope = new IRope<T>(ts.left(0));
		var size = ts.size;
		var p = 0;
		while (p < size) {
			var p1 = min(p + minBranchFactor, size);
			rope = meld(rope, new IRope<T>(ts.subList.apply(p, p1)));
			p = p1;
		}
		return rope;
	}

	private static IRopeList<Character> ropeList(String s) {
		return new IRopeList<>( //
				s.length(), //
				s::charAt, //
				(i0, ix) -> ropeList(s.substring(i0, ix)), //
				list -> ropeList(s + list.toString())) {
			public String toString() {
				return s;
			}
		};
	}

	private static <T> IRopeList<T> ropeList(IRope<T> rope) {
		class W extends IRopeList<T> {
			private IRope<T> rope_ = rope;

			private W() {
				super(rope.weight, rope::at, (i0, ix) -> ropeList(rope.left(ix).right(i0)), null);
			}
		}

		var ropeList = new W();
		ropeList.rope_ = rope;
		ropeList.concat = list -> ropeList(meld(rope, ((W) list).rope_));
		return ropeList;
	}

	// minBranchFactor <= ts.size() && ts.size() < maxBranchFactor
	public IRope(IRopeList<T> ts) {
		this.depth = 0;
		this.weight = ts.size;
		this.ts = ts;
		this.ropes = null;
	}

	// minBranchFactor <= ropes.size() && ropes.size() < maxBranchFactor
	public IRope(int depth, List<IRope<T>> ropes) {
		var weight = 0;
		for (var rope : ropes)
			weight += rope.weight;
		this.depth = depth;
		this.weight = weight;
		this.ts = null;
		this.ropes = ropes;
	}

	// 0 <= p && p < weight
	public T at(int p) {
		if (0 < depth) {
			int index = 0, w;
			IRope<T> rope;
			while (!(p < (w = (rope = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			return rope.at(p);
		} else
			return ts.get.apply(p);
	}

	public IRope<T> left(int p) {
		return 0 < p ? left_(p) : empty();
	}

	public IRope<T> right(int p) {
		return p < weight ? right_(p) : empty();
	}

	private IRope<T> empty() {
		var rope = this;
		List<IRope<T>> ropes;
		while ((ropes = rope.ropes) != null)
			rope = ropes.get(0);
		return new IRope<T>(rope.ts.left(0));
	}

	// 0 < p && p <= weight
	public IRope<T> left_(int p) {
		var deque = new ArrayDeque<IRope<T>>();
		var rope = this;
		List<IRope<T>> ropes;

		while ((ropes = rope.ropes) != null) {
			int index = 0, w;
			IRope<T> rope_;
			while (!(p <= (w = (rope_ = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			for (var i = 0; i < index; i++)
				deque.push(ropes.get(i));
			rope = rope_;
		}

		return meldLeft(deque, new IRope<>(rope.ts.subList.apply(0, p)));
	}

	// 0 <= p && p < weight
	public IRope<T> right_(int p) {
		var deque = new ArrayDeque<IRope<T>>();
		var rope = this;
		List<IRope<T>> ropes;

		while ((ropes = rope.ropes) != null) {
			int index = 0, w;
			IRope<T> rope_;
			while (!(p < (w = (rope_ = ropes.get(index)).weight))) {
				p -= w;
				index++;
			}
			for (var i = ropes.size() - 1; index < i; i--)
				deque.push(ropes.get(i));
			rope = rope_;
		}

		return meldRight(new IRope<>(rope.ts.subList.apply(p, rope.weight)), deque);
	}

	public IRope<T> validateRoot() {
		return validate(true) ? this : null;
	}

	public boolean validate(boolean isRoot) {
		Streamlet<IRope<T>> rs;
		int s;
		return (false //
				|| depth == 0 //
						&& weight == (s = ts.size) //
						&& s < maxBranchFactor //
						&& ropes == null //
				|| (rs = Read.from(ropes)) != null //
						&& rs.isAll(rope -> rope.depth + 1 == depth) //
						&& rs.toInt(Obj_Int.sum(rope -> rope.weight)) == weight //
						&& ts == null //
						&& (s = rs.size()) < maxBranchFactor //
						&& rs.isAll(rope -> rope.validate(false))) //
				&& (isRoot || minBranchFactor <= s) ? true : Fail.t();
	}

	public static <T> IRope<T> meld(IRope<T> rope0, IRope<T> rope1) {
		return newRoot(meld_(rope0, rope1)).validateRoot();
	}

	private static <T> List<IRope<T>> meld_(IRope<T> rope0, IRope<T> rope1) {
		var depth0 = rope0.depth;
		var depth1 = rope1.depth;
		var depth = max(depth0, depth1);

		if (0 < depth) {
			List<IRope<T>> ropes;

			if (depth1 < depth0)
				ropes = List_.concat(List_.left(rope0.ropes, -1), meld_(List_.last(rope0.ropes), rope1));
			else if (depth0 < depth1)
				ropes = List_.concat(meld_(rope0, List_.first(rope1.ropes)), List_.right(rope1.ropes, 1));
			else
				ropes = List_.concat(rope0.ropes, rope1.ropes);

			List<IRope<T>> list;
			var size = ropes.size();

			if (maxBranchFactor <= size) {
				var p = size / 2;
				var left = List_.left(ropes, p);
				var right = List_.right(ropes, p);
				list = List.of(new IRope<>(depth, left), new IRope<>(depth, right));
			} else
				list = List.of(new IRope<>(depth, ropes));

			return list;
		} else {
			var ts = rope0.ts.concat.apply(rope1.ts);
			var size = ts.size;

			if (maxBranchFactor <= size) {
				var p = size / 2;
				var left = ts.left(p);
				var right = ts.right(p);
				return List.of(new IRope<>(left), new IRope<>(right));
			} else
				return List.of(new IRope<>(ts));
		}
	}

	private static <T> IRope<T> meldLeft(Deque<IRope<T>> queue, IRope<T> rope) {
		var branchFactor = minBranchFactor;

		while (true) {
			var queue1 = new ArrayDeque<IRope<T>>(List.of(rope));
			var depth = rope.depth;

			Source<IRope<T>> pack = () -> {
				var ix = queue1.size();
				var ropes = new ArrayList<IRope<T>>(Collections.nCopies(ix, null));
				for (var i = 0; i < ix; i++)
					ropes.set(i, queue1.pop());
				return new IRope<>(depth + 1, ropes);
			};

			while (queue1.size() < branchFactor) {
				var rope1 = queue.pollFirst();

				if (rope1 != null)
					new Object() {
						public void add(IRope<T> rope_) {
							if (depth < rope_.depth) {
								var ropes = rope_.ropes;
								for (var i = ropes.size() - 1; 0 <= i; i--)
									add(ropes.get(i));
							} else
								queue1.push(rope_);
						}
					}.add(rope1);
				else
					return pack.source();
			}

			rope = pack.source();
		}
	}

	private static <T> IRope<T> meldRight(IRope<T> rope, Deque<IRope<T>> queue) {
		var branchFactor = minBranchFactor;

		while (true) {
			var queue1 = new ArrayDeque<IRope<T>>(List.of(rope));
			var depth = rope.depth;

			Source<IRope<T>> pack = () -> {
				var ix = queue1.size();
				var ropes = new ArrayList<IRope<T>>(Collections.nCopies(ix, null));
				for (var i = 0; i < ix; i++)
					ropes.set(ix - i - 1, queue1.pop());
				return new IRope<>(depth + 1, ropes);
			};

			while (queue1.size() < branchFactor) {
				var rope1 = queue.pollFirst();

				if (rope1 != null)
					new Object() {
						public void add(IRope<T> rope_) {
							if (depth < rope_.depth) {
								var ropes = rope_.ropes;
								for (var i = 0; i < ropes.size(); i++)
									add(ropes.get(i));
							} else
								queue1.push(rope_);
						}
					}.add(rope1);
				else
					return pack.source();
			}

			rope = pack.source();
		}
	}

	private static <T> IRope<T> newRoot(List<IRope<T>> ropes) {
		var rope = ropes.get(0);
		return ropes.size() != 1 ? new IRope<>(rope.depth + 1, ropes) : rope;
	}

}
