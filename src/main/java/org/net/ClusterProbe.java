package org.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.util.FormatUtil;
import org.util.LogUtil;
import org.util.Util;
import org.util.Util.Setter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Probes existence of other nodes in a cluster, using the un-reliable UDP
 * protocol (due to its small messaging overhead).
 */
public class ClusterProbe extends ThreadedService {

	private final static int BUFFERSIZE = 65536; // UDP packet size
	private final static int CHECKALIVEDURATION = 1500;
	private final static int TIMEOUTDURATION = 5000;

	private Selector selector;
	private DatagramChannel channel = DatagramChannel.open();
	private ByteBuffer buffer = ByteBuffer.allocate(BUFFERSIZE);

	private String me;

	/**
	 * Name/address pairs of all possible peers.
	 */
	private BiMap<String, Address> peers = HashBiMap.create();

	/**
	 * Active nodes with their ages.
	 */
	private Map<String, Long> lastActiveTime = Util.createHashMap();

	/**
	 * Time-stamp to avoid HELO bombing.
	 */
	private Map<String, Long> lastSentTime = Util.createHashMap();

	private Setter<String> onJoined = Util.nullSetter();
	private Setter<String> onLeft = Util.nullSetter();

	private enum Command {
		HELO, FINE, BYEE
	}

	private static class Address {
		private byte ip[];
		private int port;

		private Address(InetSocketAddress isa) {
			ip = isa.getAddress().getAddress();
			port = isa.getPort();
		}

		private InetSocketAddress get() throws UnknownHostException {
			return new InetSocketAddress(InetAddress.getByAddress(ip), port);
		}

		@Override
		public int hashCode() {
			int result = 1;
			result = 31 * result + Arrays.hashCode(ip);
			result = 31 * result + port;
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if (object instanceof Address) {
				Address other = (Address) object;
				return ip[0] == other.ip[0] //
						&& ip[1] == other.ip[1] //
						&& ip[2] == other.ip[2] //
						&& ip[3] == other.ip[3] //
						&& port == other.port;
			} else
				return false;
		}

		@Override
		public String toString() {
			return Arrays.toString(ip) + ":" + port;
		}
	}

	public ClusterProbe(String me, Map<String, InetSocketAddress> peers)
			throws IOException {
		this();
		setMe(me);
		setPeers(peers);
	}

	public ClusterProbe() throws IOException {
		channel.configureBlocking(false);
	}

	@Override
	public synchronized void spawn() {
		lastActiveTime.put(me, System.currentTimeMillis()); // Puts myself in
		broadcast(Command.HELO);
		super.spawn();

	}

	@Override
	public synchronized void unspawn() {
		super.unspawn();
		broadcast(Command.BYEE);
		lastActiveTime.clear();
	}

