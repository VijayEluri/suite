package suite.concurrent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LockFreeLazyQueueTest {

	@Test
	public void test() {
		var lfq = new LockFreeLazyQueue<>();
		for (var i = 0; i < 256; i++) {
			for (var j = 0; j < 256; j++)
				lfq.enqueue(j);
			for (var j = 0; j < 256; j++)
				assertEquals(j, (int) lfq.dequeue());
		}
	}

}
