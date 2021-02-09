package org.project.test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.vavr.CheckedFunction0;
import io.vavr.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
@SpringBootTest(classes = { TestApplication.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class BulkheadVerificationTest {

    @RegisterExtension
    static WireMockExtension wireMockExtension = new WireMockExtension();

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void failOnSendingMultipleConcurrentRequestsThroughBulkhead() throws InterruptedException, ExecutionException {
        final String dummyRequest = "{\"dummyField1\":\"Value of dummy\",\"dummyField2\":\"dummy-value-extended\"}";

        wireMockExtension.stubFor(get("/stub/endpoint")
            .willReturn(aResponse().withStatus(200).withBody("Successful forward").withFixedDelay(5000)));

        final HttpEntity<String> request = new HttpEntity<>(dummyRequest);

        // WHEN
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ZERO)
            .build();

        Bulkhead bulkhead = Bulkhead.of("tester", bulkheadConfig);

        CheckedFunction0<ResponseEntity<String>> decoratedSupplier  = Bulkhead.decorateCheckedSupplier(bulkhead, 
            () -> testRestTemplate.exchange("http://localhost:9050/stub/endpoint", HttpMethod.GET, request, String.class));

        CompletableFuture<ResponseEntity<String>> waiting = Future.of(decoratedSupplier).toCompletableFuture();
        CompletableFuture<ResponseEntity<String>> blocked1 = Future.of(decoratedSupplier).toCompletableFuture();
        CompletableFuture<ResponseEntity<String>> blocked2 = Future.of(decoratedSupplier).toCompletableFuture();
        CompletableFuture<ResponseEntity<String>> blocked3 = Future.of(decoratedSupplier).toCompletableFuture();

        // THEN
        assertThat(waiting.get().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(waiting.get().getBody()).isEqualTo("Successful forward");

        assertThat(blocked1.isCompletedExceptionally()).isTrue();
        blocked1.whenComplete((msg, ex) -> assertThat(ex).isInstanceOf(BulkheadFullException.class));

        assertThat(blocked2.isCompletedExceptionally()).isTrue();
        blocked2.whenComplete((msg, ex) -> assertThat(ex).isInstanceOf(BulkheadFullException.class));

        assertThat(blocked3.isCompletedExceptionally()).isTrue();
        blocked3.whenComplete((msg, ex) -> assertThat(ex).isInstanceOf(BulkheadFullException.class));
    }
}
