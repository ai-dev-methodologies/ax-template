package com.ax.template.authblueprint.webhooksigning;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * webhook-signing-l0 reference workload — a thin INBOUND signed-webhook receiver (distinct from the
 * OUTBOUND {@code webhook} domain which signs+emits). The signing secret is NEVER echoed in any
 * response except the one-time provision/rotate handout a real fork-receiver would surface in an admin
 * console; verification responses carry boolean/code forms only (WHSIGN-SECRET-001).
 *
 * <ul>
 *   <li>POST /endpoints/{endpoint}/provision — generate + return the endpoint's 256-bit secret ONCE;</li>
 *   <li>POST /endpoints/{endpoint}/rotate — rotate (overlap of 2), return the new secret ONCE;</li>
 *   <li>POST /endpoints/{endpoint}/issue — OUTBOUND: sign the raw body, return the
 *       {@code Webhook-Signature} header (drives WHSIGN-OBSERVABILITY-001 issued counter);</li>
 *   <li>POST /endpoints/{endpoint}/receive — INBOUND: run the WHSIGN-VERIFY-001 four-step contract over
 *       the EXACT raw bytes BEFORE any deserialization; 2xx only when all checks pass.</li>
 * </ul>
 *
 * Spec: specs/webhook-signing-l0.yaml.
 */
@RestController
@RequestMapping("/api/webhook-signing-demo")
public class WebhookSigningDemoController {

    static final String SIGNATURE_HEADER = "Webhook-Signature";
    static final String EVENT_ID_HEADER = "Webhook-Id";

    private final SigningSecretStore secretStore;
    private final InboundSignatureVerifier verifier;
    private final WebhookSigningMetrics metrics;

    public WebhookSigningDemoController(SigningSecretStore secretStore,
                                        InboundSignatureVerifier verifier,
                                        WebhookSigningMetrics metrics) {
        this.secretStore = secretStore;
        this.verifier = verifier;
        this.metrics = metrics;
    }

    /** WHSIGN-SECRET-001 — provision a fresh per-endpoint ≥256-bit secret; the hex is returned ONCE. */
    @PostMapping("/endpoints/{endpoint}/provision")
    public ResponseEntity<Map<String, Object>> provision(@PathVariable String endpoint) {
        String secretHex = secretStore.provision(endpoint);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("endpoint", endpoint, "secret", secretHex, "provisioned", true));
    }

    /** WHSIGN-SECRET-001 — rotate (overlap of 2 active secrets); the NEW hex is returned ONCE. */
    @PostMapping("/endpoints/{endpoint}/rotate")
    public ResponseEntity<Map<String, Object>> rotate(@PathVariable String endpoint) {
        String secretHex = secretStore.rotate(endpoint);
        return ResponseEntity.ok(Map.of("endpoint", endpoint, "secret", secretHex, "rotated", true));
    }

    /**
     * WHSIGN-OBSERVABILITY-001 (issued counter) + the outbound side of WHSIGN-HMAC-001 — sign the raw
     * body with the endpoint's CURRENT secret and return the structured header value. Convenience so a
     * caller without the raw secret can still obtain a valid signature for a legitimate delivery.
     */
    @PostMapping("/endpoints/{endpoint}/issue")
    public ResponseEntity<Map<String, Object>> issue(@PathVariable String endpoint,
                                                     @RequestBody(required = false) String rawBody) {
        var secrets = secretStore.activeSecrets(endpoint);
        if (secrets.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("endpoint", endpoint, "error", "unknown endpoint"));
        }
        long ts = Instant.now().getEpochSecond();
        byte[] body = rawBody == null ? new byte[0] : rawBody.getBytes(StandardCharsets.UTF_8);
        String hex = InboundSignatureVerifier.hexSign(secrets.get(0), ts, body);
        metrics.issued(endpoint);
        return ResponseEntity.ok(Map.of(
                "endpoint", endpoint,
                "header", "t=" + ts + ",v1=" + hex,
                "issued", true));
    }

    /**
     * WHSIGN-VERIFY-001 — the inbound receiver. Reads the EXACT raw bytes ({@code byte[]} — no
     * deserialization), runs the fixed four-step verification, and only on success "acts on" the body
     * (here: returns accepted). Any failure surfaces as the spec's distinct 400/401/409 via
     * {@link WebhookSigningAdvice}.
     */
    @PostMapping("/endpoints/{endpoint}/receive")
    public ResponseEntity<Map<String, Object>> receive(
            @PathVariable String endpoint,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = EVENT_ID_HEADER, required = false) String eventId,
            @RequestBody(required = false) String rawBody) {
        byte[] body = rawBody == null ? new byte[0] : rawBody.getBytes(StandardCharsets.UTF_8);
        verifier.verify(endpoint, signature, body, eventId,
                Instant.now().getEpochSecond(), System.nanoTime());
        // verified — safe to act on the body now (WHSIGN-VERIFY-001: never before this point)
        return ResponseEntity.ok(Map.of("endpoint", endpoint, "accepted", true, "bytes", body.length));
    }
}
