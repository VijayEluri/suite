package suite.os;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;

import suite.Constants;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.util.DataOutput_;
import suite.util.Util;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfWriter {

	public Execute exec(byte[] input, Int_Obj<Bytes> source) {
		Path path = Constants.tmp("a.out." + Util.temp());
		int org = 0x08048000;

		write(org, source.apply(org + 84), path);
		return new Execute(new String[] { path.toString(), }, input);
	}

	private void write(int org, Bytes code, Path path) {
		try (OutputStream os = FileUtil.out(path); DataOutput_ do_ = DataOutput_.of(os)) {
			write(org, code, do_);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		try {
			Files.setPosixFilePermissions(path, new HashSet<>(List.of( //
					PosixFilePermission.GROUP_EXECUTE, //
					PosixFilePermission.GROUP_READ, //
					PosixFilePermission.OTHERS_EXECUTE, //
					PosixFilePermission.OTHERS_READ, //
					PosixFilePermission.OWNER_EXECUTE, //
					PosixFilePermission.OWNER_READ, //
					PosixFilePermission.OWNER_WRITE)));
		} catch (UnsupportedOperationException ex) {
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void write(int org, Bytes code, DataOutput_ do_) throws IOException {
		Bytes header = new Writer_() //
				.db(0x7F) // e_ident
				.append("ELF".getBytes(Constants.charset)) //
				.append(new byte[] { 1, 1, 1, 0, }) //
				.append(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, }) //
				.dw(2) // e_type
				.dw(3) // e_machine
				.dd(1) // e_version
				.dd(org + 84) // e_entry
				.dd(52) // e_phoff
				.dd(0) // e_shoff
				.dd(0) // e_flags
				.dw(52) // e_ehsize
				.dw(32) // e_phentsize
				.dw(1) // e_phnum
				.dw(0) // e_shentsize
				.dw(0) // e_shnum
				.dw(0) // e_shstrndx
				.dd(1) // p_type
				.dd(0) // p_offset
				.dd(org) // p_vaddr
				.dd(org) // p_paddr
				.dd(code.size() + 84) // p_filesz
				.dd(code.size() + 84) // p_memsz
				.dd(7) // p_flags PF_R|PF_W|PF_X
				.dd(0x1000) // p_align
				.toBytes();

		do_.writeBytes(header);
		do_.writeBytes(code);
	}

	private class Writer_ {
		private BytesBuilder bb = new BytesBuilder();

		private Writer_ db(int i) {
			return d(1, i);
		}

		private Writer_ dw(int i) {
			return d(2, i);
		}

		private Writer_ dd(int i) {
			return d(4, i);
		}

		private Writer_ append(byte[] bs) {
			bb.append(bs);
			return this;
		}

		private Writer_ d(int n, int i) {
			for (int j = 0; j < n; j++) {
				bb.append((byte) (i & 0xFF));
				i = i >> 8;
			}
			return this;
		}

		private Bytes toBytes() {
			return bb.toBytes();
		}
	}

}
