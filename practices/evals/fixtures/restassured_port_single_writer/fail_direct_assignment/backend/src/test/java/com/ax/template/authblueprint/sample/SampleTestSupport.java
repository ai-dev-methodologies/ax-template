package com.ax.template.authblueprint.sample;

import static io.restassured.RestAssured.port;

/**
 * FAIL fixture, shape (B) — the simple-name evasion. Java permits assignment through a
 * single-static-import name, so after this import `port = 8080;` writes the process-global
 * without the token `RestAssured.port` appearing anywhere. The import line is the one place
 * this is declarable, so that is where it is refused.
 */
public final class SampleTestSupport {

    private SampleTestSupport() {}

    public static void aimSomewhereElse(int somewhereElse) {
        port = somewhereElse;
    }
}
