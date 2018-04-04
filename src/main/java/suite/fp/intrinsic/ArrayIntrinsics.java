package suite.fp.intrinsic;

import java.util.Arrays;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tuple;
import suite.util.Array_;

public class ArrayIntrinsics {

	public Intrinsic append = (callback, inputs) -> {
		var array0 = ((Tuple) inputs.get(0)).nodes;
		var array1 = ((Tuple) inputs.get(1)).nodes;
		var array = new Node[array0.length + array1.length];
		Array_.copy(array0, 0, array, 0, array0.length);
		Array_.copy(array1, 0, array, array0.length, array1.length);
		return Tuple.of(array);
	};

	public Intrinsic arrayList = (callback, inputs) -> {
		var array = ((Tuple) inputs.get(0)).nodes;
		return Intrinsics.drain(callback, i -> array[i], array.length);
	};

	public Intrinsic listArray = (callback, inputs) -> {
		return Tuple.of(ThunkUtil.yawnList(callback::yawn, inputs.get(0), true).toArray(Node.class));
	};

	public Intrinsic slice = (callback, inputs) -> {
		var s = ((Int) inputs.get(0)).number;
		var e = ((Int) inputs.get(1)).number;
		var array = ((Tuple) inputs.get(2)).nodes;
		if (s < 0)
			s += array.length;
		if (e < s)
			e += array.length;
		return Tuple.of(Arrays.copyOfRange(array, s, e, Node[].class));
	};

}
