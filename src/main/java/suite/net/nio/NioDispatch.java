package suite.net.nio;

import static suite.util.Friends.rethrow;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import suite.cfg.Defaults;
import suite.net.NetUtil;
import suite.object.Object_;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil2.Sink2;
import suite.util.Util;

public class NioDispatch implements Closeable {

	private boolean isRunning = true;
	private Selector selector = Selector.open();
	private ThreadLocal<byte[]> threadBuffer = ThreadLocal.withInitial(() -> new byte[Defaults.bufferSize]);

	public NioDispatch() throws IOException {
	}

	@Override
	public void close() throws IOException {
		stop();
		selector.close();
	}

	public void stop() {
		isRunning = false;
	}

	public class Requester extends Reconnect {
		private Map<Integer, Sink<Bytes>> handlers = new ConcurrentHashMap<>();
		private PacketId packetId = new PacketId();

		public void request(Bytes request, Sink<Bytes> okay) {
			request(request, okay, ex -> {
				sc = null;
				LogUtil.error(ex);
			});
		}

		public void request(Bytes request, Sink<Bytes> okay, Sink<IOException> fail) {
			var id = Util.temp();
			handlers.put(id, okay);
			connect(() -> packetId.write(sc, id, request, v -> getClass(), fail), fail);
		}

		private void connect(Runnable okay, Sink<IOException> fail) {
			connect(f -> {
				packetId.read(sc, (id, bs) -> {
					handlers.remove(id).sink(bs);
					f.run();
				}, fail);
			}, okay, fail);
		}
	}

	public class Responder {
		private PacketId packetId = new PacketId();

		public void listen(int port, Iterate<Bytes> fun, Sink<IOException> fail) {
			Sink<IOException> failRequest = LogUtil::error;

			asyncListen(port, sc -> {
				new Object() {
					public void run() {
						packetId.read(sc, (id, bs) -> packetId.write(sc, id, fun.apply(bs), v -> run(), failRequest), failRequest);
					}
				}.run();
			}, fail);
		}
	}

	public class Reconnect {
		public InetSocketAddress address;
		public SocketChannel sc;

		public void connect(Sink<Runnable> read, Runnable okay, Sink<IOException> fail) {
			if (sc == null)
				asyncConnect(address, sc_ -> {
					sc = sc_;
					new Runnable() {
						public void run() {
							read.sink(this);
						}
					}.run();
					okay.run();
				}, fail);
			else
				okay.run();
		}
	}

	public class PacketId {
		private Packet packet = new Packet();

		public void read(SocketChannel sc, Sink2<Integer, Bytes> okay, Sink<IOException> fail) {
			packet.buffer.read(sc, 4, bs0 -> packet.read(sc, bs1 -> okay.sink2(NetUtil.bytesToInt(bs0), bs1), fail), fail);
		}

		public void write(SocketChannel sc, int id, Bytes bs, Sink<Void> okay, Sink<IOException> fail) {
			packet.buffer.writeAll(sc, NetUtil.intToBytes(id), v -> packet.write(sc, bs, okay, fail), fail);
		}
	}

	public class Packet {
		public Buffer buffer = new Buffer();

		public void read(SocketChannel sc, Sink<Bytes> okay, Sink<IOException> fail) {
			buffer.read(sc, 4, bs0 -> buffer.read(sc, NetUtil.bytesToInt(bs0), okay, fail), fail);
		}

		public void write(SocketChannel sc, Bytes bs, Sink<Void> okay, Sink<IOException> fail) {
			buffer.writeAll(sc, NetUtil.intToBytes(bs.size()), v -> buffer.writeAll(sc, bs, okay, fail), fail);
		}
	}

	public class Buffer {
		private Map<SelectableChannel, BytesBuilder> reads = new WeakHashMap<>();

		public void writeAll(SocketChannel sc, Bytes bytes, Sink<Void> okay, Sink<IOException> fail) {
			new Sink<Bytes>() {
				public void sink(Bytes bytes) {
					if (0 < bytes.size())
						asyncWrite(sc, bytes, this, fail);
					else
						okay.sink(null);
				}
			}.sink(bytes);
		}

		public void readLine(SocketChannel sc, byte delim, Sink<Bytes> okay, Sink<IOException> fail) {
			var bb = getReadBuffer(sc);

			new Sink<Integer>() {
				public void sink(Integer start) {
					var bytes_ = bb.toBytes();

					for (int i = start; i < bytes_.size(); i++)
						if (bytes_.get(i) == delim) {
							okay.sink(bytes_.range(0, i));
							bb.clear();
							bb.append(bytes_.range(i + 1));
							return;
						}

					asyncRead(sc, bytes1 -> {
						var size0 = bb.size();
						bb.append(bytes1);
						this.sink(size0);
					}, fail);
				}
			}.sink(0);
		}

