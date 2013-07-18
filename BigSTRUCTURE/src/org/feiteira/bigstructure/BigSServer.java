package org.feiteira.bigstructure;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.feiteira.bigstructure.auxi.BigSCoordinator;
import org.feiteira.bigstructure.auxi.BigSDataMap;
import org.feiteira.bigstructure.auxi.CoordinatorException;
import org.feiteira.bigstructure.auxi.EPUAddress;
import org.feiteira.bigstructure.core.abstracts.BigSRequest;
import org.feiteira.bigstructure.core.abstracts.BigSService;
import org.feiteira.network.SeriDataPackage;
import org.feiteira.network.SeriEventHandler;
import org.feiteira.network.SeriServer;

public class BigSServer extends Thread implements SeriEventHandler {
	private static final String PROP_THREADS = "Threads";
	private static final String PROP_MAX_EPUS_CREATED_PER_SECOND = "MaxEPUsCreatedPerSecond";
	private static final int DEFAULT_EPU_TIMEOUT_MS = 30000;

	// Statics
	private static Properties properties = null;

	private static BigSDataMap dataMap = null;
	public static Logger log = Logger.getLogger(BigSServer.class);
	private static String externalIP;
	private static Object lockIP = new Object();

	// each server has a (connection to the) coordinator
	private BigSCoordinator coordinator = null;

	private int epuTimeout = DEFAULT_EPU_TIMEOUT_MS;
	private int nthreads;
	private String id = null;
	private SeriServer server;

	private Vector<String> incomingRequests = null;
	int maxEPUsCreatedPerSecond = 1;
	private AtriumWatcherTask atriumWatcher;
	private Timer timerAtrium;
	private boolean running;

	/**
	 * This is a list of services available for EPUs to process by this
	 * BigSServer instance.
	 */
	private HashMap<Class<? extends BigSRequest>, BigSService<?, ?>> services = null;

	/**
	 * Key is the node name
	 * 
	 * 
	 * Object is the request class and BigSService is the Service itself
	 */
	private HashMap<String, HashMap<Class<?>, BigSService<?, ?>>> epuServices = null;
	private EPUDisposerTask epuGarbageCollector;
	private Timer timerEPUGarbageCollector;

	public BigSServer() throws CoordinatorException {
		// Statics
		BigSServer.properties = BigStructure.getProperties();
		coordinator = BigStructure.getCoordinator();
		BigSServer.dataMap = BigStructure.getDataMap();

		// vars
		this.id = properties.getProperty(BigStructure.PROP_STRUCTURE_ID);
		this.nthreads = Integer.parseInt(properties.getProperty(PROP_THREADS));
		String epuTimeoutString = properties
				.getProperty(BigStructure.PROP_EPU_TIMEOUT_MS);
		if (epuTimeoutString != null) {
			this.epuTimeout = Integer.parseInt(epuTimeoutString);
		}

		// structures
		incomingRequests = new Vector<String>();
		maxEPUsCreatedPerSecond = Integer.parseInt(properties
				.getProperty(PROP_MAX_EPUS_CREATED_PER_SECOND));

		services = new HashMap<Class<? extends BigSRequest>, BigSService<?, ?>>();
		epuServices = new HashMap<String, HashMap<Class<?>, BigSService<?, ?>>>();

		// nodes
		if (coordinator.exists(getRootPath())) {
			log.info("Good, id " + this.id + " already running");
		} else {
			log.info("Initiating new Server with ID:" + this.id);
			coordinator.create(getRootPath());
			coordinator.create(getAtriumPath());
		}

		this.atriumWatcher = new AtriumWatcherTask();
		this.epuGarbageCollector = new EPUDisposerTask();
	}

	/**
	 * Creates a instance of type {@code serviceClass} that will be executed
	 * when instances of {@code requestClass} arrive to an accepting node.
	 * 
	 * @param requestClass
	 * @param serviceClass
	 */
	@SuppressWarnings("rawtypes")
	public void addService(Class<? extends BigSRequest> requestClass,
			Class<? extends BigSService> serviceClass) {
		Constructor<? extends BigSService> serviceConstructor = null;
		try {
			serviceConstructor = serviceClass.getConstructor(BigSDataMap.class);
		} catch (SecurityException e) {
			log.error(
					"Could not load service " + serviceClass.getCanonicalName()
							+ "Security Exception", e);
		} catch (NoSuchMethodException e) {
			log.fatal(
					"Unexpected error, did you define the constructor with BigSDataMap?",
					e);
			System.exit(0);
		}

		BigSService<?, ?> currService = null;
		try {
			currService = serviceConstructor.newInstance(BigSServer.dataMap);
		} catch (Exception e) {
			log.error("Unknown error, could not create instance for service "
					+ serviceClass.getCanonicalName(), e);
		}

		services.put(requestClass, currService);
		log.info("Added service " + currService.getClass() + " for "
				+ requestClass.getCanonicalName());
	}

