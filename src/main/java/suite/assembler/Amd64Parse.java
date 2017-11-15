package suite.assembler;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.Operand;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class Amd64Parse {

	private static Amd64 amd64 = Amd64.me;

	public Instruction parse(Node node) {
		Tree tree = Tree.decompose(node, TermOp.TUPLE_);
		Insn insn = Enum.valueOf(Insn.class, ((Atom) tree.getLeft()).name);
		Node ops = tree.getRight();
		List<Operand> operands = scan(ops, ".0, .1").map(this::parseOperand).toList();

		return amd64.instruction(insn, //
				0 < operands.size() ? operands.get(0) : amd64.none, //
				1 < operands.size() ? operands.get(1) : amd64.none, //
				2 < operands.size() ? operands.get(2) : amd64.none);
	}

	public Operand parseOperand(Node node) {
		Operand operand;
		Node[] m;

		if ((operand = amd64.regsByName.get(node)) != null)
			return operand;
		else if ((operand = amd64.cregsByName.get(node)) != null)
			return operand;
		else if ((operand = amd64.sregsByName.get(node)) != null)
			return operand;
		else if ((m = Suite.match("BYTE `.0`").apply(node)) != null)
			return parseOpMem(m, 1);
		else if ((m = Suite.match("WORD `.0`").apply(node)) != null)
			return parseOpMem(m, 2);
		else if ((m = Suite.match("DWORD `.0`").apply(node)) != null)
			return parseOpMem(m, 4);
		else if ((m = Suite.match("`.0`").apply(node)) != null)
			return parseOpMem(m, 4);
		else if (node instanceof Int) {
			OpImm opImm = amd64.new OpImm();
			opImm.imm = ((Int) node).number;
			opImm.size = 4;
			return opImm;
		} else
			throw new RuntimeException("bad operand");
	}

	private Operand parseOpMem(Node[] m, int size) {
		OpMem opMem = amd64.new OpMem();
		opMem.size = size;
		opMem.indexReg = -1;
		opMem.baseReg = -1;
		opMem.dispSize = 0;

		for (Node component : scan(m[0], ".0 + .1"))
			if ((m = Suite.match(".0 * .1").apply(component)) != null)
				if (opMem.indexReg < 0) {
					opMem.indexReg = amd64.regsByName.get(m[0]).reg;
					opMem.scale = ((Int) m[1]).number;
				} else
					throw new RuntimeException("bad operand");
			else if (component instanceof Int)
				if (opMem.dispSize == 0) {
					opMem.disp = ((Int) component).number;
					opMem.dispSize = 4;
				} else
					throw new RuntimeException("bad operand");
			else if (opMem.baseReg < 0)
				opMem.baseReg = amd64.regsByName.get(component).reg;
			else
				throw new RuntimeException("bad operand");
		return opMem;
	}

	private Streamlet<Node> scan(Node ops, String pattern) {
		List<Node> nodes = new ArrayList<>();
		Node[] m;
		while ((m = Suite.match(pattern).apply(ops)) != null) {
			nodes.add(m[0]);
			ops = m[1];
		}
		if (ops != Atom.NIL)
			nodes.add(ops);
		return Read.from(nodes);
	}

}