import java.util.HashMap;

import org.feiteira.bigstructure.auxi.BigSDataMap;
import org.feiteira.bigstructure.core.abstracts.BigSRequest
import org.feiteira.bigstructure.core.abstracts.BigSResponse
import org.feiteira.bigstructure.core.abstracts.BigSService
import SquareRequest;

//class SquareService extends BigSService<<SquareRequest, SquareResponse>{
class SquareService extends BigSService{

	public SquareService(BigSDataMap dataMap){
		super(dataMap);
	}

	protected void open(String nodePath, HashMap<String, Object> cache) {
		System.out.println("Opening Square for: " + nodePath);
	}

	public SquareResponse handle(String nodePath, BigSRequest req,
			HashMap<String, Object> cache) {
		System.out.println("Received: " + req.val);
		SquareResponse resp = new SquareResponse();
		resp.val = req.val * req.val
		return resp
	}

	public void close(String nodePath, HashMap<String, Object> cache) {
		System.out.println("Closing: " + nodePath);
	}
}