	/**
	 * Starts the server.
	 */
	public void start() {

		Thread.currentThread().setContextClassLoader(
				BigStructure.getClassLoader());

		try {
			server = new SeriServer(0, nthreads);
		} catch (IOException e) {
			log.fatal("Could not start server", e);
			System.exit(0);
		}
		server.setEventListner(this);
		this.running = true;

		this.timerAtrium = new Timer();
		timerAtrium.schedule(this.atriumWatcher, 1000, 1000);

		this.timerEPUGarbageCollector = new Timer();
		timerEPUGarbageCollector.schedule(this.epuGarbageCollector,
				this.epuTimeout, this.epuTimeout);

		super.start();
	}

	public void shutdown() {
		this.timerAtrium.cancel();
		this.timerEPUGarbageCollector.cancel();

		synchronized (incomingRequests) {
			this.running = false;
			incomingRequests.clear();
			incomingRequests.notify();
		}
		try {
			this.coordinator.disconnect();
		} catch (CoordinatorException e) {
			log.warn("Exception raised while disconnecting from coordinator", e);
		}
		this.server.shutdown();

	}

	/**
	 * Looks at {@code incommingRequests} and processes new incoming requests.
	 * 
	 * {@code incomingRequests} are updated via the {@code AtriumWatcherTask}.
	 */
	@Override
	public void run() {

		while (this.running) {
			synchronized (incomingRequests) {
				if (incomingRequests.size() == 0)
					try {
						incomingRequests.wait();
					} catch (InterruptedException e) {
						log.debug("Wait interrupted");
					}

				for (String requestNode : incomingRequests) {
					log.debug("Processing request: " + requestNode);
					processEPURequest(requestNode);
				}
				incomingRequests.clear();
			}
		}
	}

	/**
	 * Checks all criteria and if possible creates an EPU for the
	 * {@code requestNode}.
	 * 
	 * @param requestNode
	 *            path to the requested node
	 */
	private void processEPURequest(String requestNode) {
		String targetNode;
		log.debug(requestNode);
		try {
			targetNode = (String) coordinator.get(requestNode);
		} catch (CoordinatorException e) {
			log.info("Could not get request, maybe some deleted it?");
			return;
		}

		if (coordinator.exists(getEPUPath(targetNode))) {
			log.info("EPU already available, deleting request and exiting.");
			coordinator.delete(requestNode);
			return;
		}

		/**
		 * If there is no list of available services for this node, then creates
		 * the list
		 */
		if (epuServices.get(targetNode) == null)
			epuServices.put(targetNode,
					new HashMap<Class<?>, BigSService<?, ?>>());

		try {
			EPUAddress epuRef = new EPUAddress();
			epuRef.setHostName(getExternalIP());
			epuRef.setServerPort(this.server.getPort());
			epuRef.setNodePath(targetNode);
			coordinator.createEphemeral(getEPUPath(targetNode), epuRef);
		} catch (CoordinatorException e) {
			log.error("Error, could not create EPU for node: " + targetNode);
			return;
		}

		HashMap<Class<?>, BigSService<?, ?>> nodeServiceList = epuServices
				.get(targetNode);

		for (Class<?> reqClass : services.keySet()) {
			if (services.get(reqClass).accepts(targetNode)) {
				nodeServiceList.put(reqClass, services.get(reqClass));
			}
		}
		coordinator.delete(requestNode);
	}

