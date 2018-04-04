package suite.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

import suite.concurrent.Condition;
import suite.net.NetUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.Signal;
import suite.util.Fail;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Sink;

public interface NioChannelFactory {

	public static char RESPONSE = 'P';
	public static char REQUEST = 'Q';

	public class PersistentNioChannel extends RequestResponseNioChannel {
		private NioDispatcher<PersistentNioChannel> nio;
		private InetSocketAddress address;
		private boolean isStarted;

		public PersistentNioChannel(NioDispatcher<PersistentNioChannel> nio, InetSocketAddress address) {
			this.nio = nio;
			this.address = address;
		}

		public void start() {
			isStarted = true;
			reconnect();
		}

		public void stop() {
			isStarted = false;
		}

		private void reconnect() {
			if (isStarted && !isConnected())
				try {
					nio.reconnect(this, address);
				} catch (IOException ex) {
					Fail.t(ex);
				}
		}
	}

	public class RequestResponseNioChannel extends PacketedNioChannel {
		public static char RESPONSE = 'P';
		public static char REQUEST = 'Q';
		private boolean isConnected;
		private Condition condition = new Condition(() -> isConnected);

		public void send(char type, int token, Bytes data) {
			Bytes packet = new BytesBuilder() //
					.append((byte) type) //
					.append(NetUtil.intToBytes(token)) //
					.append(data) //
					.toBytes();

			if (!isConnected)
				condition.waitThen(() -> {
				}, () -> true);

			sendPacket(packet);
		}

		public void setConnected(boolean isConnected) {
			condition.thenNotify(() -> this.isConnected = isConnected);
		}

		public boolean isConnected() {
			return isConnected;
		}
	}

	public class PacketedNioChannel extends BufferedNioChannel {
		public final Signal<Bytes> onReceivePacket = Signal.of();

		public void sendPacket(Bytes packet) {
			send(new BytesBuilder() //
					.append(NetUtil.intToBytes(packet.size())) //
					.append(packet) //
					.toBytes());
		}
	}

	public class BufferedNioChannel extends NioChannel {
		private Bytes toSend = Bytes.empty;
		private Iterate<Bytes> sender;

		public void send(Bytes out) {
			toSend = toSend.append(out);
			trySend();
		}

		public void setSender(Iterate<Bytes> sender) {
			this.sender = sender;
			trySend();
		}

		public void trySend() {
			if (sender != null)
				toSend = sender.apply(toSend);
		}
	}

	public class NioChannel {
		public final Signal<Iterate<Bytes>> onConnected = Signal.of();
		public final Signal<Bytes> onReceive = Signal.of();
		public final Signal<Boolean> onTrySend = Signal.of();
	}

	public static <C extends PersistentNioChannel> C persistent( //
			C channel0, //
			RequestResponseMatcher matcher, //
			ThreadPoolExecutor executor, //
			Iterate<Bytes> handler) {
		return requestResponse(channel0, matcher, executor, handler);
	}

	public static <C extends RequestResponseNioChannel> C requestResponse( //
			C channel0, //
			RequestResponseMatcher matcher, //
			ThreadPoolExecutor executor, //
			Iterate<Bytes> handler) {
		C channel = packeted(channel0);
		channel.onConnected.wire(sender -> channel.setConnected(sender != null));
		channel.onReceivePacket.wire(packet -> {
			if (5 <= packet.size()) {
				char type = (char) packet.get(0);
				int token = NetUtil.bytesToInt(packet.range(1, 5));
				Bytes contents = packet.range(5);

				if (type == RESPONSE)
					matcher.onResponseReceived(token, contents);
				else if (type == REQUEST)
					executor.execute(() -> channel.send(RESPONSE, token, handler.apply(contents)));
			}
		});
		return channel;
	}

	public static <C extends PacketedNioChannel> C packeted(C channel0) {
		C channel = buffered(channel0);
		channel.onReceive.wire(new Sink<>() {
			private Bytes received = Bytes.empty;

			public void sink(Bytes message) {
				received = received.append(message);
				var size = received.size();

				if (4 <= size) {
					int end = 4 + NetUtil.bytesToInt(received.range(0, 4));

					if (end <= size) {
						Bytes in = received.range(4, end);
						received = received.range(end);
						channel.onReceivePacket.fire(in);
					}
				}
			}
		});
		return channel;
	}

	public static <C extends BufferedNioChannel> C buffered(C channel) {
		channel.onConnected.wire(channel::setSender);
		channel.onTrySend.wire(channel::trySend);
		return channel;
	}

}
