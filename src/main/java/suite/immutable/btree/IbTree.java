package suite.immutable.btree;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;
import suite.util.To;
import suite.util.Util;

/**
 * Immutable, on-disk B-tree implementation.
 *
 * To allow efficient page management, a large B-tree has one smaller B-tree for
 * storing unused pages, called allocation B-tree. That smaller one might
 * contain a even smaller allocation B-tree, until it becomes small enough to
 * fit in a single disk page.
 *
 * Transaction control is done by a "stamp" consisting of a chain of root page
 * numbers of all B-trees. The holder object persist the stmap into another
 * file.
 *
 * @author ywsing
 */
public class IbTree<Key> implements Closeable {

	private String filename;
	private Comparator<Key> comparator;
	private Serializer<Key> serializer;

	private PageFile pageFile;
	private SerializedPageFile<Page> serializedPageFile;
	private SerializedPageFile<Bytes> serializedPayloadPageFile;
	private IbTree<Integer> allocationIbTree;

	private int maxBranchFactor; // Exclusive
	private int minBranchFactor; // Inclusive

	public static Serializer<Integer> pointerSerializer = SerializeUtil.nullable(SerializeUtil.intSerializer);

	private class Page {
		private List<Slot> slots;

		private Page(List<Slot> slots) {
			this.slots = slots;
		}
	}

	private enum SlotType {
		BRANCH, DATA, TERMINAL
	}

	/**
	 * In leaves, pointer would be null, and pivot stores the leaf value.
	 *
	 * Pivot would be null at the minimum side of a tree as the guarding key.
	 */
	private class Slot {
		private SlotType type;
		private Key pivot;
		private Integer pointer;

		private Slot(SlotType type, Key pivot, Integer pointer) {
			this.type = type;
			this.pivot = pivot;
			this.pointer = pointer;
		}

		private List<Slot> slots() {
			return type == SlotType.BRANCH ? read(pointer).slots : null;
		}
	}

	private class FindSlot {
		private Slot slot;
		private int i, c;

		private FindSlot(List<Slot> slots, Key key) {
			this(slots, key, false);
		}

		private FindSlot(List<Slot> slots, Key key, boolean isExclusive) {
			i = slots.size() - 1;
			while ((c = compare((slot = slots.get(i)).pivot, key)) > 0 || isExclusive && c == 0)
				i--;
		}
	}

	private interface Allocator {
		public Integer allocate();

		public void discard(Integer pointer);

		public List<Integer> flush();
	}

	/**
	 * Protect discarded pages belonging to previous transactions, so that they
	 * are not being allocated immediately. This supports immutability (i.e.
	 * copy-on-write) and with this recovery can succeed.
	 *
	 * On the other hand, allocated and discarded pages are reused here, since
	 * they belong to current transaction.
	 */
	private class DelayedDiscardAllocator implements Allocator {
		private Allocator allocator;
		private Set<Integer> allocated = new HashSet<>();
		private Deque<Integer> discarded = new ArrayDeque<>(); // Non-reusable
		private Deque<Integer> allocateDiscarded = new ArrayDeque<>(); // Reusable

		private DelayedDiscardAllocator(Allocator allocator) {
			this.allocator = allocator;
		}

		public Integer allocate() {
			Integer pointer = allocateDiscarded.isEmpty() ? allocator.allocate() : allocateDiscarded.pop();
			allocated.add(pointer);
			return pointer;
		}

		public void discard(Integer pointer) {
			(allocated.remove(pointer) ? allocateDiscarded : discarded).push(pointer);
		}

		public List<Integer> flush() {
			while (!discarded.isEmpty())
				allocator.discard(discarded.pop());
			while (!allocateDiscarded.isEmpty())
				allocator.discard(allocateDiscarded.pop());
			return allocator.flush();
		}
	}

	private class SwappingAllocator implements Allocator {
		private int active;
		private Deque<Integer> deque;

		private SwappingAllocator(int active) {
			reset(active);
		}

		public Integer allocate() {
			return deque.pop();
		}

		public void discard(Integer pointer) {
		}

		public List<Integer> flush() {
			List<Integer> stamp = Arrays.asList(active);
			reset(1 - active);
			return stamp;
		}

		private void reset(int active) {
			this.active = active;
			deque = new ArrayDeque<>(Arrays.asList(1 - active));
		}
	}

	private class SubIbTreeAllocator implements Allocator {
		private IbTree<Integer>.Transaction transaction;

