package org.project.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class WireMockExtension extends WireMockServer implements BeforeAllCallback, AfterAllCallback {

    private static int DEFAULT_SERVER_PORT = 9050;

    public WireMockExtension() {
        super(DEFAULT_SERVER_PORT);
    }

    public WireMockExtension(final Options options) {
        super(options);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        start();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        stop();
        resetAll();
    }
}
