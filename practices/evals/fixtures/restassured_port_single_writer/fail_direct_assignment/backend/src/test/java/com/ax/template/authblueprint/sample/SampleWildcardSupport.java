package com.ax.template.authblueprint.sample;

import static io.restassured.RestAssured.*;

/**
 * FAIL fixture, shape (B) again — the WILDCARD spelling of the same evasion.
 *
 * <p>A single-static-import of `port` is the obvious form and {@code SampleTestSupport} carries
 * it. This file carries the form that is one keystroke away from the tree's dominant line,
 * {@code import static io.restassured.RestAssured.given;}: change `given` to `*` and the field
 * arrives under its simple name with nothing else on the line looking unusual. The write below
 * spells no `RestAssured.port` token anywhere, so detector (A) cannot see it — only refusing the
 * import line can.
 */
public final class SampleWildcardSupport {

    private SampleWildcardSupport() {}

    public static void aimSomewhereElse(int somewhereElse) {
        port = somewhereElse;
        given().when().get("/api/health").then().statusCode(200);
    }
}
