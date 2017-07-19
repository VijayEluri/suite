package suite.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import suite.Constants;
import suite.net.ThreadService;
import suite.net.nio.NioChannelFactory.NioChannel;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.Rethrow;

public class NioDispatcherImpl<C extends NioChannel> implements NioDispatcher<C> {

	private Source<C> channelSource;
	private Selector selector = Selector.open();
	private ThreadService threadService = new ThreadService(this::serve);

	public NioDispatcherImpl(Source<C> channelSource) throws IOException {
		this.channelSource = channelSource;
	}

	@Override
	public void start() {
		threadService.start();
	}

	@Override
	public void stop() {
		threadService.stop();
	}

	/**
	 * Establishes connection to a host actively.
	 */
	@Override
	public C connect(InetSocketAddress address) throws IOException {
		C cl = channelSource.source();
		reconnect(cl, address);
		return cl;
	}

	/**
	 * Re-establishes connection using specified listener, if closed or dropped.
	 */
	@Override
	public void reconnect(NioChannel channel, InetSocketAddress address) throws IOException {
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.connect(address);
		sc.register(selector, SelectionKey.OP_CONNECT, channel);

		wakeUpSelector();
	}

	/**
	 * Ends connection.
	 */
	@Override
	public void disconnect(NioChannel channel) throws IOException {
		for (SelectionKey key : selector.keys())
			if (key.attachment() == channel)
				key.channel().close();
	}

	/**
	 * Waits for incoming connections.
	 *
	 * @return event for switching off the server.
	 */
	@Override
	public Closeable listen(int port) throws IOException {
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		ssc.register(selector, SelectionKey.OP_ACCEPT);

		wakeUpSelector();
		return () -> Object_.closeQuietly(ssc);
	}

	private void serve() throws IOException {
		try (Closeable started = threadService.started()) {
			while (threadService.isRunning()) {

				// unfortunately Selector.wakeup() does not work on my Linux
				// machines. Thus we specify a time out to allow the selector
				// freed out temporarily; otherwise the register() methods in
				// other threads might block forever.
				selector.select(500);

				// this seems to allow other threads to gain access. Not exactly
				// the behavior as documented in NIO, but anyway.
				selector.wakeup();

				Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					iter.remove();

					try {
						processSelectedKey(key);
					} catch (Exception ex) {
						LogUtil.error(ex);
					}
				}
			}
		}

		selector.close();
	}

	private void processSelectedKey(SelectionKey key) throws IOException {
		// logUtil.info("KEY", dumpKey(key));

		byte[] buffer = new byte[Constants.bufferSize];
		Object attachment = key.attachment();
		SelectableChannel sc0 = key.channel();
		int ops = key.readyOps();

		if ((ops & SelectionKey.OP_ACCEPT) != 0) {
			C channel = channelSource.source();
			SocketChannel sc = ((ServerSocketChannel) sc0).accept().socket().getChannel();
			sc.configureBlocking(false);
			sc.register(selector, SelectionKey.OP_READ, channel);
			channel.onConnected.fire(newSender(sc));
		}

		if ((ops & ~SelectionKey.OP_ACCEPT) != 0)
			synchronized (attachment) {
				@SuppressWarnings("unchecked")
				C channel = (C) attachment;
				SocketChannel sc1 = (SocketChannel) sc0;

				if ((ops & SelectionKey.OP_CONNECT) != 0) {
					sc1.finishConnect();
					key.interestOps(SelectionKey.OP_READ);
					channel.onConnected.fire(newSender(sc1));
				}

				if ((ops & SelectionKey.OP_READ) != 0) {
					int n = sc1.read(ByteBuffer.wrap(buffer));
					if (0 <= n)
						channel.onReceive.fire(Bytes.of(buffer, 0, n));
					else {
						channel.onConnected.fire(null);
						sc1.close();
					}
				}

				if ((ops & SelectionKey.OP_WRITE) != 0)
					channel.onTrySend.fire(Boolean.TRUE);
			}
	}

	private Iterate<Bytes> newSender(SocketChannel sc) {
		return in -> {

			// try to send immediately. If cannot sent all, wait for the
			// writable event (and send again at that moment).
			byte[] bytes = in.toArray();
			int sent = Rethrow.ex(() -> sc.write(ByteBuffer.wrap(bytes)));
			Bytes out = in.range(sent);
			int ops = SelectionKey.OP_READ | (!out.isEmpty() ? SelectionKey.OP_WRITE : 0);
			SelectionKey key = sc.keyFor(selector);

			if (key != null && key.interestOps() != ops)
				key.interestOps(ops);

			wakeUpSelector();
			return out;
		};
	}

	private void wakeUpSelector() {
		// selector.wakeup(); // not working in my Linux machines
	}

}
