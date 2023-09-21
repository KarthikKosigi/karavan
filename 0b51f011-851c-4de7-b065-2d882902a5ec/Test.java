import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("Test")
public class Test implements Processor {

  public void process(Exchange exchange) throws Exception {
      exchange.getIn().setBody("Hello World");
  }
}