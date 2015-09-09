package suite.lp.predicate;

import java.util.HashMap;
import java.util.Map;

import suite.adt.IdentityKey;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.TreeIntern;
import suite.node.io.Operator;
import suite.node.io.TermOp;

public class InternPredicates {

	private static ThreadLocal<Map<IdentityKey<Node>, Node>> internMap = ThreadLocal.withInitial(() -> new HashMap<>());
	private static ThreadLocal<TreeIntern> treeIntern_ = ThreadLocal.withInitial(() -> new TreeIntern());

	public BuiltinPredicate internMapClear = PredicateUtil.run(n -> {
		internMap.get().clear();
		treeIntern_.get().clear();
	});

	public BuiltinPredicate internMapContains = PredicateUtil.p1((prover, p0) -> {
		IdentityKey<Node> key = IdentityKey.of(p0);
		return internMap.get().containsKey(key);
	});

	public BuiltinPredicate internMapPut = PredicateUtil.fun(n -> //
	internMap.get().computeIfAbsent(IdentityKey.of(n), any -> new Reference()));

	public BuiltinPredicate internTree = PredicateUtil.p4((prover, t, l, op, r) -> {
		Operator operator = TermOp.find(((Atom) op).name);
		return prover.bind(t, treeIntern_.get().of(operator, l, r));
	});

}
