package suite.stm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import suite.os.LogUtil;
import suite.stm.Mutex.DeadlockException;
import suite.stm.Mutex.MutexLock;
import suite.streamlet.Read;
import suite.util.Util;

public class MutexTest {

	public interface MutexTestRunnable {
		public void run() throws DeadlockException;
	}

	@Test
	public void testDeadlock() throws InterruptedException {
		Mutex a = new Mutex();
		Mutex b = new Mutex();

		MutexTestRunnable ra = () -> {
			try (MutexLock mla = new MutexLock(a)) {
				Util.sleepQuietly(500);
				try (MutexLock mlb = new MutexLock(b)) {
				}
			}
		};
		MutexTestRunnable rb = () -> {
			try (MutexLock mlb = new MutexLock(b)) {
				Util.sleepQuietly(500);
				try (MutexLock mla = new MutexLock(a)) {
				}
			}
		};

		assertTrue(isDeadlock(ra, rb));
	}

	@Test
	public void testNoDeadlock() throws InterruptedException {
		Mutex a = new Mutex();
		Mutex b = new Mutex();

		MutexTestRunnable ra = () -> {
			try (MutexLock mla = new MutexLock(a)) {
				Util.sleepQuietly(500);
				try (MutexLock mlb = new MutexLock(b)) {
				}
			}
		};
		MutexTestRunnable rb = () -> {
			try (MutexLock mla = new MutexLock(a)) {
				Util.sleepQuietly(500);
				try (MutexLock mlb = new MutexLock(b)) {
				}
			}
		};

		assertFalse(isDeadlock(ra, rb));
	}

	private boolean isDeadlock(MutexTestRunnable... mtrs) throws InterruptedException {
		boolean result[] = new boolean[] { false };
		List<Thread> threads = Read.from(mtrs) //
				.map(mtr -> new Thread(() -> {
					try {
						mtr.run();
					} catch (DeadlockException ex1) {
						result[0] = true;
					} catch (Exception ex2) {
						LogUtil.error(ex2);
					}
				})) //
				.toList();
		threads.forEach(Thread::start);
		for (Thread thread : threads)
			thread.join();
		return result[0];
	}

}
