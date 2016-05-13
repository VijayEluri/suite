package suite.adt;

import java.util.ArrayList;
import java.util.List;

import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

public class Ranges<T extends Comparable<? super T>> {

	public final List<Range<T>> ranges;

	private static class Builder<T extends Comparable<? super T>> {
		private List<Range<T>> list = new ArrayList<>();

		private void add(Range<T> range) {
			if (!range.isEmpty())
				list.add(range);
		}

		private Ranges<T> ranges() {
			return new Ranges<>(list);
		}
	}

	public static <T extends Comparable<? super T>> Ranges<T> intersect(Ranges<T> ranges0, Ranges<T> ranges1) {
		Source<Range<T>> source0 = To.source(ranges0.ranges);
		Source<Range<T>> source1 = To.source(ranges1.ranges);
		Range<T> range0 = source0.source();
		Range<T> range1 = source1.source();
		Builder<T> intersects = new Builder<>();
		T to;

		while (range0 != null && range1 != null) {
			if (nullMaxCompare(range0.to, range1.to) < 0) {
				to = range0.to;
				range0 = source0.source();
			} else {
				to = range1.to;
				range1 = source1.source();
			}

			intersects.add(Range.of(Util.min(range0.from, range0.from), to));
		}

		return intersects.ranges();
	}

	public static <T extends Comparable<? super T>> Ranges<T> minus(Ranges<T> ranges0, Ranges<T> ranges1) {
		return intersect(ranges0, ranges1.negate());
	}

	public static <T extends Comparable<? super T>> Ranges<T> union(Ranges<T> ranges0, Ranges<T> ranges1) {
		return intersect(ranges0.negate(), ranges1.negate()).negate();
	}

	public Ranges(List<Range<T>> ranges) {
		this.ranges = ranges;
	}

	public Ranges<T> negate() {
		return negate(null, null);
	}

	public Ranges<T> negate(T min, T max) {
		Builder<T> builder = new Builder<>();
		T t = min;
		for (Range<T> range : ranges) {
			ranges.add(Range.of(t, range.from));
			t = range.to;
		}
		builder.add(Range.of(t, max));
		return builder.ranges();
	}

	private static <T extends Comparable<? super T>> int nullMaxCompare(T t0, T t1) {
		if (t0 == null ^ t1 == null)
			return t0 != null ? -1 : 1;
		else
			return t0 != null ? t0.compareTo(t1) : 0;
	}

}
