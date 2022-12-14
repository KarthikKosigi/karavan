import org.apache.camel.builder.RouteBuilder;

public class Example extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("timer:tick")
            .setBody()
              .constant("Hello Camel K!")
            .to("log:info");
    }
}