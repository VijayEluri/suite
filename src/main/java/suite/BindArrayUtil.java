package suite;

import java.util.ArrayList;
import java.util.List;

import suite.lp.compile.impl.CompileBinderImpl;
import suite.lp.doer.BinderFactory.BindEnv;
import suite.lp.doer.BinderFactory.Bind_;
import suite.lp.doer.Generalizer;
import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper.NodeEnv;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.primitive.Ints_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Memoize;
import suite.util.To;

public class BindArrayUtil {

	public interface Match {
		public Node[] apply(Node node);

		public Node substitute(Node... nodes);
	}

	public Match match(String pattern) {
		return matches.apply(pattern);
	}

	private Fun<String, Match> matches = Memoize.fun(pattern_ -> {
		Generalizer generalizer = new Generalizer();
		Node fs = Suite.parse(pattern_);
		Node toMatch = generalizer.generalize(fs);

		CompileBinderImpl cb = new CompileBinderImpl(false);
		Bind_ pred = cb.binder(toMatch);
		List<Integer> indexList = new ArrayList<>();
		Integer index;
		int n = 0;

		while ((index = cb.getIndex(generalizer.getVariable(Atom.of("." + n++)))) != null)
			indexList.add(index);

		int size = indexList.size();
		int[] indices = Ints_.toArray(size, indexList::get);

		Source<NodeEnv> source = new SewingGeneralizerImpl().g(fs);

		return new Match() {
			public Node[] apply(Node node) {
				Env env = cb.env();
				BindEnv be = new BindEnv(env);
				if (pred.test(be, node))
					return To.array(size, Node.class, i -> env.get(indices[i]));
				else
					return null;

			}

			public Node substitute(Node... nodes) {
				NodeEnv ne = source.source();
				int i = 0;
				for (Node node : nodes) {
					Node variable = ne.getVariable(Atom.of("." + i++));
					((Reference) variable).bound(node);
				}
				return ne.node;
			}
		};
	});

}
