package suite.concurrent;

import java.util.concurrent.atomic.AtomicStampedReference;

import suite.util.Fail;
import suite.util.FunUtil.Iterate;

/**
 * A compare-and-set atomic reference that also uses stamp to resolve ABA
 * problem. Actually just a wrapper for atomic stamped reference.
 *
 * The reference would die after ~4 billion generations. After that all updates
 * would fail with exceptions. If your updates are really intensive, you need to
 * replace the reference after some time, or consider other types of
 * concurrency.
 * 
 * @author ywsing
 */
public class CasReference<T> {

	private AtomicStampedReference<T> asr;

	public CasReference(T t) {
		asr = new AtomicStampedReference<>(t, 0);
	}

	public T apply(Iterate<T> fun) {
		while (true) {
			int[] arr = new int[1];
			T t0 = asr.get(arr);
			T t1 = fun.apply(t0);
			int stamp = arr[0];
			if (stamp != -1)
				if (asr.compareAndSet(t0, t1, stamp, stamp + 1))
					return t1;
				else
					new Backoff().yield(); // back-off
			else
				return Fail.t("stamp overflow");
		}
	}

	public T getReference() {
		return asr.getReference();
	}

}
