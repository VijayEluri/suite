package suite.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import suite.Suite;
import suite.adt.pair.Pair;
import suite.immutable.IMap;
import suite.immutable.ISet;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TreeRewriter;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.To;

/**
 * Constraint handling rules implementation.
 *
 * @author ywsing
 */
public class Chr {

	private TreeRewriter trw = new TreeRewriter();

	private List<Rule> rules = new ArrayList<>();
	private Prover prover = new Prover(Suite.newRuleSet());

	private class Rule {
		private List<Node> givens = new ArrayList<>();
		private List<Node> ifs = new ArrayList<>();
		private List<Node> thens = new ArrayList<>();
		private Node when = Atom.NIL;
	}

	private class State {
		private IMap<Prototype, ISet<Node>> factsByPrototype;

		public State(IMap<Prototype, ISet<Node>> factsByPrototype) {
			this.factsByPrototype = factsByPrototype;
		}
	}

	public void addRule(Node node) {
		Rule rule = new Rule();

		while (node != Atom.of("end")) {
			Tree t0 = Tree.decompose(node, TermOp.TUPLE_);
			Tree t1 = t0 != null ? Tree.decompose(t0.getRight(), TermOp.TUPLE_) : null;

			if (t1 != null) {
				Node key = t0.getLeft();
				Node value = t1.getLeft();
				node = t1.getRight();

				if (key == Atom.of("given"))
					rule.givens = To.list(Tree.iter(value));
				else if (key == Atom.of("if"))
					rule.ifs = To.list(Tree.iter(value));
				else if (key == Atom.of("then"))
					rule.thens = To.list(Tree.iter(value));
				else if (key == Atom.of("when"))
					rule.when = value;
				else
					Fail.t("invalid key " + key);
			} else
				Fail.t("invalid rule " + node);
		}

		rules.add(rule);
	}

	public Collection<Node> chr(Collection<Node> facts) {
		State state = new State(IMap.empty());

		for (Node fact : facts) {
			Prototype prototype = Prototype.of(fact);
			state = setFacts(state, prototype, getFacts(state, prototype).replace(fact));
		}

		State state1;

		while ((state1 = chr(state)) != null)
			state = state1;

		List<Node> nodes1 = new ArrayList<>();

		for (Pair<Prototype, ISet<Node>> e : state.factsByPrototype)
			nodes1.addAll(To.list(e.t1));

		return nodes1;
	}

	private State chr(State state) {
		return Read.from(rules).concatMap(rule -> chr(state, rule)).first();
	}

	private Streamlet<State> chr(State state, Rule rule) {
		Generalizer generalizer = new Generalizer();
		Trail trail = new Trail();
		Streamlet<State> states = Read.each(state);

		for (Node if_ : rule.ifs)
			states = chrIf(states, trail, generalizer.generalize(if_));

		for (Node given : rule.givens)
			states = chrGiven(states, trail, generalizer.generalize(given));

		states = chrWhen(states, generalizer.generalize(rule.when));

		for (Node then : rule.thens)
			states = chrThen(states, generalizer.generalize(then));

		return states;
	}

	private Streamlet<State> chrIf(Streamlet<State> states, Trail trail, Node if_) {
		Prototype prototype = Prototype.of(if_);

		Fun<State, Streamlet<State>> fun = state -> {
			ISet<Node> facts = getFacts(state, prototype);
			Predicate<Node> bindFun = bindFun(trail, if_);
			return facts.streamlet().filter(bindFun).map(node -> setFacts(state, prototype, facts.remove(node)));
		};

		return states.concatMap(fun);
	}

	private Streamlet<State> chrGiven(Streamlet<State> states, Trail trail, Node given) {
		Prototype prototype = Prototype.of(given);

		return states.filter(state -> {
			ISet<Node> facts = getFacts(state, prototype);
			Predicate<Node> bindFun = bindFun(trail, given);
			return facts.streamlet().isAny(bindFun);
		});
	}

	private Streamlet<State> chrThen(Streamlet<State> states, Node then) {
		Generalizer generalizer = new Generalizer();
		Atom a = atom(".a"), b = atom(".b");

		if (Binder.bind(then, generalizer.generalize(Suite.substitute(".0 = .1", a, b)), new Trail())) {

			// built-in syntactic equality
			Reference from = generalizer.getVariable(a);
			Reference to = generalizer.getVariable(b);

			states = states.map(new Fun<>() {
				public State apply(State state) {
					IMap<Prototype, ISet<Node>> factsByPrototype1 = IMap.empty();
					for (Pair<Prototype, ISet<Node>> e : state.factsByPrototype)
						factsByPrototype1 = factsByPrototype1.put(e.t0, replace(e.t1));
					return new State(factsByPrototype1);
				}

				private ISet<Node> replace(ISet<Node> facts) {
					ISet<Node> facts1 = new ISet<>();
					for (Node node : facts)
						facts1 = facts1.replace(trw.replace(from, to, node));
					return facts1;
				}
			});
		}

		return states.map(state -> {
			Prototype prototype = Prototype.of(then);
			ISet<Node> facts = getFacts(state, prototype);
			return setFacts(state, prototype, facts.replace(then));
		});
	}

	private Streamlet<State> chrWhen(Streamlet<State> states, Node when) {
		return states.filter(state -> prover.prove(when));
	}

	private Predicate<Node> bindFun(Trail trail, Node node0) {
		int pit = trail.getPointInTime();

		return node1 -> {
			trail.unwind(pit);
			return Binder.bind(node0, node1, trail);
		};
	}

	private Atom atom(String name) {
		return Atom.of(name);
	}

	private ISet<Node> getFacts(State state, Prototype prototype) {
		ISet<Node> results = state.factsByPrototype.get(prototype);
		return results != null ? results : new ISet<>();
	}

	private State setFacts(State state, Prototype prototype, ISet<Node> nodes) {
		IMap<Prototype, ISet<Node>> facts = state.factsByPrototype;
		return new State(nodes.streamlet().first() != null ? facts.replace(prototype, nodes) : facts.remove(prototype));
	}

}
