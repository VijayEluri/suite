package suite.fp;

import suite.Suite;
import suite.immutable.IMap;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;

public class LazyFunInterpreter {

	private Atom ERROR = Atom.of("error");
	private Atom FST__ = Atom.of("fst");
	private Atom SND__ = Atom.of("snd");

	public interface Thunk_ {
		public Node get();
	}

	private static class Fun_ extends Node {
		private Fun<Thunk_, Thunk_> fun;

		private Fun_(Fun<Thunk_, Thunk_> fun) {
			this.fun = fun;
		}
	}

	private static class Pair_ extends Node {
		private Thunk_ first;
		private Thunk_ second;

		private Pair_(Thunk_ left, Thunk_ right) {
			this.first = left;
			this.second = right;
		}
	}

	public Thunk_ lazy(Node node) {
		Thunk_ error = () -> {
			throw new RuntimeException("Error termination");
		};

		IMap<String, Thunk_> env = new IMap<>();
		env = env.put(Atom.TRUE.name, () -> Atom.TRUE);
		env = env.put(Atom.FALSE.name, () -> Atom.FALSE);

		env = env.put(TermOp.AND___.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> new Pair_(a, b))));
		env = env.put(TermOp.EQUAL_.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) == i(b)))));
		env = env.put(TermOp.NOTEQ_.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) != i(b)))));
		env = env.put(TermOp.LE____.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) <= i(b)))));
		env = env.put(TermOp.LT____.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) < i(b)))));
		env = env.put(TermOp.GE____.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) >= i(b)))));
		env = env.put(TermOp.GT____.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> b(i(a) > i(b)))));
		env = env.put(TermOp.PLUS__.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) + i(b)))));
		env = env.put(TermOp.MINUS_.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) - i(b)))));
		env = env.put(TermOp.MULT__.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) * i(b)))));
		env = env.put(TermOp.DIVIDE.getName(), () -> new Fun_(a -> () -> new Fun_(b -> () -> Int.of(i(a) / i(b)))));

		env = env.put(ERROR.name, error);
		env = env.put(FST__.name, () -> new Fun_(in -> ((Pair_) in.get()).first));
		env = env.put(SND__.name, () -> new Fun_(in -> ((Pair_) in.get()).second));

		return lazy0(node).apply(env);
	}

	private Fun<IMap<String, Thunk_>, Thunk_> lazy0(Node node) {
		Fun<IMap<String, Thunk_>, Thunk_> result;
		Tree tree;
		Node m[];

		if ((m = Suite.match(".0 {.1}", node)) != null) {
			Fun<IMap<String, Thunk_>, Thunk_> fun = lazy0(m[0]);
			Fun<IMap<String, Thunk_>, Thunk_> param = lazy0(m[1]);
			result = env -> ((Fun_) fun.apply(env).get()).fun.apply(param.apply(env));
		} else if ((m = Suite.match(".0 := .1 >> .2", node)) != null) {
			String vk = v(m[0]);
			Fun<IMap<String, Thunk_>, Thunk_> value = lazy0(m[1]);
			Fun<IMap<String, Thunk_>, Thunk_> expr = lazy0(m[2]);
			result = env -> {
				Thunk_ val[] = new Thunk_[] { null };
				IMap<String, Thunk_> env1 = env.put(vk, () -> val[0].get());
				val[0] = value.apply(env1)::get;
				return expr.apply(env1);
			};
		} else if ((m = Suite.match(".0 => .1", node)) != null) {
			String vk = v(m[0]);
			Fun<IMap<String, Thunk_>, Thunk_> value = lazy0(m[1]);
			result = env -> () -> new Fun_(in -> value.apply(env.put(vk, in)));
		} else if ((m = Suite.match("if .0 then .1 else .2", node)) != null) {
			Fun<IMap<String, Thunk_>, Thunk_> if_ = lazy0(m[0]);
			Fun<IMap<String, Thunk_>, Thunk_> then_ = lazy0(m[1]);
			Fun<IMap<String, Thunk_>, Thunk_> else_ = lazy0(m[2]);
			result = env -> (if_.apply(env).get() == Atom.TRUE ? then_ : else_).apply(env);
		} else if ((tree = Tree.decompose(node)) != null) {
			Operator operator = tree.getOperator();
			Fun<IMap<String, Thunk_>, Thunk_> p0 = lazy0(tree.getLeft());
			Fun<IMap<String, Thunk_>, Thunk_> p1 = lazy0(tree.getRight());
			result = env -> {
				Thunk_ r0 = env.get(operator.getName());
				Thunk_ r1 = ((Fun_) r0.get()).fun.apply(p0.apply(env));
				Thunk_ r2 = ((Fun_) r1.get()).fun.apply(p1.apply(env));
				return r2;
			};
		} else if (node instanceof Atom) {
			String vk = v(node);
			result = env -> env.get(vk);
		} else
			result = env -> () -> node;

		return result;
	}

	private Atom b(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	private int i(Thunk_ thunk) {
		return ((Int) thunk.get()).number;
	}

	private String v(Node node) {
		return ((Atom) node).name;
	}

}
