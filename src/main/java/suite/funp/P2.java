package suite.funp;

import java.util.List;

import suite.adt.Mutable;
import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Pair;
import suite.assembler.Amd64.Operand;
import suite.funp.Funp_.Funp;
import suite.primitive.adt.pair.IntIntPair;

public class P2 {

	public interface End {
	}

	public static class FunpAllocGlobal implements Funp, P4.End {
		public String var;
		public int size;
		public Funp expr;
		public Mutable<Operand> address;

		public static FunpAllocGlobal of(String var, int size, Funp expr, Mutable<Operand> address) {
			var f = new FunpAllocGlobal();
			f.var = var;
			f.size = size;
			f.expr = expr;
			f.address = address;
			return f;
		}

		public <R> R apply(FixieFun4<String, Integer, Funp, Mutable<Operand>, R> fun) {
			return fun.apply(var, size, expr, address);
		}
	}

	public static class FunpAllocStack implements Funp, P4.End {
		public int size;
		public Funp value;
		public Funp expr;
		public Mutable<Integer> stack;

		public static FunpAllocStack of(int size, Funp value, Funp expr, Mutable<Integer> stack) {
			var f = new FunpAllocStack();
			f.size = size;
			f.value = value;
			f.expr = expr;
			f.stack = stack;
			return f;
		}

		public <R> R apply(FixieFun4<Integer, Funp, Funp, Mutable<Integer>, R> fun) {
			return fun.apply(size, value, expr, stack);
		}
	}

	public static class FunpAssign implements Funp, P4.End {
		public FunpMemory memory;
		public Funp value;
		public Funp expr;

		public static FunpAssign of(FunpMemory memory, Funp value, Funp expr) {
			var f = new FunpAssign();
			f.memory = memory;
			f.value = value;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<FunpMemory, Funp, Funp, R> fun) {
			return fun.apply(memory, value, expr);
		}
	}

	public static class FunpData implements Funp, P4.End {
		public List<Pair<Funp, IntIntPair>> pairs;

		public static FunpData of(List<Pair<Funp, IntIntPair>> pairs) {
			var f = new FunpData();
			f.pairs = pairs;
			return f;
		}

		public <R> R apply(FixieFun1<List<Pair<Funp, IntIntPair>>, R> fun) {
			return fun.apply(pairs);
		}
	}

	public static class FunpFramePointer implements Funp, P4.End {
		public static FunpFramePointer of() {
			return new FunpFramePointer();
		}

		public <R> R apply(FixieFun0<R> fun) {
			return fun.apply();
		}
	}

	public static class FunpInvoke implements Funp, P4.End {
		public Funp routine;

		public static FunpInvoke of(Funp routine) {
			var f = new FunpInvoke();
			f.routine = routine;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(routine);
		}
	}

	public static class FunpInvoke2 implements Funp, P4.End {
		public Funp routine;

		public static FunpInvoke2 of(Funp routine) {
			var f = new FunpInvoke2();
			f.routine = routine;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(routine);
		}
	}

	public static class FunpInvokeIo implements Funp, P4.End {
		public Funp routine;

		public static FunpInvokeIo of(Funp routine) {
			var f = new FunpInvokeIo();
			f.routine = routine;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(routine);
		}
	}

	public static class FunpMemory implements Funp, P4.End {
		public Funp pointer;
		public int start;
		public int end;

		public static FunpMemory of(Funp pointer, int start, int end) {
			var f = new FunpMemory();
			f.pointer = pointer;
			f.start = start;
			f.end = end;
			return f;
		}

		public int size() {
			return end - start;
		}

		public <R> R apply(FixieFun3<Funp, Integer, Integer, R> fun) {
			return fun.apply(pointer, start, end);
		}
	}

	public static class FunpOperand implements Funp, P4.End {
		public Mutable<Operand> operand;

		public static FunpOperand of(Mutable<Operand> operand) {
			var f = new FunpOperand();
			f.operand = operand;
			return f;
		}

		public <R> R apply(FixieFun1<Mutable<Operand>, R> fun) {
			return fun.apply(operand);
		}
	}

	public static class FunpRoutine implements Funp, P4.End {
		public Funp frame;
		public Funp expr;

		public static FunpRoutine of(Funp frame, Funp expr) {
			var f = new FunpRoutine();
			f.frame = frame;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Funp, R> fun) {
			return fun.apply(frame, expr);
		}
	}

	public static class FunpRoutine2 implements Funp, P4.End {
		public Funp frame;
		public Funp expr;

		public static FunpRoutine2 of(Funp frame, Funp expr) {
			var f = new FunpRoutine2();
			f.frame = frame;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun2<Funp, Funp, R> fun) {
			return fun.apply(frame, expr);
		}

	}

	public static class FunpRoutineIo implements Funp, P4.End {
		public Funp frame;
		public Funp expr;
		public int is, os;

		public static FunpRoutineIo of(Funp frame, Funp expr, int is, int os) {
			var f = new FunpRoutineIo();
			f.frame = frame;
			f.expr = expr;
			f.is = is;
			f.os = os;
			return f;
		}

		public <R> R apply(FixieFun4<Funp, Funp, Integer, Integer, R> fun) {
			return fun.apply(frame, expr, is, os);
		}
	}

	public static class FunpSaveRegisters implements Funp, P4.End {
		public Funp expr;

		public static FunpSaveRegisters of(Funp expr) {
			var f = new FunpSaveRegisters();
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun1<Funp, R> fun) {
			return fun.apply(expr);
		}
	}

	public static class FunpWhile implements Funp, P4.End {
		public Funp while_;
		public Funp do_;
		public Funp expr;

		public static FunpWhile of(Funp while_, Funp do_, Funp expr) {
			var f = new FunpWhile();
			f.while_ = while_;
			f.do_ = do_;
			f.expr = expr;
			return f;
		}

		public <R> R apply(FixieFun3<Funp, Funp, Funp, R> fun) {
			return fun.apply(while_, do_, expr);
		}
	}

}
