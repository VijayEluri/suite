package suite.asm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import suite.Suite;
import suite.adt.Pair;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.predicate.EvalPredicates;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.util.TreeRewriter;
import suite.util.FunUtil.Fun;

public class StackAssembler extends Assembler {

	private Node rsOp0 = Atom.of("$0");
	private Node rsOp1 = Atom.of("$1");
	private Node registers[] = { Atom.of("EAX"), Atom.of("EBX"), Atom.of("ESI") };

	private Node FRBGN_ = Atom.of("FR-BEGIN");
	private Node FREND_ = Atom.of("FR-END");
	private Fun<Node, Node[]> FRGET_ = Suite.matcher("FR-GET .0");
	private Fun<Node, Node[]> FRPOP_ = Suite.matcher("FR-POP .0");
	private Fun<Node, Node[]> FRPOPN = Suite.matcher("FR-POPN .0");
	private Fun<Node, Node[]> FRPSH_ = Suite.matcher("FR-PUSH .0");
	private Fun<Node, Node[]> FRPSHN = Suite.matcher("FR-PUSHN .0");
	private Fun<Node, Node[]> LET___ = Suite.matcher("LET (.0, .1)");
	private Node RPOP__ = Atom.of("R-");
	private Node RPSH__ = Atom.of("R+");
	private Node RREST_ = Atom.of("RRESTORE");
	private Node RSAVE_ = Atom.of("RSAVE");

	public StackAssembler(int bits) {
		super(bits);
	}

	@Override
	public List<Pair<Reference, Node>> preassemble(List<Pair<Reference, Node>> lnis0) {
		List<Pair<Reference, Node>> lnis1 = new ArrayList<>();
		Deque<int[]> deque = new ArrayDeque<>();
		Trail trail = new Trail();
		int fs = 0, rs = 0;

		for (Pair<Reference, Node> lni0 : lnis0) {
			Node node0 = lni0.t1;
			Node node1;
			Node m[];

			if (node0 == FRBGN_) {
				deque.push(new int[] { fs, rs });
				fs = 0;
				rs = 0;
				node1 = Atom.NIL;
			} else if (node0 == FREND_) {
				if (fs != 0)
					throw new RuntimeException("Unbalanced frame stack in subroutine definition");
				else if (rs != 0)
					throw new RuntimeException("Unbalanced register stack in subroutine definition");
				else {
					int arr[] = deque.pop();
					fs = arr[0];
					rs = arr[1];
				}
				node1 = Atom.NIL;
			} else if ((m = FRGET_.apply(node0)) != null)
				if (Binder.bind(m[0], Int.of(-fs), trail))
					node1 = Atom.NIL;
				else
					throw new RuntimeException("Cannot bind local variable offset");
			else if ((m = FRPOP_.apply(node0)) != null) {
				fs -= 4;
				node1 = Suite.substitute("POP .0", rewrite(rs, m[0]));
			} else if ((m = FRPOPN.apply(node0)) != null) {
				Int int_ = (Int) m[0].finalNode();
				fs -= int_.number;
				node1 = Atom.NIL;
			} else if ((m = FRPSH_.apply(node0)) != null) {
				fs += 4;
				node1 = Suite.substitute("PUSH .0", rewrite(rs, m[0]));
			} else if ((m = FRPSHN.apply(node0)) != null) {
				Int int_ = (Int) m[0].finalNode();
				fs += int_.number;
				node1 = Atom.NIL;
			} else if ((m = LET___.apply(node0)) != null)
				if (Binder.bind(m[0], Int.of(new EvalPredicates().evaluate(m[1])), trail))
					node1 = Atom.NIL;
				else
					throw new RuntimeException("Cannot calculate expression");
			else if (node0 == RPOP__) {
				rs--;
				node1 = Atom.NIL;
			} else if (node0 == RPSH__) {
				rs++;
				node1 = Atom.NIL;
			} else if (node0 == RREST_) {
				fs -= 4 * rs;
				for (int r = rs - 1; r >= 0; r--)
					lnis1.add(Pair.of(new Reference(), Suite.substitute("POP .0", getRegister(r))));
				node1 = Atom.NIL;
			} else if (node0 == RSAVE_) {
				for (int r = 0; r < rs; r++)
					lnis1.add(Pair.of(new Reference(), Suite.substitute("PUSH .0", getRegister(r))));
				fs += 4 * rs;
				node1 = Atom.NIL;
			} else
				node1 = rewrite(rs, node0);

			lnis1.add(Pair.of(lni0.t0, node1));
		}

		return lnis1;
	}

	private Node rewrite(int sp, Node n) {
		if (sp - 1 >= 0)
			n = new TreeRewriter().replace(rsOp0, getRegister(sp - 1), n);
		if (sp - 2 >= 0)
			n = new TreeRewriter().replace(rsOp1, getRegister(sp - 2), n);
		return n;
	}

	private Node getRegister(int p) {
		if (p < registers.length)
			return registers[p];
		else
			throw new RuntimeException("Register stack overflow");
	}

}
