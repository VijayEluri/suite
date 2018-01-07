package suite.lp.sewing.impl;

import java.util.ArrayList;
import java.util.List;

import suite.adt.pair.Pair;
import suite.lp.doer.GeneralizerFactory;
import suite.lp.doer.ProverConstant;
import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper;
import suite.lp.sewing.VariableMapper.NodeEnv;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.Tuple;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;

public class SewingGeneralizerImpl implements GeneralizerFactory {

	public final VariableMapper vm = new VariableMapper();

	public static Node generalize(Node node) {
		return new SewingGeneralizerImpl().g(node).source().node;
	}

	public Source<NodeEnv> g(Node node) {
		return vm.g(generalizer(node)::apply);
	}

	@Override
	public Env env() {
		return vm.env();
	}

	@Override
	public Generalize_ generalizer(Node node) {
		List<Generalize_> funs = new ArrayList<>();
		Generalize_ fun;

		while (true) {
			Node node0 = node;
			Tree tree;

			if (node0 instanceof Atom) {
				String name = ((Atom) node0).name;
				if (ProverConstant.isCut(node0) || ProverConstant.isVariable(name)) {
					int index = vm.computeIndex(node0);
					fun = env -> env.get(index);
				} else if (ProverConstant.isWildcard(name))
					fun = env -> new Reference();
				else
					fun = env -> node0;
			} else if (node0 instanceof Dict) {
				Generalize_[][] array = Read //
						.from2(((Dict) node0).map) //
						.map((key, value) -> new Generalize_[] { generalizer(key), generalizer(value), }) //
						.toArray(Generalize_[].class);
				int length = array.length;
				fun = env -> {
					@SuppressWarnings("unchecked")
					Pair<Node, Reference>[] pairs = new Pair[length];
					for (int i = 0; i < length; i++)
						pairs[i] = Pair.of(array[i][0].apply(env), Reference.of(array[i][1].apply(env)));
					return Dict.of(pairs);
				};
			} else if ((tree = Tree.decompose(node0)) != null) {
				Operator operator = tree.getOperator();
				if (operator != TermOp.OR____) {
					Generalize_ f = generalizer(tree.getLeft());
					funs.add(env -> Tree.of(operator, f.apply(env), null));
					node = tree.getRight();
					continue;
				} else { // delay generalizing for performance
					Generalize_ lf = generalizer(tree.getLeft());
					Generalize_ rf = generalizer(tree.getRight());
					fun = env -> Tree.of(operator, lf.apply(env), new Suspend(() -> rf.apply(env)));
				}
			} else if (node0 instanceof Tuple) {
				Generalize_[] fs = Read.from(((Tuple) node0).nodes).map(this::generalizer).toArray(Generalize_.class);
				int length = fs.length;
				fun = env -> {
					Node[] array = new Node[length];
					for (int i = 0; i < length; i++)
						array[i] = fs[i].apply(env);
					return Tuple.of(array);
				};
			} else
				fun = env -> node0;

			funs.add(fun);
			break;
		}

		if (1 < funs.size())
			return env -> {
				Tree t = Tree.of(null, null, null);
				Node node_ = t;
				for (Generalize_ fun_ : funs) {
					Tree t_ = Tree.decompose(node_);
					Tree.forceSetRight(t_, fun_.apply(env));
					node_ = t_.getRight();
				}
				return t.getRight();
			};
		else
			return funs.get(0);
	}

}
