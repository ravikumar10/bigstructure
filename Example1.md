# Introduction #

Sample of code using core services.

`DemoClient.groovy:`
```
import org.feiteira.bigstructure.client.BigSClient
import org.feiteira.bigstructure.core.EchoRequest
import org.feiteira.bigstructure.client.EPU
import org.feiteira.bigstructure.core.EchoResponse

class DemoClient extends BigSClient {
	@Override
	public void Main() {

	    requestEPUBlocking("/echo");// ensures that the host node /<id>/echo exists (or takes the current one if available)  
                                
            //takes some arguments
            int count = arguments[1].toInteger()
            String value = arguments[2]
                
            EPU echo = epu("/echo");

            EchoResponse out;
            while(count>0){
                 echo.request(new EchoRequest(value));
                 out = echo.getResponse();            
                 printf "[%d]ECHO: %s\n" , [count,out.getValue()] as Object[]
                 count--;
                 sleep 1000
            }                       
	}
}
```



Command line: `java -jar BigSTRUCTURE-0.5.jar Client DemoClient.groovy 10 Test!`

_Note:_ don't forget to run `java -jar BigSTRUCTURE-0.5.jar Server` on another shell on the side

Output:
```
[10]ECHO: Test!Test!
[9]ECHO: Test!Test!
[8]ECHO: Test!Test!
[7]ECHO: Test!Test!
[6]ECHO: Test!Test!
[5]ECHO: Test!Test!
[4]ECHO: Test!Test!
[3]ECHO: Test!Test!
[2]ECHO: Test!Test!
[1]ECHO: Test!Test!

```