package suite.adt.pair;

import java.util.Comparator;
import java.util.Objects;

import suite.adt.pair.Fixie_.Get0;
import suite.adt.pair.Fixie_.Get1;
import suite.adt.pair.Fixie_.Get2;
import suite.adt.pair.Fixie_.Get3;
import suite.adt.pair.Fixie_.Get4;
import suite.adt.pair.Fixie_.Get5;
import suite.adt.pair.Fixie_.Get6;
import suite.adt.pair.Fixie_.Get7;
import suite.adt.pair.Fixie_.Get8;
import suite.adt.pair.Fixie_.Get9;
import suite.util.Object_;

public class Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> implements //
		Get0<T0>, //
		Get1<T0, T1>, //
		Get2<T0, T1, T2>, //
		Get3<T0, T1, T2, T3>, //
		Get4<T0, T1, T2, T3, T4>, //
		Get5<T0, T1, T2, T3, T4, T5>, //
		Get6<T0, T1, T2, T3, T4, T5, T6>, //
		Get7<T0, T1, T2, T3, T4, T5, T6, T7>, //
		Get8<T0, T1, T2, T3, T4, T5, T6, T7, T8>, //
		Get9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> {

	private static D_ D = new D_();

	public T0 t0;
	public T1 t1;
	public T2 t2;
	public T3 t3;
	public T4 t4;
	public T5 t5;
	public T6 t6;
	public T7 t7;
	public T8 t8;
	public T9 t9;

	public static Fixie<D_, D_, D_, D_, D_, D_, D_, D_, D_, D_> //
			of() {
		return of(D);
	}

	public static <T0> Fixie<T0, D_, D_, D_, D_, D_, D_, D_, D_, D_> //
			of(T0 t0) {
		return of(t0, D);
	}

	public static <T0, T1> Fixie<T0, T1, D_, D_, D_, D_, D_, D_, D_, D_> //
			of(T0 t0, T1 t1) {
		return of(t0, t1, D);
	}

	public static <T0, T1, T2> Fixie<T0, T1, T2, D_, D_, D_, D_, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2) {
		return of(t0, t1, t2, D);
	}

	public static <T0, T1, T2, T3> Fixie<T0, T1, T2, T3, D_, D_, D_, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3) {
		return of(t0, t1, t2, t3, D);
	}

	public static <T0, T1, T2, T3, T4> Fixie<T0, T1, T2, T3, T4, D_, D_, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4) {
		return of(t0, t1, t2, t3, t4, D);
	}

	public static <T0, T1, T2, T3, T4, T5> Fixie<T0, T1, T2, T3, T4, T5, D_, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
		return of(t0, t1, t2, t3, t4, t5, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6> Fixie<T0, T1, T2, T3, T4, T5, T6, D_, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
		return of(t0, t1, t2, t3, t4, t5, t6, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7> Fixie<T0, T1, T2, T3, T4, T5, T6, T7, D_, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
		return of(t0, t1, t2, t3, t4, t5, t6, t7, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8> Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, D_> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
		return of(t0, t1, t2, t3, t4, t5, t6, t7, t8, D);
	}

	public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> //
			of(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
		return new Fixie<>(t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}

	public static class D_ {
	}

	private Fixie(T0 t0, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
		this.t0 = t0;
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
		this.t4 = t4;
		this.t5 = t5;
		this.t6 = t6;
		this.t7 = t7;
		this.t8 = t8;
		this.t9 = t9;
	}

	public static //
	<T0 extends Comparable<? super T0> //
			, T1 extends Comparable<? super T1> //
			, T2 extends Comparable<? super T2> //
			, T3 extends Comparable<? super T3> //
			, T4 extends Comparable<? super T4> //
			, T5 extends Comparable<? super T5> //
			, T6 extends Comparable<? super T6> //
			, T7 extends Comparable<? super T7> //
			, T8 extends Comparable<? super T8> //
			, T9 extends Comparable<? super T9> //
	> Comparator<Fixie<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9>> comparator() {
		return (fixie0, fixie1) -> {
			int c = 0;
			c = c == 0 ? Object_.compare(fixie0.t0, fixie1.t0) : c;
			c = c == 0 ? Object_.compare(fixie0.t1, fixie1.t1) : c;
			c = c == 0 ? Object_.compare(fixie0.t2, fixie1.t2) : c;
			c = c == 0 ? Object_.compare(fixie0.t3, fixie1.t3) : c;
			c = c == 0 ? Object_.compare(fixie0.t4, fixie1.t4) : c;
			c = c == 0 ? Object_.compare(fixie0.t5, fixie1.t5) : c;
			c = c == 0 ? Object_.compare(fixie0.t6, fixie1.t6) : c;
			c = c == 0 ? Object_.compare(fixie0.t7, fixie1.t7) : c;
			c = c == 0 ? Object_.compare(fixie0.t8, fixie1.t8) : c;
			c = c == 0 ? Object_.compare(fixie0.t9, fixie1.t9) : c;
			return c;
		};
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Fixie.class) {
			Fixie<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> other = (Fixie<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
			return true //
					&& Objects.equals(t0, other.t0) //
					&& Objects.equals(t1, other.t1) //
					&& Objects.equals(t2, other.t2) //
					&& Objects.equals(t3, other.t3) //
					&& Objects.equals(t4, other.t4) //
					&& Objects.equals(t5, other.t5) //
					&& Objects.equals(t6, other.t6) //
					&& Objects.equals(t7, other.t7) //
					&& Objects.equals(t8, other.t8) //
					&& Objects.equals(t9, other.t9) //
			;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		int h = 0;
		h = Objects.hashCode(t0) + 31 * h;
		h = Objects.hashCode(t1) + 31 * h;
		h = Objects.hashCode(t2) + 31 * h;
		h = Objects.hashCode(t3) + 31 * h;
		h = Objects.hashCode(t4) + 31 * h;
		h = Objects.hashCode(t5) + 31 * h;
		h = Objects.hashCode(t6) + 31 * h;
		h = Objects.hashCode(t7) + 31 * h;
		h = Objects.hashCode(t8) + 31 * h;
		h = Objects.hashCode(t9) + 31 * h;
		return h;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean cont = true;
		if (cont &= t0 != D)
			sb.append(t0.toString());
		if (cont &= t1 != D)
			sb.append(":" + t1.toString());
		if (cont &= t2 != D)
			sb.append(":" + t2.toString());
		if (cont &= t3 != D)
			sb.append(":" + t3.toString());
		if (cont &= t4 != D)
			sb.append(":" + t4.toString());
		if (cont &= t5 != D)
			sb.append(":" + t5.toString());
		if (cont &= t6 != D)
			sb.append(":" + t6.toString());
		if (cont &= t7 != D)
			sb.append(":" + t7.toString());
		if (cont &= t8 != D)
			sb.append(":" + t8.toString());
		if (cont &= t9 != D)
			sb.append(":" + t9.toString());
		return sb.toString();
	}

	@Override
	public T0 get0() {
		return t0;
	}

	@Override
	public T1 get1() {
		return t1;
	}

	@Override
	public T2 get2() {
		return t2;
	}

	@Override
	public T3 get3() {
		return t3;
	}

	@Override
	public T4 get4() {
		return t4;
	}

	@Override
	public T5 get5() {
		return t5;
	}

	@Override
	public T6 get6() {
		return t6;
	}

	@Override
	public T7 get7() {
		return t7;
	}

	@Override
	public T8 get8() {
		return t8;
	}

	@Override
	public T9 get9() {
		return t9;
	}

}
