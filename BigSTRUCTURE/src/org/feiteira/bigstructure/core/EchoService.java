package org.feiteira.bigstructure.core;

import java.util.HashMap;

import org.feiteira.bigstructure.auxi.BigSDataMap;
import org.feiteira.bigstructure.core.abstracts.BigSService;

public class EchoService extends BigSService<EchoRequest, EchoResponse> {

	public EchoService(BigSDataMap dataMap) {
		super(dataMap);
	}

	@Override
	protected void open(String nodePath, HashMap<String, Object> cache) {		
		System.out.println("Initiating ECHO service");
	}

	@Override
	protected EchoResponse handle(String nodePath, EchoRequest req,
			HashMap<String, Object> cache) {
		EchoResponse resp = new EchoResponse();
		resp.setValue(req.getMessage() + req.getMessage());
		return resp;

	}

	@Override
	protected void close(String nodePath, HashMap<String, Object> cache) {
		System.out.println("Closing ECHO service");
	}

}
