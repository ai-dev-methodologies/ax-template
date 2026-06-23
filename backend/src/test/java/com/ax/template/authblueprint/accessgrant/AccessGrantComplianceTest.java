package com.ax.template.authblueprint.accessgrant;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * time-bounded-access-grant-l0 compliance — verified against the live access-grant reference
 * workload. The invariant: access is allowed ONLY while the injected Clock's now ∈ [validFrom,
 * validUntil) AND status == ACTIVE (a recomputed predicate, never a stored flag); a grant outside
 * its window or revoked FAILS CLOSED (403 GRANT_NOT_YET_VALID / GRANT_EXPIRED / GRANT_REVOKED);
 * grants are append-only + revocable (who/when recorded, no delete); a multi-credential
 * eligibility gate passes ONLY when EVERY required class is held and non-expired at now.
 * Spec: specs/time-bounded-access-grant-l0.yaml (NIST SP 800-162 ABAC + SP 800-53 AC-2/AC-3).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ACCESSGRANT")
class AccessGrantComplianceTest {

    @LocalServerPort int port;
    @Autowired Clock clock;
    String member;

    @BeforeEach
    void setup() {
        AccessGrantTestSupport.useRandomPort(port);
        member = AccessGrantTestSupport.obtainToken(AccessGrantTestSupport.freshEmail("ag-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private Instant now() { return Instant.now(clock); }

    private String createGrant(String subject, String resource, String relation,
                               Instant validFrom, Instant validUntil) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectId\":\"" + subject + "\",\"resourceRef\":\"" + resource + "\","
                + "\"relation\":\"" + relation + "\",\"validFrom\":\"" + validFrom + "\","
                + "\"validUntil\":\"" + validUntil + "\"}")
        .when().post("/api/access-grant/grants").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> check(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/access-grant/grants/" + id + "/check").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> revoke(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/access-grant/grants/" + id + "/revoke").thenReturn().then().extract();
    }

    private void issueCredential(String subject, String credentialClass,
                                 Instant validFrom, Instant validUntil) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectId\":\"" + subject + "\",\"credentialClass\":\"" + credentialClass + "\","
                + "\"validFrom\":\"" + validFrom + "\",\"validUntil\":\"" + validUntil + "\"}")
        .when().post("/api/access-grant/credentials").then().statusCode(201);
    }

    private ExtractableResponse<Response> eligibility(String subject, String... requiredClasses) {
        StringBuilder arr = new StringBuilder("[");
        for (int i = 0; i < requiredClasses.length; i++) {
            if (i > 0) arr.append(",");
            arr.append("\"").append(requiredClasses[i]).append("\"");
        }
        arr.append("]");
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectId\":\"" + subject + "\",\"requiredClasses\":" + arr + "}")
        .when().post("/api/access-grant/eligibility").thenReturn().then().extract();
    }

    // ── AGRANT-WINDOW-001 — access allowed only inside [validFrom, validUntil) AND ACTIVE ──
    @Test @Tag("AGRANT-WINDOW-001")
    void access_allowedOnlyWithinWindow_recomputedPredicate() {
        String subject = "courier-" + java.util.UUID.randomUUID();

        // a grant whose window is open right now → check 200 allowed
        String open = createGrant(subject, "delivery-1", "courier",
            now().minus(1, ChronoUnit.HOURS), now().plus(1, ChronoUnit.HOURS));
        ExtractableResponse<Response> allowed = check(open);
        assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(allowed.jsonPath().getString("status")).isEqualTo("ACTIVE");

        // a grant whose window opens in the future → 403 GRANT_NOT_YET_VALID
        String future = createGrant(subject, "delivery-2", "courier",
            now().plus(1, ChronoUnit.HOURS), now().plus(2, ChronoUnit.HOURS));
        ExtractableResponse<Response> notYet = check(future);
        assertThat(notYet.statusCode()).isEqualTo(403);
        assertThat(notYet.jsonPath().getString("code")).isEqualTo("GRANT_NOT_YET_VALID");

        // a grant whose window has elapsed → 403 GRANT_EXPIRED
        String past = createGrant(subject, "delivery-3", "courier",
            now().minus(2, ChronoUnit.HOURS), now().minus(1, ChronoUnit.HOURS));
        ExtractableResponse<Response> expired = check(past);
        assertThat(expired.statusCode()).isEqualTo(403);
        assertThat(expired.jsonPath().getString("code")).isEqualTo("GRANT_EXPIRED");
    }

    // ── AGRANT-REVOKE-001 — revoke fails the grant closed; records who/when; idempotent ──
    @Test @Tag("AGRANT-REVOKE-001")
    void revoke_failsClosed_recordsActor_idempotent() {
        String subject = "courier-" + java.util.UUID.randomUUID();
        // a grant with an OPEN window — only the revoke should deny it
        String id = createGrant(subject, "delivery-r", "courier",
            now().minus(1, ChronoUnit.HOURS), now().plus(8, ChronoUnit.HOURS));
        assertThat(check(id).statusCode()).isEqualTo(200);

        ExtractableResponse<Response> revoked = revoke(id);
        assertThat(revoked.statusCode()).isEqualTo(200);
        assertThat(revoked.jsonPath().getString("status")).isEqualTo("REVOKED");
        assertThat(revoked.jsonPath().getString("revokedBy")).as("the revoking actor is recorded").isNotBlank();
        assertThat(revoked.jsonPath().getString("revokedAt")).as("the revoke instant is recorded").isNotBlank();
        String firstRevokedBy = revoked.jsonPath().getString("revokedBy");
        String firstRevokedAt = revoked.jsonPath().getString("revokedAt");

        // a check on the revoked grant → 403 GRANT_REVOKED even though the window is still open
        ExtractableResponse<Response> afterRevoke = check(id);
        assertThat(afterRevoke.statusCode()).isEqualTo(403);
        assertThat(afterRevoke.jsonPath().getString("code")).isEqualTo("GRANT_REVOKED");

        // a second revoke is idempotent — same (revokedBy, revokedAt). Compare the instant by VALUE
        // (parsed Instant), not by string, so serialization-scale differences cannot break it.
        ExtractableResponse<Response> again = revoke(id);
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat(again.jsonPath().getString("revokedBy")).isEqualTo(firstRevokedBy);
        assertThat(Instant.parse(again.jsonPath().getString("revokedAt")))
            .isEqualTo(Instant.parse(firstRevokedAt));
    }

    // ── AGRANT-WINDOW-001 — a missing grant is a 404 problem+json ──
    @Test @Tag("AGRANT-WINDOW-001")
    void check_unknownGrant_is404() {
        ExtractableResponse<Response> missing = check(java.util.UUID.randomUUID().toString());
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.jsonPath().getString("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ── AGRANT-ELIGIBILITY-001 — passes only when EVERY required class is held + valid ──
    @Test @Tag("AGRANT-ELIGIBILITY-001")
    void eligibility_requiresEveryClass_namesMissingOrExpired() {
        String subject = "courier-" + java.util.UUID.randomUUID();
        issueCredential(subject, "LICENSE", now().minus(1, ChronoUnit.HOURS), now().plus(30, ChronoUnit.DAYS));
        issueCredential(subject, "INSURANCE", now().minus(1, ChronoUnit.HOURS), now().plus(30, ChronoUnit.DAYS));

        // every required class held + valid → 204 eligible
        assertThat(eligibility(subject, "LICENSE", "INSURANCE").statusCode()).isEqualTo(204);

        // a required class is ABSENT → 403 CREDENTIAL_INELIGIBLE naming the missing class
        ExtractableResponse<Response> missing = eligibility(subject, "LICENSE", "INSURANCE", "BACKGROUND_CHECK");
        assertThat(missing.statusCode()).isEqualTo(403);
        assertThat(missing.jsonPath().getString("code")).isEqualTo("CREDENTIAL_INELIGIBLE");
        assertThat(missing.jsonPath().getString("detail")).contains("BACKGROUND_CHECK");
    }

    // ── AGRANT-ELIGIBILITY-001 — a held-but-EXPIRED credential fails closed naming the class ──
    @Test @Tag("AGRANT-ELIGIBILITY-001")
    void eligibility_expiredCredentialFailsClosed_naming() {
        String subject = "courier-" + java.util.UUID.randomUUID();
        // LICENSE is held but its window has elapsed; INSURANCE is valid
        issueCredential(subject, "LICENSE", now().minus(40, ChronoUnit.DAYS), now().minus(1, ChronoUnit.DAYS));
        issueCredential(subject, "INSURANCE", now().minus(1, ChronoUnit.HOURS), now().plus(30, ChronoUnit.DAYS));

        ExtractableResponse<Response> expired = eligibility(subject, "LICENSE", "INSURANCE");
        assertThat(expired.statusCode()).isEqualTo(403);
        assertThat(expired.jsonPath().getString("code")).isEqualTo("CREDENTIAL_INELIGIBLE");
        assertThat(expired.jsonPath().getString("detail")).contains("LICENSE");
    }
}
