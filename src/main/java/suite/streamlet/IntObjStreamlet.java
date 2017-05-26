package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import suite.adt.map.IntObjMap;
import suite.adt.map.ListMultimap;
import suite.adt.pair.IntObjPair;
import suite.adt.pair.Pair;
import suite.primitive.IntObjFunUtil;
import suite.primitive.IntObj_Int;
import suite.primitive.IntPrimitiveFun.IntObj_Obj;
import suite.primitive.IntPrimitiveFun.Obj_Int;
import suite.primitive.IntPrimitivePredicate.IntObjPredicate;
import suite.primitive.IntPrimitivePredicate.IntPredicate_;
import suite.primitive.IntPrimitiveSource.IntObjSource;
import suite.primitive.Int_Int;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.primitive.PrimitiveFun.Obj_Double;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class IntObjStreamlet<V> implements Iterable<IntObjPair<V>> {

	private Source<IntObjOutlet<V>> in;

	@SafeVarargs
	public static <V> IntObjStreamlet<V> concat(IntObjStreamlet<V>... streamlets) {
		return intObjStreamlet(() -> {
			List<IntObjSource<V>> sources = new ArrayList<>();
			for (IntObjStreamlet<V> streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return IntObjOutlet.of(IntObjFunUtil.concat(To.source(sources)));
		});
	}

	private static <V> IntObjStreamlet<V> intObjStreamlet(Source<IntObjOutlet<V>> in) {
		return new IntObjStreamlet<>(in);
	}

	public IntObjStreamlet(Source<IntObjOutlet<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<IntObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public IntObjStreamlet<V> append(int key, V value) {
		return intObjStreamlet(() -> spawn().append(key, value));
	}

	public IntObjStreamlet<V> closeAtEnd(Closeable c) {
		return intObjStreamlet(() -> {
			IntObjOutlet<V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<IntObjOutlet<V>, R> fun) {
		return fun.apply(spawn());
	}

	public double collectAsDouble(Obj_Double<IntObjOutlet<V>> fun) {
		return fun.applyAsDouble(spawn());
	}

	public int collectAsInt(Obj_Int<IntObjOutlet<V>> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(ObjObj_Obj<Integer, V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(ObjObj_Obj<Integer, V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> IntObjStreamlet<V1> concatMapIntObj(ObjObj_Obj<Integer, V, IntObjStreamlet<V1>> fun) {
		return concatMapIntObj_(fun);
	}

	public <V1> IntObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return intObjStreamlet(() -> IntObjOutlet.of(spawn().concatMapValue(f)));
	}

	public IntObjStreamlet<V> cons(int key, V value) {
		return intObjStreamlet(() -> spawn().cons(key, value));
	}

	public IntObjStreamlet<V> distinct() {
		return intObjStreamlet(() -> spawn().distinct());
	}

	public IntObjStreamlet<V> drop(int n) {
		return intObjStreamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == IntObjStreamlet.class ? Objects.equals(spawn(), ((IntObjStreamlet<?>) object).spawn())
				: false;
	}

	public IntObjStreamlet<V> filter(IntObjPredicate<V> fun) {
		return intObjStreamlet(() -> spawn().filter(fun));
	}

	public IntObjStreamlet<V> filterKey(IntPredicate_ fun) {
		return intObjStreamlet(() -> spawn().filterKey(fun));
	}

	public IntObjStreamlet<V> filterValue(Predicate<V> fun) {
		return intObjStreamlet(() -> spawn().filterValue(fun));
	}

	public IntObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(IntObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public IntObjStreamlet<List<V>> groupBy() {
		return new IntObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V1> IntObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return new IntObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(IntObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(IntObjPredicate<V> pred) {
		return spawn().isAny(pred);
	}

	public Streamlet<Integer> keys() {
		return new Streamlet<>(() -> spawn().keys());
	}

	public IntObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(IntObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Integer, V1> map2(IntObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(IntObj_Obj<V, K1> kf, IntObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> IntObjStreamlet<V1> mapIntObj(IntObj_Int<V> kf, IntObj_Obj<V, V1> vf) {
		return mapIntObj_(kf, vf);
	}

	public IntObjStreamlet<V> mapKey(Int_Int fun) {
		return new IntObjStreamlet<>(() -> spawn().mapKey(fun));
	}

	public <O> Streamlet<O> mapNonNull(IntObj_Obj<V, O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public <V1> IntObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return new IntObjStreamlet<>(() -> spawn().mapValue(fun));
	}

	public IntObjPair<V> min(Comparator<IntObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public IntObjPair<V> minOrNull(Comparator<IntObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public IntObjOutlet<V> out() {
		return spawn();
	}

	public Streamlet<IntObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<IntObjStreamlet<V>, IntObjStreamlet<V>> partition(IntObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public IntObjStreamlet<V> reverse() {
		return intObjStreamlet(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Integer, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public IntObjStreamlet<V> skip(int n) {
		return intObjStreamlet(() -> spawn().skip(n));
	}

	public IntObjStreamlet<V> sort(Comparator<IntObjPair<V>> comparator) {
		return intObjStreamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> IntObjStreamlet<V> sortBy(IntObj_Obj<V, O> fun) {
		return intObjStreamlet(() -> spawn().sortBy(fun));
	}

	public IntObjStreamlet<V> sortByKey(Comparator<Integer> comparator) {
		return intObjStreamlet(() -> spawn().sortByKey(comparator));
	}

	public IntObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return intObjStreamlet(() -> spawn().sortByValue(comparator));
	}

	public IntObjSource<V> source() {
		return spawn().source();
	}

	public IntObjStreamlet<V> take(int n) {
		return intObjStreamlet(() -> spawn().take(n));
	}

	public IntObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<IntObjPair<V>> toList() {
		return spawn().toList();
	}

	public IntObjMap<List<V>> toListMap() {
		return spawn().toListMap();
	}

	public IntObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Integer, V> toMultimap() {
		return spawn().toMultimap();
	}

	public Set<IntObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public IntObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public IntObjPair<V> uniqueResult() {
		return spawn().uniqueResult();
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(ObjObj_Obj<Integer, V, Streamlet<T>> fun) {
		ObjObj_Obj<Integer, V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(ObjObj_Obj<Integer, V, Streamlet2<K1, V1>> fun) {
		ObjObj_Obj<Integer, V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).out();
		return new Streamlet2<>(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <V1> IntObjStreamlet<V1> concatMapIntObj_(ObjObj_Obj<Integer, V, IntObjStreamlet<V1>> fun) {
		ObjObj_Obj<Integer, V, IntObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).out();
		return intObjStreamlet(() -> IntObjOutlet.of(spawn().concatMapIntObj(bf)));
	}

	private <T> Streamlet<T> map_(IntObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(IntObj_Obj<V, K1> kf, IntObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> IntObjStreamlet<V1> mapIntObj_(IntObj_Int<V> kf, IntObj_Obj<V, V1> vf) {
		return new IntObjStreamlet<>(() -> spawn().mapIntObj(kf, vf));
	}

	private IntObjOutlet<V> spawn() {
		return in.source();
	}

}
