package suite.assembler;

import static suite.util.Friends.min;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import suite.assembler.Amd64.Insn;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.IntInt_Obj;
import suite.primitive.IntObj_Int;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.adt.map.IntIntMap;
import suite.primitive.adt.pair.IntIntPair;
import suite.util.Fail;
import suite.util.FunUtil.Sink;
import suite.util.To;

public class Amd64Interpret {

	public final BytesBuilder out = new BytesBuilder();

	private IntInt_Obj<IntIntPair> f = (s, p) -> IntIntPair.of(s, s + p);

	private IntIntPair baseNull = IntIntPair.of(0, 0);
	private IntIntPair baseCode = f.apply(baseNull.t1, 0x08000000);
	private IntIntPair baseData = f.apply(baseCode.t1, 0x08000000);
	private IntIntPair baseStack = f.apply(baseData.t1, 0x00040000);
	// private IntIntPair baseEnd = f.apply(baseStack.t1, 0);

	private IntIntPair posNull = IntIntPair.of(0, 0);
	private IntIntPair posCode = f.apply(posNull.t1, 65536);
	private IntIntPair posData = f.apply(posCode.t1, 262144);
	private IntIntPair posStack = f.apply(posData.t1, 262144);
	private IntIntPair posEnd = f.apply(posStack.t1, 0);

	private int diffCode = baseCode.t0 - posCode.t0;
	private int diffData = baseData.t0 - posData.t0;
	private int diffStack = baseStack.t0 - posStack.t0;
	private ByteBuffer mem = ByteBuffer.allocate(posEnd.t0);
	private int[] regs = new int[16];
	private int c;

	private Amd64 amd64 = Amd64.me;
	private Amd64Dump dump = new Amd64Dump();
	private int eax = amd64.eax.reg;
	private int ebx = amd64.ebx.reg;
	private int ecx = amd64.ecx.reg;
	private int edx = amd64.edx.reg;
	private int esp = amd64.esp.reg;
	private int esi = amd64.esi.reg;
	private int edi = amd64.edi.reg;
	private int eip;

	private int[] scales = { 1, 2, 4, 8, };

	private Sink<Bytes> output = out::append;

	public Amd64Interpret() {
	}

	public int interpret(List<Instruction> instructions, Bytes code, Bytes input) {
		mem.order(ByteOrder.LITTLE_ENDIAN);
		mem.position(posCode.t0);
		mem.put(code.bs);
		eip = baseCode.t0;
		regs[esp] = baseStack.t1 - 16;

		var labels = new IntIntMap();

		for (var i = 0; i < instructions.size(); i++) {
			var i_ = i;
			var instruction = instructions.get(i_);
			if (instruction.insn == Insn.LABEL)
				labels.update((int) ((OpImm) instruction.op0).imm, i0 -> i_ + 1);
		}

		while (true) {
			var instruction = instructions.get(eip++);

			if (Boolean.FALSE)
				LogUtil.info(state(instruction));

			try {
				IntObj_Int<Operand> trim = (i, op) -> {
					if (op.size == 1)
						return (int) (byte) i;
					else
						return i;
				};

				Obj_Int<Operand> fetch32 = op -> {
					int v0;
					if (op instanceof OpImm)
						v0 = (int) ((OpImm) op).imm;
					else if (op instanceof OpMem)
						v0 = mem.getInt(index(address((OpMem) op)));
					else if (op instanceof OpReg) {
						var reg = ((OpReg) op).reg;
						v0 = regs[reg];
					} else
						v0 = 0;
					return trim.apply(v0, op);
				};

				var op0 = instruction.op0;
				var op1 = instruction.op1;
				int source0 = fetch32.apply(op0);
				int source1 = fetch32.apply(op1);
				IntSink assign;
				Runnable r;

				if (op0 instanceof OpMem) {
					var index = index(address((OpMem) op0));
					if (op0.size == 1)
						assign = i -> mem.put(index, (byte) i);
					else if (op0.size == 4)
						assign = i -> mem.putInt(index, i);
					else
						assign = null;
				} else if (op0 instanceof OpReg) {
					var reg = ((OpReg) op0).reg;
					if (op0.size == 1)
						assign = i -> regs[reg] = regs[reg] & 0xFFFFFF00 | i;
					else if (op0.size == 4)
						assign = i -> regs[reg] = i;
					else
						assign = null;
				} else
					assign = null;

				switch (instruction.insn) {
				case ADD:
					assign.sink(setFlags(source0 + source1));
					break;
				case AND:
					assign.sink(setFlags(source0 & source1));
					break;
				case CALL:
					push(eip);
					eip = labels.get(source0);
					break;
				case CLD:
					break;
				case CMP:
					c = Integer.compare(source0, source1);
					break;
				case CMPSB:
					cmpsb();
					break;
				case CMPSD:
					cmpsd();
					break;
				case DEC:
					assign.sink(source0 - 1);
					break;
				case INC:
					assign.sink(source0 + 1);
					break;
				case INT:
					var p0 = regs[eax] & 0xFF;
					var p1 = regs[ebx];
					var p2 = regs[ecx];
					var p3 = regs[edx];
					int rc;
					if ((byte) source0 == -128)
						if (p0 == 0x01) // exit
							return p1;
						else if (p0 == 0x03) { // read
							var length = min(p3, input.size());
							var di = index(p2);
							for (var i = 0; i < length; i++)
								mem.put(di++, input.get(i));
							input = input.range(length);
							rc = length;
						} else if (p0 == 0x04) { // write
							var length = p3;
							var si = index(p2);
							var bs = new byte[length];
							for (var i = 0; i < length; i++)
								bs[i] = mem.get(si++);
							output.sink(Bytes.of(bs));
							rc = length;
						} else if (regs[eax] == 0x5A) { // map
							var size = mem.getInt(index(p1) + 4);
							rc = size < posData.t1 - posData.t0 ? baseData.t0 : Fail.t();
						} else
							rc = Fail.t("invalid syscall " + regs[eax]);
					else
						rc = Fail.t();
					regs[eax] = rc;
					break;
				case JE:
					if (c == 0)
						eip = labels.get(source0);
					break;
				case JMP:
					eip = labels.get(source0);
					break;
				case JG:
					if (0 < c)
						eip = labels.get(source0);
					break;
				case JGE:
					if (0 <= c)
						eip = labels.get(source0);
					break;
				case JL:
					if (c < 0)
						eip = labels.get(source0);
					break;
				case JLE:
					if (c <= 0)
						eip = labels.get(source0);
					break;
				case JNE:
					if (c != 0)
						eip = labels.get(source0);
					break;
				case JNZ:
					if (c != 0)
						eip = labels.get(source0);
					break;
				case JZ:
					if (c == 0)
						eip = labels.get(source0);
					break;
				case LABEL:
					break;
				case LEA:
					assign.sink(address((OpMem) op1));
					break;
				case MOV:
					assign.sink(source1);
					break;
				case MOVSB:
					movsb();
					break;
				case MOVSD:
					movsd();
					break;
				case OR:
					assign.sink(setFlags(source0 | source1));
					break;
				case POP:
					assign.sink(pop());
					break;
				case PUSH:
					push(source0);
					break;
				case REP:
					r = getNextRepeatInsn(instructions);
					while (0 < regs[ecx]--)
						r.run();
					break;
				case REPE:
					r = getNextRepeatInsn(instructions);
					while (0 < regs[ecx]--) {
						r.run();
						if (c != 0)
							break;
					}
					break;
				case REPNE:
					r = getNextRepeatInsn(instructions);
					while (0 < regs[ecx]--) {
						r.run();
						if (c == 0)
							break;
					}
					break;
				case RET:
					eip = pop();
					break;
				case SETE:
					assign.sink(c == 0 ? 1 : 0);
					break;
				case SETG:
					assign.sink(0 < c ? 1 : 0);
					break;
				case SETGE:
					assign.sink(0 <= c ? 1 : 0);
					break;
				case SETL:
					assign.sink(c < 0 ? 1 : 0);
					break;
				case SETLE:
					assign.sink(c <= 0 ? 1 : 0);
					break;
				case SETNE:
					assign.sink(c != 0 ? 1 : 0);
					break;
				case SUB:
					assign.sink(setFlags(source0 - source1));
					break;
				case XOR:
					assign.sink(setFlags(source0 ^ source1));
					break;
				default:
					Fail.t();
				}
			} catch (Exception ex) {
				LogUtil.info(state(instruction));
				throw ex;
			}
		}
	}

