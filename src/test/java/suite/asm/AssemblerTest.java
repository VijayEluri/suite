package suite.asm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.Suite;
import suite.primitive.Bytes;

public class AssemblerTest {

	@Test
	public void testAssemble() {
		boolean isProverTrace0 = Suite.isProverTrace;
		Suite.isProverTrace = true;
		try {
			Assembler assembler = new Assembler(32);
			assembler.assemble(Suite.parse(".org = 0, .l LEA (EAX, `EAX * 4`),"));
		} finally {
			Suite.isProverTrace = isProverTrace0;
		}
	}

	@Test
	public void testAssembleLongMode() throws IOException {
		Assembler assembler = new Assembler(32, true);
		Bytes bytes = assembler.assemble(Suite.parse(".org = 0, .l MOV (R9D, DWORD 16),"));
		assertEquals(bytes.size(), 7);
	}

	@Test
	public void testAssembleMisc() throws IOException {
		Assembler assembler = new Assembler(32, true);
		assembler.assemble(Suite.parse(".org = 0, _ JMP (BYTE .label), .label (),"));
	}

	@Test
	public void testAssemblePerformance() {
		Assembler assembler = new Assembler(32);
		for (int i = 0; i < 4096; i++)
			assembler.assemble(Suite.parse(".org = 0, .l CLD (),"));
	}

	@Test
	public void testAssembler() {
		Bytes bytes = new Assembler(32).assemble("" //
				+ ".org = 0 \n" //
				+ "	JMP (.end) \n" //
				+ "	MOV (AX, 16) \n" //
				+ "	MOV (EAX, 16) \n" //
				+ ".end	() \n" //
				+ "	ADVANCE (16) \n" //
		);
		assertEquals(16, bytes.size());
		System.out.println(bytes);
	}

}
