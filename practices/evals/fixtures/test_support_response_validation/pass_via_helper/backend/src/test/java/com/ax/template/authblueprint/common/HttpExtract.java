package com.ax.template.authblueprint.common;

import io.restassured.response.Response;

/**
 * Stub of the real helper, present in this fixture to prove the SUBJECT/OBJECT distinction: this
 * file calls {@code response.path(...)} — the very shape the guard forbids elsewhere — and is not
 * reported, because it is not named {@code *TestSupport.java} and so is outside the target set by
 * construction. That is not an allowlist entry; the guard has none.
 */
public final class HttpExtract {

    private HttpExtract() {}

    public static <T> T path(Response response, String jsonPath, String context) {
        return pathAt(response, -1, jsonPath, context);
    }

    @SuppressWarnings("unchecked")
    public static <T> T pathAt(Response response, int expectedStatus, String jsonPath, String context) {
        int status = response.getStatusCode();
        boolean ok = (expectedStatus == -1) ? (status >= 200 && status <= 299) : (status == expectedStatus);
        if (!ok) {
            throw new AssertionError(context + ": unexpected status " + status);
        }
        return (T) response.path(jsonPath);
    }
}
