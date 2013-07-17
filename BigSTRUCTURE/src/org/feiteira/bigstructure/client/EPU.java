package org.feiteira.bigstructure.client;

import java.io.IOException;

import org.feiteira.bigstructure.auxi.EPUAddress;
import org.feiteira.bigstructure.core.abstracts.BigSRequest;
import org.feiteira.bigstructure.core.abstracts.BigSResponse;
import org.feiteira.network.SeriClient;


public class EPU {
	@SuppressWarnings("unused")
	private Object ref;
	private SeriClient client;
	private String path;

	public EPU(EPUAddress ref) throws IOException {
		this.ref = ref;
		this.client = new SeriClient(ref.getHostName(), ref.getServerPort());
	}
	 
	public void request(BigSRequest req) throws IOException{
		req.setNodePath(this.path);
		this.client.send(req);
	}
	
	public BigSResponse getResponse() throws IOException{
		return (BigSResponse) this.client.read().getObject();
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