		private SubIbTreeAllocator(IbTree<Integer>.Transaction transaction) {
			this.transaction = transaction;
		}

		public Integer allocate() {
			Integer pointer = transaction.keys().source();
			if (pointer != null) {
				transaction.remove(pointer);
				return pointer;
			} else
				throw new RuntimeException("Pages exhausted");
		}

		public void discard(Integer pointer) {
			transaction.put(pointer);
		}

		public List<Integer> flush() {
			return transaction.flush();
		}
	}

	public class Transaction {
		private Allocator allocator;
		private Integer root;

		private Transaction(Allocator allocator) {
			this.allocator = allocator;
			root = persist(Arrays.asList(new Slot(SlotType.TERMINAL, null, null)));
		}

		private Transaction(Allocator allocator, Integer root) {
			this.allocator = allocator;
			this.root = root;
		}

		public Source<Key> keys() {
			return keys(null, null);
		}

		public Source<Key> keys(Key start, Key end) {
			return IbTree.this.keys(root, start, end);
		}

		public Integer getData(Key key) {
			return get(root, key, SlotType.TERMINAL);
		}

		public Bytes getPayload(Key key) {
			Integer pointer = get(root, key, SlotType.DATA);
			return pointer != null ? serializedPayloadPageFile.load(pointer) : null;
		}

		/**
		 * Replaces a value with another without payload. For dictionary cases
		 * to replace stored value of the same key.
		 */
		public void put(Key key) {
			update(key, new Slot(SlotType.TERMINAL, key, null));
		}

		public void put(Key key, Integer data) {
			update(key, new Slot(SlotType.TERMINAL, key, data));
		}

		/**
		 * Replaces a value with another, attached with a payload of page data.
		 * For dictionary cases to replace stored value of the same key.
		 *
		 * Asserts comparator.compare(<original-key>, key) == 0.
		 */
		public <Payload> void put(Key key, Bytes payload) {
			Integer pointer = allocator.allocate();
			serializedPayloadPageFile.save(pointer, payload);
			update(key, new Slot(SlotType.DATA, key, pointer));
		}

		public void remove(Key key) {
			allocator.discard(root);
			root = createRootPage(delete(read(root).slots, key));
		}

		private void update(Key key, Slot slot1) {
			update(key, slot -> slot1);
		}

		private void update(Key key, Fun<Slot, Slot> replacer) {
			allocator.discard(root);
			root = createRootPage(update(read(root).slots, key, replacer));
		}

		private List<Slot> update(List<Slot> slots0, Key key, Fun<Slot, Slot> replacer) {
			FindSlot fs = new FindSlot(slots0, key);

			// Adds the node into it
			List<Slot> replaceSlots;

			if (fs.slot.type == SlotType.BRANCH)
				replaceSlots = update(discard(fs.slot).slots(), key, replacer);
			else if (fs.c != 0)
				replaceSlots = Arrays.asList(fs.slot, replacer.apply(null));
			else
				replaceSlots = Arrays.asList(replacer.apply(discard(fs.slot)));

			List<Slot> slots1 = Util.add(Util.left(slots0, fs.i), replaceSlots, Util.right(slots0, fs.i + 1));

			List<Slot> slots2;

			// Checks if need to split
			if (slots1.size() < maxBranchFactor)
				slots2 = Arrays.asList(slot(slots1));
			else { // Splits into two if reached maximum number of nodes
				List<Slot> leftSlots = Util.left(slots1, minBranchFactor);
				List<Slot> rightSlots = Util.right(slots1, minBranchFactor);
				slots2 = Arrays.asList(slot(leftSlots), slot(rightSlots));
			}

			return slots2;
		}

		private List<Slot> delete(List<Slot> slots0, Key key) {
			FindSlot fs = new FindSlot(slots0, key);

			int size = slots0.size();

			// Removes the node from it
			int s0 = fs.i, s1 = fs.i + 1;
			List<Slot> replaceSlots;

			if (fs.slot.type == SlotType.BRANCH) {
				List<Slot> slots1 = delete(discard(fs.slot).slots(), key);

				// Merges with a neighbor if reached minimum number of nodes
				if (slots1.size() < minBranchFactor)
					if (s0 > 0)
						replaceSlots = merge(discard(slots0.get(--s0)).slots(), slots1);
					else if (s1 < size)
						replaceSlots = merge(slots1, discard(slots0.get(s1++)).slots());
					else
						replaceSlots = Arrays.asList(slot(slots1));
				else
					replaceSlots = Arrays.asList(slot(slots1));
			} else if (fs.c == 0)
				replaceSlots = Collections.emptyList();
			else
				throw new RuntimeException("Node not found " + key);

			return Util.add(Util.left(slots0, s0), replaceSlots, Util.right(slots0, s1));
		}