	public String dumpActivePeers() {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Long> e : lastActiveTime.entrySet()) {
			String dateStr = FormatUtil.dtFmt.format(new Date(e.getValue()));
			sb.append(e.getKey() + " (last-active = " + dateStr + ")\n");
		}
		return sb.toString();
	}

	protected void serve() throws IOException {
		InetSocketAddress address = peers.get(me).get();

		selector = Selector.open();

		final DatagramChannel dc = DatagramChannel.open();
		dc.configureBlocking(false);
		dc.socket().bind(address);
		dc.register(selector, SelectionKey.OP_READ);

		setStarted(true);

		while (running) {
			long current = System.currentTimeMillis();
			keepAlive(current);
			eliminateOutdatedPeers(current);

			// Handle network events
			selector.select(500);

			Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next();
				keyIter.remove();

				try {
					processSelectedKey(current, key);
				} catch (Exception ex) {
					LogUtil.error(getClass(), ex);
				}
			}
		}

		setStarted(false);

		dc.close();
		selector.close();
	}

	private void processSelectedKey(long current, SelectionKey key)
			throws IOException {
		DatagramChannel dc = (DatagramChannel) key.channel();

		if (key.isReadable()) {
			dc.receive(buffer);
			buffer.flip();

			byte bytes[] = new byte[buffer.remaining()];
			buffer.get(bytes);
			buffer.rewind();

			String splitted[] = new String(bytes).split(",");
			Command data = Command.valueOf(splitted[0]);
			String remote = splitted[1];

			// Refreshes member time accordingly
			for (int i = 2; i < splitted.length; i += 2) {
				String node = splitted[i];
				Long oldTime = lastActiveTime.get(node);
				Long newTime = Long.valueOf(splitted[i + 1]);

				if (oldTime == null || oldTime < newTime)
					lastActiveTime.put(node, newTime);
			}

			if (peers.get(remote) != null)
				if (data == Command.HELO || data == Command.FINE) {
					if (lastActiveTime.put(remote, current) == null)
						onJoined.perform(remote);

					// Reply HELO messages
					if (data == Command.HELO) {
						byte replyBytes[] = formMessage(Command.FINE);
						sendMessage(remote, peers.get(remote), replyBytes);
					}
				} else if (data == Command.BYEE
						&& lastActiveTime.remove(remote) != null)
					onLeft.perform(remote);
		}
	}

	private void keepAlive(long current) {
		byte bytes[] = formMessage(Command.HELO);

		for (Entry<String, Address> e : peers.entrySet()) {
			String remote = e.getKey();

			if (!Util.equals(remote, me)) {
				Long lastActive = lastActiveTime.get(remote);
				Long lastSent = lastSentTime.get(remote);

				// Sends to those who are nearly forgotten, i.e.:
				// - The node is not active, or node's active time is expired
				// - The last sent time was long ago (avoid message bombing)
				if (lastActive == null
						|| lastActive + CHECKALIVEDURATION < current)
					if (lastSent == null
							|| lastSent + CHECKALIVEDURATION < current)
						sendMessage(remote, e.getValue(), bytes);
			}
		}
	}

	private void eliminateOutdatedPeers(long current) {
		Set<Entry<String, Long>> entries = lastActiveTime.entrySet();
		Iterator<Entry<String, Long>> peerIter = entries.iterator();

		while (peerIter.hasNext()) {
			Entry<String, Long> e = peerIter.next();
			String node = e.getKey();

			if (!Util.equals(node, me)
					&& current - e.getValue() > TIMEOUTDURATION) {
				peerIter.remove();
				onLeft.perform(node);
			}
		}
	}

	/**
	 * Sends message to all nodes.
	 * 
	 * TODO this is costly and un-scalable.
	 */
	private void broadcast(Command data) {
		byte bytes[] = formMessage(data);

		for (Entry<String, Address> e : peers.entrySet()) {
			String remote = e.getKey();

			if (!Util.equals(remote, me))
				sendMessage(remote, e.getValue(), bytes);
		}
	}

	private void sendMessage(String remote, Address address, byte bytes[]) {
		try {
			channel.send(ByteBuffer.wrap(bytes), address.get());
			lastSentTime.put(remote, System.currentTimeMillis());
		} catch (IOException ex) {
			LogUtil.error(getClass(), ex);
		}
	}

	private byte[] formMessage(Command data) {
		StringBuilder sb = new StringBuilder(data.name() + "," + me);

		for (Entry<String, Long> e : lastActiveTime.entrySet())
			sb.append("," + e.getKey() + "," + e.getValue());

		byte bytes[] = sb.toString().getBytes();
		return bytes;
	}

	public boolean isActive(String node) {
		return lastActiveTime.containsKey(node);
	}

	public Set<String> getActivePeers() {
		return lastActiveTime.keySet();
	}

	public void setMe(String me) {
		this.me = me;
	}

	private void setPeers(Map<String, InetSocketAddress> peers) {
		for (Entry<String, InetSocketAddress> e : peers.entrySet())
			this.peers.put(e.getKey(), new Address(e.getValue()));
	}

	public void setOnJoined(Setter<String> onJoined) {
		this.onJoined = onJoined;
	}

	public void setOnLeft(Setter<String> onLeft) {
		this.onLeft = onLeft;
	}

}
