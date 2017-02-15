package suite.net.cluster.impl;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import suite.net.NetUtil;
import suite.net.cluster.Cluster;
import suite.net.cluster.ClusterProbe;
import suite.net.nio.NioChannelFactory;
import suite.net.nio.NioChannelFactory.PersistentNioChannel;
import suite.net.nio.NioDispatcher;
import suite.net.nio.NioDispatcherImpl;
import suite.net.nio.RequestResponseMatcher;
import suite.primitive.Bytes;
import suite.streamlet.Reactive;
import suite.util.FunUtil.Fun;
import suite.util.Util;

public class ClusterImpl implements Cluster {

	private String me;
	private Map<String, InetSocketAddress> peers;
	private NioDispatcher<PersistentNioChannel> nio;
	private ClusterProbe probe;

	private RequestResponseMatcher matcher = new RequestResponseMatcher();
	private ThreadPoolExecutor executor;

	private Closeable unlisten;

	/**
	 * Established channels connecting to peers.
	 */
	private Map<String, PersistentNioChannel> channels = new HashMap<>();

	private Reactive<String> onJoined;
	private Reactive<String> onLeft;
	private Map<Class<?>, Fun<?, ?>> onReceive = new HashMap<>();

	public ClusterImpl(String me, Map<String, InetSocketAddress> peers) throws IOException {
		this.me = me;
		this.peers = peers;
		this.nio = new NioDispatcherImpl<>(() -> NioChannelFactory.persistent( //
				new PersistentNioChannel(nio, peers.get(me)), //
				matcher, //
				executor, //
				this::respondToRequest));
		probe = new ClusterProbeImpl(me, peers);
	}

	@Override
	public void start() throws IOException {
		executor = Util.newExecutor();
		unlisten = nio.listen(peers.get(me).getPort());
		nio.start();

		onJoined = probe.getOnJoined();

		onLeft = probe.getOnLeft().map(node -> {
			PersistentNioChannel channel = channels.get(node);
			if (channel != null)
				channel.stop();
			return node;
		});

		probe.start();
	}

	@Override
	public void stop() {
		for (PersistentNioChannel channel : channels.values())
			channel.stop();

		probe.stop();
		nio.stop();
		Util.closeQuietly(unlisten);
		executor.shutdown();
	}

	@Override
	public Object requestForResponse(String peer, Object request) {
		if (probe.isActive(peer)) {
			Bytes req = NetUtil.serialize(request);
			Bytes resp = matcher.requestForResponse(getChannel(peer), req);
			return NetUtil.deserialize(resp);
		} else
			throw new RuntimeException("Peer " + peer + " is not active");
	}

	private PersistentNioChannel getChannel(String peer) {
		PersistentNioChannel channel = channels.get(peer);

		if (channel == null || !channel.isConnected())
			try {
				if (channel != null)
					nio.disconnect(channel);

				channel = nio.connect(peers.get(peer));
				channels.put(peer, channel);
			} catch (IOException ex) {
				throw new ClusterException(ex);
			}

		return channel;
	}

	private Bytes respondToRequest(Bytes req) {
		Object request = NetUtil.deserialize(req);
		@SuppressWarnings("unchecked")
		Fun<Object, Object> handler = (Fun<Object, Object>) onReceive.get(request.getClass());
		return NetUtil.serialize(handler.apply(request));
	}

	@Override
	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive) {
		this.onReceive.put(clazz, onReceive);
	}

	@Override
	public Set<String> getActivePeers() {
		return probe.getActivePeers();
	}

	@Override
	public Reactive<String> getOnJoined() {
		return onJoined;
	}

	@Override
	public Reactive<String> getOnLeft() {
		return onLeft;
	}

	@Override
	public String getMe() {
		return me;
	}

}
