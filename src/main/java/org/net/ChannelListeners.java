package org.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

import org.net.Bytes.BytesBuilder;
import org.util.LogUtil;
import org.util.Util;
import org.util.Util.Fun;
import org.util.Util.FunEx;

public class ChannelListeners {

	/**
	 * Channel that will reconnect if failed for any reasons.
	 */
	public abstract static class PersistableChannel<CL extends ChannelListener>
			extends RequestResponseChannel {
		private NioDispatcher<CL> dispatcher;
		private InetSocketAddress address;
		boolean started;

		public PersistableChannel(NioDispatcher<CL> dispatcher //
				, RequestResponseMatcher matcher //
				, ThreadPoolExecutor executor //
				, InetSocketAddress address //
				, Fun<Bytes, Bytes> handler) {
			super(matcher, executor, handler);
			this.dispatcher = dispatcher;
			this.address = address;
		}

		public synchronized void start() {
			started = true;
			reconnect();
		}

		public synchronized void stop() {
			started = false;
		}

		@Override
		public void onClose() {
			super.onClose();
			reconnect();
		}

		private void reconnect() {
			if (started && !isConnected())
				try {
					dispatcher.reconnect(this, address);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
		}
	}

	/**
	 * Channel that exhibits client/server message exchange.
	 */
	public static class RequestResponseChannel extends PacketChannel {
		private static final char RESPONSE = 'P';
		static final char REQUEST = 'Q';

		private RequestResponseMatcher matcher;
		private ThreadPoolExecutor executor;
		private boolean connected;
		private Fun<Bytes, Bytes> handler;

		public RequestResponseChannel(RequestResponseMatcher matcher //
				, ThreadPoolExecutor executor //
				, Fun<Bytes, Bytes> handler) {
			this.matcher = matcher;
			this.executor = executor;
			this.handler = handler;
		}

		@Override
		public void onConnected() {
			setConnected(true);
		}

		@Override
		public void onClose() {
			setConnected(false);
		}

		@Override
		public void onReceivePacket(Bytes packet) {
			if (packet.size() >= 5) {
				char type = (char) packet.byteAt(0);
				final int token = NetUtil.intValue(packet.subbytes(1, 5));
				final Bytes contents = packet.subbytes(5);

				if (type == RESPONSE)
					matcher.onRespondReceived(token, contents);
				else if (type == REQUEST)
					executor.execute(new Runnable() {
						public void run() {
							send(RESPONSE, token, handler.apply(contents));
						}
					});
			}
		}

		public void send(char type, int token, Bytes data) {
			if (!connected)
				synchronized (this) {
					while (!connected)
						Util.wait(this);
				}

			sendPacket(new BytesBuilder() //
					.append((byte) type) //
					.append(NetUtil.bytesValue(token)) //
					.append(data) //
					.toBytes());
		}

		public boolean isConnected() {
			return connected;
		}

		private synchronized void setConnected(boolean isConnected) {
			connected = isConnected;
			notify();
		}
	}

	/**
	 * Channel that transfer data in the unit of packets.
	 */
	public abstract static class PacketChannel extends BufferedChannel {
		private Bytes received = Bytes.emptyBytes;

		public abstract void onReceivePacket(Bytes packet);

		@Override
		public final void onReceive(Bytes message) {
			received = received.append(message);
			Bytes packet = receivePacket();

			if (packet != null)
				onReceivePacket(packet);
		}

		protected void sendPacket(Bytes packet) {
			send(new BytesBuilder() //
					.append(NetUtil.bytesValue(packet.size())) //
					.append(packet) //
					.toBytes());
		}

		protected Bytes receivePacket() {
			Bytes packet = null;

			if (received.size() >= 4) {
				int end = 4 + NetUtil.intValue(received.subbytes(0, 4));

				if (received.size() >= end) {
					packet = received.subbytes(4, end);
					received = received.subbytes(end);
				}
			}

			return packet;
		}
	}

	/**
	 * Channel with a send buffer.
	 */
	public abstract static class BufferedChannel implements ChannelListener {
		private FunEx<Bytes, Bytes, IOException> sender;
		private Bytes toSend = Bytes.emptyBytes;

		@Override
		public void onConnected() {
		}

		@Override
		public void onClose() {
		}

		@Override
		public void trySend() throws IOException {
			toSend = sender.apply(toSend);
		}

		@Override
		public void setTrySendDelegate(FunEx<Bytes, Bytes, IOException> sender) {
			this.sender = sender;
		}

		public void send(Bytes message) {
			toSend = toSend.append(message);

			try {
				trySend();
			} catch (IOException ex) {
				LogUtil.error(getClass(), ex);
			}
		}
	}

	public interface ChannelListener {
		public void onConnected();

		public void onClose() throws IOException;

		public void onReceive(Bytes message);

		public void trySend() throws IOException;

		/**
		 * The event would be invoked when the channel wants to send anything,
		 * i.e. getMessageToSend() would return data.
		 */
		public void setTrySendDelegate(FunEx<Bytes, Bytes, IOException> sender);
	}

}
