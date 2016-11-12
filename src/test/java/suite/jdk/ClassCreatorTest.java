package suite.jdk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

public class ClassCreatorTest implements Opcodes {

	@Test
	public void testCreateFun() {
		ClassCreator classCreator = new ClassCreator();
		assertEquals("Hello", classCreator.create(classCreator.parameter(1)));
	}

}
