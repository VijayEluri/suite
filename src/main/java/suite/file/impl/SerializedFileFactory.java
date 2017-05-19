package suite.file.impl;

import java.io.IOException;

import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.streamlet.As;
import suite.util.DataInput_;
import suite.util.Rethrow;
import suite.util.Serialize.Serializer;
import suite.util.To;

public class SerializedFileFactory {

	public static <V> SerializedPageFile<V> serialized(PageFile pageFile, Serializer<V> serializer) {
		return new SerializedPageFile<V>() {
			public void close() throws IOException {
				pageFile.close();
			}

			public void sync() {
				pageFile.sync();
			}

			public V load(int pointer) {
				return Rethrow.ex(() -> serializer.read(DataInput_.of(pageFile.load(pointer).collect(As::inputStream))));
			}

			public void save(int pointer, V value) {
				pageFile.save(pointer, To.bytes(dataOutput -> serializer.write(dataOutput, value)));
			}
		};
	}

}
