package suite.funp;

import static suite.util.Friends.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import suite.adt.map.BiListMultimap;
import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpImmLabel;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.persistent.PerList;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Read;

public class P4Emit {

	private Amd64 amd64 = Amd64.me;
	private int ps = Funp_.pointerSize;

	private List<Block> blocks = new ArrayList<>();

	public class Block {
		public int align = 1;
		public OpImmLabel in;
		public List<Instruction> instructions;
		public OpImmLabel out;

		private Block(OpImmLabel in, List<Instruction> instructions, OpImmLabel out) {
			this.in = in;
			this.instructions = instructions;
			this.out = out;
		}
	}

	public class Emit {
		private OpImmLabel in;
		private List<Instruction> instructions = new ArrayList<>();

		private Emit(OpImmLabel in) {
			this.in = in;
		}

		public OpReg emitRegInsn(Insn insn, OpReg op0, Operand op1) {
			if (op1 instanceof OpImm) {
				var i = ((OpImm) op1).imm;
				if (insn == Insn.ADD)
					addImm(op0, i);
				else if (insn == Insn.AND)
					andImm(op0, i);
				else if (insn == Insn.IMUL)
					imulImm(op0, i);
				else if (insn == Insn.OR)
					orImm(op0, i);
				else if (insn == Insn.SHL)
					shiftImm(insn, op0, i);
				else if (insn == Insn.SHR)
					shiftImm(insn, op0, i);
				else if (insn == Insn.SUB)
					addImm(op0, -i);
				else if (insn == Insn.XOR)
					xorImm(op0, i);
				else
					emit(amd64.instruction(insn, op0, op1));
			} else
				emit(amd64.instruction(insn, op0, op1));
			return op0;
		}

		public void addImm(Operand op0, long i) {
			if (i == -1l)
				emit(amd64.instruction(Insn.DEC, op0));
			else if (i == 1l)
				emit(amd64.instruction(Insn.INC, op0));
			else if (i != 0l)
				emit(amd64.instruction(Insn.ADD, op0, amd64.imm(i, i == (byte) i ? 1 : op0.size)));
		}

		private void andImm(Operand op0, long i) {
			if (i != -1l)
				emit(amd64.instruction(Insn.AND, op0, amd64.imm(i, i == (byte) i ? 1 : op0.size)));
		}

		private void orImm(Operand op0, long i) {
			if (i != 0l)
				emit(amd64.instruction(Insn.OR, op0, amd64.imm(i, i == (byte) i ? 1 : op0.size)));
		}

		private void xorImm(Operand op0, long i) {
			if (i == -1l)
				emit(amd64.instruction(Insn.NOT, op0));
			else if (i != 0l)
				emit(amd64.instruction(Insn.XOR, op0, amd64.imm(i, i == (byte) i ? 1 : op0.size)));
		}

		private void imulImm(OpReg r0, long i) {
			if (i != 1l)
				if (Long.bitCount(i) == 1)
					shiftImm(Insn.SHL, r0, Long.numberOfTrailingZeros(i));
				else
					emit(amd64.instruction(Insn.IMUL, r0, r0, amd64.imm(i, r0.size)));
		}

		public void shiftImm(Insn insn, Operand op0, long z) {
			if (z != 0l)
				emit(amd64.instruction(insn, op0, amd64.imm8(z)));
		}

		public void lea(Operand op0, OpMem op1) {
			var op = lea(op1);
			if (op != null)
				mov(op0, op);
			else
				emit(amd64.instruction(Insn.LEA, op0, op1));
		}

		public Operand lea(OpMem op) {
			if (op.baseReg < 0 && op.indexReg < 0)
				return amd64.imm(op.disp.imm, ps);
			else if (op.indexReg < 0 && op.disp.imm == 0)
				return Funp_.pointerRegs[op.baseReg];
			else
				return null;
		}

