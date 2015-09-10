package suite.lp.predicate;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import suite.lp.doer.Cloner;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.TermOp;
import suite.node.util.SuiteException;
import suite.os.FileUtil;
import suite.os.LogUtil;
import suite.primitive.Bytes.BytesBuilder;

public class IoPredicates {

	public BuiltinPredicate dump = PredicateUtil.ps((prover, ps) -> {
		for (Node p : ps)
			System.out.print(Formatter.dump(p) + " ");
		return true;
	});

	public BuiltinPredicate dumpStack = (prover, ps) -> {
		String date = LocalDateTime.now().toString();
		String trace = prover.getTracer().getStackTrace();
		LogUtil.info("-- Stack trace at " + date + " --\n" + trace);
		return true;
	};

	public BuiltinPredicate exec = PredicateUtil.p1((prover, p0) -> {
		if (p0 instanceof Str)
			try {
				String cmd = ((Str) p0).value;
				return Runtime.getRuntime().exec(cmd).waitFor() == 0;
			} catch (Exception ex)

			{ // IOException or InterruptedException
				LogUtil.error(ex);
			}
		return false;
	});

	public BuiltinPredicate exit = PredicateUtil.sink(n -> System.exit(n instanceof Int ? ((Int) n).number : 0));

	public BuiltinPredicate fileExists = PredicateUtil.bool(n -> Files.exists(Paths.get(Formatter.display(n))));

	public BuiltinPredicate fileRead = PredicateUtil.fun(n -> {
		String filename = Formatter.display(n);
		try {
			return new Str(FileUtil.read(filename));
		} catch (IOException ex)

		{
			throw new RuntimeException(ex);
		}
	});

	public BuiltinPredicate fileWrite = PredicateUtil.p2((prover, fn, contents) -> {
		String filename = Formatter.display(fn);
		String content = Formatter.display(contents);

		try (OutputStream fos = FileUtil.out(filename)) {
			fos.write(content.getBytes(FileUtil.charset));
		} catch (IOException ex)

		{
			throw new RuntimeException(ex);
		}

		return true;
	});

	public BuiltinPredicate homeDir = PredicateUtil.p1((prover, p0) -> prover.bind(new Str(FileUtil.homeDir()), p0));

	public BuiltinPredicate nl = PredicateUtil.run(() -> System.out.println());

	public BuiltinPredicate readLine = PredicateUtil.p1((prover, p0) -> {
		try {
			BytesBuilder bb = new BytesBuilder();
			byte b;
			while ((b = (byte) System.in.read()) >= 0 && b != 10)
				bb.append(b);
			String s = new String(bb.toBytes().toBytes(), FileUtil.charset);
			return prover.bind(new Str(s), p0);
		} catch (IOException ex)

		{
			throw new RuntimeException(ex);
		}
	});

	public BuiltinPredicate log = PredicateUtil.sink(n -> LogUtil.info(Formatter.dump(n)));

	public BuiltinPredicate sink = (prover, ps) -> {
		prover.config().getSink().sink(ps);
		return false;
	};

	public BuiltinPredicate source = PredicateUtil.p1((prover, p0) -> {
		Node source = prover.config().getSource().source();
		return prover.bind(p0, source);
	});

	public BuiltinPredicate throwPredicate = PredicateUtil.sink(n -> {
		throw new SuiteException(new Cloner().clone(n));
	});

	public BuiltinPredicate tryPredicate = PredicateUtil.p3((prover, try_, catch_, throw_) -> {
		try {
			return PredicateUtil.tryProve(prover, prover1 -> prover1.prove0(try_));
		} catch (SuiteException ex)

		{
			if (prover.bind(catch_, ex.getNode())) {
				prover.setRemaining(Tree.of(TermOp.AND___, throw_, prover.getRemaining()));
				return true;
			} else
				throw ex;
		}

	});

	public BuiltinPredicate write(PrintStream printStream) {
		return PredicateUtil.ps((prover, ps) -> {
			for (Node p : ps)
				printStream.print(Formatter.display(p) + " ");
			return true;
		});
	}

}
