package org.project.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
@SpringBootTest(classes = { TestApplication.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class WireMockRouteTest {

    @RegisterExtension
    static WireMockExtension wireMockExtension = new WireMockExtension();
    
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void failOnSendingMultipleConcurrentRequestsThroughBulkhead() throws InterruptedException, ExecutionException {
        final String dummyRequest = "{\"dummyField1\":\"Value of dummy\",\"dummyField2\":\"dummy-value-extended\"}";

        wireMockExtension.stubFor(post("/stub/endpoint")
            .willReturn(aResponse().withStatus(200).withBody("Successful forward").withFixedDelay(5000)));

        final HttpEntity<String> request = new HttpEntity<>(dummyRequest);

        // WHEN
        Supplier<ResponseEntity<String>> supplier = () -> testRestTemplate.exchange("/test/endpoint", HttpMethod.POST, request, String.class);
        
        CompletableFuture<ResponseEntity<String>> waiting = CompletableFuture.supplyAsync(supplier);
        CompletableFuture<ResponseEntity<String>> blocked1 = CompletableFuture.supplyAsync(supplier);
        CompletableFuture<ResponseEntity<String>> blocked2 = CompletableFuture.supplyAsync(supplier);
        CompletableFuture<ResponseEntity<String>> blocked3 = CompletableFuture.supplyAsync(supplier);

        // THEN
        assertThat(waiting.get().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(waiting.get().getBody()).isEqualTo("Successful forward");

        assertThat(blocked1.get().getStatusCode()).isNotEqualTo(HttpStatus.OK);
        assertThat(blocked1.get().getBody()).isNotEqualTo("Successful forward");

        assertThat(blocked2.get().getStatusCode()).isNotEqualTo(HttpStatus.OK);
        assertThat(blocked2.get().getBody()).isNotEqualTo("Successful forward");

        assertThat(blocked3.get().getStatusCode()).isNotEqualTo(HttpStatus.OK);
        assertThat(blocked3.get().getBody()).isNotEqualTo("Successful forward");
    }
}