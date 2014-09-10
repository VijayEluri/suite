package suite.file;

import java.io.Closeable;
import java.io.IOException;

import suite.primitive.Bytes;

public class SubPageFileImpl implements Closeable, PageFile {

	private PageFile parent;
	private int startPage, endPage;

	public SubPageFileImpl(PageFile parent, int startPage, int endPage) {
		this.parent = parent;
		this.startPage = startPage;
		this.endPage = endPage;
	}

	@Override
	public void sync() throws IOException {
		parent.sync();
	}

	@Override
	public Bytes load(int pageNo) throws IOException {
		return parent.load(validate(pageNo + startPage));
	}

	@Override
	public void save(int pageNo, Bytes bytes) throws IOException {
		parent.save(validate(pageNo + startPage), bytes);
	}

	@Override
	public void close() throws IOException {
	}

	private int validate(int index) throws IOException {
		if (startPage <= index && index < endPage)
			return index;
		else
			throw new IOException("Page index out of range");
	}

}
