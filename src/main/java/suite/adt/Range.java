package suite.adt;

import java.util.Objects;

import suite.util.Object_;

public class Range<T extends Comparable<? super T>> implements Comparable<Range<T>> {

	public final T from;
	public final T to;

	public static <T extends Comparable<? super T>> Range<T> of(T from, T to) {
		return new Range<>(from, to);
	}

	protected Range(T from, T to) {
		this.from = from;
		this.to = to;
	}

	public boolean contains(T t) {
		return from.compareTo(t) <= 0 && t.compareTo(to) < 0;
	}

	public Range<T> intersect(Range<T> other) {
		T fr0 = from;
		T fr1 = other.from;
		T to0 = to;
		T to1 = other.to;
		T fr = fr0.compareTo(fr1) < 0 ? fr1 : fr0;
		T to = to0.compareTo(to1) < 0 ? to0 : to1;
		return of(fr, to);
	}

	public boolean isEmpty() {
		return Object_.compare(from, to) < 0;
	}

	@Override
	public int compareTo(Range<T> other) {
		int c = 0;
		c = c == 0 ? from.compareTo(other.from) : c;
		c = c == 0 ? to.compareTo(other.to) : c;
		return c;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Range.class) {
			Range<?> other = (Range<?>) object;
			return Objects.equals(from, other.from) && Objects.equals(to, other.to);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(from) ^ Objects.hashCode(to);
	}

	@Override
	public String toString() {
		return from + "~" + to;
	}

}
