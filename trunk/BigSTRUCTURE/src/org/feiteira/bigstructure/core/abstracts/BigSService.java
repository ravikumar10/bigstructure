package org.feiteira.bigstructure.core.abstracts;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.feiteira.bigstructure.auxi.BigSDataMap;

/**
 * 
 * Services must extend this class.
 * 
 * @author jlfeitei
 * 
 * @param <Request>
 *            The request class, must extend {@link BigSRequest}.
 * @param <Response>
 *            The Response class, must extend @link {@link BigSResponse}.
 */
public abstract class BigSService<Request extends BigSRequest, Response extends BigSResponse> {
	public static final String K_ACTIVITY_TIMESTAMP = "K_ACTIVITY_TIMESTAMP";
	protected ConcurrentHashMap<String, HashMap<String, Object>> motherCache;
	protected BigSDataMap dataMap;

	public BigSService(BigSDataMap dataMap) {
		this.dataMap = dataMap;
		motherCache = new ConcurrentHashMap<String, HashMap<String, Object>>();
	}

	public boolean accepts(String nodePath) {
		return true;
	}

	public void open(String nodePath) {
		if (motherCache.containsKey(nodePath))
			throw new RuntimeException(
					"Error, node service initiated twice (or not closed) : "
							+ this.getClass());

		motherCache.put(nodePath, new HashMap<String, Object>());

		HashMap<String, Object> cache = motherCache.get(nodePath);
		cache.put(K_ACTIVITY_TIMESTAMP, new Long(System.currentTimeMillis()));
		open(nodePath, cache);
	}

	@SuppressWarnings("unchecked")
	public Response handle(String nodePath, BigSRequest req) {
		HashMap<String, Object> cache = motherCache.get(nodePath);
		if (cache == null) {
			open(nodePath);
			cache = motherCache.get(nodePath);
		}
		cache.put(K_ACTIVITY_TIMESTAMP, new Long(System.currentTimeMillis()));
		Response resp = handle(nodePath, (Request) req, cache);
		resp.setNodePath(req.getNodePath());
		return resp;
	}

	public void close(String nodePath) {
		HashMap<String, Object> cacheVal = motherCache.remove(nodePath);
		close(nodePath, cacheVal);
	}

	protected abstract void open(String nodePath, HashMap<String, Object> cache);

	protected abstract Response handle(String nodePath, Request req,
			HashMap<String, Object> cache);

	protected abstract void close(String nodePath, HashMap<String, Object> cache);

	public BigSDataMap getDataMap() {
		return dataMap;
	}

	public void setDataMap(BigSDataMap dataMap) {
		this.dataMap = dataMap;
	}
}
