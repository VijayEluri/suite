package suite.node;

import java.util.concurrent.atomic.AtomicInteger;

public class Reference extends Node {

	private Node node = this;
	private int id = counter.getAndIncrement();

	private static AtomicInteger counter = new AtomicInteger();

	public static Reference of(Node node) {
		Reference reference = new Reference();
		reference.bound(node);
		return reference;
	}

	public boolean isFree() {
		return finalNode() instanceof Reference;
	}

	public void bound(Node node) {
		this.node = node;
	}

	public void unbound() { // Happens during backtracking
		node = this;
	}

	@Override
	public Node finalNode() {
		return node != this ? node.finalNode() : node;
	}

	@Override
	public int hashCode() {
		if (node != this)
			return node.hashCode();
		else
			throw new RuntimeException("No hash code for free references");
	}

	@Override
	public boolean equals(Object object) {
		if (node != this)
			return node.equals(object);
		else
			throw new RuntimeException("No equals for free references");
	}

	public int getId() {
		return id;
	}

}
