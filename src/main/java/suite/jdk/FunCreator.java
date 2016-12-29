package suite.jdk;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import javassist.bytecode.Opcode;
import suite.adt.Pair;
import suite.editor.Listen.SinkEx;
import suite.immutable.IList;
import suite.jdk.FunExpression.BinaryFunExpr;
import suite.jdk.FunExpression.CastFunExpr;
import suite.jdk.FunExpression.CheckCastFunExpr;
import suite.jdk.FunExpression.ConstantFunExpr;
import suite.jdk.FunExpression.FieldFunExpr;
import suite.jdk.FunExpression.FunExpr;
import suite.jdk.FunExpression.If1FunExpr;
import suite.jdk.FunExpression.If2FunExpr;
import suite.jdk.FunExpression.IfFunExpr;
import suite.jdk.FunExpression.InstanceOfFunExpr;
import suite.jdk.FunExpression.InvokeFunExpr;
import suite.jdk.FunExpression.LocalFunExpr;
import suite.jdk.FunExpression.ParameterFunExpr;
import suite.jdk.FunExpression.PrintlnFunExpr;
import suite.jdk.FunExpression.SeqFunExpr;
import suite.jdk.FunExpression.StaticFunExpr;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.Util;

public class FunCreator<I> implements Opcodes {

	private static AtomicInteger counter = new AtomicInteger();

	public final Class<I> interfaceClass;
	public final Class<?> superClass;
	public final String className;
	public final String methodName;
	public final String returnType;
	public final List<Class<?>> parameterTypes;
	public final List<String> localTypes;

	private Class<? extends I> clazz;
	private Map<String, Pair<String, Object>> constants;
	private Map<String, String> fields;

	private class OpStack {
		private MethodVisitor mv;
		private IList<String> list;

		private OpStack cons(String type) {
			OpStack opStack = new OpStack();
			opStack.mv = mv;
			opStack.list = IList.cons(type, list);
			return opStack;
		}
	}

	public static <I> FunCreator<I> of(Class<I> ic, String mn) {
		return of(ic, mn, new HashMap<>());
	}

	public static <I> FunCreator<I> of(Class<I> ic, String mn, Map<String, Class<?>> fs) {
		Method methods[] = Rethrow.reflectiveOperationException(() -> ic.getMethods());
		Method method = Read.from(methods).filter(m -> Util.stringEquals(m.getName(), mn)).uniqueResult();
		Class<?> rt = method.getReturnType();
		Class<?> pts[] = method.getParameterTypes();
		String rt1 = Type.getDescriptor(rt);
		List<Class<?>> pts1 = Arrays.asList(pts);
		return new FunCreator<>(ic, mn, rt1, pts1, fs);
	}

	private FunCreator(Class<I> ic, String mn, String rt, List<Class<?>> ps, Map<String, Class<?>> fs) {
		interfaceClass = ic;
		superClass = Object.class;
		className = interfaceClass.getSimpleName() + counter.getAndIncrement();
		methodName = mn;
		returnType = rt;
		parameterTypes = ps;
		localTypes = new ArrayList<>();
		constants = new HashMap<>();
		fields = Read.from2(fs).mapValue(Type::getDescriptor).toMap();
	}

	public void create(FunExpr expression) {
		clazz = Rethrow.ex(() -> create_(expression));
	}

