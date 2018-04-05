package suite.lp.doer;

import java.util.Objects;

import suite.lp.Trail;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.Tuple;
import suite.util.List_;

public class Binder {

	public static boolean bind(Node n0, Node n1, Trail trail) {
		n0 = n0.finalNode();
		n1 = n1.finalNode();

		if (n0 == n1)
			return true;

		Class<? extends Node> clazz0 = n0.getClass();
		Class<? extends Node> clazz1 = n1.getClass();

		if (clazz0 == Reference.class) {
			trail.addBind((Reference) n0, n1);
			return true;
		} else if (clazz1 == Reference.class) {
			trail.addBind((Reference) n1, n0);
			return true;
		}

		if (clazz0 == Dict.class && clazz1 == Dict.class) {
			var map0 = ((Dict) n0).map;
			var map1 = ((Dict) n1).map;
			var b = true;
			for (var key : List_.concat(map0.keySet(), map1.keySet())) {
				Node v0 = map0.computeIfAbsent(key, k -> new Reference());
				Node v1 = map1.computeIfAbsent(key, k -> new Reference());
				b &= bind(v0, v1, trail);
			}
			return b;
		} else if (clazz0 == Int.class && clazz1 == Int.class)
			return ((Int) n0).number == ((Int) n1).number;
		else if (clazz0 == Str.class && clazz1 == Str.class)
			return Objects.equals(((Str) n0).value, ((Str) n1).value);
		else if (Tree.class.isAssignableFrom(clazz0) && Tree.class.isAssignableFrom(clazz1)) {
			var t0 = (Tree) n0;
			var t1 = (Tree) n1;
			return t0.getOperator() == t1.getOperator() //
					&& bind(t0.getLeft(), t1.getLeft(), trail) //
					&& bind(t0.getRight(), t1.getRight(), trail);
		} else if (clazz0 == Tuple.class && clazz1 == Tuple.class) {
			var nodes0 = ((Tuple) n0).nodes;
			var nodes1 = ((Tuple) n1).nodes;
			var b = nodes0.length == nodes1.length;
			if (b) {
				for (var i = 0; i < nodes0.length; i++)
					b &= bind(nodes0[i], nodes1[i], trail);
			}
			return b;
		} else
			return false;
	}

}
