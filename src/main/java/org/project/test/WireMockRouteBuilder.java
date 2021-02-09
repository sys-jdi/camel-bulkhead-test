package org.project.test;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class WireMockRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("servlet:/test/endpoint").routeId("test-servlet-endpoint")
            .circuitBreaker()
                .resilience4jConfiguration()
                    .bulkheadEnabled(true)
                    .bulkheadMaxConcurrentCalls(1)
                    .bulkheadMaxWaitDuration(0)
                .end()
                .to("http://localhost:9050/stub/endpoint?bridgeEndpoint=true")
            .endCircuitBreaker();
    }    
}
