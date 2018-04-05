package suite.asm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import suite.ip.ImperativeCompiler;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.util.Fail;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.RunUtil.RunOption;

// mvn compile exec:java -Dexec.mainClass=suite.asm.BootMain && qemu-system-x86_64 target/boot.bin
public class BootMain extends ExecutableProgram {

	public static void main(String[] args) {
		RunUtil.run(BootMain.class, args, RunOption.TIME___);
	}

	@Override
	protected boolean run(String[] args) throws IOException {
		var bootLoader = new Assembler(16).assemble(FileUtil.read("src/main/asm/bootloader.asm"));
		var kernel = new ImperativeCompiler().compile(0x40000, Paths.get("src/main/il/kernel.il"));

		if (bootLoader.size() == 512 && kernel.size() < 65536) {

			// combine the images and align to 512 bytes
			Bytes disk0 = Bytes.concat(bootLoader, kernel);
			var disk1 = disk0.pad(disk0.size() + 511 & 0xFFFFFE00);

			var image = "target/boot.bin";
			Files.write(Paths.get(image), disk1.toArray());

			System.out.println("cat " + image + " | dd bs=512 count=1 | /opt/udis86-1.7.2/udcli/udcli -16 | less");
			System.out.println("cat " + image + " | dd bs=512 skip=1 | /opt/udis86-1.7.2/udcli/udcli -32 | less");
			System.out.println("qemu-system-x86_64 target/boot.bin");
			return true;
		} else
			return Fail.t("size not match");
	}

}
