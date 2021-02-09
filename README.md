# camel-bulkhead-test

This project implements a very simple test route configured with a Resilience4j bulkhead with a maxConcurrentCalls set to 1.

Additionally two tests have been implemented, one attempting to use the application route to call a WireMock instance and a second test calling the WireMock instance directly through a custom configured Bulkhead.
Both tests attempt to send a total of 4 concurrent requests given the 5000 ms configured delay for the WireMock to respond.

Run ```mvn verify``` to see the results.