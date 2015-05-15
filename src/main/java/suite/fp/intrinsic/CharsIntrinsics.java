package suite.fp.intrinsic;

import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.immutable.IPointer;
import suite.instructionexecutor.thunk.IPointerMapper;
import suite.instructionexecutor.thunk.IndexedSourceReader;
import suite.instructionexecutor.thunk.ThunkUtil;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.primitive.Chars;
import suite.primitive.CharsUtil;
import suite.streamlet.Outlet;
import suite.util.To;

public class CharsIntrinsics {

	public Intrinsic append = (callback, inputs) -> {
		Chars chars0 = Data.get(inputs.get(0));
		Chars chars1 = Data.get(inputs.get(1));
		return new Data<>(chars0.append(chars1));
	};

	public Intrinsic charsString = (callback, inputs) -> {
		Chars chars = Data.get(inputs.get(0));
		return Intrinsics.drain(callback, p -> Int.of(chars.get(p)), chars.size());
	};

	public Intrinsic drain = (callback, inputs) -> {
		IPointer<Chars> pointer = Data.get(inputs.get(0));
		return Intrinsics.drain(callback, IPointerMapper.map(Data<Chars>::new, pointer));
	};

	public Intrinsic concatSplit = (callback, inputs) -> {
		Chars delim = Data.get(inputs.get(0));
		Outlet<Node> s0 = ThunkUtil.yawnList(callback::yawn, inputs.get(1), true);
		Outlet<Chars> s1 = s0.map(n -> Data.<Chars> get(callback.yawn(n)));
		Outlet<Chars> s2 = CharsUtil.concatSplit(s1, delim);
		Outlet<Node> s3 = s2.map(Data<Chars>::new);
		IPointer<Node> p = IndexedSourceReader.of(s3.source());
		return Intrinsics.drain(callback, p);
	};

	public Intrinsic stringChars = (callback, inputs) -> {
		String value = ThunkUtil.yawnString(callback::yawn, inputs.get(0));
		return new Data<>(To.chars(value));
	};

	public Intrinsic subchars = (callback, inputs) -> {
		int start = ((Int) inputs.get(0)).number;
		int end = ((Int) inputs.get(1)).number;
		Chars chars = Data.get(inputs.get(2));
		return new Data<>(chars.subchars(start, end));
	};

}
