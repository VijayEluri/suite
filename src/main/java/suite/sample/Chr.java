package suite.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import suite.Suite;
import suite.immutable.IMap;
import suite.immutable.ISet;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.Prototype;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.Rewriter;
import suite.util.FunUtil.Fun;
import suite.util.Pair;
import suite.util.Streamlet;
import suite.util.To;

/**
 * Constraint handling rules implementation.
 *
 * @author ywsing
 */
public class Chr {

	private List<Rule> rules = new ArrayList<>();
	private Prover prover = new Prover(Suite.createRuleSet());

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
					throw new RuntimeException("Invalid key " + key);
			} else
				throw new RuntimeException("Invalid rule " + node);
		}

		rules.add(rule);
	}

	public Collection<Node> chr(Collection<Node> facts) {
		State state = new State(new IMap<>());

		for (Node fact : facts) {
			Prototype prototype = Prototype.of(fact);
			state = setFacts(state, prototype, getFacts(state, prototype).replace(fact));
		}

		State state1;

		while ((state1 = chr(state)) != null)
			state = state1;

		List<Node> nodes1 = new ArrayList<>();

		for (Pair<Prototype, ISet<Node>> pair : state.factsByPrototype)
			nodes1.addAll(To.list(pair.t1));

		return nodes1;
	}

	private State chr(State state) {
		return Streamlet.of(rules).concatMap(rule -> chr(state, rule)).first();
	}

	private Streamlet<State> chr(State state, Rule rule) {
		Generalizer generalizer = new Generalizer();
		Journal journal = new Journal();
		Streamlet<State> states = Streamlet.of(state);

		for (Node if_ : rule.ifs)
			states = chrIf(states, journal, generalizer.generalize(if_));

		for (Node given : rule.givens)
			states = chrGiven(states, journal, generalizer.generalize(given));

		states = chrWhen(states, generalizer.generalize(rule.when));

		for (Node then : rule.thens)
			states = chrThen(states, generalizer.generalize(then));

		return states;
	}

	private Streamlet<State> chrIf(Streamlet<State> states, Journal journal, Node if_) {
		Prototype prototype = Prototype.of(if_);

		return states.concatMap(state -> {
			ISet<Node> facts = getFacts(state, prototype);
			Fun<Node, Boolean> bindFun = bindFun(journal, if_);
			return facts.stream().filter(bindFun).map(node -> setFacts(state, prototype, facts.remove(node)));
		});
	}

	private Streamlet<State> chrGiven(Streamlet<State> states, Journal journal, Node given) {
		Prototype prototype = Prototype.of(given);

		return states.concatMap(state -> {
			ISet<Node> facts = getFacts(state, prototype);
			Fun<Node, Boolean> bindFun = bindFun(journal, given);
			boolean isMatch = or(facts.stream().map(bindFun));
			return isMatch ? Streamlet.of(state) : Streamlet.empty();
		});
	}

	private Streamlet<State> chrThen(Streamlet<State> states, Node then) {
		Generalizer generalizer = new Generalizer();
		Node a = atom(".a"), b = atom(".b");

		if (Binder.bind(then, generalizer.generalize(Suite.substitute(".0 = .1", a, b)), new Journal())) {

			// Built-in syntactic equality
			Reference from = generalizer.getVariable(a);
			Reference to = generalizer.getVariable(b);

			states = states.map(new Fun<State, State>() {
				public State apply(State state) {
					IMap<Prototype, ISet<Node>> factsByPrototype1 = new IMap<>();
					for (Pair<Prototype, ISet<Node>> pair : state.factsByPrototype)
						factsByPrototype1 = factsByPrototype1.put(pair.t0, replace(pair.t1));
					return new State(factsByPrototype1);
				}

				private ISet<Node> replace(ISet<Node> facts) {
					ISet<Node> facts1 = new ISet<>();
					for (Node node : facts)
						facts1 = facts1.replace(new Rewriter(from, to).replace(node));
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

	private Fun<Node, Boolean> bindFun(Journal journal, Node node0) {
		int pit = journal.getPointInTime();

		return node1 -> {
			journal.undoBinds(pit);
			return Binder.bind(node0, node1, journal);
		};
	}

	private boolean or(Streamlet<Boolean> st) {
		Boolean b;
		while ((b = st.source()) != null)
			if (b == Boolean.TRUE)
				return true;
		return false;
	}

	private Node atom(String name) {
		return Atom.of(name);
	}

	private ISet<Node> getFacts(State state, Prototype prototype) {
		ISet<Node> results = state.factsByPrototype.get(prototype);
		return results != null ? results : new ISet<>();
	}

	private State setFacts(State state, Prototype prototype, ISet<Node> nodes) {
		IMap<Prototype, ISet<Node>> facts = state.factsByPrototype;
		return new State(nodes.stream().source() != null ? facts.replace(prototype, nodes) : facts.remove(prototype));
	}

}
