package suite.funp;

import suite.assembler.Amd64;
import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.util.FunUtil.Sink;

public class P4Emit {

	private int is = Funp_.integerSize;
	private Amd64 amd64 = Amd64.me;

	private Sink<Instruction> emit;

	public P4Emit(Sink<Instruction> emit) {
		this.emit = emit;
	}

	public OpReg emitRegInsn(Insn insn, OpReg op0, Operand op1) {
		if (op1 instanceof OpImm) {
			var i = ((OpImm) op1).imm;
			if (insn == Insn.AND)
				andImm(op0, i);
			else if (insn == Insn.OR)
				orImm(op0, i);
			else if (insn == Insn.XOR)
				xorImm(op0, i);
			else if (insn == Insn.ADD)
				addImm(op0, i);
			else if (insn == Insn.SUB)
				addImm(op0, -i);
			else if (insn == Insn.IMUL)
				imulImm(op0, i);
			else if (insn == Insn.SHL)
				shiftImm(insn, op0, i);
			else if (insn == Insn.SHR)
				shiftImm(insn, op0, i);
			else
				emit(amd64.instruction(insn, op0, op1));
		} else
			emit(amd64.instruction(insn, op0, op1));
		return op0;
	}

	private void addImm(Operand op0, long i) {
		if (i == -1l)
			emit(amd64.instruction(Insn.DEC, op0));
		else if (i == 1l)
			emit(amd64.instruction(Insn.INC, op0));
		else if (i != 0l)
			emit(amd64.instruction(Insn.ADD, op0, amd64.imm(i, is)));
	}

	private void andImm(Operand op0, long i) {
		if (i != -1l)
			emit(amd64.instruction(Insn.AND, op0, amd64.imm(i, is)));
	}

	private void orImm(Operand op0, long i) {
		if (i != 0l)
			emit(amd64.instruction(Insn.OR, op0, amd64.imm(i, is)));
	}

	private void xorImm(Operand op0, long i) {
		if (i == -1l)
			emit(amd64.instruction(Insn.NOT, op0));
		else if (i != 0l)
			emit(amd64.instruction(Insn.XOR, op0, amd64.imm(i, is)));
	}

	private void imulImm(OpReg r0, long i) {
		if (i != 1l)
			if (Long.bitCount(i) == 1)
				shiftImm(Insn.SHL, r0, Long.numberOfTrailingZeros(i));
			else
				emit(amd64.instruction(Insn.IMUL, r0, r0, amd64.imm(i, is)));
	}

	public void shiftImm(Insn insn, Operand op0, long z) {
		if (z != 0l)
			emit(amd64.instruction(insn, op0, amd64.imm(z, 1)));
	}

	public void lea(Operand op0, OpMem op1) {
		if (op1.baseReg < 0 && op1.indexReg < 0)
			mov(op0, amd64.imm(op1.disp, is));
		else if (op1.indexReg < 0 && op1.disp == 0)
			mov(op0, amd64.reg32[op1.baseReg]);
		else
			emit(amd64.instruction(Insn.LEA, op0, op1));
	}

	public void mov(Operand op0, Operand op1) {
		if (op0 != op1)
			if (op0 instanceof OpReg && op1 instanceof OpImm && ((OpImm) op1).imm == 0)
				emit(amd64.instruction(Insn.XOR, op0, op0));
			else
				emit(amd64.instruction(Insn.MOV, op0, op1));
	}

	public void emit(Instruction instruction) {
		emit.sink(instruction);
	}

	public Operand label() {
		return amd64.imm(-1, Funp_.pointerSize);
	}

}
