package org.net;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.net.ClusterMapUtil.GetQuery;
import org.net.ClusterMapUtil.PutQuery;
import org.util.FunUtil.Fun;
import org.util.FunUtil.Sink;
import org.util.Util;

public class ClusterMap<K, V> {

	private Cluster cluster;
	private List<String> peers = new ArrayList<>();
	private Map<K, V> localMap = new HashMap<>();

	public ClusterMap(Cluster cluster) {
		this.cluster = cluster;

		synchronized (cluster) { // Avoid missed cluster events
			peers.addAll(cluster.getActivePeers());
			Collections.sort(peers);

			cluster.addOnJoined(onJoined);
			cluster.addOnLeft(onLeft);
			cluster.setOnReceive(GetQuery.Request.class, onGet);
			cluster.setOnReceive(PutQuery.Request.class, onPut);
		}
	}

	private final Sink<String> onJoined = new Sink<String>() {
		public void apply(String peer) {
			synchronized (ClusterMap.this) {
				peers.add(peer);
				Collections.sort(peers);
			}
		}
	};

	private final Sink<String> onLeft = new Sink<String>() {
		public void apply(String peer) {
			synchronized (ClusterMap.this) {
				peers.remove(peer);
				Collections.sort(peers);
			}
		}
	};

	private final Fun<GetQuery.Request, GetQuery.Response> onGet = new Fun<GetQuery.Request, GetQuery.Response>() {
		public GetQuery.Response apply(GetQuery.Request request) {
			GetQuery.Response response = new GetQuery.Response();
			response.value = localMap.get(request.key);
			return response;
		}
	};

	private final Fun<PutQuery.Request, PutQuery.Response> onPut = new Fun<PutQuery.Request, PutQuery.Response>() {
		public PutQuery.Response apply(PutQuery.Request request) {
			@SuppressWarnings("unchecked")
			K key = (K) request.key;
			@SuppressWarnings("unchecked")
			V value = (V) request.value;
			PutQuery.Response response = new PutQuery.Response();
			response.value = localMap.put(key, value);
			return response;
		}
	};

	public V get(K key) {
		return getFromPeer(getPeerByHash(key), key);
	}

	public V set(K key, V value) {
		return putToPeer(getPeerByHash(key), key, value);
	}

	private V getFromPeer(String peer, K key) {
		GetQuery.Request request = new GetQuery.Request();
		request.key = key;
		Serializable object = requestForResponse(peer, request);
		GetQuery.Response response = (GetQuery.Response) object;

		@SuppressWarnings("unchecked")
		V value = (V) response.value;
		return value;
	}

	private V putToPeer(String peer, K key, V value) {
		PutQuery.Request request = new PutQuery.Request();
		request.key = key;
		request.value = value;
		Object object = requestForResponse(peer, request);
		PutQuery.Response response = (PutQuery.Response) object;

		@SuppressWarnings("unchecked")
		V value1 = (V) response.value;
		return value1;
	}

	private Serializable requestForResponse(String peer, Serializable request) {
		return (Serializable) cluster.requestForResponse(peer, request);
	}

	private String getPeerByHash(K key) {
		int hash = Util.hashCode(key);
		return peers.get(hash % peers.size());
	}

}
