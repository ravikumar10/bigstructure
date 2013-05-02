package org.feiteira.bigstructure;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.feiteira.bigstructure.auxi.BigSCoordinator;
import org.feiteira.bigstructure.auxi.BigSWatcher;
import org.feiteira.bigstructure.auxi.CoordinatorException;
import org.feiteira.bigstructure.auxi.EPUReference;
import org.feiteira.bigstructure.core.abstracts.BigSRequest;
import org.feiteira.bigstructure.core.abstracts.BigSResponse;
import org.feiteira.network.SeriClient;

public class BigSClient implements BigSWatcher {

	public static Logger log = Logger.getLogger(BigSClient.class);

	private static Properties properties;
	private static BigSCoordinator coordinator;
	private String id;

	private HashMap<String, EPUConnection> epuConnections;

	/**
	 * Creates a BigStructure client instance
	 */
	public BigSClient() {
		// Statics
		BigSClient.properties = BigStructure.getProperties();
		BigSClient.coordinator = BigStructure.getCoordinator();

		// Structures
		epuConnections = new HashMap<String, EPUConnection>();
		// vars
		this.id = properties.getProperty(BigStructure.PROP_STRUCTURE_ID);
	}	
	
	/**
	 * Requests an EPU instance for {@code epuPath}. Path must exist.
	 * 
	 * @param epuPath
	 * @throws CoordinatorException
	 */
	public void requestEPU(String epuPath) throws CoordinatorException {
		coordinator.addChildChangeWatcher(epuPath, this);
		String nx = epuPath.replace('/', '-');
		coordinator.create("/" + id + "/atrium/" + nx, epuPath);

	}

	/**
	 * Sends data to a specific EPU.
	 * 
	 * @param epuPath
	 * @param req
	 *            Request to be sent
	 * @throws IOException
	 */
	public void send(String epuPath, BigSRequest req) throws IOException {
		if (epuConnections.get(epuPath) != null) {
			req.setNodePath(epuPath);
			epuConnections.get(epuPath).client.send(req);
		} else
			log.error("Unsupported request of type " + req.getClass()
					+ " for  node " + req.getNodePath());
	}

	/**
	 * Reads incoming data from the specified EPU path.
	 * 
	 * @param epuPath
	 * @return
	 * @throws IOException
	 */
	public BigSResponse read(String epuPath) throws IOException {
		EPUConnection epu = epuConnections.get(epuPath);
		if (epu == null)
			return null;
		return (BigSResponse) epu.client.read().getObject();
	}

	/**
	 * Used to monitor a for requested epuPaths and see if there are attached
	 * epu instances.
	 */
	@Override
	public void childrenChanged(String path, List<String> children) {
		log.debug(path);
		for (String child : children) {
			log.info("Child: " + child);
			if (child.equals(BigStructure.EPU_KEY)) {
				try {
					EPUReference ref = (EPUReference) coordinator.get(path
							+ "/" + BigStructure.EPU_KEY);
					EPUConnection cli = new EPUConnection(ref);

					epuConnections.put(path, cli);
				} catch (CoordinatorException e) {
					log.warn("Could not load node: " + path + "/"
							+ BigStructure.EPU_KEY);
				} catch (IOException e) {
					log.error("Could not connect to Ethereal Processing Unit",
							e);
				}
			}
		}
	}

	/**
	 * Not used, inherithed from {@code BigSWatcher}
	 */
	@Override
	public void dataChanged(String path, Serializable newData) {

	}

	private class EPUConnection {
		@SuppressWarnings("unused")
		private Object ref;
		private SeriClient client;

		public EPUConnection(EPUReference ref) throws IOException {
			this.ref = ref;
			this.client = new SeriClient(ref.getHostName(), ref.getServerPort());
		}
	}

}
