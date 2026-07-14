package com.ax.template.authblueprint.signedartifact;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * signed-artifact-l0 compliance — verified against the live signedartifact reference workload.
 * The invariant: an artifact verified by a party OTHER than the signer uses a detached ASYMMETRIC
 * signature (ES256) over a published verifying key, never symmetric HMAC; the verifier enforces
 * an alg allow-list resolved from server-side config keyed by kid (rejecting alg:none and
 * HS*-over-public-key algorithm-confusion), and a tampered content-hash is detected.
 * Spec: specs/signed-artifact-l0.yaml (RFC 7515/7518/8037 + OWASP JWT Cheat Sheet).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("SIGNEDARTIFACT")
class SignedArtifactComplianceTest {

    @LocalServerPort int port;
    String issuer;

    @BeforeEach
    void setup() {
        SignedArtifactTestSupport.useRandomPort(port);
        issuer = SignedArtifactTestSupport.obtainToken(
            SignedArtifactTestSupport.freshEmail("sart-issuer"), "MEMBER");
    }

    private ExtractableResponse<Response> issue(String subjectRef, String content) {
        return given().header("Authorization", "Bearer " + issuer).header("Content-Type", "application/json")
            .body("{\"subjectRef\":\"" + subjectRef + "\",\"content\":\"" + content + "\"}")
        .when().post("/api/signed-artifact/records").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> verify(String jws, String content) {
        return given().header("Content-Type", "application/json")
            .body("{\"jws\":\"" + jws.replace("\"", "\\\"") + "\",\"content\":\"" + content + "\"}")
        .when().post("/api/signed-artifact/verify").thenReturn().then().extract();
    }

    // ── SIGNED-ASYM-001 — issue + verify against the published JWKS public key succeeds ──
    @Test @Tag("SIGNED-ASYM-001")
    void issue_thenVerifyAgainstPublishedKey_succeeds() {
        ExtractableResponse<Response> issued = issue("certificate-123", "course completed");
        assertThat(issued.statusCode()).isEqualTo(201);
        String jws = issued.jsonPath().getString("jws");
        assertThat(issued.jsonPath().getString("alg")).isEqualTo("ES256");

        ExtractableResponse<Response> verified = verify(jws, "course completed");
        assertThat(verified.statusCode()).isEqualTo(200);
        assertThat(verified.jsonPath().getBoolean("valid")).isTrue();
    }

    // ── SIGNED-ASYM-001 — the signature covers a content-hash; tampered content is detected ──
    @Test @Tag("SIGNED-ASYM-001")
    void verify_tamperedContent_isRejected() {
        ExtractableResponse<Response> issued = issue("certificate-456", "original content");
        String jws = issued.jsonPath().getString("jws");

        ExtractableResponse<Response> tampered = verify(jws, "TAMPERED content");
        assertThat(tampered.statusCode()).isEqualTo(422);
        assertThat(tampered.jsonPath().getString("code")).isEqualTo("SIGNED_ARTIFACT_CONTENT_MISMATCH");
    }

    // ── SIGNED-ASYM-001 — the published JWKS carries the PUBLIC key only, no issuer secret ──
    @Test @Tag("SIGNED-ASYM-001")
    void jwks_publishesPublicKeyOnly_noIssuerSecret() {
        var jwks = given().when().get("/api/signed-artifact/jwks").then().statusCode(200).extract().jsonPath();
        var keys = jwks.getList("keys");
        assertThat(keys).hasSize(1);
        java.util.Map<String, Object> key = (java.util.Map<String, Object>) keys.get(0);
        assertThat(key).as("an EC JWK's public coordinates are present").containsKeys("x", "y");
        assertThat(key).as("the PRIVATE key parameter 'd' must NEVER be published").doesNotContainKey("d");
    }

    // ── SIGNED-ALG-ALLOWLIST-001 — alg:none is rejected; an empty signature is never verified ──
    @Test @Tag("SIGNED-ALG-ALLOWLIST-001")
    void verify_algNone_isRejected() {
        ExtractableResponse<Response> issued = issue("certificate-789", "payload content");
        String kid = issued.jsonPath().getString("kid");
        String contentHash = ContentHasher.sha256Hex("payload content");
        String payload = "{\"subjectRef\":\"certificate-789\",\"contentHash\":\"" + contentHash + "\"}";

        String forged = SignedArtifactTestSupport.forgeAlgNone(kid, payload);
        ExtractableResponse<Response> rejected = verify(forged, "payload content");
        assertThat(rejected.statusCode()).isEqualTo(401);
        assertThat(rejected.jsonPath().getString("code")).isEqualTo("SIGNED_ARTIFACT_UNSUPPORTED_ALGORITHM");
    }

    // ── SIGNED-ALG-ALLOWLIST-001 — HS256-over-public-key algorithm confusion is rejected ──
    @Test @Tag("SIGNED-ALG-ALLOWLIST-001")
    void verify_algConfusionHs256OverPublicKey_isRejected() throws Exception {
        ExtractableResponse<Response> issued = issue("certificate-abc", "payload content");
        String kid = issued.jsonPath().getString("kid");
        String contentHash = ContentHasher.sha256Hex("payload content");
        String payload = "{\"subjectRef\":\"certificate-abc\",\"contentHash\":\"" + contentHash + "\"}";

        // the attacker uses the PUBLISHED public key's x-coordinate as the HMAC "secret" —
        // exactly the classic RS/ES -> HS confusion attack surface.
        var jwks = given().when().get("/api/signed-artifact/jwks").then().statusCode(200).extract().jsonPath();
        String publicKeyBytesAsSecret = jwks.getString("keys[0].x");

        String forged = SignedArtifactTestSupport.forgeAlgConfusionHs256(kid, payload, publicKeyBytesAsSecret);
        ExtractableResponse<Response> rejected = verify(forged, "payload content");
        assertThat(rejected.statusCode()).isEqualTo(401);
        assertThat(rejected.jsonPath().getString("code")).isEqualTo("SIGNED_ARTIFACT_UNSUPPORTED_ALGORITHM");
    }

    // ── SIGNED-ALG-ALLOWLIST-001 — a PARSEABLE (non-empty signature) token with a real asymmetric
    // alg the server never configured (ES384) proves the alg-vs-kid COMPARISON branch itself is
    // live, isolated from the alg:none special case (which JWSObject can't even parse) and from
    // HS256 (which shares the "not ES256" outcome but via a symmetric-key forgery narrative).
    @Test @Tag("SIGNED-ALG-ALLOWLIST-001")
    void verify_parseableAlgMismatch_ES384_isRejectedByAllowlistBranch() {
        ExtractableResponse<Response> issued = issue("certificate-def", "payload content");
        String kid = issued.jsonPath().getString("kid");
        String contentHash = ContentHasher.sha256Hex("payload content");
        String payload = "{\"subjectRef\":\"certificate-def\",\"contentHash\":\"" + contentHash + "\"}";

        String forged = SignedArtifactTestSupport.forgeAlgMismatchParseable(kid, payload, "ES384");
        ExtractableResponse<Response> rejected = verify(forged, "payload content");
        assertThat(rejected.statusCode()).isEqualTo(401);
        assertThat(rejected.jsonPath().getString("code")).isEqualTo("SIGNED_ARTIFACT_UNSUPPORTED_ALGORITHM");
    }
}
