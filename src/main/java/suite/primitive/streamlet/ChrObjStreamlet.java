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
import suite.primitive.ChrFunUtil;
import suite.primitive.ChrObjFunUtil;
import suite.primitive.ChrObj_Chr;
import suite.primitive.ChrPrimitives.ChrObjPredicate;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.ChrObj_Obj;
import suite.primitive.ChrPrimitives.ChrTest;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.adt.map.ChrObjMap;
import suite.primitive.adt.map.ObjChrMap;
import suite.primitive.adt.pair.ChrObjPair;
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

public class ChrObjStreamlet<V> implements StreamletDefaults<ChrObjPair<V>, ChrObjOutlet<V>> {

	private Source<ChrObjOutlet<V>> in;

	public static <T, V> Fun<Outlet<T>, ChrObjStreamlet<V>> collect(Obj_Chr<T> kf0, Fun<T, V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return outlet -> streamlet(() -> {
			var source = outlet.source();
			return ChrObjOutlet.of(pair -> {
				var t = source.source();
				boolean b = t != null;
				if (b)
					pair.update(kf1.apply(t), vf1.apply(t));
				return b;
			});
		});
	}

	@SafeVarargs
	public static <V> ChrObjStreamlet<V> concat(ChrObjStreamlet<V>... streamlets) {
		return streamlet(() -> {
			var source = Read.from(streamlets).outlet().source();
			return ChrObjOutlet.of(ChrObjFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <V> ChrObjStreamlet<V> streamlet(Source<ChrObjOutlet<V>> in) {
		return new ChrObjStreamlet<>(in);
	}

	public ChrObjStreamlet(Source<ChrObjOutlet<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<ChrObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public ChrObjStreamlet<V> append(char key, V value) {
		return streamlet(() -> spawn().append(key, value));
	}

	public <R> R apply(Fun<ChrObjStreamlet<V>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<ChrObjOutlet<V>> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public ChrObjStreamlet<V> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(ChrObj_Obj<V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(ChrObj_Obj<V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> ChrObjStreamlet<V1> concatMapChrObj(ChrObj_Obj<V, ChrObjStreamlet<V1>> fun) {
		return concatMapChrObj_(fun);
	}

	public <V1> ChrObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return streamlet(() -> ChrObjOutlet.of(spawn().concatMapValue(f)));
	}

	public ChrObjStreamlet<V> cons(char key, V value) {
		return streamlet(() -> spawn().cons(key, value));
	}

	public ChrObjStreamlet<V> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public ChrObjStreamlet<V> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == ChrObjStreamlet.class ? Objects.equals(spawn(), ((ChrObjStreamlet<?>) object).spawn())
				: false;
	}

	public ChrObjStreamlet<V> filter(ChrObjPredicate<V> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public ChrObjStreamlet<V> filterKey(ChrTest fun) {
		return streamlet(() -> spawn().filterKey(fun));
	}

	public ChrObjStreamlet<V> filterValue(Predicate<V> fun) {
		return streamlet(() -> spawn().filterValue(fun));
	}

	public ChrObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(ChrObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public ChrObjStreamlet<List<V>> groupBy() {
		return streamlet(() -> spawn().groupBy());
	}

	public <V1> ChrObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return streamlet(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(ChrObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(ChrObjPredicate<V> pred) {
		return spawn().isAny(pred);
	}

	public ChrStreamlet keys() {
		return new ChrStreamlet(() -> spawn().keys());
	}

	public ChrObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(ChrObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Character, V1> map2(ChrObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(ChrObj_Obj<V, K1> kf, ChrObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> ChrObjStreamlet<V1> mapChrObj(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return mapChrObj_(kf, vf);
	}

	public <V1> ChrObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return streamlet(() -> spawn().mapValue(fun));
	}

	public ChrObjPair<V> min(Comparator<ChrObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public ChrObjPair<V> minOrNull(Comparator<ChrObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public ChrObjPair<V> opt() {
		return spawn().opt();
	}

	public ChrObjOutlet<V> outlet() {
		return spawn();
	}

	public Streamlet<ChrObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<ChrObjStreamlet<V>, ChrObjStreamlet<V>> partition(ChrObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public ChrObjStreamlet<V> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Character, V> sink) {
		spawn().sink(sink);
	}

	public ChrObjStreamlet<V> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public ChrObjStreamlet<V> sort(Comparator<ChrObjPair<V>> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> ChrObjStreamlet<V> sortBy(ChrObj_Obj<V, O> fun) {
		return streamlet(() -> spawn().sortBy(fun));
	}

	public ChrObjStreamlet<V> sortByKey(Comparator<Character> comparator) {
		return streamlet(() -> spawn().sortByKey(comparator));
	}

	public ChrObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return streamlet(() -> spawn().sortByValue(comparator));
	}

	public ChrObjSource<V> source() {
		return spawn().source();
	}

	public ChrObjStreamlet<V> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public ChrObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<ChrObjPair<V>> toList() {
		return spawn().toList();
	}

	public ChrObjMap<List<V>> toListMap() {
		return spawn().toListMap();
	}

	public ChrObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Character, V> toMultimap() {
		return spawn().toMultimap();
	}

	public ObjChrMap<V> toObjChrMap() {
		return spawn().toObjChrMap();
	}

	public Set<ChrObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public ChrObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public ChrObjPair<V> uniqueResult() {
		var pair = spawn().opt();
		if (pair.t0 != ChrFunUtil.EMPTYVALUE)
			return pair;
		else
			return Fail.t("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(ChrObj_Obj<V, Streamlet<T>> fun) {
		ChrObj_Obj<V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(ChrObj_Obj<V, Streamlet2<K1, V1>> fun) {
		ChrObj_Obj<V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet2<>(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <V1> ChrObjStreamlet<V1> concatMapChrObj_(ChrObj_Obj<V, ChrObjStreamlet<V1>> fun) {
		ChrObj_Obj<V, ChrObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return streamlet(() -> ChrObjOutlet.of(spawn().concatMapChrObj(bf)));
	}

	private <T> Streamlet<T> map_(ChrObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(ChrObj_Obj<V, K1> kf, ChrObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> ChrObjStreamlet<V1> mapChrObj_(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return streamlet(() -> spawn().mapChrObj(kf, vf));
	}

	private ChrObjOutlet<V> spawn() {
		return in.source();
	}

}
