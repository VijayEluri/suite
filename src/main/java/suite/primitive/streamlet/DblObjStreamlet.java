package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.DblFunUtil;
import suite.primitive.DblObjFunUtil;
import suite.primitive.DblObj_Dbl;
import suite.primitive.DblPrimitives.DblObjPredicate;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.DblPrimitives.DblTest;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.adt.map.DblObjMap;
import suite.primitive.adt.map.ObjDblMap;
import suite.primitive.adt.pair.DblObjPair;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;
import suite.util.Fail;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;

public class DblObjStreamlet<V> implements StreamletDefaults<DblObjPair<V>, DblObjOutlet<V>> {

	private Source<DblObjOutlet<V>> in;

	public static <T, V> Fun<Outlet<T>, DblObjStreamlet<V>> collect(Obj_Dbl<T> kf0, Fun<T, V> vf0) {
		Obj_Dbl<T> kf1 = kf0.rethrow();
		Fun<T, V> vf1 = vf0.rethrow();
		return outlet -> streamlet(() -> {
			Source<T> source = outlet.source();
			return DblObjOutlet.of(pair -> {
				T t = source.source();
				boolean b = t != null;
				if (b)
					pair.update(kf1.apply(t), vf1.apply(t));
				return b;
			});
		});
	}

	@SafeVarargs
	public static <V> DblObjStreamlet<V> concat(DblObjStreamlet<V>... streamlets) {
		return streamlet(() -> {
			Source<DblObjStreamlet<V>> source = Read.from(streamlets).outlet().source();
			return DblObjOutlet.of(DblObjFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <V> DblObjStreamlet<V> streamlet(Source<DblObjOutlet<V>> in) {
		return new DblObjStreamlet<>(in);
	}

	public DblObjStreamlet(Source<DblObjOutlet<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<DblObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public DblObjStreamlet<V> append(double key, V value) {
		return streamlet(() -> spawn().append(key, value));
	}

	public <R> R apply(Fun<DblObjStreamlet<V>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<DblObjOutlet<V>> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public DblObjStreamlet<V> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			DblObjOutlet<V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(DblObj_Obj<V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(DblObj_Obj<V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> DblObjStreamlet<V1> concatMapDblObj(DblObj_Obj<V, DblObjStreamlet<V1>> fun) {
		return concatMapDblObj_(fun);
	}

	public <V1> DblObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return streamlet(() -> DblObjOutlet.of(spawn().concatMapValue(f)));
	}

	public DblObjStreamlet<V> cons(double key, V value) {
		return streamlet(() -> spawn().cons(key, value));
	}

	public DblObjStreamlet<V> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public DblObjStreamlet<V> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == DblObjStreamlet.class ? Objects.equals(spawn(), ((DblObjStreamlet<?>) object).spawn())
				: false;
	}

	public DblObjStreamlet<V> filter(DblObjPredicate<V> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public DblObjStreamlet<V> filterKey(DblTest fun) {
		return streamlet(() -> spawn().filterKey(fun));
	}

	public DblObjStreamlet<V> filterValue(Predicate<V> fun) {
		return streamlet(() -> spawn().filterValue(fun));
	}

	public DblObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(DblObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public DblObjStreamlet<List<V>> groupBy() {
		return streamlet(() -> spawn().groupBy());
	}

	public <V1> DblObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return streamlet(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(DblObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(DblObjPredicate<V> pred) {
		return spawn().isAny(pred);
	}

	public DblStreamlet keys() {
		return new DblStreamlet(() -> spawn().keys());
	}

	public DblObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(DblObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Double, V1> map2(DblObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(DblObj_Obj<V, K1> kf, DblObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> DblObjStreamlet<V1> mapDblObj(DblObj_Dbl<V> kf, DblObj_Obj<V, V1> vf) {
		return mapDblObj_(kf, vf);
	}

	public <V1> DblObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return streamlet(() -> spawn().mapValue(fun));
	}

	public DblObjPair<V> min(Comparator<DblObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public DblObjPair<V> minOrNull(Comparator<DblObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public DblObjPair<V> opt() {
		return spawn().opt();
	}

	public DblObjOutlet<V> outlet() {
		return spawn();
	}

	public Streamlet<DblObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<DblObjStreamlet<V>, DblObjStreamlet<V>> partition(DblObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public DblObjStreamlet<V> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Double, V> sink) {
		spawn().sink(sink);
	}

	public DblObjStreamlet<V> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public DblObjStreamlet<V> sort(Comparator<DblObjPair<V>> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> DblObjStreamlet<V> sortBy(DblObj_Obj<V, O> fun) {
		return streamlet(() -> spawn().sortBy(fun));
	}

	public DblObjStreamlet<V> sortByKey(Comparator<Double> comparator) {
		return streamlet(() -> spawn().sortByKey(comparator));
	}

	public DblObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return streamlet(() -> spawn().sortByValue(comparator));
	}

	public DblObjSource<V> source() {
		return spawn().source();
	}

	public DblObjStreamlet<V> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public DblObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<DblObjPair<V>> toList() {
		return spawn().toList();
	}

	public DblObjMap<List<V>> toListMap() {
		return spawn().toListMap();
	}

	public DblObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Double, V> toMultimap() {
		return spawn().toMultimap();
	}

	public ObjDblMap<V> toObjDblMap() {
		return spawn().toObjDblMap();
	}

	public Set<DblObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public DblObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public DblObjPair<V> uniqueResult() {
		DblObjPair<V> pair = spawn().opt();
		if (pair.t0 != DblFunUtil.EMPTYVALUE)
			return pair;
		else
			return Fail.t("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(DblObj_Obj<V, Streamlet<T>> fun) {
		DblObj_Obj<V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(DblObj_Obj<V, Streamlet2<K1, V1>> fun) {
		DblObj_Obj<V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet2<>(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <V1> DblObjStreamlet<V1> concatMapDblObj_(DblObj_Obj<V, DblObjStreamlet<V1>> fun) {
		DblObj_Obj<V, DblObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return streamlet(() -> DblObjOutlet.of(spawn().concatMapDblObj(bf)));
	}

	private <T> Streamlet<T> map_(DblObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(DblObj_Obj<V, K1> kf, DblObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> DblObjStreamlet<V1> mapDblObj_(DblObj_Dbl<V> kf, DblObj_Obj<V, V1> vf) {
		return streamlet(() -> spawn().mapDblObj(kf, vf));
	}

	private DblObjOutlet<V> spawn() {
		return in.source();
	}

}
