package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.LngFunUtil;
import suite.primitive.LngOpt;
import suite.primitive.LngPrimitives.LngComparator;
import suite.primitive.LngPrimitives.LngObj_Obj;
import suite.primitive.LngPrimitives.LngPredicate;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.LngPrimitives.Lng_Obj;
import suite.primitive.Lng_Lng;
import suite.primitive.Longs;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.adt.map.LngObjMap;
import suite.primitive.adt.set.LngSet;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
import suite.util.Object_;

public class LngStreamlet implements StreamletDefaults<Long, LngOutlet> {

	private Source<LngOutlet> in;

	@SafeVarargs
	public static LngStreamlet concat(LngStreamlet... streamlets) {
		return Read.from(streamlets).collect(LngStreamlet::concat);
	}

	public static LngStreamlet concat(Outlet<LngStreamlet> streamlets) {
		return streamlet(() -> {
			Source<LngStreamlet> source = streamlets.source();
			return LngOutlet.of(LngFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static LngStreamlet streamlet(Source<LngOutlet> in) {
		return new LngStreamlet(in);
	}

	public LngStreamlet(Source<LngOutlet> in) {
		this.in = in;
	}

	@Override
	public Iterator<Long> iterator() {
		return spawn().iterator();
	}

	public LngStreamlet append(long c) {
		return streamlet(() -> spawn().append(c));
	}

	public <R> R apply(Fun<LngStreamlet, R> fun) {
		return fun.apply(this);
	}

	public long average() {
		return spawn().average();
	}

	public Streamlet<LngOutlet> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public LngStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			LngOutlet in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(Lng_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Lng_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public LngStreamlet cons(long c) {
		return streamlet(() -> spawn().cons(c));
	}

	public <U, O> Streamlet<O> cross(Streamlet<U> st1, LngObj_Obj<U, O> fun) {
		return new Streamlet<>(() -> spawn().cross(st1.toList(), fun));
	}

	public LngStreamlet distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public LngStreamlet drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == LngStreamlet.class ? Objects.equals(spawn(), ((LngStreamlet) object).spawn()) : false;
	}

	public LngStreamlet filter(LngPredicate fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public long first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(Lng_Obj<Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, LngObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<LngStreamlet, U> fork0, Fun<LngStreamlet, V> fork1, Fun2<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> LngObjStreamlet<LongsBuilder> groupBy() {
		return new LngObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V> LngObjStreamlet<V> groupBy(Fun<Longs, V> fun) {
		return new LngObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public LngObjStreamlet<Integer> index() {
		return new LngObjStreamlet<>(() -> spawn().index());
	}

	public boolean isAll(LngPredicate pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(LngPredicate pred) {
		return spawn().isAny(pred);
	}

	public <O> Streamlet2<Long, O> join2(Streamlet<O> streamlet) {
		return concatMap2_(t -> streamlet.map2(v -> t, v -> v));
	}

	public long last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(Lng_Obj<O> fun) {
		return map_(fun);
	}

	public <K, V> Streamlet2<K, V> map2(Lng_Obj<K> kf, Lng_Obj<V> vf) {
		return map2_(kf, vf);
	}

	public LngStreamlet mapLng(Lng_Lng fun) {
		return streamlet(() -> spawn().mapLng(fun));
	}

	public <K, V> LngObjStreamlet<V> mapLngObj(Lng_Obj<V> fun0) {
		return new LngObjStreamlet<>(() -> spawn().mapLngObj(fun0));
	}

	public LngStreamlet memoize() {
		Longs list = toList().toLongs();
		return streamlet(() -> LngOutlet.of(list));
	}

	public long max() {
		return spawn().max();
	}

	public long min() {
		return spawn().min();
	}

	public long min(LngComparator comparator) {
		return spawn().min(comparator);
	}

	public long minOrEmpty(LngComparator comparator) {
		return spawn().minOrEmpty(comparator);
	}

	public LngOpt opt() {
		return spawn().opt();
	}

	public LngOutlet outlet() {
		return spawn();
	}

	public Pair<LngStreamlet, LngStreamlet> partition(LngPredicate pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public LngStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(LngSink sink) {
		spawn().sink(sink);
	}

	public LngStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public LngStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public LngSource source() {
		return spawn().source();
	}

	public long sum() {
		return spawn().sum();
	}

	public LngStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public long[] toArray() {
		return spawn().toArray();
	}

	public LongsBuilder toList() {
		return spawn().toList();
	}

	public <K> LngObjMap<LongsBuilder> toListMap() {
		return spawn().toListMap();
	}

	public <K> LngObjMap<LongsBuilder> toListMap(Lng_Lng valueFun) {
		return spawn().toListMap(valueFun);
	}

	public <K> Map<K, Long> toMap(Lng_Obj<K> keyFun) {
		return spawn().toMap(keyFun);
	}

	public <K, V> Map<K, V> toMap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Long> toMultimap(Lng_Obj<K> keyFun) {
		return spawn().toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return spawn().toMultimap(keyFun, valueFun);
	}

	public LngSet toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public long uniqueResult() {
		return spawn().opt().get();
	}

	private <O> Streamlet<O> concatMap_(Lng_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).outlet()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Lng_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).outlet()));
	}

	private <O> Streamlet<O> map_(Lng_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Lng_Obj<K> kf, Lng_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private LngOutlet spawn() {
		return in.source();
	}

}
