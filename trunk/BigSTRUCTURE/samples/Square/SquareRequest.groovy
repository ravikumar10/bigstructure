import org.feiteira.bigstructure.core.abstracts.BigSRequest
import org.feiteira.bigstructure.core.abstracts.BigSResponse
import org.feiteira.bigstructure.core.abstracts.BigSService


class SquareRequest extends BigSRequest{
	private static final long serialVersionUID = 1L;
	
	float val = 0.0
	
	public SquareRequest(float val){
		this.val = val
	}
}