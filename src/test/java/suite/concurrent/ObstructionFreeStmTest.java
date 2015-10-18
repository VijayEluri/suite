package suite.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

import suite.concurrent.ObstructionFreeStm.Memory;
import suite.concurrent.Stm.Transaction;
import suite.concurrent.Stm.TransactionStatus;

public class ObstructionFreeStmTest {

	private Random random = new Random();

	private int nMemories = 5;
	private int nTransactions = 1;

	private class Worker {
		private ObstructionFreeStm stm;
		private Transaction transaction;
		private List<Integer> orders = new ArrayList<>();
		private List<Integer> adjustments = new ArrayList<>();

		private int step;
		private List<Integer> readValues = new ArrayList<>(Collections.nCopies(nMemories, 0));

		private Worker(ObstructionFreeStm stm) {
			this.stm = stm;
			this.transaction = stm.begin();

			IntStream.range(0, nMemories * 2).forEach(i -> orders.add(i % nMemories));
			Collections.shuffle(orders, random);

			Set<Integer> isRead = new HashSet<>();

			IntStream.range(0, nMemories * 2).forEach(i -> {
				Integer mi = orders.get(i);
				orders.set(i, isRead.add(mi) ? mi : mi + nMemories);
			});

			orders.add(nMemories * 2);

			int sum = 0;

			for (int i = 1; i < nMemories; i++) {
				int adjustment = random.nextInt(100) - 50;
				adjustments.add(adjustment);
				sum += adjustment;
			}

			adjustments.add(-sum);
		}

		private void work(List<Memory<Integer>> memories) throws InterruptedException {
			int order = orders.get(step++);

			if (order >= nMemories * 2) { // Commit or rollback
				System.out.println(this + " COMMIT");
				transaction.end(TransactionStatus.DONE____);
			} else if (order >= nMemories) { // Write a memory
				int mi = order - nMemories;
				System.out.println(this + " WRITE " + mi);

				Memory<Integer> memory = memories.get(mi);
				int read = stm.get(transaction, memory);

				if (read == readValues.get(mi))
					stm.put(transaction, memory, read + adjustments.get(mi));
				else
					throw new RuntimeException("Value changed between reads");
			} else { // Read a memory
				int mi = order;
				System.out.println(this + " READ " + mi);

				Memory<Integer> memory = memories.get(mi);
				Integer read = stm.get(transaction, memory);
				readValues.set(mi, read);
			}
		}
	}

	@Before
	public void before() {
		random.setSeed(0);
	}

	@Test
	public void test() throws InterruptedException {
		ObstructionFreeStm stm = new ObstructionFreeStm();
		List<Memory<Integer>> memories = IntStream.range(0, nMemories) //
				.mapToObj(i -> stm.create(0)).collect(Collectors.toList());
		List<Worker> workers = IntStream.range(0, nTransactions) //
				.mapToObj(i -> new Worker(stm)).collect(Collectors.toList());

		List<Integer> workingOrders = new ArrayList<>();
		IntStream.range(0, nMemories * 2 + 1).forEach(mi -> IntStream.range(0, nTransactions).forEach(workingOrders::add));
		Collections.shuffle(workingOrders, random);

		for (int workingOrder : workingOrders)
			workers.get(workingOrder).work(memories);

		stm.transaction(transaction -> {
			int sum = 0;

			for (Memory<Integer> memory : memories) {
				int read = stm.get(transaction, memory);
				System.out.println("FINAL MEMORY VALUE = " + read);
				sum += read;
			}

			if (sum != 0)
				throw new RuntimeException("Final sum is not zero, but is " + sum);

			return true;
		});
	}

}
