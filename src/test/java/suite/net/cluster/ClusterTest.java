package suite.net.cluster;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Test;

import suite.adt.Pair;
import suite.net.cluster.impl.ClusterImpl;
import suite.util.To;
import suite.util.Util;

public class ClusterTest {

	@Test
	public void testCluster() throws IOException {
		InetAddress localHost = InetAddress.getLocalHost();

		Map<String, InetSocketAddress> peers = To.map(null //
				, Pair.of("NODE0", new InetSocketAddress(localHost, 3000)) //
				, Pair.of("NODE1", new InetSocketAddress(localHost, 3001)));

		Cluster cluster0 = new ClusterImpl("NODE0", peers);
		Cluster cluster1 = new ClusterImpl("NODE1", peers);

		cluster1.setOnReceive(Integer.class, i -> i + 1);

		cluster0.start();
		cluster1.start();

		Util.sleepQuietly(2 * 1000);

		System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");

		assertEquals(12346, cluster0.requestForResponse("NODE1", 12345));

		cluster0.stop();
		cluster1.stop();

		System.out.println("=== CLUSTER STOPPED (" + LocalDateTime.now() + ") ===\n");
	}

}
