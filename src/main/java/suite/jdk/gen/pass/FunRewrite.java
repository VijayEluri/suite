package suite.jdk.gen.pass;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.Type;

import suite.adt.pair.Pair;
import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExprK.Declare0ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare1ParameterFunExpr;
import suite.jdk.gen.FunExprK.Declare2ParameterFunExpr;
import suite.jdk.gen.FunExprK.DeclareParameterFunExpr;
import suite.jdk.gen.FunExprK.PlaceholderFunExpr;
import suite.jdk.gen.FunExprL.ApplyFunExpr;
import suite.jdk.gen.FunExprL.DeclareLocalFunExpr;
import suite.jdk.gen.FunExprL.FieldFunExpr_;
import suite.jdk.gen.FunExprL.FieldInjectFunExpr;
import suite.jdk.gen.FunExprL.FieldSetFunExpr;
import suite.jdk.gen.FunExprL.InvokeLambdaFunExpr;
import suite.jdk.gen.FunExprL.ObjectFunExpr;
import suite.jdk.gen.FunExprM.AssignLocalFunExpr;
import suite.jdk.gen.FunExprM.CastFunExpr;
import suite.jdk.gen.FunExprM.FieldStaticFunExpr;
import suite.jdk.gen.FunExprM.NewFunExpr;
import suite.jdk.gen.FunExprM.ProfileFunExpr;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.jdk.gen.Type_;
import suite.jdk.lambda.LambdaImplementation;
import suite.jdk.lambda.LambdaInstance;
import suite.jdk.lambda.LambdaInterface;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.Fail;
import suite.util.Rethrow;
import suite.util.Util;

public class FunRewrite extends FunFactory {

	public final FunExpr expr;
	public final Map<String, Pair<Type, Object>> fieldTypeValues = new HashMap<>();
	public final FunTypeInformation fti;

	private List<Type> localTypes;
	private Map<String, Type> fieldTypes;
	private Map<PlaceholderFunExpr, FunExpr> placeholders = new HashMap<>();

	public FunRewrite(Map<String, Type> fieldTypes, List<Type> parameterTypes, FunExpr expr0) {
		this.fieldTypes = fieldTypes;
		this.localTypes = new ArrayList<>(parameterTypes);
		this.fti = new FunTypeInformation(localTypes, placeholders::get);
		this.expr = rewriteFun(expr0);
	}

	private FunExpr rewriteFun(FunExpr e0) {
		return e0.<FunExpr> switch_( //
		).applyIf(CastFunExpr.class, //
				e1 -> e1.expr.<FunExpr> switch_( //
				).applyIf(Declare0ParameterFunExpr.class, e2 -> {
					return rewrite(e2.do_);
				}).applyIf(Declare1ParameterFunExpr.class, e2 -> {
					placeholders.put(e2.parameter, local(1));
					return rewrite(e2.do_);
				}).applyIf(Declare2ParameterFunExpr.class, e2 -> {
					placeholders.put(e2.p0, local(1));
					placeholders.put(e2.p1, local(2));
					return rewrite(e2.do_);
				}).nonNullResult() //
		).nonNullResult();
	}

	private FunExpr rewrite(FunExpr expr0) {
		return rewrite(this::rewrite_, expr0);
	}

