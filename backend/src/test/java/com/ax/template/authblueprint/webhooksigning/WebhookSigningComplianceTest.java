package com.ax.template.authblueprint.webhooksigning;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * webhook-signing-l0 compliance — every item verified against the live INBOUND signature-verification
 * reference workload over black-box HTTP. Domain @Tag("WEBHOOK_SIGNING") drives ./gradlew
 * testWebhookSigning; the per-item @Tag binds the spec item to its test (spec_item_verification_binding
 * guard). The endpoint is signature-authenticated (no JWT), so the test signs the body itself with the
 * secret handed out at provision time and exercises valid / tampered / stale / replayed / malformed
 * deliveries. Spec: specs/webhook-signing-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebhookSigningComplianceTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;

    /** Provision a fresh endpoint and return its 256-bit signing secret (hex), handed out ONCE. */
    private String provision() {
        String endpoint = "ep-" + UUID.randomUUID();
        String secret = given().contentType("application/json")
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/provision")
                .then().statusCode(201).body("provisioned", equalTo(true))
                .extract().path("secret");
        provisionedEndpoint = endpoint;
        return secret;
    }

    private String provisionedEndpoint;

    private String hexSign(String secretHex, long ts, String body) {
        try {
            byte[] secret = HexFormat.of().parseHex(secretHex);
            byte[] signedInput = (ts + "." + body).getBytes(StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(signedInput));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private io.restassured.specification.RequestSpecification post(String endpoint, String header,
                                                                   String eventId, String body) {
        var spec = given().contentType("application/json");
        if (header != null) {
            spec = spec.header("Webhook-Signature", header);
        }
        if (eventId != null) {
            spec = spec.header("Webhook-Id", eventId);
        }
        return spec.body(body);
    }

    // ── WHSIGN-HMAC-001 ──────────────────────────────────────────────────────

    @Test
    @Tag("WEBHOOK_SIGNING")
    @Tag("WHSIGN-HMAC-001")
    void hmacSha256OverTimestampDotBody_validAccepted_tamperedAndBodyOnlyRejected() {
        String secret = provision();
        long ts = Instant.now().getEpochSecond();
        String body = "{\"event\":\"order.created\",\"amount\":1000}";

        // a correct HMAC-SHA256 over (ts + '.' + body) verifies → 200
        String good = hexSign(secret, ts, body);
        post(provisionedEndpoint, "t=" + ts + ",v1=" + good, null, body)
                .post("/api/webhook-signing-demo/endpoints/" + provisionedEndpoint + "/receive")
                .then().statusCode(200).body("accepted", equalTo(true));

        // a TAMPERED body under the same signature does NOT verify → 401 (constant-time compare must fail)
        post(provisionedEndpoint, "t=" + ts + ",v1=" + good, null, body.replace("1000", "9999"))
                .post("/api/webhook-signing-demo/endpoints/" + provisionedEndpoint + "/receive")
                .then().statusCode(401).body("code", equalTo("WEBHOOK_SIGNATURE_INVALID"));

        // a BODY-ONLY MAC (no timestamp prefix) MUST be rejected — the signed input always includes ts
        String bodyOnly = hexSignNoPrefix(secret, body);
        post(provisionedEndpoint, "t=" + ts + ",v1=" + bodyOnly, null, body)
                .post("/api/webhook-signing-demo/endpoints/" + provisionedEndpoint + "/receive")
                .then().statusCode(401).body("code", equalTo("WEBHOOK_SIGNATURE_INVALID"));
    }

    private String hexSignNoPrefix(String secretHex, String body) {
        try {
            byte[] secret = HexFormat.of().parseHex(secretHex);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ── WHSIGN-TIMESTAMP-001 ─────────────────────────────────────────────────

    @Test
    @Tag("WEBHOOK_SIGNING")
    @Tag("WHSIGN-TIMESTAMP-001")
    void timestampOutsideToleranceWindowRejected() {
        String secret = provision();
        String body = "{\"event\":\"ping\"}";

        // a timestamp 10 minutes in the past is outside the ±300s window → 400 STALE (even with a valid MAC)
        long staleTs = Instant.now().getEpochSecond() - 600;
        String sig = hexSign(secret, staleTs, body);
        post(provisionedEndpoint, "t=" + staleTs + ",v1=" + sig, null, body)
                .post("/api/webhook-signing-demo/endpoints/" + provisionedEndpoint + "/receive")
                .then().statusCode(400).body("code", equalTo("WEBHOOK_TIMESTAMP_STALE"));

        // a fresh timestamp within the window verifies → 200
        long freshTs = Instant.now().getEpochSecond();
        String freshSig = hexSign(secret, freshTs, body);
        post(provisionedEndpoint, "t=" + freshTs + ",v1=" + freshSig, null, body)
                .post("/api/webhook-signing-demo/endpoints/" + provisionedEndpoint + "/receive")
                .then().statusCode(200);
    }

    // ── WHSIGN-REPLAY-001 ────────────────────────────────────────────────────

    @Test
    @Tag("WEBHOOK_SIGNING")
    @Tag("WHSIGN-REPLAY-001")
    void replayedDeliveryWithinWindowRejected() {
        String secret = provision();
        long ts = Instant.now().getEpochSecond();
        String body = "{\"event\":\"charge.succeeded\"}";
        String sig = hexSign(secret, ts, body);
        String eventId = "evt-" + UUID.randomUUID();
        String header = "t=" + ts + ",v1=" + sig;

        // first delivery with this event-id → 200
        post(provisionedEndpoint, header, eventId, body)
                .post("/api/webhook-signing-demo/endpoints/" + provisionedEndpoint + "/receive")
                .then().statusCode(200).body("accepted", equalTo(true));

        // an identical, still-fresh REPLAY (same event-id) → 409 EVENT_REPLAYED
        post(provisionedEndpoint, header, eventId, body)
                .post("/api/webhook-signing-demo/endpoints/" + provisionedEndpoint + "/receive")
                .then().statusCode(409).body("code", equalTo("WEBHOOK_EVENT_REPLAYED"));
    }

    // ── WHSIGN-SECRET-001 ────────────────────────────────────────────────────

    @Test
    @Tag("WEBHOOK_SIGNING")
    @Tag("WHSIGN-SECRET-001")
    void perEndpointSecret_rotationOverlapAcceptsBoth_andSecretNeverInErrorBody() {
        String v1Secret = provision();
        String endpoint = provisionedEndpoint;
        long ts = Instant.now().getEpochSecond();
        String body = "{\"event\":\"rotated\"}";

        // a signature from a DIFFERENT endpoint's secret must NOT verify here (per-endpoint isolation)
        String foreignSecret = provision();          // also moves provisionedEndpoint — restore below
        String foreignSig = hexSign(foreignSecret, ts, body);
        post(endpoint, "t=" + ts + ",v1=" + foreignSig, null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(401)
                // the error body NEVER echoes any secret material
                .body(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(v1Secret)))
                .body(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(foreignSecret)));

        // rotate: current=v2, previous=v1 (overlap of EXACTLY 2)
        String v2Secret = given().contentType("application/json")
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/rotate")
                .then().statusCode(200).body("rotated", equalTo(true)).extract().path("secret");

        // DURING overlap both the NEW (v2) and the PREVIOUS (v1) signatures verify → 200
        long t2 = Instant.now().getEpochSecond();
        post(endpoint, "t=" + t2 + ",v1=" + hexSign(v2Secret, t2, body), null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(200);
        post(endpoint, "t=" + t2 + ",v1=" + hexSign(v1Secret, t2, body), null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(200);

        // rotate again → previous becomes v2, v1 is DROPPED past the overlap → v1 now 401
        String v3Secret = given().contentType("application/json")
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/rotate")
                .then().statusCode(200).extract().path("secret");
        long t3 = Instant.now().getEpochSecond();
        post(endpoint, "t=" + t3 + ",v1=" + hexSign(v3Secret, t3, body), null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(200);
        post(endpoint, "t=" + t3 + ",v1=" + hexSign(v2Secret, t3, body), null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(200);
        post(endpoint, "t=" + t3 + ",v1=" + hexSign(v1Secret, t3, body), null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(401).body("code", equalTo("WEBHOOK_SIGNATURE_INVALID"));
    }

    // ── WHSIGN-HEADER-001 ────────────────────────────────────────────────────

    @Test
    @Tag("WEBHOOK_SIGNING")
    @Tag("WHSIGN-HEADER-001")
    void malformedOrVersionUnknownHeaderRejected() {
        String secret = provision();
        String endpoint = provisionedEndpoint;
        long ts = Instant.now().getEpochSecond();
        String body = "{\"event\":\"hdr\"}";
        String validHex = hexSign(secret, ts, body);

        // missing header entirely → 400 MALFORMED
        post(endpoint, null, null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(400).body("code", equalTo("WEBHOOK_SIGNATURE_MALFORMED"));

        // garbage (no key=value structure) → 400 MALFORMED
        post(endpoint, "not-a-signature", null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(400).body("code", equalTo("WEBHOOK_SIGNATURE_MALFORMED"));

        // missing the timestamp field → 400 MALFORMED
        post(endpoint, "v1=" + validHex, null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(400).body("code", equalTo("WEBHOOK_SIGNATURE_MALFORMED"));

        // an UNKNOWN scheme token (future-version gate) → 400 MALFORMED
        post(endpoint, "t=" + ts + ",v9=" + validHex, null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(400).body("code", equalTo("WEBHOOK_SIGNATURE_MALFORMED"));

        // the well-formed t=..,v1=.. header verifies → 200
        post(endpoint, "t=" + ts + ",v1=" + validHex, null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(200);
    }

    // ── WHSIGN-VERIFY-001 ────────────────────────────────────────────────────

    @Test
    @Tag("WEBHOOK_SIGNING")
    @Tag("WHSIGN-VERIFY-001")
    void verificationOrderIsFixed_malformedBeforeStaleBeforeBadMacBeforeReplay() {
        String secret = provision();
        String endpoint = provisionedEndpoint;
        String body = "{\"event\":\"order\"}";

        // (1) a malformed header short-circuits at step 1 → 400 MALFORMED, even with a stale timestamp
        //     embedded in the (broken) header — parse failure precedes the window check.
        post(endpoint, "garbage", null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(400).body("code", equalTo("WEBHOOK_SIGNATURE_MALFORMED"));

        // (2) a STALE timestamp is caught at step 2 → 400 STALE BEFORE the MAC compare, so even a
        //     deliberately WRONG v1 still surfaces STALE (timestamp precedes signature).
        long staleTs = Instant.now().getEpochSecond() - 600;
        post(endpoint, "t=" + staleTs + ",v1=" + "00".repeat(32), null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(400).body("code", equalTo("WEBHOOK_TIMESTAMP_STALE"));

        // (3) a FRESH timestamp with a wrong MAC reaches step 3 → 401 BAD_MAC
        long freshTs = Instant.now().getEpochSecond();
        post(endpoint, "t=" + freshTs + ",v1=" + "11".repeat(32), null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(401).body("code", equalTo("WEBHOOK_SIGNATURE_INVALID"));

        // (4) a fully valid request with an event-id reaches step 4; a repeat → 409 REPLAYED.
        String good = hexSign(secret, freshTs, body);
        String eventId = "evt-" + UUID.randomUUID();
        post(endpoint, "t=" + freshTs + ",v1=" + good, eventId, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(200);
        post(endpoint, "t=" + freshTs + ",v1=" + good, eventId, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive")
                .then().statusCode(409).body("code", equalTo("WEBHOOK_EVENT_REPLAYED"));
    }

    // ── WHSIGN-OBSERVABILITY-001 ─────────────────────────────────────────────

    @Test
    @Tag("WEBHOOK_SIGNING")
    @Tag("WHSIGN-OBSERVABILITY-001")
    void exposesExactlyThreeBoundedLabelMeters() {
        String secret = provision();
        String endpoint = provisionedEndpoint;
        String body = "{\"event\":\"metered\"}";

        // (a) issued counter — drive the outbound /issue path
        given().contentType("application/json").body(body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/issue")
                .then().statusCode(200).body("issued", equalTo(true));

        // (b) verification failures across the bounded reason enum {malformed, stale, bad_mac}
        post(endpoint, "garbage", null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive").then().statusCode(400); // malformed
        long staleTs = Instant.now().getEpochSecond() - 600;
        post(endpoint, "t=" + staleTs + ",v1=" + "00".repeat(32), null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive").then().statusCode(400); // stale
        long freshTs = Instant.now().getEpochSecond();
        post(endpoint, "t=" + freshTs + ",v1=" + "11".repeat(32), null, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive").then().statusCode(401); // bad_mac

        // (c) replay rejected counter
        String good = hexSign(secret, freshTs, body);
        String eventId = "evt-" + UUID.randomUUID();
        post(endpoint, "t=" + freshTs + ",v1=" + good, eventId, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive").then().statusCode(200);
        post(endpoint, "t=" + freshTs + ",v1=" + good, eventId, body)
                .post("/api/webhook-signing-demo/endpoints/" + endpoint + "/receive").then().statusCode(409); // replay

        // exactly the 3 canonical meters exist
        assertThat(registry.find(WebhookSigningMetrics.ISSUED).counter()).isNotNull();
        assertThat(registry.find(WebhookSigningMetrics.VERIFY_FAILURE).counter()).isNotNull();
        assertThat(registry.find(WebhookSigningMetrics.REPLAY_REJECTED).counter()).isNotNull();

        // bounded labels — issued carries ONLY {endpoint}; verify-failure ONLY {reason} from the closed
        // enum; replay-rejected carries NO labels at all. NO secret / signature / event_id / body dim.
        assertIssuedTagKeyOnly(WebhookSigningMetrics.ISSUED, WebhookSigningMetrics.TAG_ENDPOINT);
        assertReasonLabels(WebhookSigningMetrics.VERIFY_FAILURE, WebhookSigningMetrics.TAG_REASON,
                Set.of("malformed", "stale", "bad_mac"));
        assertNoLabels(WebhookSigningMetrics.REPLAY_REJECTED);

        // each bounded value we exercised was recorded with count >= 1
        assertCountAtLeastOne(WebhookSigningMetrics.ISSUED, WebhookSigningMetrics.TAG_ENDPOINT, endpoint);
        assertCountAtLeastOne(WebhookSigningMetrics.VERIFY_FAILURE, WebhookSigningMetrics.TAG_REASON, "malformed");
        assertCountAtLeastOne(WebhookSigningMetrics.VERIFY_FAILURE, WebhookSigningMetrics.TAG_REASON, "stale");
        assertCountAtLeastOne(WebhookSigningMetrics.VERIFY_FAILURE, WebhookSigningMetrics.TAG_REASON, "bad_mac");
        var replay = registry.find(WebhookSigningMetrics.REPLAY_REJECTED).counter();
        assertThat(replay).isNotNull();
        assertThat(replay.count()).isGreaterThanOrEqualTo(1.0);
    }

    /** issued counter — every meter carries EXACTLY one tag (the endpoint dim), never a secret. */
    private void assertIssuedTagKeyOnly(String name, String onlyKey) {
        for (Meter m : registry.find(name).meters()) {
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                assertThat(t.getKey()).as("%s tag key bounded", name).isEqualTo(onlyKey);
            }
        }
    }

    /** verify-failure — every meter carries EXACTLY its reason dim, value from the closed enum. */
    private void assertReasonLabels(String name, String onlyKey, Set<String> allowed) {
        for (Meter m : registry.find(name).meters()) {
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                assertThat(t.getKey()).as("%s tag key bounded", name).isEqualTo(onlyKey);
                assertThat(allowed).as("%s value from closed set", name).contains(t.getValue());
            }
        }
    }

    /** replay-rejected — NO labels at all (the spec forbids an event_id dimension). */
    private void assertNoLabels(String name) {
        for (Meter m : registry.find(name).meters()) {
            assertThat(m.getId().getTags()).as("%s carries no labels", name).isEmpty();
        }
    }

    private void assertCountAtLeastOne(String name, String key, String value) {
        var counter = registry.find(name).tag(key, value).counter();
        assertThat(counter).as("%s{%s=%s} recorded", name, key, value).isNotNull();
        assertThat(counter.count()).as("%s{%s=%s} count", name, key, value).isGreaterThanOrEqualTo(1.0);
    }
}