		private List<Slot> merge(List<Slot> slots0, List<Slot> slots1) {
			List<Slot> merged;

			if (slots0.size() + slots1.size() >= maxBranchFactor) {
				List<Slot> leftSlots, rightSlots;

				if (slots0.size() > minBranchFactor) {
					leftSlots = Util.left(slots0, -1);
					rightSlots = Util.add(Arrays.asList(Util.last(slots0)), slots1);
				} else {
					leftSlots = Util.add(slots0, Arrays.asList(Util.first(slots1)));
					rightSlots = Util.right(slots1, 1);
				}

				merged = Arrays.asList(slot(leftSlots), slot(rightSlots));
			} else
				merged = Arrays.asList(slot(Util.add(slots0, slots1)));

			return merged;
		}

		private List<Integer> flush() {
			return Util.add(Arrays.asList(root), allocator.flush());
		}

		private Integer createRootPage(List<Slot> slots) {
			Slot slot;
			Integer pointer;
			if (slots.size() == 1 && (slot = slots.get(0)).type == SlotType.BRANCH)
				pointer = slot.pointer;
			else
				pointer = persist(slots);
			return pointer;
		}

		private Slot slot(List<Slot> slots) {
			return new Slot(SlotType.BRANCH, Util.first(slots).pivot, persist(slots));
		}

		private Slot discard(Slot slot) {
			if (slot != null && slot.type != SlotType.TERMINAL)
				allocator.discard(slot.pointer);
			return slot;
		}

		private Integer persist(List<Slot> slots) {
			Integer pointer = allocator.allocate();
			write(pointer, new Page(slots));
			return pointer;
		}
	}

	public class Txm implements Closeable {
		private SerializedPageFile<List<Integer>> stampFile;

		private Txm() {
			stampFile = new SerializedPageFile<>(filename + ".stamp", SerializeUtil.list(SerializeUtil.intSerializer));
		}

		public Transaction begin() {
			return transaction(stampFile.load(0));
		}

		public void commit(Transaction transaction) {
			List<Integer> stamp = transaction.flush();
			sync();
			stampFile.save(0, stamp);
		}

		public void close() {
			stampFile.close();
		}
	}

	/**
	 * Constructor for larger trees that require another tree for page
	 * allocation management.
	 */
	public IbTree(String filename //
			, int maxBranchFactor //
			, int pageSize //
			, Comparator<Key> comparator //
			, Serializer<Key> serializer //
			, IbTree<Integer> allocationIbTree) throws FileNotFoundException {
		this.filename = filename;
		this.comparator = comparator;
		this.serializer = SerializeUtil.nullable(serializer);

		pageFile = new PageFile(filename, pageSize);
		serializedPageFile = new SerializedPageFile<>(pageFile, createPageSerializer());
		serializedPayloadPageFile = new SerializedPageFile<>(pageFile, SerializeUtil.bytes(pageFile.getPageSize()));
		this.allocationIbTree = allocationIbTree;

		this.maxBranchFactor = maxBranchFactor;
		minBranchFactor = maxBranchFactor / 2;
	}

	@Override
	public void close() {
		serializedPageFile.close();
	}

	/**
	 * @return Transaction manager for this immutable B-tree.
	 */
	public Txm txm() {
		return new Txm();
	}

