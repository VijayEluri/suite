package suite.net.channels;

import suite.net.NetUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;

/**
 * Channel that transfer data in the unit of packets.
 */
public abstract class PacketChannel extends BufferedChannel {

	private Bytes received = Bytes.emptyBytes;

	public abstract void onReceivePacket(Bytes packet);

	protected void sendPacket(Bytes packet) {
		send(new BytesBuilder() //
				.append(NetUtil.bytesValue(packet.size())) //
				.append(packet) //
				.toBytes());
	}

	@Override
	public final void onReceive(Bytes message) {
		received = received.append(message);

		if (received.size() >= 4) {
			int end = 4 + NetUtil.intValue(received.subbytes(0, 4));

			if (received.size() >= end) {
				Bytes packet = received.subbytes(4, end);
				received = received.subbytes(end);
				onReceivePacket(packet);
			}
		}
	}

}
