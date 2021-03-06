package suite.persistent;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import suite.object.Object_;

public class PerSparseBinPriorityQueueTest {

	@Test
	public void test() {
		var size = 4096;

		var list = new ArrayList<Integer>();
		for (var i = 0; i < size; i++)
			list.add(i);
		Collections.shuffle(list);

		var pq = new PerSparseBinPriorityQueue<Integer>(Object_::compare);

		for (var i : list)
			pq = pq.add(i);

		for (var i = 0; i < size; i++) {
			assertEquals(i, (Object) pq.findMin());
			pq = pq.deleteMin();
		}
	}

}