		public <T extends Operand> T mov(T op0, Operand op1) {
			var opImm = op1.cast(OpImm.class);
			var isRegImm = op0 instanceof OpReg && opImm != null;
			if (op0 != op1)
				if (op0.size == op1.size || op0.size == 8 && op1.size == 4 && isRegImm)
					if (isRegImm && opImm.imm == 0 && !(op1 instanceof OpImmLabel))
						emit(amd64.instruction(Insn.XOR, op0, op0));
					else
						emit(amd64.instruction(Insn.MOV, op0, op1));
				else
					fail();
			return op0;
		}

		public void emit(Insn insn, Operand... ops) {
			emit(amd64.instruction(insn, ops));
		}

		public void emit(Instruction instruction) {
			instructions.add(instruction);
		}

		public Block spawn(Sink<Emit> sink) {
			var block = spawn(label(), sink, null);
			block.align = Funp_.pushSize;
			return block;
		}

		public Block spawn(OpImmLabel in, Sink<Emit> sink, OpImmLabel out) {
			return P4Emit.this.spawn(in, sink, out);
		}

		public void label(OpImmLabel label) {
			jumpLabel(label, label);
		}

		public void jumpLabel(OpImmLabel target, OpImmLabel label) {
			blocks.add(new Block(in, instructions, target));
			in = label;
			instructions = new ArrayList<>();
		}

		public OpImmLabel label() {
			return P4Emit.this.label();
		}
	}

	public List<Instruction> generate(OpImmLabel in, Sink<Emit> sink, OpImmLabel out) {
		Predicate<Block> isForward = b -> b.instructions.isEmpty() && b.out != null;

		spawn(in, sink, out);

		var list = new ArrayList<Instruction>();
		var blocks_ = Read.from(blocks);
		var blockByLabel = blocks_.toMap(block -> block.in);
		var inByOut = blocks_.filter(b -> b.out != null).toMultimap(b -> b.out, b -> b.in);
		var ids = blocks_.filter(isForward).toMap(b -> b.in, b -> b.out);

		var labelGroups = new BiListMultimap<OpImmLabel, OpImmLabel>();
		var set = new HashSet<OpImmLabel>();

		var gen = new Object() {
			private void g(OpImmLabel label, PerList<OpImmLabel> stack) {
				for (var label_ : inByOut.get(label)) {
					if (!stack.contains(label_) && !set.contains(label_))
						g(label_, PerList.cons(label_, stack));
					break;
				}
				gj(label, false);
			}

			private void gj(OpImmLabel label, boolean jump) {
				var labelRep = getLabelRep(label);
				var b = blockByLabel.get(labelRep);
				var g = false;

				if (!isForward.test(b)) {
					for (var label_ : labelGroups.get(labelRep))
						g |= set.add(label_) //
								&& list.add(amd64.instruction(Insn.ALIGN, amd64.imm32(b.align))) //
								&& list.add(amd64.instruction(Insn.LABEL, label_));

					if (g) {
						list.addAll(b.instructions);
						var out = b.out;
						if (out != null)
							gj(out, true);
					} else if (jump)
						list.add(amd64.instruction(Insn.JMP, label));
				}
			}

			private OpImmLabel getLabelRep(OpImmLabel label) {
				OpImmLabel label_;
				while ((label_ = ids.get(label)) != null)
					label = label_;
				return label;
			}
		};

		for (var block : blocks)
			labelGroups.put(gen.getLabelRep(block.in), block.in);

		gen.gj(in, true);
		blocks.forEach(block -> gen.g(block.in, PerList.end()));

		return list;
	}

	public Block spawn(OpImmLabel in, Sink<Emit> sink, OpImmLabel out) {
		var em = new Emit(in);
		sink.f(em);

		var block = new Block(em.in, em.instructions, out);
		blocks.add(block);
		return block;
	}

	public OpImmLabel label() {
		return amd64.new OpImmLabel(ps);
	}

}
