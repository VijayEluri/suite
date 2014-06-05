package suite.instructionexecutor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.instructionexecutor.InstructionUtil.Closure;
import suite.instructionexecutor.InstructionUtil.Insn;
import suite.instructionexecutor.InstructionUtil.Instruction;
import suite.node.Node;
import suite.node.Tree;
import suite.util.FunUtil.Source;

public class InstructionAnalyzer {

	private List<AnalyzedFrame> framesByIp = new ArrayList<>();
	private Set<Integer> tailCalls = new HashSet<>();

	public static class AnalyzedFrame {
		private int id;
		private boolean isRequireParent = false;
		private AnalyzedFrame parent;
		private List<AnalyzedRegister> registers;

		public AnalyzedFrame(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public boolean isRequireParent() {
			return isRequireParent;
		}

		public AnalyzedFrame getParent() {
			return parent;
		}

		public List<AnalyzedRegister> getRegisters() {
			return registers;
		}
	}

	public static class AnalyzedRegister {
		private Class<?> clazz;
		private boolean isUsedExternally = true;

		public Class<?> getClazz() {
			return clazz;
		}

		/**
		 * Analyzes whether code of other frames would access this variable.
		 */
		public boolean isUsedExternally() {
			return isUsedExternally;
		}

		/**
		 * Analyzes whether the variable can be stored in a local variable,
		 * instead of a instance variable in a frame.
		 */
		public boolean isTemporal() {
			return false;
		}
	}

	public void analyze(List<Instruction> instructions) {

		// Identify frame regions
		analyzeFrames(instructions);

		// Discover frame hierarchy
		analyzeParentFrames(instructions);

		// Find out register types in each frame
		analyzeFrameRegisters(instructions);

		// Find out tail call sites possible for optimization
		analyzeFpTailCalls(instructions);
	}

	private void analyzeFrames(List<Instruction> instructions) {
		Deque<AnalyzedFrame> analyzedFrames = new ArrayDeque<>();

		// Find out the parent of closures.
		// Assumes every ENTER has a ASSIGN-CLOSURE referencing it.
		for (int ip = 0; ip < instructions.size(); ip++) {
			Instruction insn = instructions.get(ip);

			if (insn.insn == Insn.ENTER_________)
				analyzedFrames.push(new AnalyzedFrame(ip));

			AnalyzedFrame frame = !analyzedFrames.isEmpty() ? analyzedFrames.peek() : null;
			framesByIp.add(frame);

			if (insn.insn == Insn.LEAVE_________)
				analyzedFrames.pop();
		}
	}

	private void analyzeParentFrames(List<Instruction> instructions) {
		for (int ip = 0; ip < instructions.size(); ip++) {
			Instruction insn = instructions.get(ip);

			// Recognize frames and their parents.
			// Assumes ENTER instruction should be after LABEL.
			if (insn.insn == Insn.ASSIGNCLOSURE_)
				framesByIp.get(insn.op1 + 1).parent = framesByIp.get(ip);
		}
	}

