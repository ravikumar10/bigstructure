package org.feiteira.bigstructure;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilationFailedException;
import org.feiteira.bigstructure.auxi.BigSCoordinator;
import org.feiteira.bigstructure.auxi.BigSDataMap;
import org.feiteira.bigstructure.auxi.CoordinatorException;
import org.feiteira.bigstructure.client.BigSClient;
import org.feiteira.bigstructure.core.EchoRequest;
import org.feiteira.bigstructure.core.EchoService;
import org.feiteira.bigstructure.impl.LocalFilesystemDataMap;
import org.feiteira.bigstructure.impl.ZooKeeperCoordinador;

public class BigStructure {
	// global
	/**
	 * The keyword used to name the Ephemeral nodes that represent the EPUs
	 */
	public static final String EPU_KEY = "epu";

	public static final String PROP_GROOVY_PLUGINS_FOLDER = "GroovyFolder";

	// Main
	/**
	 * Simple enumeration, used in conjunction with {@code Modes.<mode>.name} to
	 * perceive command line entries
	 */
	public static enum Modes {
		Client, Server
	};

	public static Logger log = Logger.getLogger(BigStructure.class);
	public static final String PROPERTIES_FILENAME = "properties.ini";
	public static final String PROP_STRUCTURE_ID = "ID";
	public static final String PROP_EPU_TIMEOUT_MS = "EPUTimeout";

	private static final String GROOVY_REQUEST_SUFFIX = "Request.groovy";
	private static final String GROOVY_RESPONSE_SUFFIX = "Response.groovy";
	private static final String GROOVY_SERVICE_SUFFIX = "Service.groovy";

	private static Properties properties;
	private static BigSCoordinator coordinator = null;
	private static LocalFilesystemDataMap dataMap = null;

	private static GroovyClassLoader classLoader;

	/**
	 * 
	 * @param args
	 *            Can be Client or Server (see {@code Modes})
	 * @throws IOException
	 * @throws CoordinatorException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws CompilationFailedException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
			CoordinatorException, CompilationFailedException,
			IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			InterruptedException {
		if (args.length == 0) {
			printUsageMesage();
			return;
		}

		String mode = args[0];

		// This is necessary because it loads the configuration for either
		// Client or Server
		BigStructure structure = new BigStructure();

		if (mode.equals(Modes.Server.name())) {
			log.info("Starting in server mode");
			BigSServer app = new BigSServer();
			app.addService(EchoRequest.class, EchoService.class);

			addGroovyPlugins(app);
			app.start();
			// waits forever

		} else if (mode.equals(Modes.Client.name())) {
			String mainClientFile = args[1];
			log.info("Starting in client mode");
			classLoader = new GroovyClassLoader();

			@SuppressWarnings("unchecked")
			BigSClient app = (BigSClient) classLoader
					.parseClass(new File(mainClientFile)).getConstructor()
					.newInstance();

			app.setArguments(Arrays.copyOfRange(args, 1, args.length));
			app.Main();
		} else {
			System.err.println("Error, unknown mode: " + mode);
		}

	}

	private static void printUsageMesage() {
		System.out.println("Usage:");
		System.out
				.println("\t./BigStructure.jar <Client|Server> [Groovy Client file [arguments...]]");
		System.out.println("Examples:");
		System.out
				.println("\t./BigStructure.jar Client sampleClient.groovy testInputParameter");
		System.out.println("\t./BigStructure.jar Server");
		System.out.println("Note: Requires file " + PROPERTIES_FILENAME);
	}

	@SuppressWarnings({ "rawtypes", "unused", "unchecked" })
	public static void addGroovyPlugins(BigSServer app)
			throws CompilationFailedException, IOException {
		log.debug("Looking for Groovy plugins");

		String folderPath = (String) properties.get(PROP_GROOVY_PLUGINS_FOLDER);
		if (folderPath == null) {
			log.info("Groovy Plugin Folder not defined.");
			return;
		}
		File folder = new File(folderPath);

		if (!folder.isDirectory()) {
			log.error("Goovy Plugin Folder is not a real folder.");
			System.exit(0);
		}

		classLoader = new GroovyClassLoader();

		File[] listOfFiles = folder.listFiles();

		for (File plugFolder : listOfFiles) {
			if (plugFolder.isDirectory()) {
				String pluginName = plugFolder.getName();
				File[] plugFiles = plugFolder.listFiles();
				log.info("Found plugin: " + pluginName);

				Class reqClass = classLoader.parseClass(new File(plugFolder
						.getAbsolutePath()
						+ "/"
						+ pluginName
						+ GROOVY_REQUEST_SUFFIX));
				Class respClass = classLoader.parseClass(new File(plugFolder
						.getAbsolutePath()
						+ "/"
						+ pluginName
						+ GROOVY_RESPONSE_SUFFIX));
				Class serviClass = classLoader.parseClass(new File(plugFolder
						.getAbsolutePath()
						+ "/"
						+ pluginName
						+ GROOVY_SERVICE_SUFFIX));

				app.addService(reqClass, serviClass);

			}

		}

	}

	/**
	 * The Constructor, starts all common parts required to have either a Client
	 * or Server.
	 * 
	 * @throws IOException
	 */
	public BigStructure() throws IOException {
		properties = new Properties();
		try {
			properties.load(new FileInputStream(
					BigStructure.PROPERTIES_FILENAME));
		} catch (IOException e) {
			log.fatal("Could not load properties file", e);
			System.exit(0);
		}

		// Connectors
		//coordinator = new ZooKeeperCoordinador();
		dataMap = new LocalFilesystemDataMap();
	}

	/**
	 * 
	 * @return Returns the global set of properties from the
	 *         {@code PROPERTIES_FILENAME}.
	 */
	public static Properties getProperties() {
		return properties;
	}

	/**
	 * 
	 * @return Return the global coordinator.
	 */
	public static BigSCoordinator getCoordinator() {
		return new ZooKeeperCoordinador();
	}

	/**
	 * 
	 * @return Returns the currently used dataMap
	 */
	public static BigSDataMap getDataMap() {
		return dataMap;
	}

	public static GroovyClassLoader getClassLoader() {
		return classLoader;
	}

}
