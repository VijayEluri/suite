package suite.immutable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.immutable.IbTree.Pointer;
import suite.util.FileUtil;
import suite.util.FunUtil.Source;
import suite.util.SerializeUtil;
import suite.util.Util;

public class IbTreeTest {

	private int nSlotsPerPage = 16; // Must be same as IbTree.nSlotsPerPage

	@Test
	public void testSingleLevel() throws IOException {
		try (IbTree<Integer> ibTree0 = new IbTree<Integer>( //
				FileUtil.tmp + "/ibTree", Util.<Integer> comparator(), SerializeUtil.intSerializer)) {
			List<Integer> stamp = Arrays.asList(0);

			IbTree<Integer>.Holder holder = ibTree0.holder();
			holder.build(stamp);

			IbTree<Integer>.Transaction transaction = holder.begin();
			int size = nSlotsPerPage - 2;
			for (int i = 0; i < size; i++)
				transaction.add(i);
			for (int i = size - 1; i >= 0; i--)
				transaction.remove(i);
			for (int i = 0; i < size; i++)
				transaction.add(i);

			Source<Integer> source = transaction.source();
			Integer integer;

			while ((integer = source.source()) != null)
				System.out.println(integer);

			holder.commit(transaction);
		}
	}

	@Test
	public void testMultipleLevels() throws IOException {
		int i = 0;
		String f0 = FileUtil.tmp + "/ibTree" + i++;
		String f1 = FileUtil.tmp + "/ibTree" + i++;
		String f2 = FileUtil.tmp + "/ibTree" + i++;

		try (IbTree<Pointer> ibTree0 = new IbTree<Pointer>( //
				f0, Pointer.comparator, Pointer.serializer); //
				IbTree<Pointer> ibTree1 = new IbTree<Pointer>( //
						f1, Pointer.comparator, Pointer.serializer, ibTree0); //
				IbTree<String> ibTree2 = new IbTree<String>( //
						f2, Util.<String> comparator(), SerializeUtil.string(16), ibTree1); //
		) {
			List<Integer> stamp = Arrays.asList(0);
			int size = 2;
			stamp = IbTree.buildAllocator(ibTree0, stamp, size = size * nSlotsPerPage / 2 - 2);
			stamp = IbTree.buildAllocator(ibTree1, stamp, size = size * nSlotsPerPage / 2 - 2);

			IbTree<String>.Holder holder = ibTree2.holder();
			holder.build(stamp);

			IbTree<String>.Transaction transaction0 = holder.begin();
			transaction0.add("abc");
			transaction0.add("def");
			transaction0.add("ghi");

			holder.commit(transaction0);

			IbTree<String>.Transaction transaction1 = holder.begin();
			Source<String> source = transaction1.source();
			String string;

			while ((string = source.source()) != null)
				System.out.println(string);

			stamp = transaction1.stamp();
		}
	}

}
