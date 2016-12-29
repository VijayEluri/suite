package suite.jdk;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FunExpression {

	public static abstract class FunExpr {
		protected String type; // type.getDescriptor()

		public FunExpr cast(Class<?> clazz) {
			CastFunExpr expr = new CastFunExpr();
			expr.type = Type.getDescriptor(clazz);
			expr.expr = this;
			return expr;
		}

		public FunExpr checkCast(Class<?> clazz) {
			CheckCastFunExpr expr = new CheckCastFunExpr();
			expr.type = Type.getDescriptor(clazz);
			expr.expr = this;
			return expr;
		}

		public FunExpr field(String field, Class<?> type) {
			return field(field, Type.getDescriptor(type));
		}

		public FunExpr field(String field, String type) {
			FieldFunExpr expr = new FieldFunExpr();
			expr.type = type;
			expr.field = field;
			expr.object = this;
			return expr;
		}

		public FunExpr instanceOf(Class<?> clazz) {
			InstanceOfFunExpr expr = new InstanceOfFunExpr();
			expr.type = Type.getDescriptor(boolean.class);
			expr.instanceType = clazz;
			expr.object = this;
			return expr;
		}

		public FunExpr invoke(FunCreator<?> cc, FunExpr... parameters) {
			return invoke(Opcodes.INVOKEINTERFACE, cc.methodName, cc.returnType, parameters);
		}

		public FunExpr invokeInterface(String methodName, Class<?> clazz, FunExpr... parameters) {
			return invoke(Opcodes.INVOKEINTERFACE, methodName, Type.getDescriptor(clazz), parameters);
		}

		public FunExpr invokeVirtual(String methodName, Class<?> clazz, FunExpr... parameters) {
			return invoke(Opcodes.INVOKEVIRTUAL, methodName, Type.getDescriptor(clazz), parameters);
		}

		private FunExpr invoke(int opcode, String methodName, String type, FunExpr... parameters) {
			InvokeFunExpr expr = new InvokeFunExpr();
			expr.type = type;
			expr.methodName = methodName;
			expr.object = this;
			expr.opcode = opcode;
			expr.parameters = Arrays.asList(parameters);
			return expr;
		}
	}

	public static class BinaryFunExpr extends FunExpr {
		public int opcode;
		public FunExpr left, right;
	}

	public static class CastFunExpr extends FunExpr {
		public FunExpr expr;
	}

	public static class CheckCastFunExpr extends FunExpr {
		public FunExpr expr;
	}

	public static class ConstantFunExpr extends FunExpr {
		public Object constant; // primitives, class, handles etc.
	}

	public static class SeqFunExpr extends FunExpr {
		public FunExpr left, right;
	}

	public static class FieldFunExpr extends FunExpr {
		public FunExpr object;
		public String field;
	}

	public static class IfFunExpr extends FunExpr {
		public int ifInsn;
		public FunExpr then, else_;
	}

	public static class If1FunExpr extends IfFunExpr {
		public FunExpr if_;
	}

	public static class If2FunExpr extends IfFunExpr {
		public FunExpr left, right;
	}

	public static class InstanceOfFunExpr extends FunExpr {
		public FunExpr object;
		public Class<?> instanceType;
	}

	public static class InvokeFunExpr extends FunExpr {
		public int opcode;
		public String methodName;
		public FunExpr object;
		public List<FunExpr> parameters;
	}

	public static class LocalFunExpr extends FunExpr {
		public int index;
		public FunExpr value;
		public FunExpr do_;

	}

	public static class ParameterFunExpr extends FunExpr {
		public int index;
	}

	public static class PrintlnFunExpr extends FunExpr {
		public FunExpr expression;
	}

	public static class StaticFunExpr extends FunExpr {
		public String clazzType;
		public String field;
	}

	public static class ThisFunExpr extends FunExpr {
	}

}
