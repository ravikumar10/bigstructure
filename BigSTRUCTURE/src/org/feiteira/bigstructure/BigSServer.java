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
import org.feiteira.bigstructure.auxi.EPUReference;
import org.feiteira.bigstructure.core.abstracts.BigSRequest;
import org.feiteira.bigstructure.core.abstracts.BigSService;
import org.feiteira.network.SeriDataPackage;
import org.feiteira.network.SeriEventHandler;
import org.feiteira.network.SeriServer;

public class BigSServer extends Thread implements SeriEventHandler {
	private static final String PROP_THREADS = "Threads";
	private static final String PROP_MAX_EPUS_CREATED_PER_SECOND = "MaxEPUsCreatedPerSecond";

	// Statics
	private static Properties properties = null;
	private static BigSCoordinator coordinator = null;
	private static BigSDataMap dataMap = null;
	public static Logger log = Logger.getLogger(BigSServer.class);
	private static String externalIP;
	private static Object lockIP = new Object();

	private int nthreads;
	private String id = null;
	private SeriServer server;

	private Vector<String> incomingRequests = null;
	int maxEPUsCreatedPerSecond = 1;
	private AtriumWatcherTask atriumWatcher;
	private Timer timer;
	private boolean running;

	private HashMap<Class<? extends BigSRequest>, BigSService<?, ?>> services = null;
	private HashMap<String, HashMap<Class<?>, BigSService<?, ?>>> nodeServices = null;

	public BigSServer() throws CoordinatorException {
		// Statics
		BigSServer.properties = BigStructure.getProperties();
		BigSServer.coordinator = BigStructure.getCoordinator();
		BigSServer.dataMap = BigStructure.getDataMap();

		// vars
		this.id = properties.getProperty(BigStructure.PROP_STRUCTURE_ID);
		this.nthreads = Integer.parseInt(properties.getProperty(PROP_THREADS));

		// structures
		incomingRequests = new Vector<String>();
		maxEPUsCreatedPerSecond = Integer.parseInt(properties
				.getProperty(PROP_MAX_EPUS_CREATED_PER_SECOND));

		services = new HashMap<Class<? extends BigSRequest>, BigSService<?, ?>>();
		nodeServices = new HashMap<String, HashMap<Class<?>, BigSService<?, ?>>>();

		// nodes
		if (coordinator.exists(getRootPath())) {
			log.info("Good, id " + this.id + " already running");
		} else {
			log.info("Initiating new Server with ID:" + this.id);
			coordinator.create(getRootPath());
			coordinator.create(getAtriumPath());
		}

		this.atriumWatcher = new AtriumWatcherTask();
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
		this.timer = new Timer();
		timer.schedule(this.atriumWatcher, 1000, 1000);
		super.start();
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
					processServiceRequest(requestNode);
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
	private void processServiceRequest(String requestNode) {
		String targetNode;
		try {
			targetNode = (String) coordinator.get(requestNode);
		} catch (CoordinatorException e) {
			log.info("Could not get request, maybe some deleted it?");
			return;
		}

		if (nodeServices.get(targetNode) == null)
			nodeServices.put(targetNode,
					new HashMap<Class<?>, BigSService<?, ?>>());

		if (!coordinator.exists(getEPUPath(targetNode)))
			try {
				EPUReference epuRef = new EPUReference();
				epuRef.setHostName(getExternalIP());
				epuRef.setServerPort(this.server.getPort());
				epuRef.setNodePath(targetNode);
				coordinator.createEphemeral(getEPUPath(targetNode), epuRef);
			} catch (CoordinatorException e) {
				log.error("Error, could not create EPU for node: " + targetNode);
				return;
			}

		HashMap<Class<?>, BigSService<?, ?>> nodeServiceList = nodeServices
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
			Serializable resp = (Serializable) service.handle(
					req.getNodePath(), req);
			try {
				SeriServer.reply(pack, resp);
			} catch (IOException e) {
				log.error("Could not reply to client", e);
			}
		}
	}

	/**
	 * Executed upon SeriServer shutdown completed
	 */
	@Override
	public void shutdownCompleted() {
		// TODO Auto-generated method stub

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
	 * This is a Timer class that periodically checks the Atrium for requests
	 */
	public class AtriumWatcherTask extends TimerTask {

		public AtriumWatcherTask() throws CoordinatorException {
			log.info("Adding Atrium watcher @ "
					+ BigSServer.this.getAtriumPath());
			updateIncomingRequests();
		}

		public void updateIncomingRequests() throws CoordinatorException {
			List<String> in_nodes = BigSServer.coordinator
					.getChildren(getAtriumPath());
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
}
