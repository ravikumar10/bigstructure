import org.feiteira.bigstructure.BigSClient
import org.feiteira.bigstructure.core.EchoRequest


class DemoClient extends BigSClient {

	@Override
	public void Main() {
		ensureHost("/core");
		requestEPU("/core");
		
		// waitForEPU
		sleep(1500);
		
		send("/core", new EchoRequest("REPEAT"));
		
		def out =  read("/core");
		
		println $out
		
		
	}

}
