package suite.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import suite.net.channels.Channel;
import suite.net.channels.Channel.Sender;
import suite.primitive.Bytes;
import suite.util.FunUtil.Source;
import suite.util.os.LogUtil;

public class NioDispatcherImpl<C extends Channel> extends ThreadedService implements NioDispatcher<C> {

	private static int bufferSize = 4096;

	private Source<C> channelSource;
	private Selector selector = Selector.open();

	public NioDispatcherImpl(Source<C> channelSource) throws IOException {
		this.channelSource = channelSource;
	}

	/**
	 * Establishes connection to other host actively.
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
	public void reconnect(Channel channel, InetSocketAddress address) throws IOException {
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
	public void disconnect(C channel) throws IOException {
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

		return () -> {
			try {
				ssc.close();
			} catch (IOException ex) {
				LogUtil.error(ex);
			}
		};
	}

	@Override
	protected void serve() throws IOException {
		try (Closeable started = started()) {
			while (running) {

				// Unfortunately Selector.wakeup() does not work on my Linux
				// machines. Thus we specify a time out to allow the selector
				// freed out temporarily; otherwise the register() methods in
				// other threads might block forever.
				selector.select(500);

				// This seems to allow other threads to gain access. Not exactly
				// the behavior as documented in NIO, but anyway.
				selector.wakeup();

				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> iter = selectedKeys.iterator();

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
		// LogUtil.info("KEY", dumpKey(key));

		byte buffer[] = new byte[bufferSize];
		SelectableChannel sc0 = key.channel();
		Object attachment = key.attachment();

		if (key.isAcceptable()) {
			Channel channel = channelSource.source();
			ServerSocketChannel ssc = (ServerSocketChannel) sc0;
			Socket socket = ssc.accept().socket();
			SocketChannel sc = socket.getChannel();

			sc.configureBlocking(false);
			sc.register(selector, SelectionKey.OP_READ, channel);

			channel.onConnected(createSender(sc));
		} else
			synchronized (attachment) {
				Channel channel = (Channel) attachment;
				SocketChannel sc1 = (SocketChannel) sc0;

				if (key.isConnectable()) {
					sc1.finishConnect();

					key.interestOps(SelectionKey.OP_READ);
					channel.onConnected(createSender(sc1));
				} else if (key.isReadable()) {
					int n = sc1.read(ByteBuffer.wrap(buffer));

					if (n >= 0)
						channel.onReceive(Bytes.of(buffer, 0, n));
					else {
						channel.onClose();
						sc1.close();
					}
				} else if (key.isWritable())
					channel.onTrySend();
			}
	}

	private Sender createSender(SocketChannel sc) {
		return in -> {

			// Try to send immediately. If cannot sent all, wait for the
			// writable event (and send again at that moment).
			byte bytes[] = in.toBytes();
			int sent = sc.write(ByteBuffer.wrap(bytes));

			Bytes out = in.subbytes(sent);

			int ops;
			if (!out.isEmpty())
				ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
			else
				ops = SelectionKey.OP_READ;

			SelectionKey key = sc.keyFor(selector);
			if (key != null && key.interestOps() != ops)
				key.interestOps(ops);

			wakeUpSelector();
			return out;
		};
	}

	private void wakeUpSelector() {
		// selector.wakeup(); // Not working in my Linux machines
	}

}
