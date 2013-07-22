package suite.net.channels;

import java.util.concurrent.ThreadPoolExecutor;

import suite.net.Bytes;
import suite.net.Bytes.BytesBuilder;
import suite.net.NetUtil;
import suite.net.RequestResponseMatcher;
import suite.util.FunUtil.Fun;
import suite.util.Util;

/**
 * Channel that exhibits client/server message exchange.
 */
public class RequestResponseChannel extends PacketChannel {

	public static final char RESPONSE = 'P';
	public static final char REQUEST = 'Q';

	private RequestResponseMatcher matcher;
	private ThreadPoolExecutor executor;
	private boolean isConnected;
	private Fun<Bytes, Bytes> handler;

	public RequestResponseChannel(RequestResponseMatcher matcher, ThreadPoolExecutor executor, Fun<Bytes, Bytes> handler) {
		this.matcher = matcher;
		this.executor = executor;
		this.handler = handler;
	}

	public void send(char type, int token, Bytes data) {
		if (!isConnected)
			synchronized (this) {
				while (!isConnected)
					Util.wait(this);
			}

		sendPacket(new BytesBuilder() //
				.append((byte) type) //
				.append(NetUtil.bytesValue(token)) //
				.append(data) //
				.toBytes());
	}

	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public void onConnected(Sender sender) {
		setConnected(true);
		super.onConnected(sender);
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

	private synchronized void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
		notify();
	}

}
