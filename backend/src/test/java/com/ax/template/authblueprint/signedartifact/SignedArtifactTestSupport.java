package com.ax.template.authblueprint.signedartifact;

import io.restassured.RestAssured;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for signed-artifact integration tests (mirrors ValuationRunTestSupport). */
public final class SignedArtifactTestSupport {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SignedArtifactTestSupport() {}

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String obtainToken(String email, String role) {
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        return given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }

    /** SIGNED-ALG-ALLOWLIST-001 negative — an Unsecured JWS ({@code alg:none}), empty signature. */
    public static String forgeAlgNone(String kid, String payloadJson) {
        String header = "{\"alg\":\"none\",\"kid\":\"" + kid + "\"}";
        String encodedHeader = URL_ENCODER.encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return encodedHeader + "." + encodedPayload + ".";
    }

    /**
     * SIGNED-ALG-ALLOWLIST-001 negative — the classic RS/ES-to-HS algorithm-confusion forgery: an
     * attacker takes the PUBLISHED public key bytes as an HMAC secret and re-signs with
     * {@code alg:HS256}, keeping the real {@code kid}. A verifier that trusted the token's own
     * {@code alg} header would accept this; this catalog's verifier rejects it purely on the
     * kid-vs-alg server-side mismatch, before ever attempting an HMAC check.
     */
    public static String forgeAlgConfusionHs256(String kid, String payloadJson, String publicKeyBytesAsSecret)
            throws Exception {
        String header = "{\"alg\":\"HS256\",\"kid\":\"" + kid + "\"}";
        String encodedHeader = URL_ENCODER.encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedPayload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(publicKeyBytesAsSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + URL_ENCODER.encodeToString(sig);
    }

    /**
     * SIGNED-ALG-ALLOWLIST-001 negative — a token that IS parseable by {@code JWSObject} (a real,
     * non-empty signature segment, unlike {@link #forgeAlgNone}) but declares an alg the server
     * never configured for this kid ({@code ES384}, a real asymmetric JWS alg — not {@code none},
     * not HS*). This isolates the server-side alg-vs-kid COMPARISON branch from the alg:none
     * special case: the signature bytes are arbitrary garbage (never cryptographically verified,
     * since the alg mismatch must reject BEFORE any {@code Verifier} is even constructed).
     */
    public static String forgeAlgMismatchParseable(String kid, String payloadJson, String algName) {
        String header = "{\"alg\":\"" + algName + "\",\"kid\":\"" + kid + "\"}";
        String encodedHeader = URL_ENCODER.encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        // Arbitrary non-empty "signature" — its bytes are irrelevant because the alg-vs-kid check
        // must reject this token before any verifier ever inspects them.
        String garbageSignature = URL_ENCODER.encodeToString("not-a-real-signature".getBytes(StandardCharsets.UTF_8));
        return encodedHeader + "." + encodedPayload + "." + garbageSignature;
    }
}
