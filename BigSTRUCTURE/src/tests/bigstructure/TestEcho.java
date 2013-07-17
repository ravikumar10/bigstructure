package tests.bigstructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.feiteira.bigstructure.BigSServer;
import org.feiteira.bigstructure.BigStructure;
import org.feiteira.bigstructure.auxi.CoordinatorException;
import org.feiteira.bigstructure.client.BigSClient;
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

		this.client = new BigSClient() {

			@Override
			public void Main() {
				// TODO Auto-generated method stub

			}
		};

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEcho() throws CoordinatorException, InterruptedException,
			IOException {
		server.addService(EchoRequest.class, EchoService.class);

		server.start();

		this.client.requestEPU("/echo");

		Thread.sleep(3000);

		this.client.epu("/echo").request(new EchoRequest("TEST! "));

		Thread.sleep(1500);

		Object o = this.client.epu("/echo").getResponse();
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

		this.client.requestEPU("/echo");

		Thread.sleep(3000);

		this.client.epu("/echo").request(new EchoRequest("TEST! "));

		Thread.sleep(1500);

		Object o = this.client.epu("/does-not-exist");
		if (o != null) {
			log.debug("Received object of type: " + o.getClass());

			EchoResponse resp = (EchoResponse) o;

			log.debug("Received message: " + resp.getValue());
			log.debug("\tfrom: " + resp.getNodePath());
			fail();
		}
	}

}
