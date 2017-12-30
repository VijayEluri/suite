package suite;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import suite.lp.Trail;
import suite.lp.doer.Generalizer;
import suite.lp.sewing.Env;
import suite.lp.sewing.SewingBinder.BindEnv;
import suite.lp.sewing.SewingBinder.BindPredicate;
import suite.lp.sewing.impl.SewingBinderImpl;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
import suite.lp.sewing.impl.VariableMapperImpl.Generalization;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.Formatter;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil.Fun;
import suite.util.Memoize;

public class BindMapUtil {

	public interface Match {
		public Map<String, Node> apply(Node node);

		public Node substitute(Map<String, Node> map);
	}

	public Match match(String pattern) {
		return matches.apply(pattern);
	}

	private Fun<String, Match> matches = Memoize.fun(pattern_ -> {
		Generalizer generalizer = new Generalizer();
		Node fs = Suite.parse(pattern_);
		Node toMatch = generalizer.generalize(fs);

		SewingBinderImpl sb = new SewingBinderImpl(false);
		BindPredicate pred = sb.compileBind(toMatch);

		Streamlet2<String, Integer> indices = Read.from(generalizer.getVariablesNames()) //
				.map2(Formatter::display, name -> sb.getVariableIndex(generalizer.getVariable(name))) //
				.collect(As::streamlet2);

		return new Match() {
			public Map<String, Node> apply(Node node) {
				Env env = sb.env();
				Trail trail = new Trail();
				BindEnv be = new BindEnv() {
					public Env getEnv() {
						return env;
					}

					public Trail getTrail() {
						return trail;
					}
				};
				if (pred.test(be, node)) {
					Map<String, Node> results = new HashMap<>();
					indices.sink((name, index) -> results.put(name, env.get(index)));
					return results;
				} else
					return null;
			}

			public Node substitute(Map<String, Node> map) {
				Generalization generalization = new SewingGeneralizerImpl().g(Suite.parse(pattern_)).source();
				for (Entry<String, Node> e : map.entrySet()) {
					Node variable = Atom.of(e.getKey());
					((Reference) variable).bound(e.getValue());
				}
				return generalization.node;
			}
		};

	});

}
