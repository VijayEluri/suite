package suite.trade.data;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.HomeDir;
import suite.util.Object_;
import suite.util.String_;
import suite.util.To;
import suite.util.Util;

public class TextDatabase {

	private Path path = HomeDir.resolve("quote-database.csv");
	private TreeSet<Datum> data = new TreeSet<>();
	private long lastSaveTime;
	private long saveTime;
	private volatile Thread saveThread;

	public TextDatabase(Path path) {
		this.path = path;
		load();
	}

	public void join() {
		Thread thread = saveThread;
		if (thread != null)
			try {
				thread.join();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
	}

	public synchronized SortedSet<Datum> range(Datum start, Datum end) {
		return data.subSet(start, end);
	}

	public synchronized void merge(Streamlet<Datum> data_) {
		for (Datum datum : data_)
			merge(datum);

		// 30 seconds between every save
		saveTime = Math.max(lastSaveTime + 30 * 1000l, System.currentTimeMillis());

		if (saveThread == null) {
			saveThread = new Thread(() -> {
				long now;
				while ((now = System.currentTimeMillis()) < saveTime)
					Util.sleepQuietly(saveTime - now);
				synchronized (TextDatabase.this) {
					save();
					saveThread = null;
				}
			});
			saveThread.setDaemon(false);
			saveThread.start();
			lastSaveTime = saveTime;
		}
	}

	private void load() {
		if (Files.exists(path))
			merge(path);
	}

	private void save() {
		Outlet<Bytes> outlet = Read.from(data) //
				.map(this::toBytes) //
				.collect(BytesUtil::buffer);

		try (OutputStream os = FileUtil.out(path)) {
			BytesUtil.copy(outlet, os);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void merge(Path path) {
		Read.bytes(path).collect(As::lines).map(this::toDatum).forEach(this::merge);
	}

	private void merge(Datum datum) {
		Datum datum0 = data.ceiling(datum);
		if (datum0 == null || !Objects.equals(datum, datum0))
			data.add(datum);
		else if (!Objects.equals(datum0.value, datum.value))
			throw new RuntimeException("value mismatch for key " + datum.key + ": " + datum0.value + " != " + datum.value);
	}

	private Datum toDatum(Bytes bytes) {
		String[] array = To.string(bytes).split(",");
		return datum(array[0], array[1]);
	}

	private Bytes toBytes(Datum datum) {
		return To.bytes(datum.key + "," + datum.value + "\n");
	}

	public Datum datum(String key, String value) {
		return new Datum(key, value);
	}

	public class Datum implements Comparable<Datum> {
		public final String key;
		public final String value;

		private Datum(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public int compareTo(Datum other) {
			return Object_.compare(key, other.key);
		}

		public boolean equals(Object object) {
			if (object.getClass() == Datum.class) {
				Datum other = (Datum) object;
				return String_.equals(key, other.key);
			} else
				return false;
		}

		public int hashCode() {
			return Objects.hashCode(key);
		}
	}

}