	private Runnable getNextRepeatInsn(List<Instruction> instructions) {
		Insn insn = instructions.get(eip++).insn;
		Runnable r = null;
		r = insn == Insn.CMPSB ? this::cmpsb : r;
		r = insn == Insn.CMPSD ? this::cmpsd : r;
		r = insn == Insn.MOVSB ? this::movsb : r;
		r = insn == Insn.MOVSD ? this::movsd : r;
		return r != null ? r : Fail.t();
	}

	private void cmpsb() {
		c = Byte.compare(mem.get(index(regs[esi])), mem.get(index(regs[edi])));
		regs[esi]++;
		regs[edi]++;
	}

	private void cmpsd() {
		int aa = mem.getInt(index(regs[esi]));
		int bb = mem.getInt(index(regs[edi]));
		c = Integer.compare(aa, bb);
		regs[esi] += 4;
		regs[edi] += 4;
	}

	private void movsb() {
		mem.put(index(regs[edi]), mem.get(index(regs[esi])));
		regs[esi]++;
		regs[edi]++;
	}

	private void movsd() {
		mem.putInt(index(regs[edi]), mem.getInt(index(regs[esi])));
		regs[esi] += 4;
		regs[edi] += 4;
	}

	private void push(int value) {
		regs[esp] -= Funp_.integerSize;
		mem.putInt(index(regs[esp]), value);
	}

	private int pop() {
		var i = mem.getInt(index(regs[esp]));
		regs[esp] += Funp_.integerSize;
		return i;
	}

	private int setFlags(int value) {
		c = Integer.compare(value, 0);
		return value;
	}

	private int address(OpMem opMem) {
		var br = opMem.baseReg;
		var ir = opMem.indexReg;
		return (int) opMem.disp + (0 <= br ? regs[br] : 0) + (0 <= ir ? regs[ir] * scales[opMem.scale] : 0);
	}

	private int index(int address) {
		if (address < baseCode.t1)
			return address - diffCode;
		else if (address < baseData.t1)
			return address - diffData;
		else if (address < baseStack.t1)
			return address - diffStack;
		else
			return Fail.t("address gone wild: " + Integer.toHexString(address));
	}

	private String state(Instruction instruction) {
		var sb = new StringBuilder();
		for (var i = 0; i < 8; i++)
			sb.append((i % 2 == 0 ? "\n" : " ") + amd64.regByName.inverse().get(amd64.reg32[i]) + ":" + To.hex8(regs[i]));
		sb.append("\nCMP = " + c);
		sb.append("\nINSTRUCTION = " + dump.dump(instruction));
		return sb.toString();
	}

}
