package suite.file.impl;

import static suite.util.Friends.rethrow;

import java.io.Closeable;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import suite.object.Object_;
import suite.os.FileUtil;
import suite.primitive.Bytes;

public class RandomAccessibleFile implements Closeable {

	private RandomAccessFile file;
	private FileChannel channel;

	public RandomAccessibleFile(Path path) {
		FileUtil.mkdir(path.getParent());
		file = rethrow(() -> new RandomAccessFile(path.toFile(), "rw"));
		channel = file.getChannel();
	}

	@Override
	public void close() {
		Object_.closeQuietly(channel, file);
	}

	public void sync() {
		rethrow(() -> {
			channel.force(true);
			return channel;
		});
	}

	public Bytes load(int start, int end) {
		return rethrow(() -> {
			var size = end - start;
			var bb = ByteBuffer.allocate(size);
			channel.read(bb, start);
			bb.limit(size);
			return Bytes.of(bb);
		});
	}

	public void save(int start, Bytes bytes) {
		rethrow(() -> {
			channel.write(bytes.toByteBuffer(), start);
			return channel;
		});
	}

}
