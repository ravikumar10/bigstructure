package tests.bigstructure;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.feiteira.bigstructure.BigSServer;
import org.feiteira.bigstructure.BigStructure;
import org.feiteira.bigstructure.auxi.CoordinatorException;
import org.feiteira.bigstructure.client.BigSClient;
import org.feiteira.bigstructure.client.EPU;
import org.feiteira.bigstructure.core.EchoRequest;
import org.feiteira.bigstructure.core.EchoResponse;
import org.feiteira.bigstructure.core.EchoService;
import org.feiteira.bigstructure.core.abstracts.BigSResponse;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestEPUdiscard {
	public static Logger log = Logger.getLogger(TestEPUdiscard.class);
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
		this.server.shutdown();
	}

	@Test
	public void testDiscard() throws CoordinatorException,
			InterruptedException, IOException {

		server.setEpuTimeout(2000); // two seconds
		server.addService(EchoRequest.class, EchoService.class);

		server.start();

		this.client.requestEPUBlocking("/echo");

		EPU echo = this.client.epu("/echo");

		echo.request(new EchoRequest("Repeat this!"));
		EchoResponse resp = (EchoResponse) echo.getResponse();
		assertEquals("Repeat this!Repeat this!", resp.getValue());

		Thread.sleep(5000);

		echo.request(new EchoRequest("Repeat this too!"));

		resp = (EchoResponse) echo.getResponse();
		if (resp != null)
			log.warn(resp + " :: " + resp.getValue());

		assertNull(resp);
		log.info(resp);

	}

}
