package suite.primitive;

import java.util.Objects;

import suite.adt.Opt;
import suite.primitive.ChrPrimitives.ChrPredicate;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.util.Object_;

public class ChrOpt {

	private static ChrOpt none_ = ChrOpt.of(ChrFunUtil.EMPTYVALUE);
	private char value;

	public static ChrOpt none() {
		return none_;
	}

	public static ChrOpt of(char t) {
		ChrOpt p = new ChrOpt();
		p.value = t;
		return p;
	}

	public boolean isEmpty() {
		return value == ChrFunUtil.EMPTYVALUE;
	}

	public ChrOpt filter(ChrPredicate pred) {
		return isEmpty() || pred.test(value) ? this : none();
	}

	public <T> Opt<T> map(Chr_Obj<T> fun) {
		return !isEmpty() ? Opt.of(fun.apply(value)) : Opt.none();
	}

	public char get() {
		if (!isEmpty())
			return value;
		else
			throw new RuntimeException("no result");
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == ChrOpt.class && Objects.equals(value, ((ChrOpt) object).value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return value != ChrFunUtil.EMPTYVALUE ? Character.toString(value) : "null";
	}

}
