package suite.java7util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import suite.util.Copy;
import suite.util.Fail;
import suite.util.FunUtil.Source;

public class FileUtil {

	public static void copyFile(File from, File to) {
		try (OutputStream fos = new FileOutputStream(to)) {
			Copy.stream(new FileInputStream(from), fos);
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	public static Source<File> findFiles(File file) {
		Deque<File> stack = new ArrayDeque<>();
		stack.push(file);

		return new Source<File>() {
			public File source() {
				while (!stack.isEmpty()) {
					File f = stack.pop();

					if (f.isDirectory())
						for (File child : f.listFiles())
							stack.push(child);
					else
						return f;
				}

				return null;
			}
		};
	}

	public static void moveFile(File from, File to) {

		// serious problem that renameTo do not work across partitions in Linux!
		// we fall back to copy the file if renameTo() failed.
		if (!from.renameTo(to)) {
			copyFile(from, to);
			from.delete();
		}
	}

}
