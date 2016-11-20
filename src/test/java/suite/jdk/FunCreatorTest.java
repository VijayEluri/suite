package suite.jdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.function.BiPredicate;

import org.junit.Test;
import org.objectweb.asm.Type;

import suite.streamlet.Read;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;

public class FunCreatorTest {

	public interface IntFun {
		public int apply(int i);
	}

	@Test
	public void testBiPredicate() {
		@SuppressWarnings("rawtypes")
		FunCreator<BiPredicate> fc = FunCreator.of( //
				BiPredicate.class, //
				"test", //
				Type.getDescriptor(boolean.class), //
				Arrays.asList(Type.getDescriptor(Object.class), Type.getDescriptor(Object.class)));
		fc.create(fc.constant(Boolean.TRUE));
		@SuppressWarnings("unchecked")
		BiPredicate<Object, Object> bp = fc.instantiate();
		assertTrue(bp.test("Hello", "world"));
	}

	@Test
	public void testField() {
		String fieldName = "f";
		FunCreator<IntFun> fc = intFun(fieldName, Type.getDescriptor(int.class));
		fc.create(fc.add(fc.field(fieldName), fc.parameter(1)));
		IntFun intFun = Rethrow.reflectiveOperationException(() -> {
			Class<? extends IntFun> clazz = fc.get();
			IntFun f = clazz.newInstance();
			clazz.getDeclaredField(fieldName).set(f, 1);
			return f;
		});
		assertEquals(6, intFun.apply(5));
	}

	@Test
	public void testFun() {
		@SuppressWarnings("rawtypes")
		FunCreator<Fun> fc = FunCreator.of( //
				Fun.class, //
				"apply", //
				Type.getDescriptor(Object.class), //
				Arrays.asList(Type.getDescriptor(Object.class)));
		fc.create(fc.parameter(1));
		@SuppressWarnings("unchecked")
		Fun<Object, Object> fun = fc.instantiate();
		assertEquals("Hello", fun.apply("Hello"));
	}

	@Test
	public void testInvoke() {
		String fieldName0 = "f0";
		String fieldName1 = "f1";

		FunCreator<IntFun> fc0 = intFun(fieldName0, Type.getDescriptor(int.class));
		fc0.create(fc0.add(fc0.field(fieldName0), fc0.parameter(1)));

		FunCreator<IntFun> fc1 = intFun(fieldName1, Type.getDescriptor(IntFun.class));
		fc1.create(fc1.field(fieldName1).invoke(fc0, fc1.constant(3)));

		IntFun intFun = Rethrow.reflectiveOperationException(() -> {
			Class<? extends IntFun> clazz0 = fc0.get();
			Class<? extends IntFun> clazz1 = fc1.get();
			IntFun f0 = clazz0.newInstance();
			IntFun f1 = clazz1.newInstance();
			clazz0.getDeclaredField(fieldName0).set(f0, 1);
			clazz1.getDeclaredField(fieldName1).set(f1, f0);
			return f1;
		});

		assertEquals(4, intFun.apply(5));
	}

	private FunCreator<IntFun> intFun(String fieldName, String fieldType) {
		return new FunCreator<>( //
				IntFun.class, //
				"apply", //
				Type.getDescriptor(int.class), //
				Arrays.asList(Type.getDescriptor(int.class)), //
				Read.<String, String> empty2() //
						.cons(fieldName, fieldType) //
						.toMap());
	}

}
