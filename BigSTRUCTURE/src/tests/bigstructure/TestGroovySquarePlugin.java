package tests.bigstructure;

import static org.junit.Assert.*;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.feiteira.bigstructure.BigSClient;
import org.feiteira.bigstructure.BigSServer;
import org.feiteira.bigstructure.BigStructure;
import org.feiteira.bigstructure.auxi.CoordinatorException;
import org.feiteira.bigstructure.core.EchoRequest;
import org.feiteira.bigstructure.core.EchoService;
import org.feiteira.bigstructure.core.abstracts.BigSRequest;
import org.feiteira.bigstructure.core.abstracts.BigSResponse;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGroovySquarePlugin {

	public static Logger log = Logger.getLogger(TestGroovySquarePlugin.class);

	@SuppressWarnings("unused")
	private BigStructure structure;

	private BigSServer server;

	private BigSClient client;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		log.setLevel(Level.DEBUG);
		BigSServer.log.setLevel(Level.DEBUG);

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		this.structure = new BigStructure();
		log.info("Starting in server mode");
		BigStructure.log.setLevel(Level.DEBUG);
		BigSClient.log.setLevel(Level.DEBUG);

		this.server = new BigSServer();
		BigStructure.addGroovyPlugins(this.server);

		this.client = new BigSClient() {
			@Override
			public void Main() {
			}
		};

	}

	@After
	public void tearDown() throws Exception {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test() throws CoordinatorException, InterruptedException,
			IOException, IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		server.addService(EchoRequest.class, EchoService.class);

		server.start();

		this.client.requestEPU("/default/square");

		Thread.sleep(5000);

		// create instance
		String fileName = "samples/Square/SquareRequest.groovy";
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class clazz = gcl.parseClass(new File(fileName));

		BigSRequest instance = (BigSRequest) clazz.getConstructor(float.class)
				.newInstance(5.0f);

		this.client.send("/default/square", instance);

		BigSResponse obj = this.client.read("/default/square");

		log.info("Read object from class: " + obj.getClass());
		log.info("Obj: " + obj);
		assertEquals("SquareResponse", obj.getClass().getCanonicalName());
		assertEquals("25.0", obj.toString());
	}

}