	private Class<? extends I> create_(FunExpr expression) throws NoSuchMethodException {
		ClassWriter cw = new ClassWriter(0);
		Type types[] = Read.from(parameterTypes).map(Type::getType).toList().toArray(new Type[0]);

		cw.visit(V1_8, //
				ACC_PUBLIC + ACC_SUPER, //
				className, //
				null, //
				Type.getInternalName(superClass), //
				new String[] { Type.getInternalName(interfaceClass), });

		for (Entry<String, Pair<String, Object>> entry : constants.entrySet())
			cw.visitField(ACC_PUBLIC | ACC_STATIC, entry.getKey(), entry.getValue().t0, null, null).visitEnd();

		for (Entry<String, String> entry : fields.entrySet())
			cw.visitField(ACC_PUBLIC, entry.getKey(), entry.getValue(), null, null).visitEnd();

		createMethod(cw, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false, mv -> {
			String cd = Type.getConstructorDescriptor(superClass.getConstructor());

			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(superClass), "<init>", cd, false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
		});

		createMethod(cw, methodName, Type.getMethodDescriptor(Type.getType(returnType), types), true, mv -> {
			OpStack os = new OpStack();
			os.list = IList.end();
			os.mv = mv;

			visit(os, expression);
			mv.visitInsn(choose(returnType, ARETURN, DRETURN, FRETURN, IRETURN, LRETURN));
			mv.visitMaxs(1 + parameterTypes.size(), 1 + parameterTypes.size() + localTypes.size());
		});

		cw.visitEnd();

		byte bytes[] = cw.toByteArray();

		Class<? extends I> clazz = new UnsafeUtil().defineClass(interfaceClass, className, bytes);

		for (Entry<String, Pair<String, Object>> entry : constants.entrySet())
			try {
				clazz.getField(entry.getKey()).set(null, entry.getValue().t1);
			} catch (ReflectiveOperationException ex) {
				throw new RuntimeException(ex);
			}

		return clazz;
	}

