package suite.inspect;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import suite.adt.pair.Pair;
import suite.node.Tree;
import suite.node.io.Operator.Assoc;
import suite.node.util.Singleton;
import suite.object.MapInterface;
import suite.object.MapObject_;
import suite.os.Log_;
import suite.streamlet.As;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.ParseUtil;
import suite.util.String_;
import suite.util.Switch;
import suite.util.Thread_;
import suite.util.Util;

public class Dump {

	private static Inspect inspect = Singleton.me.inspect;

	public static <T> T t(T t) {
		details(t);
		return t;
	}

	public static void lines(Object node) {
		Log_.info(format(toLine(node)));
	}

	public static void line(Object node) {
		Log_.info(toLine(node));
	}

	public static String toLine(Object node) {
		var dumpedObjects = new IdentityHashMap<Object, Object>();
		var sb = new StringBuilder();
		Sink<String> sink = sb::append;

		new Object() {
			private void d(Object object, String suffix) {
				if (object == null)
					sink.f("null");
				else if (dumpedObjects.put(object, true) == null)
					try {
						d_(object);
					} finally {
						dumpedObjects.remove(object);
					}
				else
					sink.f("(recursed)");
				sink.f(suffix);
			}

			private void d_(Object object) {
				var clazz = object.getClass();

				if (clazz.isArray()) {
					sink.f("[");
					for (var i = 0; i < Array.getLength(object); i++)
						d(Array.get(object, i), ",");
					sink.f("]");
				} else if (Util.isSimple(clazz))
					sink.f(object.toString());
				else
					new Switch<Object>(object //
					).doIf(Collection.class, collection -> {
						sink.f("[");
						for (var object1 : collection)
							d(object1, ",");
						sink.f("]");
					}).doIf(Map.class, map -> {
						sink.f("{");
						for (var e : ((Map<?, ?>) object).entrySet()) {
							d(e.getKey(), ":");
							d(e.getValue(), ",");
						}
						sink.f("}");
					}).doIf(MapInterface.class, mi -> {
						sink.f(mi.getClass().getSimpleName());
						sink.f("{");
						for (var object1 : MapObject_.list(object))
							d(object1, ",");
						sink.f("}");
					}).doIf(Pair.class, pair -> {
						sink.f("<");
						d(pair.t0, "|");
						d(pair.t1, ">");
					}).doIf(Object.class, o -> {
						sink.f(o.getClass().getSimpleName());
						sink.f("{");
						for (var pair : readers(object)) {
							Object value;
							try {
								value = pair.t1.call();
							} catch (Throwable ex) {
								value = "<" + ex.getClass() + ">";
							}

							if (value != null) {
								sink.f(pair.t0 + ":");
								d(value, ",");
							}
						}
						sink.f("}");
					}).nonNullResult();
			}
		}.d(node, "");

		return sb.toString();
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a log4j.
	 */
	public static void details(Object object) {
		var trace = Thread_.getStackTrace(3);
		details(trace.getClassName() + "." + trace.getMethodName(), object);
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a log4j,
	 * with a descriptive name which you gave.
	 */
	public static void details(String name, Object object) {
		var sb = new StringBuilder();
		sb.append("Dumping ");
		sb.append(name);
		Dump.toDetails("", object, sb);
		Log_.info(sb.toString());
	}

	public static String object(Object object) {
		return toDetails("", object);
	}

	/**
	 * Dumps object content (public data and getters) through Reflection to a
	 * string, line-by-line.
	 *
	 * Private fields are not dumped.
	 *
	 * @param prefix
	 *                   To be appended before each line.
	 * @param object
	 *                   The monster.
	 */
	public static String toDetails(String prefix, Object object) {
		var sb = new StringBuilder();
		toDetails(prefix, object, sb);
		return sb.toString();
	}

	public static void toDetails(String prefix, Object object, StringBuilder sb) {
		toDetails(prefix, object, sb::append);
	}

	private static void toDetails(String prefix, Object object, Sink<String> sink) {
		var dumpedObjects = new IdentityHashMap<Object, Object>();

		new Object() {
			private void d(String prefix, Object object) {
				d(prefix, object, object != null ? object.getClass() : void.class);
			}

			private void d(String prefix, Object object, Class<?> clazz) {
				sink.f(prefix);
				sink.f(" =");

				if (object == null)
					sink.f(" null\n");
				else if (dumpedObjects.put(object, true) == null)
					try {
						d_(prefix, object, clazz);
					} finally {
						dumpedObjects.remove(object);
					}
				else
					sink.f(" <<recursed>>");
			}

			private void d_(String prefix, Object object, Class<?> clazz) {
				if (clazz.isArray() && Util.isSimple(clazz.getComponentType())) {
					sink.f(" [ ");
					for (var i = 0; i < Array.getLength(object); i++)
						sink.f(Array.get(object, i) + ", ");
					sink.f("]\n");
				} else if (clazz == String.class)
					sink.f(" \"" + object + "\"");
				else if (Util.isSimple(clazz))
					sink.f(" " + object + " [" + clazz.getSimpleName() + "]\n");
				else
					d_c(prefix, object, clazz);
			}

			private void d_c(String prefix, Object object, Class<?> clazz) {
				if (!Collection.class.isAssignableFrom(clazz))
					sink.f(" " + object);

				sink.f(" [" + clazz.getSimpleName() + "]\n");

				var count = 0;

				// simple listings for simple classes
				if (clazz.isArray())
					if (Util.isSimple(clazz.getComponentType())) {
						var sb = new StringBuilder();
						sb.append("[");
						for (var i = 0; i < Array.getLength(object); i++)
							sb.append(Array.get(object, i) + ", ");
						sb.append("]");
						d(prefix, sb.toString());
					} else
						for (var i = 0; i < Array.getLength(object); i++)
							d(prefix + "[" + count++ + "]", Array.get(object, i));
				else if (Collection.class.isAssignableFrom(clazz))
					for (var o1 : (Collection<?>) object)
						d(prefix + "[" + count++ + "]", o1);
				else if (Map.class.isAssignableFrom(clazz))
					for (var e : ((Map<?, ?>) object).entrySet()) {
						Object key = e.getKey(), value = e.getValue();
						d(prefix + "[" + count + "].getKey()", key);
						d(prefix + "[" + count + "].getValue()", value);
						count++;
					}
				else if (MapInterface.class.isAssignableFrom(clazz)) {
					var i = 0;
					for (var object1 : MapObject_.list(object))
						d(prefix + "." + i++, object1);
				} else if (Pair.class.isAssignableFrom(clazz)) {
					var pair = (Pair<?, ?>) object;
					d(prefix + ".fst", pair.t0);
					d(prefix + ".snd", pair.t1);
				} else
					for (var pair : readers(object)) {
						var k = prefix + "." + pair.t0;

						try {
							d(k, pair.t1.call());
						} catch (Throwable ex) {
							sink.f(k + "()");
							sink.f(" caught " + ex + "\n");
						}
					}
			}
		}.d(prefix, object);
	}

	private static String format(String string) {
		return new Object() {
			private Streamlet<String> split(String indent, String s, String tail) {
				if (80 <= s.length()) {
					var last = s.charAt(s.length() - 1);

					if (last == ',')
						return ParseUtil.splitn(s, ",", Assoc.RIGHT).concatMap(s_ -> split(indent, s_, ","));

					for (var pair : List.of("[]", "{}", "<>")) {
						var open = pair.charAt(0);
						var close = pair.charAt(1);
						int pos = s.indexOf(open);

						if (last == close && 0 <= pos) {
							var left = String_.left(s, pos);
							var right = String_.range(s, pos + 1, -1);
							return Streamlet.concat( //
									Read.each(indent + left + open), //
									split(indent + "  ", right, ""), //
									Read.each(indent + Character.toString(last) + tail));
						}
					}
				}
				return Read.each(indent + s + tail);
			}
		}.split("\n", string, "").collect(As::joined);
	}

	private static Streamlet<Pair<String, Callable<Object>>> readers(Object object) {
		var clazz = object.getClass();

		var fields = inspect.fields(clazz).map(f -> Pair.<String, Callable<Object>> of(f.getName(), () -> f.get(object)));

		var getters = inspect.getters(clazz).map(m -> Pair.<String, Callable<Object>> of(m.getName() + "()", () -> {
			var o_ = m.invoke(object);
			return !(o_ instanceof Class<?>) ? o_ : null;
		}));

		if (!Tree.class.isAssignableFrom(clazz))
			return Streamlet.concat(fields, getters);
		else
			return fields;
	}

}
