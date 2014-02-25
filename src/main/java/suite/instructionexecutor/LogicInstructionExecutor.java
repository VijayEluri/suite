package suite.instructionexecutor;

import suite.instructionexecutor.InstructionUtil.Activation;
import suite.instructionexecutor.InstructionUtil.Frame;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Prover;
import suite.lp.doer.ProverConfig;
import suite.lp.predicate.SystemPredicates;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;

public class LogicInstructionExecutor extends InstructionExecutor {

	private Prover prover;
	private SystemPredicates systemPredicates;

	public LogicInstructionExecutor(Node node, ProverConfig proverConfig) {
		super(node);
		prover = new Prover(proverConfig);
		systemPredicates = new SystemPredicates(prover);
	}

	@Override
	protected void handle(Exec exec, Instruction insn) {
		Activation current = exec.current;
		Frame frame = current.frame;
		Node regs[] = frame != null ? frame.registers : null;
		Journal journal = prover.getJournal();
		Instruction insn1;

		switch (insn.insn) {
		case BACKUPCSP_____:
			regs[insn.op0] = exec.current.previous;
			break;
		case BACKUPDSP_____:
			regs[insn.op0] = number(exec.sp);
			break;
		case BIND__________:
			if (!Binder.bind(regs[insn.op0], regs[insn.op1], journal))
				current.ip = insn.op2; // Fail
			break;
		case BINDMARK______:
			regs[insn.op0] = number(journal.getPointInTime());
			break;
		case BINDUNDO______:
			journal.undoBinds(i(regs[insn.op0]));
			break;
		case DECOMPOSETREE0:
			Node node = regs[insn.op0].finalNode();

			insn1 = getInstructions()[current.ip++];
			TermOp op = TermOp.find(((Atom) constantPool.get(insn1.op0)).getName());
			int rl = insn1.op1;
			int rr = insn1.op2;

			if (node instanceof Tree) {
				Tree tree = (Tree) node;

				if (tree.getOperator() == op) {
					regs[rl] = tree.getLeft();
					regs[rr] = tree.getRight();
				} else
					current.ip = insn.op1;
			} else if (node instanceof Reference) {
				Tree tree = Tree.create(op, regs[rl] = new Reference(), regs[rr] = new Reference());
				journal.addBind((Reference) node, tree);
			} else
				current.ip = insn.op1;
			break;
		case PROVEINTERPRET:
			if (!prover.prove(regs[insn.op0]))
				current.ip = insn.op1;
			break;
		case PROVESYS______:
			if (!systemPredicates.call(regs[insn.op0]))
				current.ip = insn.op1;
			break;
		case RESTORECSP____:
			exec.current.previous = (Activation) regs[insn.op0];
			break;
		case RESTOREDSP____:
			exec.sp = i(regs[insn.op0]);
			break;
		default:
			throw new RuntimeException("Unknown instruction " + insn);
		}
	}

}
