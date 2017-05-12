package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.fs.KeyValueMutator;
import suite.fs.impl.TransactionManager;
import suite.immutable.LazyIbTreeStore;
import suite.util.FunUtil.Fun;
import suite.util.Object_;
import suite.util.Serialize;

public class Database implements Closeable {

	private JournalledPageFile journalledPageFile;
	private TransactionManager<Integer, String> transactionManager;

	public Database(Path path) {
		journalledPageFile = JournalledFileFactory.journalled(path, PageFile.defaultPageSize);

		transactionManager = new TransactionManager<>(() -> LazyIbTreeStore.ofExtent( //
				journalledPageFile, //
				Object_.comparator(), //
				Serialize.int_, //
				Serialize.variableLengthString));
	}

	@Override
	public void close() throws IOException {
		journalledPageFile.commit();
		journalledPageFile.close();
	}

	public <T> T transact(Fun<KeyValueMutator<Integer, String>, T> callback) {
		try {
			return transactionManager.begin(callback);
		} finally {
			journalledPageFile.commit();
		}
	}

}
