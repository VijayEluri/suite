package suite.net.cluster;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import suite.util.Util;

public class ClusterProbeTest {

	@Test
	public void test() throws IOException {
		int nNodes = 3;
		InetAddress localHost = InetAddress.getLocalHost();

		Map<String, InetSocketAddress> peers = new HashMap<>();
		for (int i = 0; i < nNodes; i++)
			peers.put("NODE" + i, new InetSocketAddress(localHost, 3000 + i));

		Map<String, ClusterProbe> probes = new HashMap<>();
		for (String name : peers.keySet()) {
			ClusterProbe probe = new ClusterProbe(name, peers);
			probes.put(name, probe);
			probe.start();
		}

		Util.sleepQuietly(10 * 1000);

		System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");
		dumpActivePeers(probes);
		assertActiveNodesSize(nNodes, probes);

		for (ClusterProbe probe : probes.values())
			probe.stop();

		Util.sleepQuietly(5 * 1000);

		System.out.println("=== CLUSTER STOPPED (" + LocalDateTime.now() + ") ===\n");
		dumpActivePeers(probes);
		assertActiveNodesSize(0, probes);
	}

	private void dumpActivePeers(Map<String, ClusterProbe> probes) {
		for (Entry<String, ClusterProbe> e : probes.entrySet()) {
			System.out.println("HOST " + e.getKey() + " -");
			System.out.println(e.getValue().dumpActivePeers());
		}
	}

	private void assertActiveNodesSize(int nNodes, Map<String, ClusterProbe> probes) {
		for (ClusterProbe probe : probes.values())
			assertEquals(nNodes, probe.getActivePeers().size());
	}

}
