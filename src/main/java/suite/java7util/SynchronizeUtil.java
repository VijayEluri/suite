package suite.java7util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public class SynchronizeUtil {

	public static <I> I proxy(Class<I> interface_, I object) {
		@SuppressWarnings("unchecked")
		Class<I> clazz = (Class<I>) object.getClass();
		ClassLoader classLoader = clazz.getClassLoader();
		Class<?> classes[] = { interface_ };

		InvocationHandler handler = (proxy, method, ps) -> {
			synchronized (object) {
				try {
					return method.invoke(object, ps);
				} catch (InvocationTargetException ite) {
					Throwable th = ite.getTargetException();
					throw th instanceof Exception ? (Exception) th : ite;
				}
			}
		};

		@SuppressWarnings("unchecked")
		I proxied = (I) Proxy.newProxyInstance(classLoader, classes, handler);
		return proxied;
	}

}
