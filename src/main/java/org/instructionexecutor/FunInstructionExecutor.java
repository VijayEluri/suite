package org.instructionexecutor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.instructionexecutor.InstructionUtil.Activation;
import org.instructionexecutor.InstructionUtil.Closure;
import org.instructionexecutor.InstructionUtil.Frame;
import org.instructionexecutor.InstructionUtil.Instruction;
import org.instructionexecutor.io.IndexedIo.IndexedInput;
import org.instructionexecutor.io.IndexedIo.IndexedReader;
import org.suite.Suite;
import org.suite.doer.Comparer;
import org.suite.doer.Formatter;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;
import org.suite.node.Vector;
import org.util.LogUtil;

public class FunInstructionExecutor extends InstructionExecutor {

	private static final Atom COMPARE = Atom.create("COMPARE");
	private static final Atom CONS = Atom.create("CONS");
	private static final Atom ERROR = Atom.create("ERROR");
	private static final Atom FGETC = Atom.create("FGETC");
	private static final Atom HEAD = Atom.create("HEAD");
	private static final Atom ISTREE = Atom.create("IS-TREE");
	private static final Atom ISVECTOR = Atom.create("IS-VECTOR");
	private static final Atom LOG = Atom.create("LOG");
	private static final Atom LOG2 = Atom.create("LOG2");
	private static final Atom POPEN = Atom.create("POPEN");
	private static final Atom PROVE = Atom.create("PROVE");
	private static final Atom SUBST = Atom.create("SUBST");
	private static final Atom TAIL = Atom.create("TAIL");
	private static final Atom VCONCAT = Atom.create("VCONCAT");
	private static final Atom VCONS = Atom.create("VCONS");
	private static final Atom VELEM = Atom.create("VELEM");
	private static final Atom VEMPTY = Atom.create("VEMPTY");
	private static final Atom VHEAD = Atom.create("VHEAD");
	private static final Atom VRANGE = Atom.create("VRANGE");
	private static final Atom VTAIL = Atom.create("VTAIL");

	private Comparer comparer = new Comparer();
	private Prover prover;
	private Map<Node, IndexedInput> inputs = new HashMap<>();

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
		inputs.put(Atom.NIL, new IndexedReader(reader));
		unwrap(super.execute(), writer);
	}

	@Override
	protected void handle(Exec exec, Instruction insn) {
		Activation current = exec.current;
		Frame frame = current.frame;
		Object regs[] = frame != null ? frame.registers : null;

		switch (insn.insn) {
		case SERVICE_______:
			exec.sp -= insn.op2;
			regs[insn.op0] = sys(exec, constantPool.get(insn.op1));
			break;
		default:
			super.handle(exec, insn);
		}
	}

	private Node sys(Exec exec, Node command) {
		Object stack[] = exec.stack;
		int sp = exec.sp;
		Node result;

		if (command == COMPARE) {
			Node left = (Node) stack[sp + 1];
			Node right = (Node) stack[sp];
			result = Int.create(comparer.compare(left, right));
		} else if (command == CONS) {
			Node left = (Node) stack[sp + 1];
			Node right = (Node) stack[sp];
			result = Tree.create(TermOp.AND___, left, right);
		} else if (command == ERROR)
			throw new RuntimeException("Error termination");
		else if (command == FGETC) {
			Node node = (Node) stack[sp + 1];
			int p = ((Int) stack[sp]).getNumber();
			int c = inputs.get(node).read(p);
			result = Int.create(c);
		} else if (command == HEAD)
			result = Tree.decompose((Node) stack[sp]).getLeft();
		else if (command == ISTREE)
			result = atom(Tree.decompose((Node) stack[sp]) != null);
		else if (command == ISVECTOR)
			result = atom(stack[sp] instanceof Vector);
		else if (command == LOG) {
			result = (Node) stack[sp];
			LogUtil.info(Formatter.display(unwrap(result)));
		} else if (command == LOG2) {
			Node ln = unwrap((Node) stack[sp + 1]);
			LogUtil.info(Suite.stringize(ln));
			result = (Node) stack[sp];
		} else if (command == POPEN) {
			Node n0 = unwrap((Node) stack[sp + 1]);
			Node n1 = (Node) stack[sp];

			try {
				Process process = Runtime.getRuntime().exec(unwrapToString(n0));
				InputStreamReader reader = new InputStreamReader(process.getInputStream());

				try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
					unwrap(n1, writer);
					inputs.put(result = Atom.unique(), new IndexedReader(reader));
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		} else if (command == PROVE) {
			if (prover == null)
				prover = Suite.createProver(Arrays.asList("auto.sl"));

			Node node = (Node) stack[sp];
			Tree tree = Tree.decompose(node, TermOp.JOIN__);
			if (tree != null)
				if (prover.prove(tree.getLeft()))
					result = tree.getRight().finalNode();
				else
					throw new RuntimeException("Goal failed");
			else
				result = prover.prove(node) ? Atom.TRUE : Atom.FALSE;
		} else if (command == SUBST) {
			Generalizer g = new Generalizer();
			g.setVariablePrefix("_");

			Node var = (Node) stack[sp + 1];
			Tree tree = (Tree) g.generalize((Node) stack[sp]);
			((Reference) tree.getRight()).bound(var);
			result = tree.getLeft();
		} else if (command == TAIL)
			result = Tree.decompose((Node) stack[sp]).getRight();
		else if (command == VCONCAT) {
			Vector left = (Vector) stack[sp + 1];
			Vector right = (Vector) stack[sp];
			result = Vector.concat(left, right);
		} else if (command == VCONS) {
			Node head = (Node) stack[sp + 1];
			Vector tail = (Vector) stack[sp];
			result = Vector.cons(head, tail);
		} else if (command == VELEM)
			result = new Vector((Node) stack[sp]);
		else if (command == VEMPTY)
			result = Vector.EMPTY;
		else if (command == VHEAD)
			result = ((Vector) stack[sp]).get(0);
		else if (command == VRANGE) {
			Vector vector = (Vector) stack[sp + 2];
			int s = ((Int) stack[sp + 1]).getNumber();
			int e = ((Int) stack[sp]).getNumber();
			return vector.subVector(s, e);
		} else if (command == VTAIL)
			result = ((Vector) stack[sp]).subVector(1, 0);
		else
			throw new RuntimeException("Unknown system call " + command);

		return result;
	}

	@Override
	public void close() {
		for (IndexedInput input : inputs.values())
			input.close();
	}

	public void setProver(Prover prover) {
		this.prover = prover;
	}

}