	private FunExpr rewrite_(FunExpr e0) {
		return e0.<FunExpr> switch_( //
		).applyIf(ApplyFunExpr.class, e1 -> {
			var object = rewrite(e1.object);
			var parameters = Read.from(e1.parameters).map(this::rewrite).toArray(FunExpr.class);
			var method = fti.methodOf(object);
			return object.invoke(method.getName(), parameters);
		}).applyIf(CastFunExpr.class, e1 -> {
			var e2 = e1.expr;

			if (e2 instanceof DeclareParameterFunExpr) {
				Class<?> interfaceClass = Type_.classOf(e1.type);
				Map<String, Type> fieldTypes = new HashMap<>();
				Map<String, FunExpr> fieldValues = new HashMap<>();

				var e3 = rewrite(e -> {
					FunExpr fieldValue;
					if (e instanceof FieldStaticFunExpr) {
						var e_ = (FieldStaticFunExpr) e;
						var fieldName = e_.fieldName;
						var fieldType = fieldTypes.get(fieldName);
						fieldTypes.put(fieldName, fieldType);
						fieldValues.put(fieldName, e_);
						return e;
					} else if (e instanceof PlaceholderFunExpr && (fieldValue = placeholders.get(e)) != null) {
						var fieldName = "e" + Util.temp();
						var fieldType = fti.typeOf(fieldValue);
						fieldTypes.put(fieldName, fieldType);
						fieldValues.put(fieldName, fieldValue);
						return this_().field(fieldName, fieldType);
					} else
						return null;
				}, e2);

				FunCreator<?>.CreateClass cc = FunCreator.of(LambdaInterface.of(interfaceClass), fieldTypes).create_(e3);
				Streamlet2<String, FunExpr> fieldValues0 = Read.from2(cc.fieldTypeValues).mapValue(tv -> objectField(tv.t1, tv.t0));
				var fieldValues1 = Read.from2(fieldValues);

				var e4 = new NewFunExpr();
				e4.className = cc.className;
				e4.fieldValues = Streamlet2.concat(fieldValues0, fieldValues1).toMap();
				e4.implementationClass = cc.clazz;
				e4.interfaceClass = interfaceClass;
				return e4;
			} else
				return null;
		}).applyIf(DeclareLocalFunExpr.class, e1 -> {
			var value = rewrite(e1.value);
			var lfe = local(localTypes.size());
			localTypes.add(fti.typeOf(value));

			var alfe = new AssignLocalFunExpr();
			alfe.var = lfe;
			alfe.value = value;

			placeholders.put(e1.var, lfe);
			return seq(alfe, rewrite(e1.do_));
		}).applyIf(FieldFunExpr_.class, e1 -> {
			var set = e1 instanceof FieldSetFunExpr ? ((FieldSetFunExpr) e1).value : null;
			var object0 = rewrite(e1.object);
			var fieldName = e1.fieldName;
			Class<?> clazz = fti.classOf(object0);
			Field field = Rethrow.ex(() -> clazz.getField(fieldName));
			var object1 = object0.cast_(field.getDeclaringClass());
			var fieldType = Type.getType(field.getType());
			return set == null ? object1.field(fieldName, fieldType) : object1.fieldSet(fieldName, fieldType, set);
		}).applyIf(FieldInjectFunExpr.class, e1 -> {
			var type = fieldTypes.get(e1.fieldName);
			if (type != null)
				return rewrite(this_().field(e1.fieldName, type));
			else
				return Fail.t(e1.fieldName);
		}).applyIf(InvokeLambdaFunExpr.class, e1 -> {
			LambdaInstance<?> l_inst = e1.lambda;
			LambdaImplementation<?> l_impl = l_inst.lambdaImplementation;
			LambdaInterface<?> l_iface = l_impl.lambdaInterface;
			FunExpr object = object_(l_impl.newFun(l_inst.fieldValues), l_iface.interfaceClass);

			return rewrite(object.invoke(l_iface.interfaceClass, l_iface.methodName, e1.parameters));
		}).applyIf(ObjectFunExpr.class, e1 -> {
			return objectField(e1.object, e1.type);
		}).applyIf(PlaceholderFunExpr.class, e1 -> {
			var e2 = placeholders.get(e1);
			if (e2 != null)
				return e2;
			else
				return Fail.t("cannot resolve placeholder");
		}).applyIf(ProfileFunExpr.class, e1 -> {
			fieldTypeValues.put(e1.counterFieldName, Pair.of(Type.INT, 0));
			return null;
		}).result();
	}

	private FunExpr objectField(Object object, Type type) {
		var fieldName = "o" + Util.temp();
		fieldTypeValues.put(fieldName, Pair.of(type, object));
		return rewrite(this_().field(fieldName, type));
	}

	private FunExpr this_() {
		return local(0);
	}

}
