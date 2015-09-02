package suite.lp;

import java.util.ArrayList;
import java.util.List;

import suite.node.Node;
import suite.node.Reference;

public class Trail {

	private List<Reference> boundReferences = new ArrayList<>();

	public void addBind(Reference reference, Node target) {
		if (target instanceof Reference) {
			Reference reference1 = (Reference) target;
			if (reference.getId() < reference1.getId())
				addDirectedBind(reference1, reference);
			else
				addDirectedBind(reference, reference1);
		} else
			addDirectedBind(reference, target);
	}

	private void addDirectedBind(Reference reference, Node target) {
		boundReferences.add(reference);
		reference.bound(target);
	}

	public int getPointInTime() {
		return boundReferences.size();
	}

	public void undoAllBinds() {
		undoBinds(0);
	}

	public void undoBinds(int pointInTime) {
		int i = boundReferences.size();
		while (i > pointInTime)
			boundReferences.remove(--i).unbound();
	}

}
