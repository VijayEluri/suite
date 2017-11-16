package suite.os;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64Interpret;
import suite.funp.Funp_;
import suite.primitive.Ints;
import suite.primitive.Ints_;

// http://www.muppetlabs.com/~breadbox/software/tiny/teensy.html
public class ElfTest {

	private ElfWriter elf = new ElfWriter();

	@Test
	public void testCode() {
		Execute exec = test("" //
				+ "iterate n 0 (n < 100) (io (n + 1)) \n" //
				, "");

		assertEquals(100, exec.code);
		assertEquals("", exec.out);
	}

	@Test
	public void testIo() {
		String text = "garbage\n";

		String program = "" //
				+ "expand size := 256 >> \n" //
				+ "define linux-read := `pointer, length` => ( \n" //
				+ "	type pointer = address (size * array byte _) >> \n" //
				+ "	io (asm (EAX = 3; EBX = 0; ECX = pointer; EDX = length;) { \n" //
				+ "		INT (-128); -- length in EAX \n" //
				+ "	}) \n" //
				+ ") >> \n" //
				+ "define linux-write := `pointer, length` => ( \n" //
				+ "	type pointer = address (size * array byte _) >> \n" //
				+ "	io (asm (EAX = 4; EBX = 1; ECX = pointer; EDX = length;) { \n" //
				+ "		INT (-128); -- length in EAX \n" //
				+ "	}) \n" //
				+ ") >> \n" //
				+ "iterate n 1 (n != 0) ( \n" //
				+ "	let buffer := (size * array byte _) >> \n" //
				+ "	let pointer := address buffer >> \n" //
				+ "	pointer, size | linux-read | io-cat ( \n" //
				+ "		nBytesRead => pointer, nBytesRead | linux-write | io-cat (nBytesWrote => io nBytesRead) \n" //
				+ "	) \n" //
				+ ") \n";

		if (Boolean.TRUE) {
			Execute exec = test(program, text);
			assertEquals(0, exec.code);
			assertEquals(text, exec.out);
		} else {
			Ints array = Ints.of(Ints_.toArray(text.length(), text::charAt));
			List<Instruction> instructions = Funp_.main().compile(0, program).t0;
			assertEquals(0, new Amd64Interpret(array).interpret(instructions));
		}
	}

	private Execute test(String program, String input) {
		return elf.exec(input, offset -> Funp_.main().compile(offset, program).t1);
	}

}