	/**
	 * Since we use the SeriServer, DataPacks arriving to the server are handled
	 * via this overridden method of the SeriEventHandler interface.
	 */
	@Override
	public void messageArrived(SeriDataPackage pack) {
		BigSService<?, ?> service = services.get(pack.getObject().getClass());

		if (service == null) {
			log.warn("Service not found for request type: "
					+ pack.getObject().getClass());
		} else {
			log.debug("Service found: " + service);
			BigSRequest req = (BigSRequest) pack.getObject();


			if (this.epuServices.containsKey(req.getNodePath())) {
				Serializable resp = (Serializable) service.handle(
						req.getNodePath(), req);
				try {
					SeriServer.reply(pack, resp);
				} catch (IOException e) {
					log.error("Could not reply to client", e);
				}
			} else {
				try {
					SeriServer.reply(pack, null);
				} catch (IOException e) {
					log.warn("Error replying to client. Ignoring because it was an invalid node:"
							+ e);
				}
			}
		}
	}

	/**
	 * Executed upon SeriServer shutdown completed
	 */
	@Override
	public void shutdownCompleted() {

	}

	public String getAtriumPath() {
		return "/" + this.id + "/atrium";
	}

	public String getRootPath() {
		return "/" + this.id;
	}

	public String getEPUPath(String node) {
		return node + "/" + BigStructure.EPU_KEY;
	}

	public static String getExternalIP() {
		if (externalIP != null)
			return externalIP;
		else {
			synchronized (lockIP) {
				// in case it was queued
				if (externalIP != null)
					return externalIP;

				Socket s;
				try {
					s = new Socket("www.hexapixel.org", 80);
					externalIP = s.getLocalAddress().getHostAddress();
					s.close();
				} catch (Exception e) {
					log.warn("Cannot connect home, network down?");
					externalIP = "127.0.0.1";
				}
			}
		}
		log.warn("IP Set to " + externalIP);
		return externalIP;
	}

	/**
	 * This is a TimerTask that disposes of EPUs that have been inactive for
	 * {@code timeout} miliseconds.
	 * 
	 * @author jlfeitei
	 * 
	 */
	public class EPUDisposerTask extends TimerTask {

		public EPUDisposerTask() {
			log.info("Adding EPU dispose watcher @ "
					+ BigSServer.this.getName());

		}

		@Override
		public void run() {
			Vector<String> nodesToDelete = new Vector<String>();
			log.info(epuServices);
			for (String epuHostNodeName : epuServices.keySet()) {

				boolean deleteEPU = false;
				HashMap<Class<?>, BigSService<?, ?>> serviceList = epuServices
						.get(epuHostNodeName);
				for (BigSService<?, ?> servis : serviceList.values()) {
					log.info(epuHostNodeName + " :: " + servis.toString()
							+ " :: " + servis.getTimeSinceLastCall());
					if (servis.getTimeSinceLastCall() > epuTimeout) {
						deleteEPU = true;
						break;
					}
				}

				if (deleteEPU) {
					nodesToDelete.add(epuHostNodeName);
				}
			}

			for (String epuHostNodeName : nodesToDelete) {
				log.debug("Removing EPU: " + getEPUPath(epuHostNodeName));
				if (coordinator.delete(getEPUPath(epuHostNodeName))) {
					epuServices.remove(epuHostNodeName);
				}
			}

		}
	}

	/**
	 * This is a Timer class that periodically checks the Atrium for requests
	 */
	public class AtriumWatcherTask extends TimerTask {

		public AtriumWatcherTask() throws CoordinatorException {
			log.info("Adding Atrium watcher @ "
					+ BigSServer.this.getAtriumPath());
			updateIncomingRequests();
		}

		public void updateIncomingRequests() throws CoordinatorException {
			List<String> in_nodes = coordinator.getChildren(getAtriumPath());
			updateIncomingRequests(in_nodes);

		}

		public void updateIncomingRequests(List<String> in_nodes) {
			// I ignore the path because this Is the AtriumWatcher
			log.debug("Updating requests  #" + in_nodes.size());
			synchronized (incomingRequests) {
				int prevsize = incomingRequests.size();
				while (in_nodes.size() > 0
						&& incomingRequests.size() < BigSServer.this.maxEPUsCreatedPerSecond) {
					incomingRequests.add(getAtriumPath() + "/"
							+ in_nodes.remove(0));
				}

				if (prevsize != incomingRequests.size())
					incomingRequests.notify();
			}
		}

		@Override
		public void run() {
			try {
				updateIncomingRequests();
			} catch (CoordinatorException e) {
				log.error(
						"Coordinator exception while updating incoming requests",
						e);
			}
		}
	}

	public int getEpuTimeout() {
		return epuTimeout;
	}

	public void setEpuTimeout(int epuTimeout) {
		this.epuTimeout = epuTimeout;
	}
}
