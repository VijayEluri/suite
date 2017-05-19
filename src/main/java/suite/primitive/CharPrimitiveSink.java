package suite.primitive;

public class CharPrimitiveSink {

	@FunctionalInterface
	public interface CharSink {
		public void sink(char c);
	}

	@FunctionalInterface
	public interface CharCharSink {
		public void sink(char c, int d);
	}

	@FunctionalInterface
	public interface CharObjSink<T> { // extends ObjCharConsumer<T>
		public void sink(char c, T t);
	}

}
