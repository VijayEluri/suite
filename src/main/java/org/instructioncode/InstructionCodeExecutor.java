package org.instructioncode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.parser.Operator;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Str;
import org.suite.node.Tree;
import org.util.Util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class InstructionCodeExecutor {

	private enum Insn {
		ASSIGNBOOL____("ASSIGN-BOOL"), //
		ASSIGNFRAMEREG("ASSIGN-FRAME-REG"), //
		ASSIGNINT_____("ASSIGN-INT"), //
		ASSIGNSTR_____("ASSIGN-STR"), //
		ASSIGNCLOSURE_("ASSIGN-CLOSURE"), //
		CALLCLOSURE___("CALL-CLOSURE"), //
		ENTER_________("ENTER"), //
		EXIT__________("EXIT"), //
		EVALUATE______("EVALUATE"), //
		EVALADD_______("EVAL-ADD"), //
		EVALDIV_______("EVAL-DIV"), //
		EVALEQ________("EVAL-EQ"), //
		EVALGE________("EVAL-GE"), //
		EVALGT________("EVAL-GT"), //
		EVALLE________("EVAL-LE"), //
		EVALLT________("EVAL-LT"), //
		EVALMUL_______("EVAL-MUL"), //
		EVALNE________("EVAL-NE"), //
		EVALSUB_______("EVAL-SUB"), //
		IFFALSE_______("IF-FALSE"), //
		IFGE__________("IF-GE"), //
		IFGT__________("IF-GT"), //
		IFLE__________("IF-LE"), //
		IFLT__________("IF-LT"), //
		IFNOTEQUALS___("IF-NOT-EQ"), //
		JUMP__________("JUMP"), //
		LABEL_________("LABEL"), //
		POP___________("POP"), //
		PUSH__________("PUSH"), //
		RETURN________("RETURN"), //
		;

		private String name;

		private Insn(String name) {
			this.name = name;
		}
	};

	private final static BiMap<Insn, String> insnNames = HashBiMap.create();
	static {
		for (Insn insn : Insn.values())
			insnNames.put(insn, insn.name);
	}

	private final static Map<Operator, Insn> evalInsns = Util.createHashMap();
	static {
		evalInsns.put(TermOp.PLUS__, Insn.EVALADD_______);
		evalInsns.put(TermOp.DIVIDE, Insn.EVALDIV_______);
		evalInsns.put(TermOp.EQUAL_, Insn.EVALEQ________);
		evalInsns.put(TermOp.GE____, Insn.EVALGE________);
		evalInsns.put(TermOp.GT____, Insn.EVALGT________);
		evalInsns.put(TermOp.LE____, Insn.EVALLE________);
		evalInsns.put(TermOp.LT____, Insn.EVALLT________);
		evalInsns.put(TermOp.MULT__, Insn.EVALMUL_______);
		evalInsns.put(TermOp.MINUS_, Insn.EVALSUB_______);
		evalInsns.put(TermOp.NOTEQ_, Insn.EVALNE________);
	}

	private Map<Integer, Object> objectPool = new HashMap<Integer, Object>();

	private final static Atom trueAtom = Atom.create("true");
	private final static Atom falseAtom = Atom.create("false");

	private static class Instruction {
		private Insn insn;
		private int op1, op2, op3;

		public Instruction(Insn insn, int op1, int op2, int op3) {
			this.insn = insn;
			this.op1 = op1;
			this.op2 = op2;
			this.op3 = op3;
		}

		public String toString() {
			return insn.name + " " + op1 + ", " + op2 + ", " + op3;
		}
	}

	private Instruction instructions[];

	public InstructionCodeExecutor(Node node) {
		Tree tree;
		List<Instruction> list = new ArrayList<Instruction>();
		InstructionExtractor extractor = new InstructionExtractor();

		while ((tree = Tree.decompose(node, TermOp.AND___)) != null) {
			Instruction instruction = extractor.extract(tree.getLeft());
			list.add(instruction);
			node = tree.getRight();
		}

		instructions = list.toArray(new Instruction[list.size()]);
	}

	private class InstructionExtractor {
		private List<Instruction> enters = new ArrayList<Instruction>();

		private Instruction extract(Node node) {
			List<Node> rs = new ArrayList<Node>(5);
			Tree tree;

			while ((tree = Tree.decompose(node, TermOp.SEP___)) != null) {
				rs.add(tree.getLeft());
				node = tree.getRight();
			}

			rs.add(node);

			Atom instNode = (Atom) rs.get(1);
			Insn insn = insnNames.inverse().get(instNode.getName());

			switch (insn) {
			case ASSIGNBOOL____:
				insn = Insn.ASSIGNINT_____;
				break;
			case ASSIGNSTR_____:
				insn = Insn.ASSIGNINT_____;
				break;
			case EVALUATE______:
				Atom atom = (Atom) rs.remove(4).finalNode();
				TermOp operator = TermOp.find((atom).getName());
				insn = evalInsns.get(operator);
			}

			if (insn != null) {
				Instruction instruction = new Instruction(insn //
						, getRegisterNumber(rs, 2) //
						, getRegisterNumber(rs, 3) //
						, getRegisterNumber(rs, 4));

				if (insn == Insn.ENTER_________)
					enters.add(instruction);
				else if (insn == Insn.RETURN________)
					enters.remove(enters.size() - 1);

				return instruction;
			} else
				throw new RuntimeException("Unknown opcode"
						+ instNode.getName());
		}

		private int getRegisterNumber(List<Node> rs, int index) {
			if (rs.size() > index) {
				Node node = rs.get(index).finalNode();

				if (node == trueAtom) // ASSIGN-BOOL
					return 1;
				else if (node == falseAtom) // ASSIGN-BOOL
					return 0;
				else if (node instanceof Reference) { // Transient register

					// Allocates new register in current local frame
					Instruction enter = enters.get(enters.size() - 1);
					int registerNumber = enter.op1++;

					((Reference) node).bound(Int.create(registerNumber));
					return registerNumber;
				} else if (node instanceof Str) // ASSIGN-STR
					return allocate(((Str) node).getValue());
				else if (node instanceof Int)
					return ((Int) node).getNumber();
				else
					return allocate(node); // PROVE
			} else
				return 0;
		}
	}

	private final static int STACKSIZE = 256;

	private static class Frame {
		private Frame previous;
		private int registers[];

		private Frame(Frame previous, int frameSize) {
			this.previous = previous;
			registers = new int[frameSize];
		}
	}

	private static class Closure {
		private Frame frame;
		private int ip;

		private Closure(Frame frame, int ip) {
			this.frame = frame;
			this.ip = ip;
		}

		public Closure clone() {
			return new Closure(frame, ip);
		}
	}

	public int execute() throws IOException {
		Closure current = new Closure(null, 0);
		Closure callStack[] = new Closure[STACKSIZE];
		int dataStack[] = new int[STACKSIZE];
		int csp = 0, dsp = 0;
		int counter = 0;

		for (;;) {
			Frame frame = current.frame;
			int regs[] = frame != null ? frame.registers : null;
			int ip = current.ip++;
			Instruction insn = instructions[ip];

			// LogUtil.info("TRACE", ip + "> " + insn);

			switch (insn.insn) {
			case ASSIGNFRAMEREG:
				int i = insn.op2;
				while (i++ < 0)
					frame = frame.previous;
				regs[insn.op1] = frame.registers[insn.op3];
				break;
			case ASSIGNINT_____:
				regs[insn.op1] = insn.op2;
				break;
			case ASSIGNCLOSURE_:
				regs[insn.op1] = allocate(new Closure(frame, insn.op2));
				break;
			case CALLCLOSURE___:
				callStack[csp++] = current;
				current = ((Closure) objectPool.get(regs[insn.op2])).clone();
				break;
			case ENTER_________:
				current.frame = new Frame(frame, insn.op1);
				break;
			case EVALADD_______:
				regs[insn.op1] = regs[insn.op2] + regs[insn.op3];
				break;
			case EVALDIV_______:
				regs[insn.op1] = regs[insn.op2] / regs[insn.op3];
				break;
			case EVALEQ________:
				regs[insn.op1] = regs[insn.op2] == regs[insn.op3] ? 1 : 0;
				break;
			case EVALGE________:
				regs[insn.op1] = regs[insn.op2] >= regs[insn.op3] ? 1 : 0;
				break;
			case EVALGT________:
				regs[insn.op1] = regs[insn.op2] > regs[insn.op3] ? 1 : 0;
				break;
			case EVALLE________:
				regs[insn.op1] = regs[insn.op2] <= regs[insn.op3] ? 1 : 0;
				break;
			case EVALLT________:
				regs[insn.op1] = regs[insn.op2] < regs[insn.op3] ? 1 : 0;
				break;
			case EVALNE________:
				regs[insn.op1] = regs[insn.op2] != regs[insn.op3] ? 1 : 0;
				break;
			case EVALMUL_______:
				regs[insn.op1] = regs[insn.op2] * regs[insn.op3];
				break;
			case EVALSUB_______:
				regs[insn.op1] = regs[insn.op2] - regs[insn.op3];
				break;
			case EXIT__________:
				return regs[insn.op1];
			case IFFALSE_______:
				if (regs[insn.op2] != 1)
					current.ip = insn.op1;
				break;
			case IFNOTEQUALS___:
				if (regs[insn.op2] != regs[insn.op3])
					current.ip = insn.op1;
				break;
			case JUMP__________:
				current.ip = insn.op1;
				break;
			case PUSH__________:
				dataStack[dsp++] = regs[insn.op1];
				break;
			case POP___________:
				regs[insn.op1] = dataStack[--dsp];
				break;
			case RETURN________:
				int returnValue = regs[insn.op1]; // Saves return value
				current = callStack[--csp];
				current.frame.registers[instructions[current.ip - 1].op1] = returnValue;
			}

			if (++counter % 65536 == 0)
				collectGarbage(frame);
		}
	}

	/**
	 * Traverses frame hierarchy to find all referenced objects, and clean the
	 * unreferenced ones to free memory.
	 * 
	 * This does not free the holes in the reference list.
	 */
	private void collectGarbage(Frame frame) {
		int size = objectPool.size();
		Map<Integer, Object> objectPool1 = new HashMap<Integer, Object>(
				size * 2);

		collectGarbage0(objectPool1, frame); // Marks used objects

		objectPool = objectPool1; // Swaps object pool
	}

	private void collectGarbage0(Map<Integer, Object> objectPool1, Frame frame) {
		if (frame != null) {
			for (int content : frame.registers) {

				// Copies the used reference
				Object object = objectPool.get(content);
				Object original = objectPool1.put(content, object);

				// Traverse recursively if reference not marked before
				if (original == null && object instanceof Closure)
					collectGarbage0(objectPool1, ((Closure) object).frame);
			}

			collectGarbage0(objectPool1, frame.previous);
		}
	}

	private int allocate(Object object) {
		int pointer = objectPool.size();
		objectPool.put(pointer, object);
		return pointer;
	}

}
