package com.ax.template.authblueprint.sessionmanagement;

import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;

import static io.restassured.RestAssured.given;

/**
 * R33 iter1 dogfood closures — SESS-LIFECYCLE-004 (expiresAt in past)
 * and SESS-LIFECYCLE-005 (max active sessions per user).
 *
 * <p>This test lives in its own class because SESS-LIFECYCLE-005 needs a
 * lowered cap via @TestPropertySource — keeping the override out of
 * {@link SessionComplianceTest} avoids context-cache pollution.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "session-management.max-active-sessions-per-user=2")
@Tag("SESSION")
class SessionDogfoodIter1Test {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        SessionManagementTestSupport.useRandomPort(port);
    }

    @Test
    @Tag("SESS-LIFECYCLE-004")
    void lifecycle_004_pastExpiresAtReturns400() {
        String token = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("life4"), "MEMBER");

        String body = "{\"jti\":\"jti-life4\",\"expiresAt\":\""
                       + Instant.now().minus(Duration.ofMinutes(5)) + "\"}";

        given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/sessions")
        .then()
            .statusCode(400)
            .body("code", Matchers.equalTo("EXPIRES_AT_IN_PAST"));
    }

    @Test
    @Tag("SESS-LIFECYCLE-005")
    void lifecycle_005_maxActiveSessionsAutoRevokesOldest() {
        String token = SessionManagementTestSupport.obtainToken(
            SessionManagementTestSupport.freshEmail("life5"), "MEMBER");

        Instant futurExp = Instant.now().plus(Duration.ofHours(1));
        String first = post(token, "jti-life5-a", futurExp);
        // Tiny sleep so the createdAt ordering is deterministic.
        sleepMs(15);
        String second = post(token, "jti-life5-b", futurExp);
        sleepMs(15);
        String third = post(token, "jti-life5-c", futurExp);

        // With cap=2 and 3 registers, the oldest (first) MUST be REVOKED.
        given().header("Authorization", "Bearer " + token).when().get("/api/sessions/" + first)
            .then().body("status", Matchers.equalTo("REVOKED"));
        given().header("Authorization", "Bearer " + token).when().get("/api/sessions/" + second)
            .then().body("status", Matchers.equalTo("ACTIVE"));
        given().header("Authorization", "Bearer " + token).when().get("/api/sessions/" + third)
            .then().body("status", Matchers.equalTo("ACTIVE"));
    }

    private String post(String token, String jti, Instant expiresAt) {
        String body = "{\"jti\":\"" + jti + "\",\"expiresAt\":\"" + expiresAt + "\"}";
        return given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/sessions")
        .then().statusCode(Matchers.anyOf(Matchers.equalTo(201), Matchers.equalTo(200)))
            .extract().path("id");
    }

    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
