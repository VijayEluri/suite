package suite.lp.compile.impl;

import java.util.IdentityHashMap;

import suite.lp.doer.Generalizer;
import suite.lp.doer.GeneralizerFactory;
import suite.lp.sewing.VariableMapper;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;

public class CompileGeneralizerImpl implements GeneralizerFactory {

	private CompileClonerImpl cc = new CompileClonerImpl();
	private VariableMapper<Atom> vm = new VariableMapper<>();

	@Override
	public VariableMapper<Atom> mapper() {
		return vm;
	}

	@Override
	public Generalize_ generalizer(Node node) {
		var mapper = cc.mapper();
		var generalizer = new Generalizer();
		Generalize_ generalize = cc.cloner(generalizer.generalize(node))::apply;
		var indices = new IdentityHashMap<Reference, Atom>();
		for (var variableName : generalizer.getVariableNames())
			indices.put(generalizer.getVariable(variableName), variableName);
		vm = mapper.mapKeys(indices::get);
		return generalize;
	}

}
