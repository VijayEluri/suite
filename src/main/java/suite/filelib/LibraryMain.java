package suite.filelib;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.commons.codec.digest.Md5Crypt;

import suite.adt.pair.Pair;
import suite.os.FileUtil;
import suite.primitive.Ints_;
import suite.streamlet.Read;
import suite.streamlet.Streamlet2;
import suite.util.Rethrow;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.To;

/**
 * Maintains library of files, probably images or documents.
 * 
 * Perform tagging and de-duplication.
 * 
 * @author ywsing
 */
public class LibraryMain extends ExecutableProgram {

	private String inputDir = "/data/storey/lg";
	private List<String> fileExtensions = List.of("jpg");
	private String libraryDir = "/data/photographs & memories/library";
	private String tagsDir = "/data/photographs & memories/tags";

	private class FileInfo {
		private String md5;
		private List<String> tags;
	}

	public static void main(String[] args) {
		RunUtil.run(LibraryMain.class, args);
	}

	protected boolean run(String[] args) {
		Pair<Streamlet2<Path, Long>, Streamlet2<Path, Long>> partition = FileUtil.findPaths(Paths.get(inputDir)) //
				.filter(path -> fileExtensions.contains(FileUtil.getFileExtension(path))) //
				.map2(path -> Rethrow.ex(() -> Files.size(path))) //
				.partition((path, size) -> 0 < size);

		// remove empty files
		partition.t1.sink((path, size) -> {
			try {
				Files.delete(path);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});

		Streamlet2<Path, FileInfo> path_fileInfos = partition.t0 //
				.map2((path, size) -> {
					BasicFileAttributes attrs = Rethrow.ex(() -> Files.readAttributes(path, BasicFileAttributes.class));

					// get all file information
					List<String> tags = Ints_ //
							.range(path.getNameCount()) //
							.map(i -> path.getName(i).toString()) //
							.cons(To.string(attrs.lastModifiedTime().toInstant())) //
							.toList();

					FileInfo fileInfo = new FileInfo();
					fileInfo.md5 = Rethrow.ex(() -> Md5Crypt.md5Crypt(Files.readAllBytes(path)));
					fileInfo.tags = tags;
					return fileInfo;
				});

		// construct file listing
		try (OutputStream os = FileUtil.out(inputDir + ".listing"); PrintWriter pw = new PrintWriter(os)) {
			for (Pair<Path, FileInfo> path_fileInfo : path_fileInfos)
				pw.println(path_fileInfo.t0 + path_fileInfo.t1.md5);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		path_fileInfos //
				.map2((path, fileInfo) -> {

					// move file to library, by md5
					Path path1 = Paths.get(libraryDir, fileInfo.md5.substring(0, 2), fileInfo.md5);
					FileUtil.mkdir(path1.getParent());
					Rethrow.ex(() -> Files.move(path, path1, StandardCopyOption.REPLACE_EXISTING));
					return fileInfo;
				}) //
				.concatMap((path, fileInfo) -> Read.from(fileInfo.tags).map(tag -> {

					// add to tag indices
					Path path1 = Paths.get(tagsDir, tag, fileInfo.md5);
					return Rethrow.ex(() -> {
						Files.newOutputStream(path1).close();
						return Pair.of(tag, fileInfo);
					});
				}));

		return true;
	}

}
