package com.ax.template.authblueprint.sessionmanagement;

import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("SESSION")
class SessionComplianceTest {

    @LocalServerPort int port;

    // ─── LIFECYCLE family ────────────────────────────────────────────────────

    @Test
    @Tag("SESS-LIFECYCLE-001")
    void lifecycle_001_registerIsIdempotent() {
        String token = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("life1"), "MEMBER");

        String body = "{\"jti\":\"jti-life1\",\"deviceLabel\":\"iPhone\",\"expiresAt\":\""
                       + Instant.now().plus(Duration.ofHours(1)) + "\"}";

        UUID firstId = UUID.fromString(given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/sessions")
        .then().statusCode(201).body("status", Matchers.equalTo("ACTIVE"))
            .extract().path("id"));

        UUID secondId = UUID.fromString(given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/sessions")
        .then().statusCode(200)
            .extract().path("id"));

        org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);
    }

    @Test
    @Tag("SESS-LIFECYCLE-003")
    void lifecycle_003_logoutFlipsStatusAndPreservesRow() {
        String token = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("life3"), "MEMBER");

        UUID id = registerSession(token, "jti-life3", Instant.now().plus(Duration.ofHours(1)));

        given()
            .header("Authorization", "Bearer " + token)
        .when().delete("/api/sessions/" + id)
        .then().statusCode(204);

        // Row still present, status flipped to REVOKED.
        given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/sessions/" + id)
        .then().statusCode(200)
            .body("status", Matchers.equalTo("REVOKED"))
            .body("revokedAt", Matchers.notNullValue());
    }

    // ─── REVOCATION family ───────────────────────────────────────────────────

    @Test
    @Tag("SESS-REVOKE-001")
    void revoke_001_doubleRevokeIsIdempotent() {
        String token = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("rev1"), "MEMBER");
        UUID id = registerSession(token, "jti-rev1", Instant.now().plus(Duration.ofHours(1)));

        given().header("Authorization", "Bearer " + token).when().delete("/api/sessions/" + id).then().statusCode(204);
        Object firstRevokedAt = given().header("Authorization", "Bearer " + token)
            .when().get("/api/sessions/" + id).then().statusCode(200).extract().path("revokedAt");

        given().header("Authorization", "Bearer " + token).when().delete("/api/sessions/" + id).then().statusCode(204);
        Object secondRevokedAt = given().header("Authorization", "Bearer " + token)
            .when().get("/api/sessions/" + id).then().statusCode(200).extract().path("revokedAt");

        org.assertj.core.api.Assertions.assertThat(secondRevokedAt).isEqualTo(firstRevokedAt);
    }

    @Test
    @Tag("SESS-REVOKE-002")
    void revoke_002_revokeOthersKeepsOneAndRevokesRest() {
        String token = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("rev2"), "MEMBER");

        UUID id1 = registerSession(token, "jti-rev2-a", Instant.now().plus(Duration.ofHours(1)));
        UUID id2 = registerSession(token, "jti-rev2-b", Instant.now().plus(Duration.ofHours(1)));
        UUID id3 = registerSession(token, "jti-rev2-c", Instant.now().plus(Duration.ofHours(1)));

        given()
            .header("Authorization", "Bearer " + token)
        .when().post("/api/sessions/revoke-others?keep=" + id1)
        .then().statusCode(200)
            .body("revoked", Matchers.equalTo(2))
            .body("kept", Matchers.equalTo(1));

        // Sanity: id1 still ACTIVE, id2 + id3 REVOKED.
        given().header("Authorization", "Bearer " + token).when().get("/api/sessions/" + id1)
            .then().body("status", Matchers.equalTo("ACTIVE"));
        given().header("Authorization", "Bearer " + token).when().get("/api/sessions/" + id2)
            .then().body("status", Matchers.equalTo("REVOKED"));
        given().header("Authorization", "Bearer " + token).when().get("/api/sessions/" + id3)
            .then().body("status", Matchers.equalTo("REVOKED"));
    }

    // SESS-REVOKE-003 covered by SessionRevocationCheckTest.

    // ─── INTROSPECTION family ────────────────────────────────────────────────

    @Test
    @Tag("SESS-INTROSPECT-001")
    void introspect_001_listShowsActiveAndRevokedNewestFirst() {
        String token = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("intr1"), "MEMBER");

        UUID id1 = registerSession(token, "jti-intr1-a", Instant.now().plus(Duration.ofHours(1)));
        // Sleep a touch so createdAt orderings differ.
        sleepMs(15);
        UUID id2 = registerSession(token, "jti-intr1-b", Instant.now().plus(Duration.ofHours(1)));

        // Revoke the older one — it must still appear in the list.
        given().header("Authorization", "Bearer " + token).when().delete("/api/sessions/" + id1).then().statusCode(204);

        given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/sessions")
        .then()
            .statusCode(200)
            .body("totalElements", Matchers.greaterThanOrEqualTo(2))
            .body("items[0].id", Matchers.equalTo(id2.toString()))   // newest first
            .body("items[1].id", Matchers.equalTo(id1.toString()))
            .body("items[1].status", Matchers.equalTo("REVOKED"));
    }

    @Test
    @Tag("SESS-INTROSPECT-002")
    void introspect_002_rawIpAndUserAgentAreNeverReturned() {
        String token = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("intr2"), "MEMBER");

        String body = "{\"jti\":\"jti-intr2\",\"deviceLabel\":\"laptop\","
                       + "\"ipAddress\":\"192.168.1.42\","
                       + "\"userAgent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120\","
                       + "\"expiresAt\":\"" + Instant.now().plus(Duration.ofHours(1)) + "\"}";

        UUID id = UUID.fromString(given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/sessions")
        .then().statusCode(201)
            .body("ipAddressMasked", Matchers.equalTo("192.168.1.xxx"))
            .body("userAgentSummary", Matchers.equalTo("Chrome on Windows"))
            .body("$", Matchers.not(Matchers.hasKey("ipAddress")))
            .body("$", Matchers.not(Matchers.hasKey("userAgent")))
            .extract().path("id"));

        given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/sessions/" + id)
        .then().statusCode(200)
            .body("$", Matchers.not(Matchers.hasKey("ipAddress")))
            .body("$", Matchers.not(Matchers.hasKey("userAgent")));
    }

    @Test
    @Tag("SESS-INTROSPECT-003")
    void introspect_003_heartbeatUpdatesLastSeenAt() {
        String token = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("intr3"), "MEMBER");
        UUID id = registerSession(token, "jti-intr3", Instant.now().plus(Duration.ofHours(1)));

        // lastSeenAt is null right after register.
        given().header("Authorization", "Bearer " + token).when().get("/api/sessions/" + id)
            .then().body("lastSeenAt", Matchers.nullValue());

        given().header("Authorization", "Bearer " + token)
        .when().post("/api/sessions/" + id + "/heartbeat")
        .then().statusCode(204);

        given().header("Authorization", "Bearer " + token).when().get("/api/sessions/" + id)
            .then().body("lastSeenAt", Matchers.notNullValue());
    }

    // ─── AUTHZ family ────────────────────────────────────────────────────────

    @Test
    @Tag("SESS-AUTHZ-001")
    void authz_001_unauthenticatedReturns401() {
        given().contentType(ContentType.JSON).body("{}").when().post("/api/sessions").then().statusCode(401);
        given().when().get("/api/sessions").then().statusCode(401);
        given().when().get("/api/sessions/" + UUID.randomUUID()).then().statusCode(401);
        given().when().delete("/api/sessions/" + UUID.randomUUID()).then().statusCode(401);
        given().when().post("/api/sessions/revoke-others?keep=" + UUID.randomUUID()).then().statusCode(401);
        given().when().post("/api/sessions/" + UUID.randomUUID() + "/heartbeat").then().statusCode(401);
    }

    @Test
    @Tag("SESS-AUTHZ-002")
    void authz_002_crossUserReturns404() {
        String tokenA = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("authz2-a"), "MEMBER");
        String tokenB = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("authz2-b"), "MEMBER");

        UUID idA = registerSession(tokenA, "jti-authz2-a", Instant.now().plus(Duration.ofHours(1)));

        given().header("Authorization", "Bearer " + tokenB)
        .when().get("/api/sessions/" + idA)
        .then().statusCode(404);

        given().header("Authorization", "Bearer " + tokenB)
        .when().delete("/api/sessions/" + idA)
        .then().statusCode(404);
    }

    @Test
    @Tag("SESS-AUTHZ-003")
    void authz_003_adminCanForceRevokeAnyUserSession() {
        String victim = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("authz3-v"), "MEMBER");
        String memberAttacker = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("authz3-att"), "MEMBER");
        String admin = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("authz3-adm"), "ADMIN");

        UUID id = registerSession(victim, "jti-authz3-v", Instant.now().plus(Duration.ofHours(1)));

        // Non-admin → 403 (Spring Security catch-all matcher /api/admin/** requires ROLE_ADMIN).
        given().header("Authorization", "Bearer " + memberAttacker)
        .when().delete("/api/admin/sessions/" + id)
        .then().statusCode(403);

        // Admin → 204; victim's session is now REVOKED.
        given().header("Authorization", "Bearer " + admin)
        .when().delete("/api/admin/sessions/" + id)
        .then().statusCode(204);

        // Victim still sees the row (audit trail preserved) but it's REVOKED.
        given().header("Authorization", "Bearer " + victim)
        .when().get("/api/sessions/" + id)
        .then().statusCode(200).body("status", Matchers.equalTo("REVOKED"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID registerSession(String token, String jti, Instant expiresAt) {
        String body = "{\"jti\":\"" + jti + "\",\"expiresAt\":\"" + expiresAt + "\"}";
        return UUID.fromString(given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/sessions")
        .then().statusCode(Matchers.anyOf(Matchers.equalTo(201), Matchers.equalTo(200)))
            .extract().path("id"));
    }

    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
