package com.ax.template.authblueprint.common;

import io.restassured.RestAssured;

/**
 * The registered single writer. This file assigns RestAssured.port on purpose — it is the
 * SUBJECT of the rule, derived from the ServiceLoader registration, not an allowlist entry.
 */
public final class AxPort {
    public void beforeEach(int localServerPort) {
        RestAssured.port = localServerPort;
    }

    public static void overrideForStub(int port) {
        RestAssured.port = port;
    }
}