	private void analyzeFrameRegisters(List<Instruction> instructions) {
		int ip = 0;

		while (ip < instructions.size()) {
			int currentIp = ip;
			Instruction insn = instructions.get(ip++);
			int op0 = insn.op0, op1 = insn.op1, op2 = insn.op2;
			AnalyzedFrame frame = framesByIp.get(currentIp);
			List<AnalyzedRegister> registers = frame != null ? frame.registers : null;

			switch (insn.insn) {
			case EVALEQ________:
			case EVALGE________:
			case EVALGT________:
			case EVALLE________:
			case EVALLT________:
			case EVALNE________:
			case ISCONS________:
				registers.get(op0).clazz = boolean.class;
				break;
			case ASSIGNCLOSURE_:
				registers.get(op0).clazz = Closure.class;
				break;
			case ASSIGNINT_____:
			case BACKUPCSP_____:
			case BACKUPDSP_____:
			case BINDMARK______:
			case COMPARE_______:
			case EVALADD_______:
			case EVALDIV_______:
			case EVALMOD_______:
			case EVALMUL_______:
			case EVALSUB_______:
				registers.get(op0).clazz = int.class;
				break;
			case ASSIGNCONST___:
			case CALLINTRINSIC_:
			case CONSPAIR______:
			case CONSLIST______:
			case GETINTRINSIC__:
			case HEAD__________:
			case LOGREG________:
			case NEWNODE_______:
			case POP___________:
			case SETRESULT_____:
			case SETCLOSURERES_:
			case TAIL__________:
			case TOP___________:
				registers.get(op0).clazz = Node.class;
				break;
			case FORMTREE1_____:
				registers.get(insn.op1).clazz = Tree.class;
				break;
			case ASSIGNFRAMEREG:
				AnalyzedFrame frame1 = frame;
				for (int i = op1; i < 0; i++) {
					frame1.isRequireParent = true;
					frame1 = frame1.parent;
				}

				AnalyzedRegister op0register = registers.get(op0);
				AnalyzedRegister op2Register = frame1.registers.get(op2);

				if (frame != frame1)
					op2Register.isUsedExternally = true;

				// Merge into Node if clashed
				if (op0register.clazz != op2Register.clazz)
					op0register.clazz = op0register.clazz != null ? Node.class : op2Register.clazz;
				break;
			case DECOMPOSETREE1:
				registers.get(op1).clazz = registers.get(op2).clazz = Node.class;
				break;
			case ENTER_________:
				registers = frame.registers = new ArrayList<>();
				for (int i = 0; i < op0; i++)
					registers.add(new AnalyzedRegister());
				break;
			default:
			}
		}
	}

	private void analyzeFpTailCalls(List<Instruction> instructions) {
		for (int ip = 0; ip < instructions.size() - 1; ip++) {
			Source<Instruction> source = flow(instructions, ip);
			Instruction instruction0 = source.source();
			Instruction instruction1 = source.source();

			if (instruction0 != null && instruction0.insn == Insn.CALLCLOSURE___ //
					&& instruction1 != null && instruction1.insn == Insn.SETRESULT_____ //
					&& isReturningValue(instruction1.op0, source))
				tailCalls.add(ip);
		}
	}

	private boolean isReturningValue(int returnReg, Source<Instruction> source) {
		Instruction instruction;

		while ((instruction = source.source()) != null)
			switch (instruction.insn) {
			case ASSIGNFRAMEREG:
				if (instruction.op1 == 0 && instruction.op2 == returnReg) {
					returnReg = instruction.op0;
					break;
				} else
					return false;
			case LABEL_________:
				break;
			case RETURNVALUE___:
				return instruction.op0 == returnReg;
			default:
				return false;
			}

		return false;
	}

	private Source<Instruction> flow(List<Instruction> instructions, int ip) {
		return new Source<Instruction>() {
			private boolean end = false;
			private int ip_ = ip;

			public Instruction source() {
				if (!end && ip_ < instructions.size()) {
					Instruction instruction = instructions.get(ip_++);

					switch (instruction.insn) {
					case ASSIGNFRAMEREG:
					case CALL__________:
					case CALLCLOSURE___:
					case CALLINTRINSIC_:
					case CALLREG_______:
					case LABEL_________:
					case REMARK________:
					case SETCLOSURERES_:
					case SETRESULT_____:
						return instruction;
					case JUMP__________:
						ip_ = instruction.op0;
						return source();
					default:
						end = true;
						return instruction;
					}
				} else
					return null;
			}
		};
	}

	public void transform(List<Instruction> instructions) {
		for (int ip : tailCalls)
			instructions.get(ip).insn = Insn.JUMPCLOSURE___;
	}

	public AnalyzedFrame getFrame(Integer ip) {
		return framesByIp.get(ip);
	}

}
