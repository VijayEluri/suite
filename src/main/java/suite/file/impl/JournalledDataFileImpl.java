package suite.file.impl;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import suite.file.DataFile;
import suite.file.PageFile;
import suite.primitive.Bytes;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

/**
 * Protect data against power loss failures by recording journals.
 *
 * @author ywsing
 */
public class JournalledDataFileImpl<Pointer> implements Closeable, DataFile<Pointer> {

	private DataFile<Pointer> dataFile;
	private SerializedPageFile<JournalEntry> journalPageFile;
	private SerializedPageFile<Integer> pointerPageFile;

	private int nCommittedJournalEntries;
	private List<JournalEntry> journalEntries = new ArrayList<>();

	private Serializer<Pointer> pointerSerializer;
	private Serializer<Bytes> bytesSerializer;
	private Serializer<JournalEntry> journalEntrySerializer = new Serializer<JournalEntry>() {
		public JournalEntry read(DataInput dataInput) throws IOException {
			Pointer pointer = pointerSerializer.read(dataInput);
			Bytes bytes = bytesSerializer.read(dataInput);
			return new JournalEntry(pointer, bytes);
		}

		public void write(DataOutput dataOutput, JournalEntry journalEntry) throws IOException {
			pointerSerializer.write(dataOutput, journalEntry.pointer);
			bytesSerializer.write(dataOutput, journalEntry.bytes);
		}
	};

	private class JournalEntry {
		private Pointer pointer;
		private Bytes bytes;

		private JournalEntry(Pointer pointer, Bytes bytes) {
			this.pointer = pointer;
			this.bytes = bytes;
		}
	}

	public JournalledDataFileImpl( //
			DataFile<Pointer> df //
			, PageFile jpf //
			, PageFile ppf //
			, int pageSize //
			, Serializer<Pointer> ps) throws IOException {
		dataFile = df;
		journalPageFile = new SerializedPageFile<>(jpf, journalEntrySerializer);
		pointerPageFile = new SerializedPageFile<>(ppf, SerializeUtil.intSerializer);
		pointerSerializer = ps;
		bytesSerializer = SerializeUtil.bytes(pageSize);
		nCommittedJournalEntries = pointerPageFile.load(0);

		for (int jp = 0; jp < nCommittedJournalEntries; jp++)
			journalEntries.add(journalPageFile.load(jp));
	}

	public synchronized void create() {
		nCommittedJournalEntries = 0;
		journalEntries.clear();
		pointerPageFile.save(0, nCommittedJournalEntries);
	}

	@Override
	public void close() throws IOException {
		dataFile.close();
		journalPageFile.close();
		pointerPageFile.close();
	}

	/**
	 * Marks a snapshot that data can be recovered to.
	 */
	public synchronized void commit() throws IOException {
		while (nCommittedJournalEntries < journalEntries.size()) {
			JournalEntry journalEntry = journalEntries.get(nCommittedJournalEntries++);
			dataFile.save(journalEntry.pointer, journalEntry.bytes);
		}

		if (nCommittedJournalEntries > 8)
			saveJournal();
	}

	/**
	 * Makes sure the current snapshot of data is saved and recoverable on
	 * failure, upon the return of method call.
	 */
	@Override
	public synchronized void sync() throws IOException {
		journalPageFile.sync();
		saveJournal();
		pointerPageFile.sync();
	}

	private void saveJournal() throws IOException {
		pointerPageFile.save(0, nCommittedJournalEntries);

		if (nCommittedJournalEntries > 128)
			applyJournal();
	}

	/**
	 * Shortens the journal by applying them to page file.
	 */
	public synchronized void applyJournal() throws IOException {

		// Make sure all changes are written to main file
		dataFile.sync();

		// Clear all committed entries
		journalEntries.subList(0, nCommittedJournalEntries).clear();

		// Reset committed pointer
		nCommittedJournalEntries = 0;
		pointerPageFile.save(0, nCommittedJournalEntries);
		pointerPageFile.sync();

		// Write back entries for next commit
		for (int jp = 0; jp < journalEntries.size(); jp++)
			journalPageFile.save(jp, journalEntries.get(jp));
	}

	@Override
	public synchronized Bytes load(Pointer pointer) throws IOException {
		int jp = findPageInJournal(pointer);
		if (jp < 0)
			return dataFile.load(pointer);
		else
			return journalEntries.get(jp).bytes;
	}

	@Override
	public synchronized void save(Pointer pointer, Bytes bytes) throws IOException {
		int jp = findDirtyPageInJournal(pointer);

		if (jp < 0) {
			jp = journalEntries.size();
			journalEntries.add(new JournalEntry(pointer, null));
		}

		JournalEntry journalEntry = journalEntries.get(jp);
		journalEntry.bytes = bytes;
		journalPageFile.save(jp, journalEntry);
	}

	private int findPageInJournal(Pointer pointer) {
		return findPageInJournal(pointer, 0);
	}

	private int findDirtyPageInJournal(Pointer pointer) {
		return findPageInJournal(pointer, nCommittedJournalEntries);
	}

	private int findPageInJournal(Pointer pointer, int start) {
		int jp1 = -1;
		for (int jp = start; jp < journalEntries.size(); jp++)
			if (journalEntries.get(jp).pointer == pointer)
				jp1 = jp;
		return jp1;
	}

}
