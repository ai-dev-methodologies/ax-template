package com.ax.template.authblueprint.apikey;

import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Compliance tests for the api-key domain (R30).
 *
 * <p>All 12 items from {@code specs/api-key-l0.yaml} are covered through RestAssured
 * black-box HTTP. Unit-level coverage for KEY-STORAGE-001 is in
 * {@link ApiKeyHasherTest}; KEY-STORAGE-003 is in {@link ApiKeyEntityShapeTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// R22 fix pattern — BEFORE_CLASS (not AFTER_CLASS) so this class gets a FRESH
// ApplicationContext under the collapsed aggregate. Adding new beans to the
// catalog (e.g. EmailTemplateHistoryRepository in Wave D2) shifted the
// Spring TestContextCache LRU eviction order, causing this class to inherit
// a stale context whose Tomcat instance had already shut down — revoke flows
// then assert 401 but the dead-port HTTP path returns a stale 200. Same
// root cause as the BillingFlowIT + FeatureFlagFlowIT R22 closure.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("API_KEY")
class ApiKeyComplianceTest {

    @LocalServerPort int port;

    @Autowired ApiKeyRepository repository;
    @Autowired Clock clock;


    // ─── AUTHN family ────────────────────────────────────────────────────────

    @Test
    @Tag("KEY-AUTHN-001")
    void authn_001_postReturnsPlaintextOnce_andGetNeverDoes() {
        String token = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("authn1"), "MEMBER");

        String plaintext = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"first\"}")
        .when().post("/api/api-keys")
        .then()
            .statusCode(201)
            .body("value", Matchers.startsWith("ak_"))
            .body("prefix", Matchers.notNullValue())
            .extract().path("value");

        String id = listKeys(token).get(0).get("id").toString();

        given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/api-keys/" + id)
        .then()
            .statusCode(200)
            .body("$", Matchers.not(Matchers.hasKey("value")))
            .body("$", Matchers.not(Matchers.hasKey("hashedValue")));

        // Spot check — the plaintext bound to this test exists and is what we POSTed.
        org.assertj.core.api.Assertions.assertThat(plaintext).startsWith("ak_");
    }

    @Test
    @Tag("KEY-AUTHN-002")
    void authn_002_xApiKeyAuthenticatesAsOwner() {
        String token = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("authn2"), "MEMBER");
        String userId = given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().extract().path("userId");

        String plaintext = createKey(token, "[\"READ\"]");

        String userIdViaKey = given()
            .header(ApiKeyAuthenticationFilter.HEADER, plaintext)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(200)
            .extract().path("userId");

        org.assertj.core.api.Assertions.assertThat(userIdViaKey).isEqualTo(userId);
    }

    @Test
    @Tag("KEY-AUTHN-003")
    void authn_003_invalidRevokedExpiredKeys_return401() {
        String token = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("authn3"), "MEMBER");

        // (a) bogus key.
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, "ak_does_not_exist")
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(401);

        // (b) revoked key.
        String revokedPlaintext = createKey(token, "[\"READ\"]");
        UUID revokedId = listKeys(token).stream()
            .map(m -> UUID.fromString(m.get("id").toString()))
            .findFirst().orElseThrow();
        given()
            .header("Authorization", "Bearer " + token)
        .when().delete("/api/api-keys/" + revokedId)
        .then().statusCode(204);
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, revokedPlaintext)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(401);

        // (c) expired key — seed directly so we don't have to wait.
        String token2 = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("authn3b"), "MEMBER");
        String userId = given()
            .header("Authorization", "Bearer " + token2)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().extract().path("userId");
        String expiredPlaintext = ApiKeyHasher.newPlaintext();
        ApiKey expired = ApiKey.builder()
            .ownerUserId(userId)
            .name("expired-seed")
            .hashPrefix(ApiKeyHasher.prefixOf(expiredPlaintext))
            .hashedValue(ApiKeyHasher.hash(expiredPlaintext))
            .scopes(java.util.EnumSet.of(ApiKeyScope.READ))
            .status(ApiKeyStatus.ACTIVE)
            .createdAt(Instant.now(clock).minus(Duration.ofDays(2)))
            .expiresAt(Instant.now(clock).minus(Duration.ofMinutes(1)))
            .build();
        repository.save(expired);

        given()
            .header(ApiKeyAuthenticationFilter.HEADER, expiredPlaintext)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(401);
    }

    // ─── STORAGE family ──────────────────────────────────────────────────────

    @Test
    @Tag("KEY-STORAGE-002")
    void storage_002_listResponseDoesNotLeakHashOrPlaintext() {
        String token = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("stor2"), "MEMBER");
        createKey(token, "[\"READ\"]");

        given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/api-keys")
        .then().statusCode(200)
            .body("items[0]", Matchers.not(Matchers.hasKey("value")))
            .body("items[0]", Matchers.not(Matchers.hasKey("hashedValue")))
            .body("items[0]", Matchers.hasKey("prefix"));
    }

    // ─── LIFECYCLE family ────────────────────────────────────────────────────

    @Test
    @Tag("KEY-LIFECYCLE-001")
    void lifecycle_001_revokeDeniesSubsequentUse() {
        String token = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("life1"), "MEMBER");
        String plaintext = createKey(token, "[\"READ\"]");
        UUID id = listKeys(token).stream()
            .map(m -> UUID.fromString(m.get("id").toString()))
            .findFirst().orElseThrow();

        // Works before revoke.
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, plaintext)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(200);

        // Revoke.
        given()
            .header("Authorization", "Bearer " + token)
        .when().delete("/api/api-keys/" + id)
        .then().statusCode(204);

        // Fails after revoke.
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, plaintext)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(401);
    }

    @Test
    @Tag("KEY-LIFECYCLE-002")
    void lifecycle_002_rotateIssuesNewAndRevokesOldAtomically() {
        String token = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("life2"), "MEMBER");
        String oldPlaintext = createKey(token, "[\"READ\"]");
        UUID id = listKeys(token).stream()
            .map(m -> UUID.fromString(m.get("id").toString()))
            .findFirst().orElseThrow();

        String newPlaintext = given()
            .header("Authorization", "Bearer " + token)
        .when().post("/api/api-keys/" + id + "/rotate")
        .then().statusCode(201)
            .body("value", Matchers.startsWith("ak_"))
            .extract().path("value");

        org.assertj.core.api.Assertions.assertThat(newPlaintext).isNotEqualTo(oldPlaintext);

        // Old plaintext should now be rejected.
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, oldPlaintext)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(401);

        // New plaintext authenticates.
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, newPlaintext)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(200);
    }

    @Test
    @Tag("KEY-LIFECYCLE-003")
    void lifecycle_003_expiredKeyStillActiveInDb_rejectedAtAuthWith401() {
        // Seed a key whose DB status STAYS ACTIVE but whose expiresAt is in the
        // past. This proves the clock-based expiration is checked at every
        // authentication (ApiKeyService.resolvePlaintext -> ApiKey.isActive(now)),
        // NOT inferred from a status sweep job.
        String token = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("life3"), "MEMBER");
        String userId = given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(200)
            .extract().path("userId");

        String plaintext = ApiKeyHasher.newPlaintext();
        ApiKey expired = ApiKey.builder()
            .ownerUserId(userId)
            .name("expired-but-active")
            .hashPrefix(ApiKeyHasher.prefixOf(plaintext))
            .hashedValue(ApiKeyHasher.hash(plaintext))
            .scopes(java.util.EnumSet.of(ApiKeyScope.READ))
            .status(ApiKeyStatus.ACTIVE)
            .createdAt(Instant.now(clock).minus(Duration.ofDays(2)))
            .expiresAt(Instant.now(clock).minus(Duration.ofMinutes(1)))
            .build();
        UUID savedId = repository.save(expired).getId();

        // The DB row is genuinely ACTIVE — so a 401 below can only come from the
        // clock check, never from a status check.
        org.assertj.core.api.Assertions.assertThat(
                repository.findById(savedId).orElseThrow().getStatus())
            .isEqualTo(ApiKeyStatus.ACTIVE);

        // Authentication MUST reject the expired-but-ACTIVE key with 401.
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, plaintext)
        .when().get("/api/api-keys/scope-probe/whoami")
        .then().statusCode(401);
    }

    // ─── AUTHZ family ────────────────────────────────────────────────────────

    @Test
    @Tag("KEY-AUTHZ-001")
    void authz_001_apiKeyCannotReachManagementSurface() {
        String token = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("authz1"), "MEMBER");
        String plaintext = createKey(token, "[\"WRITE\"]");

        // POST /api/api-keys with X-API-Key — must fail with 401 because the filter
        // skips management paths entirely.
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, plaintext)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"should-fail\"}")
        .when().post("/api/api-keys")
        .then().statusCode(401);

        // GET /api/api-keys (list) with X-API-Key — also 401.
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, plaintext)
        .when().get("/api/api-keys")
        .then().statusCode(401);
    }

    @Test
    @Tag("KEY-AUTHZ-002")
    void authz_002_crossUserGetReturns404() {
        String tokenA = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("authz2-a"), "MEMBER");
        String tokenB = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("authz2-b"), "MEMBER");

        createKey(tokenA, "[\"READ\"]");
        UUID idA = listKeys(tokenA).stream()
            .map(m -> UUID.fromString(m.get("id").toString()))
            .findFirst().orElseThrow();

        given()
            .header("Authorization", "Bearer " + tokenB)
        .when().get("/api/api-keys/" + idA)
        .then().statusCode(404);
    }

    @Test
    @Tag("KEY-AUTHZ-003")
    void authz_003_readScopeCannotReachWriteEndpoint() {
        String token = ApiKeyTestSupport.obtainToken(ApiKeyTestSupport.freshEmail("authz3"), "MEMBER");
        String readKey = createKey(token, "[\"READ\"]");

        // GET /scope-probe with READ-scope key → 200.
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, readKey)
        .when().get("/api/api-keys/scope-probe")
        .then().statusCode(200);

        // POST /scope-probe with READ-scope key → 403 (authority missing).
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, readKey)
        .when().post("/api/api-keys/scope-probe")
        .then().statusCode(403);

        // Sanity: WRITE-scope key passes both.
        String writeKey = createKey(token, "[\"READ\",\"WRITE\"]");
        given()
            .header(ApiKeyAuthenticationFilter.HEADER, writeKey)
        .when().post("/api/api-keys/scope-probe")
        .then().statusCode(200);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String createKey(String token, String scopesJsonArray) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"scopes\":" + scopesJsonArray + "}")
        .when().post("/api/api-keys")
        .then().statusCode(201)
            .extract().path("value");
    }

    private List<java.util.Map<String, Object>> listKeys(String token) {
        return given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/api-keys")
        .then().statusCode(200)
            .extract().path("items");
    }
}