		public void read(SocketChannel sc, int n, Sink<Bytes> okay, Sink<IOException> fail) {
			var bb = getReadBuffer(sc);

			new Sink<Void>() {
				public void sink(Void v) {
					if (n <= bb.size()) {
						var bytes_ = bb.toBytes();
						okay.sink(bytes_.range(0, n));
						bb.clear();
						bb.append(Bytes.of(bytes_.range(n)));
					} else
						asyncRead(sc, bytes1 -> {
							bb.append(bytes1);
							this.sink(null);
						}, fail);
				}
			}.sink(null);
		}

		private BytesBuilder getReadBuffer(SocketChannel sc) {
			return reads.computeIfAbsent(sc, sc_ -> new BytesBuilder());
		}
	}

	public void asyncConnect(InetSocketAddress address, Sink<SocketChannel> okay, Sink<IOException> fail) {
		try {
			var sc = SocketChannel.open();
			sc.configureBlocking(false);
			sc.connect(address);
			reg(sc, SelectionKey.OP_CONNECT, okay, fail);
		} catch (IOException ex) {
			fail.sink(ex);
		}

	}

	public Closeable asyncListen(int port, Sink<SocketChannel> accept) {
		return asyncListen(port, accept, LogUtil::error);
	}

	public Closeable asyncListen(int port, Sink<SocketChannel> accept, Sink<IOException> fail) {
		try {
			var ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ssc.socket().bind(new InetSocketAddress(port));
			reg(ssc, SelectionKey.OP_ACCEPT, accept, fail);
			return () -> Object_.closeQuietly(ssc);
		} catch (IOException ex) {
			fail.sink(ex);
			return null;
		}
	}

	public void asyncRead(SocketChannel sc, Sink<Bytes> sink0, Sink<IOException> fail) {
		Sink<Object> okay1 = object -> {
			if (object instanceof Bytes)
				sink0.sink((Bytes) object);
			else if (object instanceof IOException)
				fail.sink((IOException) object);
			else
				fail.sink(null);
		};

		reg(sc, SelectionKey.OP_READ, okay1, fail);
	}

	public void asyncWrite(SocketChannel sc, Bytes bytes, Sink<Bytes> okay0, Sink<IOException> fail) {
		Sink<Object> okay1 = dummy -> {
			try {
				okay0.sink(bytes.range(sc.write(bytes.toByteBuffer())));
			} catch (IOException ex) {
				fail.sink(ex);
			}
		};

		reg(sc, SelectionKey.OP_WRITE, okay1, fail);
	}

	public void close(SocketChannel sc) {
		try {
			sc.register(selector, 0, null);
			sc.close();
		} catch (IOException ex) {
			LogUtil.error(ex);
		}
	}

	public void run() {
		while (isRunning) {

			// unfortunately Selector.wakeup() does not work on my Linux
			// machines. Thus we specify a time out to allow the selector
			// freed out temporarily; otherwise the register() methods in
			// other threads might block forever.
			rethrow(() -> selector.select(500));

			// this seems to allow other threads to gain access. Not exactly
			// the behavior as documented in NIO, but anyway.
			wakeUpSelector();

			var iter = selector.selectedKeys().iterator();

			while (iter.hasNext()) {
				var key = iter.next();
				iter.remove();

				try {
					processKey(key);
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
			}
		}
	}

	private void processKey(SelectionKey key) throws IOException {
		// logUtil.info("KEY", dumpKey(key));

		var buffer = threadBuffer.get();
		@SuppressWarnings("unchecked")
		var callback = (Sink<Object>) key.attachment();
		var ops = key.readyOps();
		var sc0 = key.channel();
		var sc1 = sc0 instanceof SocketChannel ? (SocketChannel) sc0 : null;

		reg(sc0, 0);

		if ((ops & SelectionKey.OP_ACCEPT) != 0) {
			var sc = ((ServerSocketChannel) sc0).accept().socket().getChannel();
			sc.configureBlocking(false);
			callback.sink(sc);
			reg(sc0, SelectionKey.OP_ACCEPT);
		}

		if ((ops & SelectionKey.OP_CONNECT) != 0) {
			sc1.finishConnect();
			callback.sink(sc1);
		}

		if ((ops & SelectionKey.OP_READ) != 0) {
			try {
				var n = sc1.read(ByteBuffer.wrap(buffer));
				if (0 <= n)
					callback.sink(Bytes.of(buffer, 0, n));
				else {
					callback.sink(null);
					sc1.close();
				}
			} catch (ClosedChannelException | NotYetConnectedException ex) {
				callback.sink(ex);
				sc1.close();
			}
		}

		if ((ops & SelectionKey.OP_WRITE) != 0)
			callback.sink(null);
	}

	private void reg(SelectableChannel sc, int key, Sink<?> attachment, Sink<IOException> fail) {
		try {
			reg(sc, key, attachment);
		} catch (ClosedChannelException ex) {
			fail.sink(ex);
		}
	}

	private void reg(SelectableChannel sc, int key, Sink<?> attachment) throws ClosedChannelException {
		sc.register(selector, key, attachment);
		reg(sc, key);
	}

	private void reg(SelectableChannel sc, int key) {
		sc.keyFor(selector).interestOps(key);
		wakeUpSelector();
	}

	private void wakeUpSelector() {
		// selector.wakeup(); // not working in Windows machines
	}

}