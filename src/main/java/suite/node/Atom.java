package suite.node;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import suite.node.util.Context;
import suite.node.util.Singleton;

public class Atom extends Node {

	private String name;

	public static Atom NIL = create("");
	public static Atom TRUE = create("true");
	public static Atom FALSE = create("false");

	private static AtomicInteger uniqueCounter = new AtomicInteger();

	private Atom(String name) {
		this.name = name;
	}

	public static Atom unique() {
		Context context = Singleton.get().getHiddenContext();
		return create(context, "TEMP" + uniqueCounter.getAndIncrement());
	}

	public static Atom create(String name) {
		return create(Singleton.get().getGrandContext(), name);
	}

	public static Atom create(Context context, String name) {
		Atom atom;
		Map<String, WeakReference<Atom>> pool = context.getAtomPool();
		synchronized (pool) {
			WeakReference<Atom> ref = pool.get(name);
			if (ref == null || (atom = ref.get()) == null)
				pool.put(name, new WeakReference<>(atom = new Atom(name)));
		}
		return atom;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object object) {
		return this == object //
				|| object instanceof Node && this == ((Node) object).finalNode();
	}

	public String getName() {
		return name;
	}

}
