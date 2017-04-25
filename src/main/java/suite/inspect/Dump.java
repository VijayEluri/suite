package suite.inspect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import suite.jdk.gen.Type_;
import suite.util.FunUtil.Sink;

public class Dump {

	private Set<Integer> dumpedIds = new HashSet<>();
	private Sink<String> sink;

	public static String object(Object object) {
		return object("", object);
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * string, line-by-line.
	 *
	 * Private fields are not dumped.
	 *
	 * @param prefix
	 *            To be appended before each line.
	 * @param object
	 *            The monster.
	 */
	public static String object(String prefix, Object object) {
		StringBuilder sb = new StringBuilder();
		object(sb, prefix, object);
		return sb.toString();
	}

	public static void object(StringBuilder sb, String prefix, Object object) {
		new Dump(sb::append).d(prefix, object);
	}

	public Dump(Sink<String> sink) {
		this.sink = sink;
	}

	private void d(String prefix, Object object) {
		if (object != null)
			d(prefix, object, object.getClass());
		else
			d(prefix, object, void.class);
	}

	private void d(String prefix, Object object, Class<?> clazz) {
		int id = System.identityHashCode(object);
		sink.sink(prefix);
		sink.sink(" =");

		if (object == null)
			sink.sink(" null\n");
		else if (dumpedIds.add(id))
			try {
				if (clazz == String.class)
					sink.sink(" \"" + object + "\"");

				if (!Collection.class.isAssignableFrom(clazz))
					sink.sink(" " + object);

				sink.sink(" [" + clazz.getSimpleName() + "]\n");

				// simple listings for simple classes
				if (Type_.isSimple(clazz))
					return;

				for (Field field : clazz.getFields())
					if (!Modifier.isStatic(field.getModifiers()))
						try {
							String name = field.getName();
							Object o = field.get(object);
							Class<?> type = field.getType();
							if (Type_.isSimple(type))
								d(prefix + "." + name, o, type);
							else
								d(prefix + "." + name, o);
						} catch (Throwable ex) {
							sink.sink(prefix + "." + field.getName());
							sink.sink(" caught " + ex + "\n");
						}

				Set<String> displayedMethod = new HashSet<>();

				for (Method method : clazz.getMethods()) {
					String name = method.getName();
					try {
						if (name.startsWith("get") //
								&& method.getParameterTypes().length == 0 //
								&& !displayedMethod.contains(name)) {
							Object o = method.invoke(object);
							if (!(o instanceof Class<?>))
								d(prefix + "." + name + "()", o);

							// do not display same method of different base
							// classes
							displayedMethod.add(name);
						}
					} catch (Throwable ex) {
						sink.sink(prefix + "." + name + "()");
						sink.sink(" caught " + ex + "\n");
					}
				}

				int count = 0;

				if (clazz.isArray()) {
					Class<?> componentType = clazz.getComponentType();
					if (componentType.isPrimitive())
						for (int i = 0; i < Array.getLength(object); i++)
							d(prefix + "[" + count++ + "]", Array.get(object, i));
					else if (Object.class.isAssignableFrom(componentType))
						for (Object o1 : (Object[]) object)
							d(prefix + "[" + count++ + "]", o1);
				}

				if (Collection.class.isAssignableFrom(clazz))
					for (Object o1 : (Collection<?>) object)
						d(prefix + "[" + count++ + "]", o1);
				else if (Map.class.isAssignableFrom(clazz))
					for (Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
						Object key = entry.getKey(), value = entry.getValue();
						d(prefix + "[" + count + "].getKey()", key);
						d(prefix + "[" + count + "].getValue()", value);
						count++;
					}
			} finally {
				dumpedIds.remove(id);
			}
		else
			sink.sink(" <<recursed>>");
	}

}
