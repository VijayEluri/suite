package org.instructionexecutor;

import java.util.ArrayList;
import java.util.List;

import org.instructionexecutor.InstructionUtil.Activation;
import org.instructionexecutor.InstructionUtil.Closure;
import org.instructionexecutor.InstructionUtil.Frame;
import org.instructionexecutor.InstructionUtil.Insn;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.suite.doer.Comparer;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.util.LogUtil;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class InstructionExecutor implements AutoCloseable {

	private static final int stackSize = 4096;

	private Instruction instructions[];
	private int unwrapEntryPoint;

	protected BiMap<Integer, Node> constantPool = HashBiMap.create();

	private InstructionAnalyzer analyzer = new InstructionAnalyzer();

	public InstructionExecutor(Node node) {
		InstructionExtractor extractor = new InstructionExtractor(constantPool);

		List<Instruction> instructions0 = extractor.extractInstructions(node);
		analyzer.analyze(instructions0);

		List<Instruction> list = new ArrayList<>();
		list.addAll(instructions0);
		list.add(new Instruction(Insn.CALLCLOSURE___, 0, 0, 0));
		list.add(new Instruction(Insn.SETRESULT_____, 1, 0, 0));
		list.add(new Instruction(Insn.EXIT__________, 1, 0, 0));

		instructions = list.toArray(new Instruction[list.size()]);

		unwrapEntryPoint = instructions0.size();
	}

	public Node execute() {
		return evaluateClosure(new Closure(null, 0));
	}

	public Node evaluateClosure(Closure c0) {
		Frame f0 = new Frame(null, 2);
		f0.registers[0] = c0;

		Activation current = new Activation(f0, unwrapEntryPoint, null);

		Node stack[] = new Node[stackSize];
		int sp = 0;
		Node returnValue = null;

		Exec exec = new Exec();
		exec.stack = stack;

		Comparer comparer = comparer();

		for (;;) {
			Frame frame = current.frame;
			Node regs[] = frame != null ? frame.registers : null;
			int ip = current.ip++;
			Instruction insn = instructions[ip];

			Closure closure;
			TermOp op;
			int i;

			// org.util.LogUtil.info(ip + "> " + insn);

			switch (insn.insn) {
			case ASSIGNCLOSURE_:
				regs[insn.op0] = new Closure(frame, insn.op1);
				break;
			case ASSIGNFRAMEREG:
				i = insn.op1;
				while (i++ < 0)
					frame = frame.previous;
				regs[insn.op0] = frame.registers[insn.op2];
				break;
			case ASSIGNCONST___:
				regs[insn.op0] = constantPool.get(insn.op1);
				break;
			case ASSIGNINT_____:
				regs[insn.op0] = number(insn.op1);
				break;
			case CALL__________:
				current = new Activation(frame, i(regs[insn.op0]), current);
				break;
			case CALLCONST_____:
				current = new Activation(frame, insn.op0, current);
				break;
			case CALLCLOSURE___:
				closure = (Closure) regs[insn.op0];
				if (closure.result == null)
					current = new Activation(closure, current);
				else
					returnValue = closure.result;
				break;
			case ENTER_________:
				Frame parent = analyzer.isRequireParent(analyzer.getFrame(ip)) ? frame : null;
				current.frame = new Frame(parent, insn.op0);
				break;
			case EVALADD_______:
				regs[insn.op0] = number(i(regs[insn.op1]) + i(regs[insn.op2]));
				break;
			case EVALDIV_______:
				regs[insn.op0] = number(i(regs[insn.op1]) / i(regs[insn.op2]));
				break;
			case EVALEQ________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i == 0);
				break;
			case EVALGE________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i >= 0);
				break;
			case EVALGT________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i > 0);
				break;
			case EVALLE________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i <= 0);
				break;
			case EVALLT________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i < 0);
				break;
			case EVALNE________:
				i = comparer.compare(regs[insn.op1], regs[insn.op2]);
				regs[insn.op0] = atom(i != 0);
				break;
			case EVALMOD_______:
				regs[insn.op0] = number(i(regs[insn.op1]) % i(regs[insn.op2]));
				break;
			case EVALMUL_______:
				regs[insn.op0] = number(i(regs[insn.op1]) * i(regs[insn.op2]));
				break;
			case EVALSUB_______:
				regs[insn.op0] = number(i(regs[insn.op1]) - i(regs[insn.op2]));
				break;
			case EXIT__________:
				return regs[insn.op0];
			case FORMTREE0_____:
				Node left = regs[insn.op0];
				Node right = regs[insn.op1];
				insn = instructions[current.ip++];
				op = TermOp.find(((Atom) constantPool.get(insn.op0)).getName());
				regs[insn.op1] = Tree.create(op, left, right);
				break;
			case IFFALSE_______:
				if (regs[insn.op1] != Atom.TRUE)
					current.ip = insn.op0;
				break;
			case IFNOTEQUALS___:
				if (regs[insn.op1] != regs[insn.op2])
					current.ip = insn.op0;
				break;
			case JUMP__________:
				current.ip = insn.op0;
				break;
			case LABEL_________:
				break;
			case LOG___________:
				LogUtil.info(constantPool.get(insn.op0).toString());
				break;
			case NEWNODE_______:
				regs[insn.op0] = new Reference();
				break;
			case PUSH__________:
				stack[sp++] = regs[insn.op0];
				break;
			case PUSHCONST_____:
				stack[sp++] = number(insn.op0);
				break;
			case POP___________:
				regs[insn.op0] = stack[--sp];
				break;
			case REMARK________:
				break;
			case RETURN________:
				current = current.previous;
				break;
			case RETURNVALUE___:
				returnValue = regs[insn.op0];
				current = current.previous;
				break;
			case SETRESULT_____:
				regs[insn.op0] = returnValue;
				break;
			case SETCLOSURERES_:
				regs[insn.op0] = returnValue;
				closure = ((Closure) regs[insn.op1]);
				closure.frame = null; // Facilitates garbage collection
				closure.result = returnValue;
				break;
			case TOP___________:
				regs[insn.op0] = stack[sp + insn.op1];
				break;
			default:
				exec.current = current;
				exec.sp = sp;
				handle(exec, insn);
				current = exec.current;
				sp = exec.sp;
			}
		}
	}

	protected class Exec {
		protected Activation current;
		protected Object stack[];
		protected int sp;
	}

	protected void handle(Exec exec, Instruction insn) {
		throw new RuntimeException("Unknown instruction " + insn);
	}

	protected Comparer comparer() {
		return new Comparer();
	}

	protected static Int number(int n) {
		return Int.create(n);
	}

	protected static Atom atom(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	protected static int i(Object node) {
		return ((Int) node).getNumber();
	}

	@Override
	public void close() {
	}

	protected Instruction[] getInstructions() {
		return instructions;
	}

}