	/**
	 * @return Calculate the maximum number of values that can be stored in this
	 *         tree before running out of pages, regardless of the branching
	 *         statuses, in a most conservative manner.
	 *
	 *         First, we relate the number of branches in nodes to the size of
	 *         the tree. For each branch node, it occupy 1 child of its parent,
	 *         and create children at the number of branch factor. Therefore its
	 *         "gain" is its branch factor minus 1. The tree root is a single
	 *         entry, thus the sum of all "gains" plus 1 result in the total
	 *         number of leave nodes.
	 *
	 *         Second, we find the smallest tree for n pages. 1 page is used as
	 *         the root which has 2 children at minimum. Other pages should have
	 *         half of branch factor at minimum.
	 *
	 *         Third, to cause page exhaustion at next insert, it require a
	 *         split to occur. Therefore 1 page should be at its maximum size.
	 *         This adds in half of branch factor minus 1 of nodes.
	 *
	 *         Fourth, the result needs to be minus by 1 to exclude the guard
	 *         node at rightmost of the tree.
	 *
	 *         Fifth, most transactions would acquire some new pages before old
	 *         pages could be discarded during commit. We have to reserve 10% of
	 *         pages for transaction use.
	 *
	 *         In formula, the minimum number of nodes causing split: 1 + (2 -
	 *         1) + (size - 1) * (minBranchFactor - 1) + (minBranchFactor - 1) -
	 *         1 = size * (minBranchFactor - 1) + 1
	 */
	public int guaranteedCapacity() {
		if (allocationIbTree != null)
			// Refers the long pile above
			return allocationIbTree.guaranteedCapacity() * 9 / 10 * (minBranchFactor - 1) + 1;
		else
			// There are at most maxBranchFactor - 1 nodes, and need to keep 1
			// for the guard node too
			return maxBranchFactor - 2;
	}

	public Transaction create() {
		List<Integer> stamp0;

		if (allocationIbTree != null) {
			IbTree<Integer>.Transaction transaction0 = allocationIbTree.create();
			int nPages = allocationIbTree.guaranteedCapacity();
			for (int p = 0; p < nPages; p++)
				transaction0.put(p);
			stamp0 = transaction0.flush();
		} else
			stamp0 = Arrays.asList(0);

		return new Transaction(allocator(stamp0));
	}

	private Source<Key> keys(Integer pointer, Key start, Key end) {
		return FunUtil.map(slot -> slot.pivot, source0(pointer, start, end));
	}

	private Integer get(Integer root, Key key, SlotType slotType) {
		Slot slot = source0(root, key, null).source();
		if (slot != null && slot.type == slotType && compare(slot.pivot, key) == 0)
			return slot.pointer;
		else
			return null;
	}

	private Source<Slot> source0(Integer pointer, Key start, Key end) {
		List<Slot> node = read(pointer).slots;
		int i0 = start != null ? new FindSlot(node, start).i : 0;
		int i1 = end != null ? new FindSlot(node, end, true).i + 1 : node.size();

		if (i0 < i1)
			return FunUtil.concat(FunUtil.map(slot -> {
				if (slot.type == SlotType.BRANCH)
					return source0(slot.pointer, start, end);
				else
					return slot.pivot != null ? To.source(slot) : FunUtil.<Slot> nullSource();
			}, To.source(node.subList(i0, i1))));
		else
			return FunUtil.nullSource();
	}

	private Transaction transaction(List<Integer> stamp) {
		return new Transaction(allocator(Util.right(stamp, 1)), stamp.get(0));
	}

	private Allocator allocator(List<Integer> stamp0) {
		Allocator allocator;
		if (allocationIbTree != null)
			allocator = new SubIbTreeAllocator(allocationIbTree.transaction(stamp0));
		else
			allocator = new SwappingAllocator(stamp0.get(0));
		return new DelayedDiscardAllocator(allocator);
	}

	private int compare(Key key0, Key key1) {
		boolean b0 = key0 != null;
		boolean b1 = key1 != null;

		if (b0 && b1)
			return comparator.compare(key0, key1);
		else
			return b0 ? 1 : b1 ? -1 : 0;
	}

	private Page read(Integer pointer) {
		return serializedPageFile.load(pointer);
	}

	private void write(Integer pointer, Page page) {
		serializedPageFile.save(pointer, page);
	}

	private void sync() {
		try {
			pageFile.sync();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Serializer<Page> createPageSerializer() {
		Serializer<List<Slot>> slotsSerializer = SerializeUtil.list(new Serializer<Slot>() {
			public Slot read(ByteBuffer buffer) {
				SlotType type = SlotType.values()[buffer.get()];
				Key pivot = serializer.read(buffer);
				Integer pointer = pointerSerializer.read(buffer);
				return new Slot(type, pivot, pointer);
			}

			public void write(ByteBuffer buffer, Slot slot) {
				buffer.put((byte) slot.type.ordinal());
				serializer.write(buffer, slot.pivot);
				pointerSerializer.write(buffer, slot.pointer);
			}
		});

		return new Serializer<Page>() {
			public Page read(ByteBuffer buffer) {
				return new Page(slotsSerializer.read(buffer));
			}

			public void write(ByteBuffer buffer, Page page) {
				slotsSerializer.write(buffer, page.slots);
			}
		};
	}

}
