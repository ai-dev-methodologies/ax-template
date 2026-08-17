package com.ax.template.authblueprint.secretsmanagement;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * secrets-management-l0 compliance — every item verified against the live reference workload. Domain
 * @Tag("SECRETS") drives ./gradlew testSecrets; the per-item @Tag binds the spec item to its test
 * (spec_item_verification_binding guard). Spec: specs/secrets-management-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecretsComplianceTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;

    String member;        // the provisioning principal (granted)
    String memberName;    // its Authentication.getName() (the email)
    String other;         // a different principal (NOT granted)

    @BeforeEach
    void setUp() {
        memberName = "sec-" + UUID.randomUUID() + "@example.com";
        member = tokenFor(memberName, "MEMBER");
        other = tokenFor("sec-other-" + UUID.randomUUID() + "@example.com", "MEMBER");
    }

    private String tokenFor(String email, String role) {
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
            .when().post("/api/auth/email/signup");
        return given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
            .when().post("/api/auth/email/login").then().statusCode(200).extract().path("accessToken");
    }

    private io.restassured.specification.RequestSpecification as(String token) {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
    }

    /**
     * Provision a fresh secret. With no explicit grant list, the controller grants the PROVISIONING
     * caller's own principal (Authentication.getName() — the JWT subject = userId, not the email), so
     * the member can read it and a DIFFERENT token (a different subject) cannot. Returns the id.
     */
    private String provision(String value) {
        String id = "secret-" + UUID.randomUUID();
        as(member).body("{\"secretId\":\"" + id + "\",\"value\":\"" + value + "\"}")
            .post("/api/secrets-demo/provision").then().statusCode(201).body("provisioned", equalTo(true));
        return id;
    }

    // ── SECRET-SOURCE-001 ────────────────────────────────────────────────────

    @Test
    @Tag("SECRETS")
    @Tag("SECRET-SOURCE-001")
    void externalizedSourcing_failFastOnLiteral_presenceOnlyStatus() {
        // config-status exposes presence booleans ONLY — never a value. An unset key is absent.
        as(member).get("/api/secrets-demo/config-status?keys=DB_PASSWORD,API_TOKEN")
            .then().statusCode(200)
            .body("DB_PASSWORD", equalTo(false))
            .body("API_TOKEN", equalTo(false));

        // the explicit "inject at runtime" placeholder is acceptable (externalized) → 200
        as(member).body("{\"propertyName\":\"app.db.password\",\"configuredValue\":\""
                + SecretSource.UNRESOLVED_PLACEHOLDER + "\"}")
            .post("/api/secrets-demo/source/validate").then().statusCode(200)
            .body("externalized", equalTo(true));

        // a REAL secret literal pasted into config FAILS FAST → 500 SECRET_CONFIG_LITERAL
        as(member).body("{\"propertyName\":\"app.db.password\",\"configuredValue\":\"hunter2-real-password\"}")
            .post("/api/secrets-demo/source/validate").then().statusCode(500)
            .body("code", equalTo("SECRET_CONFIG_LITERAL"))
            .body(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hunter2")));
    }

    // ── SECRET-NO-LOG-001 ────────────────────────────────────────────────────

    @Test
    @Tag("SECRETS")
    @Tag("SECRET-NO-LOG-001")
    void redaction_neitherInterpolationNorSerializationLeaksPlaintext() {
        String id = provision("plaintext-must-never-appear");
        var resp = as(member).get("/api/secrets-demo/" + id + "/leak-probe")
            .then().statusCode(200).extract();
        // accidental string interpolation → masked
        assertThat(resp.jsonPath().getString("interpolated")).isEqualTo("secret=" + SecretValue.MASK);
        // accidental JSON serialization → masked
        assertThat(resp.jsonPath().getString("serialized")).isEqualTo(SecretValue.MASK);
        // the wrapper's own probe value never appears anywhere in the body
        assertThat(resp.asString()).doesNotContain("super-secret-probe-value");

        // a successful READ response also never echoes the value — only its length
        var read = as(member).get("/api/secrets-demo/" + id + "/read").then().statusCode(200).extract();
        assertThat(read.asString()).doesNotContain("plaintext-must-never-appear");
        assertThat(read.jsonPath().getInt("valueLength")).isEqualTo("plaintext-must-never-appear".length());
    }

    // ── SECRET-ENCRYPTION-001 ────────────────────────────────────────────────

    @Test
    @Tag("SECRETS")
    @Tag("SECRET-ENCRYPTION-001")
    void encryption_ciphertextAtRest_nonTlsRejected() {
        String plaintext = "envelope-encrypted-value";
        String id = provision(plaintext);
        // the AT-REST form is ciphertext (base64), and it is NOT the plaintext
        var atRest = as(member).get("/api/secrets-demo/" + id + "/at-rest").then().statusCode(200).extract();
        String ciphertextB64 = atRest.jsonPath().getString("ciphertextB64");
        assertThat(atRest.jsonPath().getBoolean("encryptedAtRest")).isTrue();
        assertThat(ciphertextB64).isNotBlank();
        // ciphertext decoded must not equal / contain the plaintext bytes
        String decoded = new String(java.util.Base64.getDecoder().decode(ciphertextB64),
                java.nio.charset.StandardCharsets.ISO_8859_1);
        assertThat(decoded).doesNotContain(plaintext);
        assertThat(atRest.asString()).doesNotContain(plaintext);

        // a secret fetch over a NON-TLS hop is rejected → 400 SECRET_TLS_REQUIRED
        as(member).get("/api/secrets-demo/" + id + "/read?transport=plain")
            .then().statusCode(400).body("code", equalTo("SECRET_TLS_REQUIRED"));
        // over TLS it succeeds
        as(member).get("/api/secrets-demo/" + id + "/read?transport=tls").then().statusCode(200);
    }

    // ── SECRET-ACCESS-001 ────────────────────────────────────────────────────

    @Test
    @Tag("SECRETS")
    @Tag("SECRET-ACCESS-001")
    void leastPrivilege_grantedReads_unauthorizedIs403AndBothAudited() {
        String id = provision("acl-protected-value");
        // the GRANTED principal reads it → 200
        as(member).get("/api/secrets-demo/" + id + "/read").then().statusCode(200);
        // a DIFFERENT (un-granted) principal → 403 SECRET_ACCESS_DENIED
        as(other).get("/api/secrets-demo/" + id + "/read")
            .then().statusCode(403).body("code", equalTo("SECRET_ACCESS_DENIED"));

        // BOTH the grant (SECRET_READ) and the denial (SECRET_ACCESS_DENIED) are audited via
        // audit-log-l0; the audit row stores secret_id only, never the value.
        var grantedRows = as(member).get("/api/audit-logs?action=SECRET_READ&resourceId=" + id)
            .then().statusCode(200).extract();
        assertThat(grantedRows.jsonPath().getList("content")).as("the grant was audited").isNotEmpty();
        assertThat(grantedRows.asString()).doesNotContain("acl-protected-value");

        var deniedRows = as(member).get("/api/audit-logs?action=SECRET_ACCESS_DENIED&resourceId=" + id)
            .then().statusCode(200).extract();
        assertThat(deniedRows.jsonPath().getList("content")).as("the denial was audited").isNotEmpty();
        assertThat(deniedRows.asString()).doesNotContain("acl-protected-value");
    }

    // ── SECRET-ROTATION-001 ──────────────────────────────────────────────────

    @Test
    @Tag("SECRETS")
    @Tag("SECRET-ROTATION-001")
    void rotation_overlapAcceptsBothVersions_thenPreviousRetired() {
        String v1 = "version-one";
        String id = provision(v1);
        // initial version verifies
        as(member).body("{\"value\":\"" + v1 + "\"}").post("/api/secrets-demo/" + id + "/verify")
            .then().statusCode(200).body("accepted", equalTo(true));

        // rotate to v2 → current=v2, previous=v1 (overlap window)
        String v2 = "version-two";
        as(member).body("{\"value\":\"" + v2 + "\"}").post("/api/secrets-demo/" + id + "/rotate")
            .then().statusCode(200).body("version", equalTo(2)).body("previousVersion", equalTo(1));
        // DURING overlap BOTH the new (v2) AND the previous (v1) verify
        as(member).body("{\"value\":\"" + v2 + "\"}").post("/api/secrets-demo/" + id + "/verify")
            .then().statusCode(200).body("accepted", equalTo(true));
        as(member).body("{\"value\":\"" + v1 + "\"}").post("/api/secrets-demo/" + id + "/verify")
            .then().statusCode(200).body("accepted", equalTo(true));

        // rotate again to v3 → previous becomes v2, v1 is DROPPED past the overlap
        String v3 = "version-three";
        as(member).body("{\"value\":\"" + v3 + "\"}").post("/api/secrets-demo/" + id + "/rotate")
            .then().statusCode(200).body("version", equalTo(3)).body("previousVersion", equalTo(2));
        // v3 (current) and v2 (previous) still verify; v1 is now RETIRED → 401
        as(member).body("{\"value\":\"" + v3 + "\"}").post("/api/secrets-demo/" + id + "/verify")
            .then().statusCode(200);
        as(member).body("{\"value\":\"" + v2 + "\"}").post("/api/secrets-demo/" + id + "/verify")
            .then().statusCode(200);
        as(member).body("{\"value\":\"" + v1 + "\"}").post("/api/secrets-demo/" + id + "/verify")
            .then().statusCode(401).body("code", equalTo("SECRET_VERSION_RETIRED"));
    }

    // ── SECRET-LIFECYCLE-001 ─────────────────────────────────────────────────

    @Test
    @Tag("SECRETS")
    @Tag("SECRET-LIFECYCLE-001")
    void lifecycle_immediateRevocation_expiry_andDestroyIsUnrecoverable() {
        // immediate revocation: a read AFTER revoke fails-closed (does NOT wait for next rotation)
        String id = provision("revocable-value");
        as(member).get("/api/secrets-demo/" + id + "/read").then().statusCode(200);
        as(member).post("/api/secrets-demo/" + id + "/revoke").then().statusCode(200);
        as(member).get("/api/secrets-demo/" + id + "/read")
            .then().statusCode(401).body("code", equalTo("SECRET_REVOKED"));

        // TTL expiry: a credential provisioned with ttlSeconds=0 is already past-expiry → 401
        String expiredId = "secret-exp-" + UUID.randomUUID();
        as(member).body("{\"secretId\":\"" + expiredId + "\",\"value\":\"short-lived\"}")
            .post("/api/secrets-demo/provision-ttl?ttlSeconds=0").then().statusCode(201);
        as(member).get("/api/secrets-demo/" + expiredId + "/read")
            .then().statusCode(401).body("code", equalTo("SECRET_EXPIRED"));

        // destroy is unrecoverable: a destroyed secret reads as not-found (404), never recoverable
        String destroyId = provision("doomed-value");
        as(member).post("/api/secrets-demo/" + destroyId + "/destroy").then().statusCode(200);
        as(member).get("/api/secrets-demo/" + destroyId + "/read")
            .then().statusCode(404).body("code", equalTo("SECRET_NOT_FOUND"));
        // even re-provisioning at the same id cannot resurrect the destroyed version's value —
        // destroy left no recoverable plaintext (the record is gone from any resolvable state)
        as(member).body("{\"value\":\"whatever\"}").post("/api/secrets-demo/" + destroyId + "/rotate")
            .then().statusCode(404).body("code", equalTo("SECRET_NOT_FOUND"));
    }

    // ── SECRET-OBSERVABILITY-001 ─────────────────────────────────────────────

    @Test
    @Tag("SECRETS")
    @Tag("SECRET-OBSERVABILITY-001")
    void exposesExactlyThreeBoundedLabelMeters() {
        // exercise all three meters across their bounded enum values
        String id = provision("metered-value");
        as(member).get("/api/secrets-demo/" + id + "/read").then().statusCode(200);            // access granted
        as(other).get("/api/secrets-demo/" + id + "/read").then().statusCode(403);             // access denied
        as(member).body("{\"value\":\"rotated\"}").post("/api/secrets-demo/" + id + "/rotate")
            .then().statusCode(200);                                                            // rotation success
        as(member).post("/api/secrets-demo/" + id + "/revoke").then().statusCode(200);
        as(member).get("/api/secrets-demo/" + id + "/read").then().statusCode(401);            // resolution_failure revoked
        // expired resolution_failure
        String expId = "secret-m-" + UUID.randomUUID();
        as(member).body("{\"secretId\":\"" + expId + "\",\"value\":\"x\"}")
            .post("/api/secrets-demo/provision-ttl?ttlSeconds=0").then().statusCode(201);
        as(member).get("/api/secrets-demo/" + expId + "/read").then().statusCode(401);         // resolution_failure expired
        // not_found resolution_failure
        as(member).get("/api/secrets-demo/secret-missing-" + UUID.randomUUID() + "/read").then().statusCode(404);

        // exactly the 3 canonical meters exist
        assertThat(registry.find(SecretMetrics.ACCESS).counter()).isNotNull();
        assertThat(registry.find(SecretMetrics.ROTATION).counter()).isNotNull();
        assertThat(registry.find(SecretMetrics.RESOLUTION_FAILURE).counter()).isNotNull();

        // bounded labels — each meter carries ONLY its fixed enum dim; NO secret_id / principal / value
        Set<String> accessOutcomes = Set.of("granted", "denied");
        assertMeterLabels(SecretMetrics.ACCESS, SecretMetrics.TAG_OUTCOME, accessOutcomes);
        assertMeterLabels(SecretMetrics.ROTATION, SecretMetrics.TAG_RESULT, Set.of("success", "failure"));
        assertMeterLabels(SecretMetrics.RESOLUTION_FAILURE, SecretMetrics.TAG_REASON,
                Set.of("not_found", "revoked", "expired", "store_unavailable"));

        // each bounded value we actually exercised was recorded with count >= 1
        assertCountAtLeastOne(SecretMetrics.ACCESS, SecretMetrics.TAG_OUTCOME, "granted");
        assertCountAtLeastOne(SecretMetrics.ACCESS, SecretMetrics.TAG_OUTCOME, "denied");
        assertCountAtLeastOne(SecretMetrics.ROTATION, SecretMetrics.TAG_RESULT, "success");
        assertCountAtLeastOne(SecretMetrics.RESOLUTION_FAILURE, SecretMetrics.TAG_REASON, "revoked");
        assertCountAtLeastOne(SecretMetrics.RESOLUTION_FAILURE, SecretMetrics.TAG_REASON, "expired");
        assertCountAtLeastOne(SecretMetrics.RESOLUTION_FAILURE, SecretMetrics.TAG_REASON, "not_found");
    }

    /** Assert every meter under {@code name} carries EXACTLY one tag — its bounded enum dim — from {@code allowed}. */
    private void assertMeterLabels(String name, String onlyKey, Set<String> allowed) {
        for (Meter m : registry.find(name).meters()) {
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                assertThat(t.getKey()).as("%s tag key bounded", name).isEqualTo(onlyKey);
                assertThat(allowed).as("%s value from closed set", name).contains(t.getValue());
            }
        }
    }

    private void assertCountAtLeastOne(String name, String key, String value) {
        var counter = registry.find(name).tag(key, value).counter();
        assertThat(counter).as("%s{%s=%s} recorded", name, key, value).isNotNull();
        assertThat(counter.count()).as("%s{%s=%s} count", name, key, value).isGreaterThanOrEqualTo(1.0);
    }
}
