package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import suite.adt.Mutable;
import suite.adt.map.DblObjMap;
import suite.adt.map.ListMultimap;
import suite.adt.pair.DblObjPair;
import suite.adt.pair.Pair;
import suite.primitive.DblObjFunUtil;
import suite.primitive.DblObj_Dbl;
import suite.primitive.DblPrimitives.DblObjPredicate;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.DblPrimitives.DblPredicate;
import suite.primitive.Dbl_Dbl;
import suite.util.Array_;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.List_;
import suite.util.NullableSyncQueue;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.To;

public class DblObjOutlet<V> implements Iterable<DblObjPair<V>> {

	private DblObjSource<V> source;

	@SafeVarargs
	public static <V> DblObjOutlet<V> concat(DblObjOutlet<V>... outlets) {
		List<DblObjSource<V>> sources = new ArrayList<>();
		for (DblObjOutlet<V> outlet : outlets)
			sources.add(outlet.source);
		return of(DblObjFunUtil.concat(To.source(sources)));
	}

	public static <V> DblObjOutlet<V> empty() {
		return of(DblObjFunUtil.nullSource());
	}

	public static <V> DblObjOutlet<List<V>> of(ListMultimap<Double, V> multimap) {
		Iterator<Pair<Double, List<V>>> iter = multimap.listEntries().iterator();
		return of(pair -> {
			boolean b = iter.hasNext();
			if (b) {
				Pair<Double, List<V>> pair1 = iter.next();
				pair.t0 = pair1.t0;
				pair.t1 = pair1.t1;
			}
			return b;
		});
	}

