package suite.node;

import java.util.HashMap;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.object.Object_;

public class Dict extends Node {

	public final Reference reference = new Reference();
	private Map<Node, Reference> map;

	public static Map<Node, Reference> m(Node node) {
		return ((Dict) node).getMap();
	}

	public static Dict of() {
		return new Dict(new HashMap<>());
	}

	public static Dict ofPairs(Pair<Node, Reference>[] pairs) {
		var map = new HashMap<Node, Reference>();
		for (var pair : pairs)
			map.put(pair.t0, pair.t1);
		return of(map);
	}

	public static Dict of(Map<Node, Reference> map) {
		return new Dict(map);
	}

	private Dict() {
		this(new HashMap<>());
	}

	private Dict(Map<Node, Reference> map) {
		this.map = map;
	}

	@Override
	public Node finalNode() {
		return !reference.isFree() ? reference : this;
	}

	@Override
	public boolean equals(Object object) {
		var map0 = getMap();
		var map1 = ((Dict) object).getMap();
		return Object_.clazz(object) == Dict.class ? map0.equals(map1) : false;
	}

	@Override
	public int hashCode() {
		return getMap().hashCode();
	}

	public Map<Node, Reference> getMap() {
		var n = reference.finalNode();
		return n instanceof Dict ? ((Dict) n).map : map;
	}

}
