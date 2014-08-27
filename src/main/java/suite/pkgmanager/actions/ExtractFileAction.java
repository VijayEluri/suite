package suite.pkgmanager.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

import suite.util.Copy;

public class ExtractFileAction implements InstallAction {

	private String packageFile;
	private String filename0;
	private String filename1;

	public ExtractFileAction(String packageFilename, String filename0, String filename1) {
		this.packageFile = packageFilename;
		this.filename0 = filename0;
		this.filename1 = filename1;
	}

	public void act() throws IOException {
		try (ZipFile zipFile = new ZipFile(packageFile);
				InputStream is = zipFile.getInputStream(zipFile.getEntry(filename0));
				FileOutputStream fos = new FileOutputStream(filename1)) {
			Copy.stream(is, fos);
		}
	}

	public void unact() throws IOException {
		new File(filename1).delete();
	}

}
