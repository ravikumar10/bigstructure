package org.feiteira.bigstructure.client;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.feiteira.bigstructure.BigStructure;
import org.feiteira.bigstructure.auxi.BigSCoordinator;
import org.feiteira.bigstructure.auxi.BigSWatcher;
import org.feiteira.bigstructure.auxi.CoordinatorException;
import org.feiteira.bigstructure.auxi.EPUAddress;
import org.feiteira.bigstructure.core.abstracts.BigSRequest;
import org.feiteira.bigstructure.core.abstracts.BigSResponse;
import org.feiteira.network.SeriClient;

public abstract class BigSClient implements BigSWatcher {

	public static Logger log = Logger.getLogger(BigSClient.class);

	private static Properties properties;
	private static BigSCoordinator coordinator;
	private String id;

	private HashMap<String, EPU> epus;
	private HashMap<String, Object> epuMutexes;

	protected String[] arguments;

	public abstract void Main();

	/**
	 * Creates a BigStructure client instance
	 */
	public BigSClient() {
		// Statics
		BigSClient.properties = BigStructure.getProperties();
		BigSClient.coordinator = BigStructure.getCoordinator();

		// Structures
		epus = new HashMap<String, EPU>();
		epuMutexes = new HashMap<String, Object>();

		// vars
		this.id = properties.getProperty(BigStructure.PROP_STRUCTURE_ID);

	}

	public void setArguments(String[] args) {
		// command line arguments
		if (args == null)
			this.arguments = new String[0];
		else
			this.arguments = args;

	}

	public void requestEPUBlocking(String epuPath, int timeout)
			throws CoordinatorException, InterruptedException {
		Object mut = new Object();

		this.epuMutexes.put(getFullPath(epuPath), mut);

		synchronized (mut) {
			boolean found = requestEPU(epuPath);

			log.debug("Before wait " + mut);
			if (!found)// avoids deadlock
				mut.wait(timeout);
			log.debug("After wait " + mut);
		}
	}

	public void requestEPUBlocking(String epuPath) throws CoordinatorException,
			InterruptedException {
		requestEPUBlocking(epuPath, 0);
	}

	/**
	 * Requests an EPU instance for {@code epuPath}. Path must exist.
	 * 
	 * @param epuPath
	 * @return Returns true if the EPU was already available, otherwise requests
	 *         a new EPU and returns false.
	 * 
	 * @throws CoordinatorException
	 */
	public boolean requestEPU(String epuPath) throws CoordinatorException {
		String epuHostFullPath = getFullPath(epuPath);

		String epuFullPath = epuHostFullPath + "/" + BigStructure.EPU_KEY;
		if (coordinator.exists(epuFullPath)) {
			// it already exists so request it
			try {
				loadEPUFromHost(epuHostFullPath);
				return true;
			} catch (IOException e) {
				log.error("Could not load EPU, making new request", e);
			}
		}
		// creates request node and waits for it
		coordinator.addChildChangeWatcher(epuHostFullPath, this);
		String nx = epuPath.replace('/', '-');
		coordinator.create("/" + id + "/atrium/" + nx, epuHostFullPath);
		return false;
	}

	/**
	 * If it does not exists, creates an host node.
	 * 
	 * @param hostNodeRelativePath
	 * @return false in case the node does not exist and it was not possible to
	 *         create
	 */
	public boolean ensureHost(String hostNodeRelativePath) {
		String fullPath = getFullPath(hostNodeRelativePath);
		if (coordinator.exists(fullPath)) {
			return true;
		}

		try {
			return coordinator.create(fullPath, null);
		} catch (CoordinatorException e) {
			log.error("Error creating host node", e);
			return false;
		}
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
					loadEPUFromHost(path);
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

	private void loadEPUFromHost(String hostPath) throws CoordinatorException,
			IOException {
		EPUAddress ref = (EPUAddress) coordinator.get(hostPath + "/"
				+ BigStructure.EPU_KEY);
		EPU epu = new EPU(ref);
		epu.setPath(hostPath);

		epus.put(hostPath, epu);

		log.debug(hostPath);
		Object waitMutex = epuMutexes.get(hostPath);

		if (waitMutex != null)
			synchronized (waitMutex) {
				epuMutexes.remove(hostPath);
				log.debug("Before notify " + waitMutex);
				waitMutex.notify();
				log.debug("Aftyer notify " + waitMutex);
			}
	}

	/**
	 * Not used, inherithed from {@code BigSWatcher}
	 */
	@Override
	public void dataChanged(String path, Serializable newData) {

	}

	private String getFullPath(String path) {
		return "/" + id + path;

	}

	public EPU epu(String key) {
		return epus.get(getFullPath(key));
	}

}
