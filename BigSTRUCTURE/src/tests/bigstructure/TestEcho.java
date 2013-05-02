package tests.bigstructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.feiteira.bigstructure.BigSClient;
import org.feiteira.bigstructure.BigSServer;
import org.feiteira.bigstructure.BigStructure;
import org.feiteira.bigstructure.auxi.CoordinatorException;
import org.feiteira.bigstructure.core.EchoRequest;
import org.feiteira.bigstructure.core.EchoResponse;
import org.feiteira.bigstructure.core.EchoService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestEcho {
	public static Logger log = Logger.getLogger(TestEcho.class);

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
		this.server = new BigSServer();

		this.client = new BigSClient();

	}

	@After
	public void tearDown() throws Exception {
	}

	 @Test
	public void testEcho() throws CoordinatorException, InterruptedException,
			IOException {
		server.addService(EchoRequest.class, EchoService.class);

		server.start();

		this.client.requestEPU("/default/echo");

		Thread.sleep(3000);

		this.client.send("/default/echo", new EchoRequest("TEST! "));

		Thread.sleep(1500);

		Object o = this.client.read("/default/echo");
		log.debug("Received object of type: " + o.getClass());

		EchoResponse resp = (EchoResponse) o;

		log.debug("Received message: " + resp.getValue());
		log.debug("\tfrom: " + resp.getNodePath());

		assertEquals("TEST! TEST! ", resp.getValue());
		assertEquals("/default/echo", resp.getNodePath());
	}

	@Test
	public void testReadWrongNode() throws CoordinatorException,
			InterruptedException, IOException {
		server.addService(EchoRequest.class, EchoService.class);

		server.start();

		this.client.requestEPU("/default/echo");

		Thread.sleep(3000);

		this.client.send("/default/echo", new EchoRequest("TEST! "));

		Thread.sleep(1500);

		Object o = this.client.read("/default/does-not-exist");
		if (o != null) {
			log.debug("Received object of type: " + o.getClass());

			EchoResponse resp = (EchoResponse) o;

			log.debug("Received message: " + resp.getValue());
			log.debug("\tfrom: " + resp.getNodePath());
			fail();
		}
	}

}
