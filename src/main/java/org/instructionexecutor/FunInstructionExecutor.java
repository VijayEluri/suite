package org.instructionexecutor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.instructionexecutor.InstructionUtil.Activation;
import org.instructionexecutor.InstructionUtil.Closure;
import org.instructionexecutor.InstructionUtil.Frame;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.instructionexecutor.io.IndexedIo;
import org.instructionexecutor.io.IndexedReader;
import org.suite.doer.Comparer;
import org.suite.doer.Formatter;
import org.suite.doer.ProverConfig;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Tree;
import org.suite.node.Vector;
import org.util.LogUtil;

public class FunInstructionExecutor extends InstructionExecutor {

	private Comparer comparer = new Comparer();
	private ProverConfig proverConfig;
	private IndexedIo indexedIo = new IndexedIo();

	public FunInstructionExecutor(Node node) {
		super(node);
	}

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and converts to a
	 * string.
	 */
	public String unwrapToString(Node node) {
		StringWriter writer = new StringWriter();

		try {
			unwrap(node, writer);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return writer.toString();
	}

	/**
	 * Evaluates the whole (lazy) term to a list of numbers, and write
	 * corresponding characters into the writer.
	 */
	public void unwrap(Node node, Writer writer) throws IOException {
		while (true) {
			node = node.finalNode();

			if (node instanceof Tree) {
				Tree tree = (Tree) node;
				writer.write(((Int) unwrap(tree.getLeft())).getNumber());
				node = tree.getRight();
			} else if (node instanceof Closure)
				node = evaluateClosure((Closure) node);
			else if (node == Atom.NIL)
				break;
		}
	}

	/**
	 * Evaluates the whole (lazy) term to actual by invoking all the thunks.
	 */
	public Node unwrap(Node node) {
		node = node.finalNode();

		if (node instanceof Tree) {
			Tree tree = (Tree) node;
			Node left = unwrap(tree.getLeft());
			Node right = unwrap(tree.getRight());
			node = Tree.create(tree.getOperator(), left, right);
		} else if (node instanceof Closure)
			node = unwrap(evaluateClosure((Closure) node));

		return node;
	}

	public void executeIo(Reader reader, Writer writer) throws IOException {
		indexedIo.put(Atom.NIL, new IndexedReader(reader));
		unwrap(super.execute(), writer);
	}

	@Override
	protected void handle(Exec exec, Instruction insn) {
		Activation current = exec.current;
		Frame frame = current.frame;
		Object regs[] = frame != null ? frame.registers : null;

		Object ds[] = exec.stack;
		int dsp = exec.sp;

		Node node, left, right, result;

		switch (insn.insn) {
		case COMPARE_______:
			left = (Node) ds[--dsp];
			right = (Node) ds[--dsp];
			result = Int.create(comparer.compare(left, right));
			break;
		case CONS__________:
			left = (Node) ds[--dsp];
			right = (Node) ds[--dsp];
			result = Tree.create(TermOp.AND___, left, right);
			break;
		case ERROR_________:
			throw new RuntimeException("Error termination");
		case FGETC_________:
			node = (Node) ds[--dsp];
			int p = ((Int) ds[--dsp]).getNumber();
			int c = indexedIo.get(node).read(p);
			result = Int.create(c);
			break;
		case HEAD__________:
			result = Tree.decompose((Node) ds[--dsp]).getLeft();
			break;
		case ISTREE________:
			result = atom(Tree.decompose((Node) ds[--dsp]) != null);
			break;
		case ISVECTOR______:
			result = atom(ds[--dsp] instanceof Vector);
			break;
		case LOG1__________:
			result = (Node) ds[--dsp];
			LogUtil.info(Formatter.display(unwrap(result)));
			break;
		case LOG2__________:
			LogUtil.info(unwrapToString((Node) ds[--dsp]));
			result = (Node) ds[--dsp];
			break;
		case POPEN_________:
			Node n0 = (Node) ds[--dsp];
			Node n1 = (Node) ds[--dsp];
			result = execPopen(indexedIo, n0, n1);
			break;
		case PROVE_________:
			node = (Node) ds[--dsp];
			result = InstructionUtil.execProve(proverConfig, node);
			break;
		case SUBST_________:
			Node var = (Node) ds[--dsp];
			node = (Node) ds[--dsp];
			result = InstructionUtil.execSubst(node, var);
			break;
		case TAIL__________:
			result = Tree.decompose((Node) ds[--dsp]).getRight();
			break;
		case VCONCAT_______:
			Vector vector0 = (Vector) ds[--dsp];
			Vector vector1 = (Vector) ds[--dsp];
			result = Vector.concat(vector0, vector1);
			break;
		case VCONS_________:
			Node head = (Node) ds[--dsp];
			Vector tail = (Vector) ds[--dsp];
			result = Vector.cons(head, tail);
			break;
		case VELEM_________:
			result = new Vector((Node) ds[--dsp]);
			break;
		case VEMPTY________:
			result = Vector.EMPTY;
			break;
		case VHEAD_________:
			result = ((Vector) ds[--dsp]).get(0);
			break;
		case VRANGE________:
			Vector vector = (Vector) ds[--dsp];
			int s = ((Int) ds[--dsp]).getNumber();
			int e = ((Int) ds[--dsp]).getNumber();
			result = vector.subVector(s, e);
			break;
		case VTAIL_________:
			result = ((Vector) ds[--dsp]).subVector(1, 0);
			break;
		default:
			throw new RuntimeException("Unknown instruction " + insn);
		}

		exec.sp = dsp;
		regs[insn.op0] = result;
	}

	private Node execPopen(IndexedIo indexedIo, Node n0, final Node n1) {
		try {
			Node result = Atom.unique();
			final Process process = Runtime.getRuntime().exec(unwrapToString(n0));

			InputStreamReader reader = new InputStreamReader(process.getInputStream());
			indexedIo.put(result, new IndexedReader(reader));

			// Use a separate thread to write to the process, so that read and
			// write occur at the same time and would not block up.
			// Have to make sure the executors are thread-safe!
			new Thread() {
				public void run() {
					try (OutputStream pos = process.getOutputStream(); Writer writer = new OutputStreamWriter(pos)) {
						unwrap(n1, writer);
					} catch (IOException ex) {
						LogUtil.error(ex);
					}
				}
			}.start();

			return result;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void close() {
		indexedIo.close();
	}

	public void setProverConfig(ProverConfig proverConfig) {
		this.proverConfig = proverConfig;
	}

}