	public FunExpr add(FunExpr e0, FunExpr e1) {
		BinaryFunExpr expr = new BinaryFunExpr();
		expr.type = e0.type;
		expr.opcode = choose(expr.type, 0, DADD, FADD, IADD, LADD);
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr true_() {
		return constant(1);
	}

	public FunExpr false_() {
		return constant(0);
	}

	public FunExpr constant(int i) {
		return constant(i, int.class);
	}

	public FunExpr constant(Object object) {
		return constantStatic(object, object != null ? object.getClass() : Object.class);
	}

	private FunExpr constant(Object object, Class<?> clazz) {
		ConstantFunExpr expr = new ConstantFunExpr();
		expr.type = Type.getDescriptor(clazz);
		expr.constant = object;
		return expr;
	}

	private FunExpr constantStatic(Object object, Class<?> clazz) {
		String field = "f" + counter.getAndIncrement();
		String type = Type.getDescriptor(clazz);
		constants.put(field, Pair.of(type, object));

		StaticFunExpr expr = new StaticFunExpr();
		expr.clazzType = className;
		expr.field = field;
		expr.type = type;
		return expr;
	}

	public FunExpr field(String field) {
		return this_().field(field, fields.get(field));
	}

	public FunExpr if_(FunExpr if_, FunExpr then_, FunExpr else_) {
		int ifInsn = Opcodes.IFEQ;

		If1FunExpr expr = new If1FunExpr();
		expr.type = then_.type;
		expr.ifInsn = ifInsn;
		expr.if_ = if_;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifne(FunExpr left, FunExpr right, FunExpr then_, FunExpr else_) {
		int ifInsn = Opcodes.IF_ACMPEQ;

		If2FunExpr expr = new If2FunExpr();
		expr.type = then_.type;
		expr.ifInsn = ifInsn;
		expr.left = left;
		expr.right = right;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr local(FunExpr value, Fun<FunExpr, FunExpr> doFun) {
		int index = 1 + parameterTypes.size() + localTypes.size();
		localTypes.add(value.type);

		ParameterFunExpr pe = new ParameterFunExpr();
		pe.type = value.type;
		pe.index = index;

		FunExpr do_ = doFun.apply(pe);

		LocalFunExpr expr = new LocalFunExpr();
		expr.type = do_.type;
		expr.index = index;
		expr.value = value;
		expr.do_ = do_;
		return expr;
	}

	public FunExpr parameter(int number) { // 0 means this
		ParameterFunExpr expr = new ParameterFunExpr();
		expr.type = 0 < number ? Type.getDescriptor(parameterTypes.get(number - 1)) : className;
		expr.index = number;
		return expr;
	}

	public FunExpr seq(FunExpr e0, FunExpr e1) {
		SeqFunExpr expr = new SeqFunExpr();
		expr.type = e0.type;
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	public FunExpr this_() {
		return parameter(0);
	}

	public I instantiate() {
		return Rethrow.ex(clazz::newInstance);
	}

	public Class<? extends I> get() {
		return clazz;
	}

	private void visit(OpStack os, FunExpr e) {
		MethodVisitor mv = os.mv;

		if (e instanceof BinaryFunExpr) {
			BinaryFunExpr expr = (BinaryFunExpr) e;
			visit(os, expr.left);
			visit(os.cons(expr.left.type), expr.right);
			mv.visitInsn(expr.opcode);
		} else if (e instanceof CastFunExpr) {
			CastFunExpr expr = (CastFunExpr) e;
			visit(os, expr.expr);
		} else if (e instanceof CheckCastFunExpr) {
			CheckCastFunExpr expr = (CheckCastFunExpr) e;
			visit(os, expr.expr);
			mv.visitTypeInsn(CHECKCAST, expr.type);
		} else if (e instanceof ConstantFunExpr) {
			ConstantFunExpr expr = (ConstantFunExpr) e;
			mv.visitLdcInsn(expr.constant);
		} else if (e instanceof FieldFunExpr) {
			FieldFunExpr expr = (FieldFunExpr) e;
			visit(os, expr.object);
			mv.visitFieldInsn(GETFIELD, className, expr.field, expr.type);
		} else if (e instanceof If1FunExpr) {
			If1FunExpr expr = (If1FunExpr) e;
			visit(os, expr.if_);
			visitIf(os, expr);
		} else if (e instanceof If2FunExpr) {
			If2FunExpr expr = (If2FunExpr) e;
			visit(os, expr.left);
			visit(os.cons(expr.left.type), expr.right);
			visitIf(os, expr);
		} else if (e instanceof InstanceOfFunExpr) {
			InstanceOfFunExpr expr = (InstanceOfFunExpr) e;
			visit(os, expr.object);
			mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(expr.instanceType));
		} else if (e instanceof InvokeFunExpr) {
			InvokeFunExpr expr = (InvokeFunExpr) e;
			Type array[] = Read.from(expr.parameters) //
					.map(parameter -> Type.getType(parameter.type)) //
					.toList() //
					.toArray(new Type[0]);

			if (expr.object != null) {
				visit(os, expr.object);
				os = os.cons(expr.object.type);
			}
			for (FunExpr parameter : expr.parameters) {
				visit(os, parameter);
				os = os.cons(parameter.type);
			}

			mv.visitMethodInsn( //
					expr.opcode, //
					expr.object.type, //
					expr.methodName, //
					Type.getMethodDescriptor(Type.getType(expr.type), array), //
					expr.opcode == Opcode.INVOKEINTERFACE);
		} else if (e instanceof LocalFunExpr) {
			LocalFunExpr expr = (LocalFunExpr) e;
			visit(os, expr.value);
			mv.visitVarInsn(choose(expr.value.type, ASTORE, DSTORE, FSTORE, ISTORE, LSTORE), expr.index);
			visit(os, expr.do_);
		} else if (e instanceof ParameterFunExpr) {
			ParameterFunExpr expr = (ParameterFunExpr) e;
			mv.visitVarInsn(choose(expr.type, ALOAD, DLOAD, FLOAD, ILOAD, LLOAD), expr.index);
		} else if (e instanceof PrintlnFunExpr) {
			PrintlnFunExpr expr = (PrintlnFunExpr) e;
			String td = Type.getDescriptor(PrintStream.class);
			mv.visitFieldInsn(GETSTATIC, Type.getInternalName(System.class), "out", td);
			visit(os.cons(td), expr.expression);
			mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrintStream.class), "println",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
		} else if (e instanceof SeqFunExpr) {
			SeqFunExpr expr = (SeqFunExpr) e;
			visit(os, expr.left);
			mv.visitInsn(POP);
			visit(os, expr.right);
		} else if (e instanceof StaticFunExpr) {
			StaticFunExpr expr = (StaticFunExpr) e;
			mv.visitFieldInsn(GETSTATIC, expr.clazzType, expr.field, expr.type);
		} else
			throw new RuntimeException("Unknown expression " + e.getClass());
	}

	private void createMethod(ClassWriter cw, String mn, String md, boolean isLog,
			SinkEx<MethodVisitor, ReflectiveOperationException> sink) {
		Textifier textifier = isLog ? new Textifier() : null;
		MethodVisitor mv0 = cw.visitMethod( //
				Opcodes.ACC_PUBLIC, //
				mn, //
				md, //
				null, //
				null);
		MethodVisitor mv = textifier != null ? new TraceMethodVisitor(mv0, textifier) : mv0;

		try {
			sink.sink(mv);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}

		mv.visitEnd();

		if (textifier != null) {
			String log;

			try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
				textifier.print(pw);
				log = sw.toString();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			System.out.println(log);
		}
	}

	private void visitIf(OpStack os, IfFunExpr expr) {
		MethodVisitor mv = os.mv;
		Label l0 = new Label();
		Label l1 = new Label();
		mv.visitJumpInsn(expr.ifInsn, l0);
		visit(os, expr.then);
		mv.visitJumpInsn(GOTO, l1);
		mv.visitLabel(l0);
		visitFrame(os);
		visit(os, expr.else_);
		mv.visitLabel(l1);
		visitFrame(os.cons(expr.then.type));
	}

	private void visitFrame(OpStack os) {
		Object locals[] = Streamlet
				.concat( //
						Read.from(className), //
						Read.from(parameterTypes).map(Type::getInternalName), //
						Read.from(localTypes)) //
				.map(t -> {
					System.out.println("t " + t);
					return choose_(t, t, Opcodes.DOUBLE, Opcodes.FLOAT, Opcodes.INTEGER, Opcodes.LONG);
				}) //
				.toList() //
				.toArray(new Object[0]);

		Object stack[] = Read.from(os.list.reverse()) //
				.map(t -> {
					System.out.println("t " + t);
					return choose_(t, t, Opcodes.DOUBLE, Opcodes.FLOAT, Opcodes.INTEGER, Opcodes.LONG);
				}) //
				.toList() //
				.toArray(new Object[0]);

		os.mv.visitFrame(Opcodes.F_FULL, locals.length, locals, stack.length, stack);
	}

	private Object choose_(String type, Object a, Object d, Object f, Object i, Object l) {
		if (Util.stringEquals(type, Type.getInternalName(double.class)))
			return d;
		else if (Util.stringEquals(type, Type.getInternalName(boolean.class)))
			return i;
		else if (Util.stringEquals(type, Type.getInternalName(float.class)))
			return f;
		else if (Util.stringEquals(type, Type.getInternalName(int.class)))
			return i;
		else if (Util.stringEquals(type, Type.getInternalName(long.class)))
			return l;
		else
			return a;
	}

	private int choose(String type, int a, int d, int f, int i, int l) {
		if (Util.stringEquals(type, Type.getDescriptor(double.class)))
			return d;
		else if (Util.stringEquals(type, Type.getDescriptor(boolean.class)))
			return i;
		else if (Util.stringEquals(type, Type.getDescriptor(float.class)))
			return f;
		else if (Util.stringEquals(type, Type.getDescriptor(int.class)))
			return i;
		else if (Util.stringEquals(type, Type.getDescriptor(long.class)))
			return l;
		else
			return a;
	}

}
