package com.ax.template.authblueprint.featureflags;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * R20 feature-flags domain compliance — 11 items.
 * <p>
 * Spec: specs/feature-flags-l0.yaml
 * Blueprint: blueprints/feature-flags-manifest.yaml
 * <p>
 * Each test carries:
 * <ul>
 *   <li>{@code @Tag("FEATURE_FLAGS")} — wired to {@code ./gradlew testFeatureFlags}.</li>
 *   <li>{@code @Tag("FF-XYZ-NNN")} — direct spec-id pin for {@code specRefGuard}.</li>
 * </ul>
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        // BillingFlowIT precedent: login() rejects unverified accounts; auto-verify
        // keeps the admin/member token issuance binary so AUTHZ assertions are
        // not contaminated by the verification workflow.
        "auth.signup.auto-verify=true"
    })
@Tag("FEATURE_FLAGS")
class FeatureFlagFlowIT {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String obtainToken(String email, String role) {
        String password = "Test123!@#xy";
        // Register — ignore 201 vs 409 so emails can be reused across tests.
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"email":"%s","password":"%s","role":"%s"}
                """.formatted(email, password, role))
            .post("/api/auth/email/signup");
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {"email":"%s","password":"%s"}
                """.formatted(email, password))
            .post("/api/auth/email/login")
            .then().statusCode(200)
            .extract().path("accessToken");
    }

    private void adminCreate(String adminToken, String name, boolean enabled, String description) {
        String body = description == null
            ? """
                {"name":"%s","enabled":%s}
                """.formatted(name, enabled)
            : """
                {"name":"%s","enabled":%s,"description":"%s"}
                """.formatted(name, enabled, description);
        given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body(body)
            .post("/api/v1/admin/feature-flags")
            .then().statusCode(201);
    }

    // ─── AUTHZ family ────────────────────────────────────────────────────────

    @Test
    @Tag("FF-AUTHZ-001")
    @DisplayName("FF-AUTHZ-001 — public eval endpoint accepts unauthenticated GET; admin endpoint rejects it with 401")
    void ffAuthz001_publicEvalAllowed_adminBlocked() {
        // public eval — no auth header
        given()
            .when().get("/api/v1/feature-flags/anything/active")
            .then().statusCode(200)
            .body("active", equalTo(false));

        // admin endpoint without JWT — Spring Security 401
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"name":"unauth-flag","enabled":true}
                """)
            .when().post("/api/v1/admin/feature-flags")
            .then().statusCode(401);
    }

    @Test
    @Tag("FF-AUTHZ-002")
    @DisplayName("FF-AUTHZ-002 — ROLE_MEMBER on the admin endpoint receives 403")
    void ffAuthz002_memberOnAdminReturns403() {
        String memberToken = obtainToken("ff-member@test.test", "MEMBER");

        given()
            .header("Authorization", "Bearer " + memberToken)
            .contentType(ContentType.JSON)
            .body("""
                {"name":"member-attempt","enabled":true}
                """)
            .when().post("/api/v1/admin/feature-flags")
            .then().statusCode(403);
    }

    // ─── EVAL family ─────────────────────────────────────────────────────────

    @Test
    @Tag("FF-EVAL-001")
    @DisplayName("FF-EVAL-001 — known flag reflects current enabled state on the public eval endpoint")
    void ffEval001_knownFlagReturnsEnabledState() {
        String admin = obtainToken("ff-eval001@test.test", "ADMIN");
        adminCreate(admin, "new-checkout", true, "Test eval001");

        given()
            .when().get("/api/v1/feature-flags/new-checkout/active")
            .then().statusCode(200)
            .body("active", equalTo(true));
    }

    @Test
    @Tag("FF-EVAL-002")
    @DisplayName("FF-EVAL-002 — unknown flag returns active=false (fail-closed)")
    void ffEval002_unknownFlagFailsClosed() {
        given()
            .when().get("/api/v1/feature-flags/nonexistent-flag/active")
            .then().statusCode(200)
            .body("active", equalTo(false));
    }

    @Test
    @Tag("FF-EVAL-003")
    @DisplayName("FF-EVAL-003 — update evicts the cache so the next eval reflects the new value immediately")
    void ffEval003_updateEvictsCache() {
        String admin = obtainToken("ff-eval003@test.test", "ADMIN");
        adminCreate(admin, "cache-test-flag", false, "Test eval003");

        // Prime cache via GET — must be false from the loader.
        given()
            .when().get("/api/v1/feature-flags/cache-test-flag/active")
            .then().statusCode(200).body("active", equalTo(false));

        // Flip enabled=true via PATCH.
        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("""
                {"enabled":true}
                """)
            .when().patch("/api/v1/admin/feature-flags/cache-test-flag")
            .then().statusCode(200).body("enabled", equalTo(true));

        // Cache must be evicted: next GET reflects true within the 30s TTL.
        given()
            .when().get("/api/v1/feature-flags/cache-test-flag/active")
            .then().statusCode(200).body("active", equalTo(true));
    }

    // ─── CRUD family ─────────────────────────────────────────────────────────

    @Test
    @Tag("FF-CRUD-001")
    @DisplayName("FF-CRUD-001 — create returns 201; duplicate name returns 409")
    void ffCrud001_createAndDuplicate() {
        String admin = obtainToken("ff-crud001@test.test", "ADMIN");

        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("""
                {"name":"beta-ui","enabled":false,"description":"Beta UI"}
                """)
            .when().post("/api/v1/admin/feature-flags")
            .then().statusCode(201)
            .body("name",        equalTo("beta-ui"))
            .body("enabled",     equalTo(false))
            .body("description", equalTo("Beta UI"));

        // Duplicate name — 409 + RFC 7807
        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("""
                {"name":"beta-ui","enabled":true}
                """)
            .when().post("/api/v1/admin/feature-flags")
            .then().statusCode(409);
    }

    @Test
    @Tag("FF-CRUD-002")
    @DisplayName("FF-CRUD-002 — paginated list returns the created flags")
    void ffCrud002_listReturnsCreatedFlags() {
        String admin = obtainToken("ff-crud002@test.test", "ADMIN");
        adminCreate(admin, "list-test-a", false, null);
        adminCreate(admin, "list-test-b", true,  null);
        adminCreate(admin, "list-test-c", false, null);

        given()
            .header("Authorization", "Bearer " + admin)
            .when().get("/api/v1/admin/feature-flags?size=100")
            .then().statusCode(200)
            // At least our 3 entries exist (other tests may have written to the
            // shared in-memory schema before this method ran).
            .body("totalElements", greaterThanOrEqualTo(3))
            .body("content.name", org.hamcrest.Matchers.hasItems(
                "list-test-a", "list-test-b", "list-test-c"));
    }

    @Test
    @Tag("FF-CRUD-003")
    @DisplayName("FF-CRUD-003 — PATCH updates enabled and description, returns 200")
    void ffCrud003_patchUpdatesFlag() {
        String admin = obtainToken("ff-crud003@test.test", "ADMIN");
        adminCreate(admin, "patch-flag", false, "Original");

        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("""
                {"enabled":true,"description":"Updated"}
                """)
            .when().patch("/api/v1/admin/feature-flags/patch-flag")
            .then().statusCode(200)
            .body("enabled",     equalTo(true))
            .body("description", equalTo("Updated"));
    }

    @Test
    @Tag("FF-CRUD-004")
    @DisplayName("FF-CRUD-004 — DELETE returns 204 and subsequent eval returns active=false")
    void ffCrud004_deleteAndEvalFailsClosed() {
        String admin = obtainToken("ff-crud004@test.test", "ADMIN");
        adminCreate(admin, "delete-flag", true, null);

        // Confirm enabled=true via public eval first.
        given()
            .when().get("/api/v1/feature-flags/delete-flag/active")
            .then().statusCode(200).body("active", equalTo(true));

        given()
            .header("Authorization", "Bearer " + admin)
            .when().delete("/api/v1/admin/feature-flags/delete-flag")
            .then().statusCode(204);

        // Subsequent eval is fail-closed.
        given()
            .when().get("/api/v1/feature-flags/delete-flag/active")
            .then().statusCode(200).body("active", equalTo(false));
    }

    // ─── VALIDATION family ───────────────────────────────────────────────────

    @Test
    @Tag("FF-VALID-001")
    @DisplayName("FF-VALID-001 — invalid name (uppercase, too short) returns 400 RFC 7807")
    void ffValid001_invalidNameReturns400() {
        String admin = obtainToken("ff-valid001@test.test", "ADMIN");

        // Uppercase rejected
        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("""
                {"name":"UPPER_CASE","enabled":false}
                """)
            .when().post("/api/v1/admin/feature-flags")
            .then().statusCode(400);

        // Too short (1 char, fails {2,63})
        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("""
                {"name":"a","enabled":false}
                """)
            .when().post("/api/v1/admin/feature-flags")
            .then().statusCode(400);
    }

    @Test
    @Tag("FF-VALID-002")
    @DisplayName("FF-VALID-002 — description over 500 chars returns 400")
    void ffValid002_oversizedDescriptionReturns400() {
        String admin = obtainToken("ff-valid002@test.test", "ADMIN");

        String oversize = "x".repeat(501);
        String body = """
            {"name":"ok-flag-501","enabled":false,"description":"%s"}
            """.formatted(oversize);

        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body(body)
            .when().post("/api/v1/admin/feature-flags")
            .then().statusCode(400);
    }
}
