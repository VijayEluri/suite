package suite.persistent;

import java.util.Comparator;
import java.util.List;

import suite.adt.pair.Pair;
import suite.file.ExtentAllocator.Extent;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.FileFactory;
import suite.file.impl.SerializedFileFactory;
import suite.fs.KeyValueMutator;
import suite.fs.KeyValueStore;
import suite.node.util.Singleton;
import suite.object.Object_;
import suite.serialize.Serialize;
import suite.serialize.Serialize.Serializer;

public class LazyPbTreeStore<Pointer, Key, Value> implements KeyValueStore<Key, Value> {

	private static Serialize ser = Singleton.me.serialize;

	private SerializedPageFile<Pointer> superblockFile;
	private LazyPbTreePersister<Pointer, Pair<Key, Value>> persister;
	private LazyPbTreeMutator<Key, Value> mutator;

	public static <K, V> LazyPbTreeStore<Extent, K, V> ofExtent( //
			PageFile pageFile, //
			Comparator<K> kc, //
			Serializer<K> ks, //
			Serializer<V> vs) {
		var pc = Comparator.nullsLast(Comparator.<Pair<K, V>, K> comparing(p -> p.t0, kc));
		var ps = ser.pair(ks, vs);
		var xs = ser.nullable(ser.extent());
		var pfs = FileFactory.subPageFiles(pageFile, 0, 1, Integer.MAX_VALUE);
		var superblockFile = SerializedFileFactory.serialized(pfs[0], xs);
		var persister = new LazyPbTreeExtentFilePersister<>(pfs[1], pc, ps);
		return new LazyPbTreeStore<>(superblockFile, persister, kc);
	}

	public static <K, V> LazyPbTreeStore<Integer, K, V> ofPage( //
			PageFile pageFile, //
			Comparator<K> kc, //
			Serializer<K> ks, //
			Serializer<V> vs) {
		var pc = Comparator.<Pair<K, V>, K> comparing(p -> p.t0, kc);
		var ps = ser.pair(ks, vs);
		var pfs = FileFactory.subPageFiles(pageFile, 0, 1, Integer.MAX_VALUE);
		var superblockFile = SerializedFileFactory.serialized(pfs[0], ser.nullable(ser.int_));
		var persister = new LazyPbTreePageFilePersister<>(pfs[1], pc, ps);
		return new LazyPbTreeStore<>(superblockFile, persister, kc);
	}

	public LazyPbTreeStore( //
			SerializedPageFile<Pointer> superblockFile, //
			LazyPbTreePersister<Pointer, Pair<Key, Value>> persister, //
			Comparator<Key> kc) {
		this.superblockFile = superblockFile;
		this.persister = persister;

		var pointer = superblockFile.load(0);
		if (pointer == null)
			superblockFile.save(0, pointer = persister.save(new LazyPbTree<>((p0, p1) -> kc.compare(p0.t0, p1.t0))));
		mutator = new LazyPbTreeMutator<>(persister.load(pointer));
	}

	@Override
	public synchronized void end(boolean isComplete) {
		if (isComplete) {
			var pointer1 = persister.save(mutator.get());
			var pointerx = persister.gc(List.of(pointer1), 9).getOrDefault(pointer1, pointer1);
			superblockFile.save(0, pointerx);
		}

		Object_.closeQuietly(persister, superblockFile);
	}

	@Override
	public KeyValueMutator<Key, Value> mutate() {
		return mutator;
	}

}
