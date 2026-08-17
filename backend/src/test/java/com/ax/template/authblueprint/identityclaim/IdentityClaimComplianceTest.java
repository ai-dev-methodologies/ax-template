package com.ax.template.authblueprint.identityclaim;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioral compliance tests for identity-claim-on-auth-l0.yaml (3 invariants).
 *
 * <p>IDCLAIM-CLAIM-001, IDCLAIM-IDEMPOTENT-001, IDCLAIM-GUARD-001.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("IDENTITY_CLAIM")
class IdentityClaimComplianceTest {

    @LocalServerPort int port;

    // ─── CLAIM family ────────────────────────────────────────────────────────

    /**
     * IDCLAIM-CLAIM-001: create 2 anonymous records with the same claimKey,
     * then user A claims that key → ClaimResult.claimedCount == 2.
     * Both records are now owned by A; no partial transfer.
     */
    @Test
    @Tag("IDCLAIM-CLAIM-001")
    void claim_001_allMatchingRecordsTransferAtomically() {
        String tokenA = obtainToken("claimA");
        String userIdA = extractUserId(tokenA);

        String claimKey = "guest-claim-" + java.util.UUID.randomUUID() + "@x.com";

        // Create 2 anonymous records.
        addRecord(tokenA, claimKey, "rec1");
        addRecord(tokenA, claimKey, "rec2");

        // User A claims the key.
        Map<String, Object> result = given()
            .header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{\"claimKey\":\"" + claimKey + "\"}")
        .when().post("/api/identity-claim/claim")
        .then().statusCode(200)
            .extract().as(java.util.HashMap.class);

        assertThat((Integer) result.get("claimedCount")).isEqualTo(2);

        // Inspect: both records owned by A.
        List<Map<String, Object>> records = getByClaimKey(tokenA, claimKey);
        assertThat(records).hasSize(2);
        for (Map<String, Object> rec : records) {
            assertThat(rec.get("ownerUserId"))
                .as("every record must be owned by userA after atomic claim")
                .isEqualTo(userIdA);
        }
    }

    // ─── IDEMPOTENT family ───────────────────────────────────────────────────

    /**
     * IDCLAIM-IDEMPOTENT-001: after a successful claim, replaying the same claim
     * returns claimedCount == 0 (all rows already owned → WHERE IS NULL matches 0).
     * The count of records owned by A does not change.
     */
    @Test
    @Tag("IDCLAIM-IDEMPOTENT-001")
    void idempotent_001_replayYieldsZeroCount() {
        String tokenA = obtainToken("idempA");
        String claimKey = "guest-idem-" + java.util.UUID.randomUUID() + "@x.com";

        addRecord(tokenA, claimKey, "r1");
        addRecord(tokenA, claimKey, "r2");

        // First claim → 2.
        int first = claimKey(tokenA, claimKey);
        assertThat(first).isEqualTo(2);

        // Replay → 0 (already owned).
        int replay = claimKey(tokenA, claimKey);
        assertThat(replay).as("replay must be a no-op (IDCLAIM-IDEMPOTENT-001)").isEqualTo(0);

        // Still exactly 2 records for this key.
        List<Map<String, Object>> records = getByClaimKey(tokenA, claimKey);
        assertThat(records).hasSize(2);
    }

    // ─── GUARD family ────────────────────────────────────────────────────────

    /**
     * IDCLAIM-GUARD-001: record owned by user A MUST NOT be transferred to user B.
     * The structural WHERE owner_user_id IS NULL in the CAS query enforces this.
     */
    @Test
    @Tag("IDCLAIM-GUARD-001")
    void guard_001_alreadyOwnedRecordNotTransferredToOtherUser() {
        String tokenA = obtainToken("guardA");
        String userIdA = extractUserId(tokenA);
        String tokenB = obtainToken("guardB");

        String claimKey = "guest-guard-" + java.util.UUID.randomUUID() + "@x.com";

        // Create 1 anonymous record.
        addRecord(tokenA, claimKey, "shared-rec");

        // User A claims it first.
        int claimedByA = claimKey(tokenA, claimKey);
        assertThat(claimedByA).isEqualTo(1);

        // User B attempts to claim the same key.
        int claimedByB = claimKey(tokenB, claimKey);
        assertThat(claimedByB)
            .as("B must get 0 — A already owns the record (IDCLAIM-GUARD-001)")
            .isEqualTo(0);

        // The record still belongs to A.
        List<Map<String, Object>> records = getByClaimKey(tokenA, claimKey);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).get("ownerUserId"))
            .as("ownerUserId must remain userA — not transferred to userB")
            .isEqualTo(userIdA);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String obtainToken(String prefix) {
        return IdentityClaimTestSupport.obtainToken(
            IdentityClaimTestSupport.freshEmail(prefix), "MEMBER");
    }

    private String extractUserId(String token) {
        // The /api/auth/me endpoint returns the authenticated principal's id.
        return given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/auth/me")
        .then().statusCode(200)
            .extract().path("userId");
    }

    private void addRecord(String token, String claimKey, String label) {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"claimKey\":\"" + claimKey + "\",\"label\":\"" + label + "\"}")
        .when().post("/api/identity-claim/records")
        .then().statusCode(201);
    }

    private int claimKey(String token, String claimKey) {
        Map<String, Object> result = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"claimKey\":\"" + claimKey + "\"}")
        .when().post("/api/identity-claim/claim")
        .then().statusCode(200)
            .extract().as(java.util.HashMap.class);
        return (Integer) result.get("claimedCount");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getByClaimKey(String token, String claimKey) {
        return given()
            .header("Authorization", "Bearer " + token)
            .queryParam("claimKey", claimKey)
        .when().get("/api/identity-claim/records")
        .then().statusCode(200)
            .extract().as(List.class);
    }
}
