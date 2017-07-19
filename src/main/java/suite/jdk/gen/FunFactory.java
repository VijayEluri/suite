package suite.jdk.gen;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

import suite.inspect.Inspect;
import suite.jdk.gen.FunExprK.Declare0ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExprK.PlaceholderFunExpr;
import suite.jdk.gen.FunExprL.DeclareLocalFunExpr;
import suite.jdk.gen.FunExprL.FieldInjectFunExpr;
import suite.jdk.gen.FunExprL.InvokeLambdaFunExpr;
import suite.jdk.gen.FunExprL.ObjectFunExpr;
import suite.jdk.gen.FunExprM.BinaryFunExpr;
import suite.jdk.gen.FunExprM.ConstantFunExpr;
import suite.jdk.gen.FunExprM.If1FunExpr;
import suite.jdk.gen.FunExprM.If2FunExpr;
import suite.jdk.gen.FunExprM.IfNonNullFunExpr;
import suite.jdk.gen.FunExprM.InvokeMethodFunExpr;
import suite.jdk.gen.FunExprM.LocalFunExpr;
import suite.jdk.gen.FunExprM.ProfileFunExpr;
import suite.jdk.gen.FunExprM.SeqFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.lambda.LambdaInstance;
import suite.node.util.Singleton;
import suite.streamlet.Read;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.BinOp;

public class FunFactory {

	private static Inspect inspect = Singleton.me.inspect;

	public FunExpr _false() {
		return int_(0);
	}

	public FunExpr _true() {
		return int_(1);
	}

	public FunExpr _null() {
		ConstantFunExpr expr = new ConstantFunExpr();
		expr.type = Type.NULL;
		expr.constant = null;
		return expr;
	}

	public FunExpr add(FunExpr e0, FunExpr e1) {
		return bi("+", e0, e1);
	}

	public FunExpr and(FunExpr... exprs) {
		if (0 < exprs.length) {
			List<FunExpr> list = Read.from(exprs).reverse().toList();
			FunExpr expr = list.get(0);
			for (int i = 1; i < exprs.length; i++)
				expr = if_(list.get(i), expr, _false());
			return expr;
		} else
			return _true();
	}

	public FunExpr bi(String op, FunExpr e0, FunExpr e1) {
		BinaryFunExpr expr = new BinaryFunExpr();
		expr.op = op;
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

	private FunExpr constant(Object constant, BasicType type) {
		ConstantFunExpr expr = new ConstantFunExpr();
		expr.type = type;
		expr.constant = constant;
		return expr;
	}

	public FunExpr declare(FunExpr value, Iterate<FunExpr> doFun) {
		PlaceholderFunExpr var = new PlaceholderFunExpr();

		DeclareLocalFunExpr expr = new DeclareLocalFunExpr();
		expr.var = var;
		expr.value = value;
		expr.do_ = doFun.apply(var);
		return expr;
	}

	public FunExpr if_(FunExpr if_, FunExpr then_, FunExpr else_) {
		If1FunExpr expr = new If1FunExpr();
		expr.if_ = if_;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifEquals(FunExpr left, FunExpr right, FunExpr then_, FunExpr else_) {
		If2FunExpr expr = new If2FunExpr();
		expr.opcode = t -> !Objects.equals(t, Type.INT) ? Const.IF_ACMPNE : Const.IF_ICMPNE;
		expr.left = left;
		expr.right = right;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifNonNullAnd(FunExpr object, FunExpr then_) {
		return ifNonNull(object, then_, _false());
	}

	public FunExpr ifNonNull(FunExpr object, FunExpr then_, FunExpr else_) {
		IfNonNullFunExpr expr = new IfNonNullFunExpr();
		expr.object = object;
		expr.then = then_;
		expr.else_ = else_;
		return expr;
	}

	public FunExpr ifInstanceAnd(Class<?> clazz, FunExpr object, Iterate<FunExpr> then_) {
		return ifInstance(clazz, object, then_, _false());
	}

	public FunExpr ifInstance(Class<?> clazz, FunExpr object, Iterate<FunExpr> then_, FunExpr else_) {
		return if_(object.instanceOf(clazz), declare(object.checkCast(clazz), o_ -> then_.apply(o_)), else_);
	}

	public FunExpr inject(String field) {
		FieldInjectFunExpr expr = new FieldInjectFunExpr();
		expr.fieldName = field;
		return expr;
	}

	public FunExpr int_(int i) {
		return constant(i, Type.INT);
	}

	public FunExpr invoke(LambdaInstance<?> lambda, FunExpr... parameters) {
		InvokeLambdaFunExpr expr = new InvokeLambdaFunExpr();
		expr.isExpand = false;
		expr.lambda = lambda;
		expr.parameters = Arrays.asList(parameters);
		return expr;
	}

	public FunExpr invokeStatic(Class<?> clazz, String methodName, FunExpr... parameters) {
		InvokeMethodFunExpr expr = new InvokeMethodFunExpr();
		expr.clazz = clazz;
		expr.methodName = methodName;
		expr.object = null;
		expr.parameters = Arrays.asList(parameters);
		return expr;
	}

	public FunExpr local(int number) { // 0 means this
		LocalFunExpr expr = new LocalFunExpr();
		expr.index = number;
		return expr;
	}

	public <T> FunExpr object(T object) {
		Class<? extends Object> clazz = object.getClass();
		Class<?>[] interfaces = clazz.getInterfaces();
		return object_(object, interfaces.length == 1 ? interfaces[0] : clazz);
	}

	public FunExpr object_(Object object, Class<?> clazz) {
		return object(object, Type.getType(clazz));
	}

	public FunExpr object(Object object, Type type) {
		ObjectFunExpr expr = new ObjectFunExpr();
		expr.type = type;
		expr.object = object;
		return expr;
	}

	public FunExpr parameter0(Source<FunExpr> doFun) {
		Declare0ParameterFunExpr expr = new Declare0ParameterFunExpr();
		expr.do_ = doFun.source();
		return expr;
	}

	public FunExpr parameter1(Iterate<FunExpr> doFun) {
		PlaceholderFunExpr parameter = new PlaceholderFunExpr();

		Declare1ParameterFunExpr expr = new Declare1ParameterFunExpr();
		expr.parameter = parameter;
		expr.do_ = doFun.apply(parameter);
		return expr;
	}

	public FunExpr parameter2(BinOp<FunExpr> doFun) {
		PlaceholderFunExpr p0 = new PlaceholderFunExpr();
		PlaceholderFunExpr p1 = new PlaceholderFunExpr();

		Declare2ParameterFunExpr expr = new Declare2ParameterFunExpr();
		expr.p0 = p0;
		expr.p1 = p1;
		expr.do_ = doFun.apply(p0, p1);
		return expr;
	}

	public FunExpr profile(FunExpr do_) {
		ProfileFunExpr expr = new ProfileFunExpr();
		expr.do_ = do_;
		return expr;
	}

	public FunExpr replace(FunExpr expr0, FunExpr from, FunExpr to) {
		return rewrite(e -> e.equals(from) ? to : null, expr0);
	}

	public FunExpr rewrite(Iterate<FunExpr> fun, FunExpr t0) {
		return inspect.rewrite(FunExpr.class, fun, t0);
	}

	public FunExpr seq(FunExpr e0, FunExpr e1) {
		SeqFunExpr expr = new SeqFunExpr();
		expr.left = e0;
		expr.right = e1;
		return expr;
	}

}
