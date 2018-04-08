package suite.util;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.jdk.gen.Type_;
import suite.node.util.Singleton;
import suite.streamlet.Read;
import suite.util.FunUtil.Iterate;

public class Object_ {

	public static class Mapper {
		private Iterate<Object> map;
		private Iterate<Object> unmap;

		private Mapper(Iterate<Object> map, Iterate<Object> unmap) {
			this.map = map;
			this.unmap = unmap;
		}
	}

	public static Class<?> clazz(Object object) {
		return object != null ? object.getClass() : null;
	}

	public static void closeQuietly(Closeable o) {
		if (o != null)
			try {
				o.close();
			} catch (IOException ex) {
				Fail.t(ex);
			}
	}

	public static <T extends Comparable<? super T>> int compare(T t0, T t1) {
		var b0 = t0 != null;
		var b1 = t1 != null;
		if (b0 && b1)
			return t0.compareTo(t1);
		else
			return b0 ? 1 : b1 ? -1 : 0;
	}

	public static <T> int compareAnyway(T t0, T t1) {
		if (t0 instanceof Comparable && t1 instanceof Comparable && t0.getClass() == t1.getClass()) {
			@SuppressWarnings("unchecked")
			var c0 = (Comparable<Object>) t0;
			@SuppressWarnings("unchecked")
			var c1 = (Comparable<Object>) t1;
			return c0.compareTo(c1);
		} else
			return Integer.compare(Objects.hashCode(t0), Objects.hashCode(t1));
	}

	public static Mapper mapper(Type type) {
		Mapper mapper;

		if (type instanceof Class) {
			var clazz = (Class<?>) type;

			if (Type_.isSimple(clazz))
				mapper = new Mapper(object -> object, object -> object);
			else if (clazz.isArray()) {
				var componentType = clazz.getComponentType();

				mapper = new Mapper(object -> {
					var map = new HashMap<>();
					var length = Array.getLength(object);
					for (var i = 0; i < length; i++)
						map.put(i, Array.get(object, i));
					return map;
				}, object -> {
					var map = (Map<?, ?>) object;
					var objects = Array.newInstance(componentType, map.size());
					var i = 0;
					while (map.containsKey(i)) {
						Array.set(objects, i, map.get(i));
						i++;
					}
					return objects;
				});
			} else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) // polymorphism
				mapper = new Mapper(object -> {
					var clazz1 = object.getClass();
					var m = apply_(mapper(clazz1).map, object);
					if (m instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, String> map = (Map<String, String>) m;
						map.put("@class", clazz1.getName());
						return map;
					} else
						// happens when an enum implements an interface
						return m;
				}, object -> {
					if (object instanceof Map) {
						var map = (Map<?, ?>) object;
						var className = map.get("@class").toString();
						var clazz1 = Rethrow.ex(() -> Class.forName(className));
						return apply_(mapper(clazz1).unmap, object);
					} else
						// happens when an enum implements an interface
						return object;
				});
			else {
				var inspect = Singleton.me.inspect;

				var sfs = Read //
						.from(inspect.fields(clazz)) //
						.map(field -> Pair.of(field.getName(), field)) //
						.toList();

				mapper = new Mapper(object -> Rethrow.ex(() -> {
					var map = new HashMap<>();
					for (var sf : sfs)
						map.put(sf.t0, sf.t1.get(object));
					return map;
				}), object -> Rethrow.ex(() -> {
					var map = (Map<?, ?>) object;
					var object1 = new_(clazz);
					for (var sf : sfs)
						sf.t1.set(object1, map.get(sf.t0));
					return object1;
				}));
			}
		} else if (type instanceof ParameterizedType) {
			var pt = (ParameterizedType) type;
			var rawType = pt.getRawType();
			var clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (List.class.isAssignableFrom(clazz))
				mapper = new Mapper(object -> {
					var map = new HashMap<>();
					var i = 0;
					for (var o : (Collection<?>) object)
						map.put(i++, o);
					return map;
				}, object -> {
					var map = (Map<?, ?>) object;
					var object1 = new ArrayList<>();
					var i = 0;
					while (map.containsKey(i))
						object1.add(map.get(i++));
					return object1;
				});
			else if (Map.class.isAssignableFrom(clazz))
				mapper = new Mapper(object -> object, object -> object);
			else
				mapper = mapper(rawType);
		} else
			mapper = Fail.t("unrecognized type " + type);

		return mapper;
	}

	public static <T extends Comparable<? super T>> T min(T t0, T t1) {
		return compare(t0, t1) < 0 ? t0 : t1;
	}

	public static <T> T new_(Class<T> clazz) {
		return Rethrow.ex(() -> {
			var ctor = clazz.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor.newInstance();
		});
	}

	public static <T> Comparator<T> nullsFirst(Comparator<T> cmp0) {
		return (key0, key1) -> {
			var b0 = key0 != null;
			var b1 = key1 != null;

			if (b0 && b1)
				return cmp0.compare(key0, key1);
			else
				return b0 ? 1 : b1 ? -1 : 0;
		};
	}

	public static void wait(Object object) {
		wait(object, 0);
	}

	public static void wait(Object object, int timeOut) {
		try {
			object.wait(timeOut);
		} catch (InterruptedException e) {
		}
	}

	private static Object apply_(Iterate<Object> fun, Object object) {
		return object != null ? fun.apply(object) : null;
	}

}