	public static <V> DblObjOutlet<V> of(DblObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> DblObjOutlet<V> of(DblObjPair<V>... kvs) {
		return of(new DblObjSource<V>() {
			private int i;

			public boolean source2(DblObjPair<V> pair) {
				boolean b = i < kvs.length;
				if (b) {
					DblObjPair<V> kv = kvs[i];
					pair.t0 = kv.t0;
					pair.t1 = kv.t1;
				}
				return b;

			}
		});
	}

	public static <V> DblObjOutlet<V> of(Iterable<DblObjPair<V>> col) {
		Iterator<DblObjPair<V>> iter = col.iterator();
		return of(new DblObjSource<V>() {
			public boolean source2(DblObjPair<V> pair) {
				boolean b = iter.hasNext();
				if (b) {
					DblObjPair<V> pair1 = iter.next();
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
				}
				return b;
			}
		});
	}

	public static <V> DblObjOutlet<V> of(DblObjSource<V> source) {
		return new DblObjOutlet<>(source);
	}

	private DblObjOutlet(DblObjSource<V> source) {
		this.source = source;
	}

	@Override
	public Iterator<DblObjPair<V>> iterator() {
		return DblObjFunUtil.iterator(source);
	}

	public DblObjOutlet<V> append(Double key, V value) {
		return of(DblObjFunUtil.append(key, value, source));
	}

	public Outlet<DblObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(DblObjOutlet<V>::new, DblObjFunUtil.chunk(n, source)));
	}

	public DblObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			boolean b = next(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<DblObjOutlet<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(DblObj_Obj<V, Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(DblObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(DblObj_Obj<V, Outlet2<K1, V1>> fun) {
		return Outlet2.of(FunUtil2.concat(DblObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <V1> DblObjOutlet<V1> concatMapDblObj(DblObj_Obj<V, DblObjOutlet<V1>> fun) {
		return of(DblObjFunUtil.concat(DblObjFunUtil.map((k, v) -> fun.apply(k, v).source, source)));
	}

	public <V1> DblObjOutlet<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(DblObjFunUtil.concat(DblObjFunUtil.map((k, v) -> {
			Source<V1> source = fun.apply(v).source();
			return pair -> {
				V1 value1 = source.source();
				boolean b = value1 != null;
				if (b) {
					pair.t0 = k;
					pair.t1 = value1;
				}
				return b;
			};
		}, source)));
	}

	public DblObjOutlet<V> cons(double key, V value) {
		return of(DblObjFunUtil.cons(key, value, source));
	}

	public DblObjOutlet<V> distinct() {
		Set<DblObjPair<V>> set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(DblObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public DblObjOutlet<V> drop(int n) {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblObjOutlet.class) {
			@SuppressWarnings("unchecked")
			DblObjOutlet<V> outlet = (DblObjOutlet<V>) (DblObjOutlet<?>) object;
			DblObjSource<V> source2 = outlet.source;
			boolean b, b0, b1;
			DblObjPair<V> pair0 = DblObjPair.of((double) 0, null);
			DblObjPair<V> pair1 = DblObjPair.of((double) 0, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public DblObjOutlet<V> filter(DblObjPredicate<V> fun) {
		return of(DblObjFunUtil.filter(fun, source));
	}

	public DblObjOutlet<V> filterKey(DblPredicate fun) {
		return of(DblObjFunUtil.filterKey(fun, source));
	}

	public DblObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(DblObjFunUtil.filterValue(fun, source));
	}

	public DblObjPair<V> first() {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		return next(pair) ? pair : null;
	}

	public <O> Outlet<O> flatMap(DblObj_Obj<V, Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(DblObjFunUtil.map(fun, source)));
	}

	public DblObjOutlet<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> DblObjOutlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		while (next(pair))
			hashCode = hashCode * 31 + pair.hashCode();
		return hashCode;
	}

	public boolean isAll(DblObjPredicate<V> pred) {
		return DblObjFunUtil.isAll(pred, source);
	}

	public boolean isAny(DblObjPredicate<V> pred) {
		return DblObjFunUtil.isAny(pred, source);
	}

	public Outlet<Double> keys() {
		return map_((k, v) -> k);
	}

	public DblObjPair<V> last() {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		if (next(pair))
			while (next(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Outlet<O> map(DblObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Outlet2<K1, V1> map2(DblObj_Obj<V, K1> kf, DblObj_Obj<V, V1> vf) {
		return Outlet2.of(DblObjFunUtil.map2(kf, vf, source));
	}

	public <V1> DblObjOutlet<V1> mapDblObj(DblObj_Dbl<V> kf, DblObj_Obj<V, V1> vf) {
		return mapDblObj_(kf, vf);
	}

	public DblObjOutlet<V> mapKey(Dbl_Dbl fun) {
		return mapDblObj_((k, v) -> fun.apply(k), (k, v) -> v);
	}

	public <O> Outlet<O> mapNonNull(DblObj_Obj<V, O> fun) {
		return Outlet.of(DblObjFunUtil.mapNonNull(fun, source));
	}

	public <V1> DblObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapDblObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public DblObjPair<V> min(Comparator<DblObjPair<V>> comparator) {
		DblObjPair<V> pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			throw new RuntimeException("no result");
	}

	public DblObjPair<V> minOrNull(Comparator<DblObjPair<V>> comparator) {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		DblObjPair<V> pair1 = DblObjPair.of((double) 0, null);
		boolean b = next(pair);
		if (b) {
			while (next(pair1))
				if (0 < comparator.compare(pair, pair1)) {
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
				}
			return pair;
		} else
			return null;
	}

	public DblObjOutlet<V> nonBlocking(Double k0, V v0) {
		NullableSyncQueue<DblObjPair<V>> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			boolean b;
			do {
				DblObjPair<V> pair = DblObjPair.of((double) 0, null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new DblObjOutlet<>(pair -> {
			Mutable<DblObjPair<V>> mutable = Mutable.nil();
			boolean b = queue.poll(mutable);
			if (b) {
				DblObjPair<V> p = mutable.get();
				pair.t0 = p.t0;
				pair.t1 = p.t1;
			} else {
				pair.t0 = k0;
				pair.t1 = v0;
			}
			return b;
		});
	}

	public Outlet<DblObjPair<V>> pairs() {
		return Outlet.of(() -> {
			DblObjPair<V> pair = DblObjPair.of((double) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<DblObjOutlet<V>, DblObjOutlet<V>> partition(DblObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public DblObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(BiConsumer<Double, V> sink0) {
		BiConsumer<Double, V> sink1 = Rethrow.biConsumer(sink0);
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		while (next(pair))
			sink1.accept(pair.t0, pair.t1);
	}

	public int size() {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		int i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public DblObjOutlet<V> skip(int n) {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(source) : empty();
	}

	public DblObjOutlet<V> sort(Comparator<DblObjPair<V>> comparator) {
		List<DblObjPair<V>> list = new ArrayList<>();
		DblObjPair<V> pair;
		while (next(pair = DblObjPair.of((double) 0, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> DblObjOutlet<V> sortBy(DblObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.t0, e0.t1), fun.apply(e1.t0, e1.t1)));
	}

	public DblObjOutlet<V> sortByKey(Comparator<Double> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t0, e1.t0));
	}

	public DblObjOutlet<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t1, e1.t1));
	}

	public DblObjSource<V> source() {
		return source;
	}

	public Outlet<DblObjOutlet<V>> split(DblObjPredicate<V> fun) {
		return Outlet.of(FunUtil.map(DblObjOutlet<V>::new, DblObjFunUtil.split(fun, source)));
	}

	public DblObjOutlet<V> take(int n) {
		return of(new DblObjSource<V>() {
			private int count = n;

			public boolean source2(DblObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public DblObjPair<V>[] toArray() {
		List<DblObjPair<V>> list = toList();
		@SuppressWarnings("unchecked")
		DblObjPair<V>[] array = Array_.newArray(DblObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<DblObjPair<V>> toList() {
		List<DblObjPair<V>> list = new ArrayList<>();
		DblObjPair<V> pair;
		while (next(pair = DblObjPair.of((double) 0, null)))
			list.add(pair);
		return list;
	}

	public DblObjMap<List<V>> toListMap() {
		DblObjMap<List<V>> map = new DblObjMap<>();
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public DblObjMap<V> toMap() {
		DblObjMap<V> map = new DblObjMap<>();
		groupBy().mapValue(values -> Read.from(values).uniqueResult()).sink(map::put);
		return map;
	}

	public ListMultimap<Double, V> toMultimap() {
		ListMultimap<Double, V> map = new ListMultimap<>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public Set<DblObjPair<V>> toSet() {
		Set<DblObjPair<V>> set = new HashSet<>();
		DblObjPair<V> pair;
		while (next(pair = DblObjPair.of((double) 0, null)))
			set.add(pair);
		return set;

	}

	public DblObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public DblObjPair<V> uniqueResult() {
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				throw new RuntimeException("more than one result");
		else
			throw new RuntimeException("no result");
	}

	public Outlet<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Outlet<O> map_(DblObj_Obj<V, O> fun0) {
		return Outlet.of(DblObjFunUtil.map(fun0, source));
	}

	private <V1> DblObjOutlet<V1> mapDblObj_(DblObj_Dbl<V> kf, DblObj_Obj<V, V1> vf) {
		return of(DblObjFunUtil.mapDblObj(kf, vf, source));
	}

	private boolean next(DblObjPair<V> pair) {
		return source.source2(pair);
	}

}
