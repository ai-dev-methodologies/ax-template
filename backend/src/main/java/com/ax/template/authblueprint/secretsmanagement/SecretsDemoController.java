package com.ax.template.authblueprint.secretsmanagement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * secrets-management-l0 reference workload — a thin demo surface that triggers each item's contract
 * for black-box RestAssured assertions. The principal is the cache/owner tenant; the secret value is
 * NEVER echoed (every response carries presence/ciphertext/masked forms only).
 *
 * <ul>
 *   <li>POST /provision — create a secret granted to a set of principals (SOURCE/ENCRYPTION/ACCESS);</li>
 *   <li>GET /config-status — presence booleans only, never values (SOURCE-001);</li>
 *   <li>POST /source/validate — fail-fast on a config literal (SOURCE-001);</li>
 *   <li>GET /{id}/at-rest — the persisted ciphertext form, proving no plaintext at rest (ENCRYPTION-001);</li>
 *   <li>GET /{id}/read — least-privilege read; 403 + audit on denial (ACCESS-001);</li>
 *   <li>GET /{id}/leak-probe — deliberately interpolates the SecretValue → proves masking (NO-LOG-001);</li>
 *   <li>POST /{id}/rotate + POST /{id}/verify — 2-version overlap then retired (ROTATION-001);</li>
 *   <li>POST /{id}/revoke + POST /{id}/destroy (LIFECYCLE-001).</li>
 * </ul>
 *
 * Spec: specs/secrets-management-l0.yaml.
 */
@RestController
@RequestMapping("/api/secrets-demo")
public class SecretsDemoController {

    private final SecretService service;
    private final SecretSource source;

    public SecretsDemoController(SecretService service, SecretSource source) {
        this.service = service;
        this.source = source;
    }

    public record ProvisionRequest(String secretId, String value, Set<String> grantedPrincipals) {}
    public record SecretInput(String value) {}
    public record SourceValidateRequest(String propertyName, String configuredValue) {}

    /** SOURCE/ENCRYPTION/ACCESS — provision a secret. Returns presence + version, NEVER the value. */
    @PostMapping("/provision")
    public ResponseEntity<Map<String, Object>> provision(@RequestBody ProvisionRequest req, Authentication auth) {
        Set<String> grants = req.grantedPrincipals() == null || req.grantedPrincipals().isEmpty()
                ? Set.of(auth.getName())
                : req.grantedPrincipals();
        SecretRecord rec = service.create(req.secretId(), SecretValue.of(req.value()), grants);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "secretId", rec.secretId(),
                "version", rec.currentVersion(),
                "provisioned", true));
    }

    /** SOURCE-001 — presence booleans only, never resolved values. */
    @GetMapping("/config-status")
    public ResponseEntity<Map<String, Boolean>> configStatus(@RequestParam(name = "keys") String[] keys) {
        return ResponseEntity.ok(source.presenceStatus(keys));
    }

    /** SOURCE-001 — fail-fast: a real literal in config is rejected (→ 500), the placeholder is OK. */
    @PostMapping("/source/validate")
    public ResponseEntity<Map<String, Object>> validateSource(@RequestBody SourceValidateRequest req) {
        source.requireExternalized(req.propertyName(), req.configuredValue()); // throws → 500 on a literal
        return ResponseEntity.ok(Map.of("externalized", true));
    }

    /** ENCRYPTION-001 — the AT-REST form is ciphertext (base64), proving no plaintext column exists. */
    @GetMapping("/{secretId}/at-rest")
    public ResponseEntity<Map<String, Object>> atRest(@PathVariable String secretId, Authentication auth) {
        // read enforces the grant; the at-rest bytes are exposed only as ciphertext, never plaintext
        service.read(secretId, auth.getName());
        SecretRecord rec = requireOwned(secretId, auth);
        return ResponseEntity.ok(Map.of(
                "secretId", secretId,
                "ciphertextB64", rec.current().ciphertextB64(),
                "encryptedAtRest", true));
    }

    /**
     * ENCRYPTION-001 — a secret fetch over a non-TLS connector is rejected → 400. In production the
     * connector terminates TLS; the test simulates a non-TLS hop with an explicit header so the
     * contract is exercised black-box.
     */
    @GetMapping("/{secretId}/read")
    public ResponseEntity<Map<String, Object>> read(
            @PathVariable String secretId,
            @RequestParam(name = "transport", required = false, defaultValue = "tls") String transport,
            Authentication auth) {
        if (!"tls".equalsIgnoreCase(transport)) {
            throw new SecretException(SecretException.Kind.NON_TLS,
                    "Secret retrieval is permitted only over TLS.");
        }
        SecretValue value = service.read(secretId, auth.getName()); // 403 + audit if not granted
        // The READ response NEVER echoes the value — only that the read succeeded + its length.
        return ResponseEntity.ok(Map.of(
                "secretId", secretId,
                "read", true,
                "valueLength", value.reveal().length()));
    }

    /**
     * NO-LOG-001 — deliberately interpolate the SecretValue into a string AND serialize it. Both must
     * yield the mask, proving the wrapper cannot leak via the two accidental egress paths.
     */
    @GetMapping("/{secretId}/leak-probe")
    public ResponseEntity<Map<String, Object>> leakProbe(@PathVariable String secretId, Authentication auth) {
        service.read(secretId, auth.getName());                  // enforce grant
        SecretValue wrapper = SecretValue.of("super-secret-probe-value");
        String interpolated = "secret=" + wrapper;               // accidental concatenation
        return ResponseEntity.ok(Map.of(
                "interpolated", interpolated,                    // must be "secret=****"
                "serialized", wrapper));                         // must serialize to "****"
    }

    /** ROTATION-001 — rotate produces a new version; previous stays valid during overlap. */
    @PostMapping("/{secretId}/rotate")
    public ResponseEntity<Map<String, Object>> rotate(
            @PathVariable String secretId, @RequestBody SecretInput req, Authentication auth) {
        SecretRecord rec = service.rotate(secretId, SecretValue.of(req.value()), auth.getName());
        return ResponseEntity.ok(Map.of(
                "secretId", secretId,
                "version", rec.currentVersion(),
                "previousVersion", rec.previousVersion()));
    }

    /** ROTATION-001 — accept current OR (during overlap) previous; a retired version → 401. */
    @PostMapping("/{secretId}/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @PathVariable String secretId, @RequestBody SecretInput req, Authentication auth) {
        service.verifyPresented(secretId, auth.getName(), SecretValue.of(req.value())); // 401 if retired
        return ResponseEntity.ok(Map.of("secretId", secretId, "accepted", true));
    }

    /** LIFECYCLE-001 — immediate revocation; subsequent reads fail-closed (401 SECRET_REVOKED). */
    @PostMapping("/{secretId}/revoke")
    public ResponseEntity<Map<String, Object>> revoke(@PathVariable String secretId, Authentication auth) {
        service.revoke(secretId, auth.getName());
        return ResponseEntity.ok(Map.of("secretId", secretId, "revoked", true));
    }

    /** LIFECYCLE-001 — destroy; a destroyed secret is unrecoverable (subsequent reads → 404). */
    @PostMapping("/{secretId}/destroy")
    public ResponseEntity<Map<String, Object>> destroy(@PathVariable String secretId, Authentication auth) {
        service.destroy(secretId, auth.getName());
        return ResponseEntity.ok(Map.of("secretId", secretId, "destroyed", true));
    }

    /**
     * Provision with a configurable TTL (LIFECYCLE-001 expiry path). {@code ttlSeconds} of 0 means
     * "already expired" so the expiry contract can be exercised deterministically.
     */
    @PostMapping("/provision-ttl")
    public ResponseEntity<Map<String, Object>> provisionTtl(
            @RequestBody ProvisionRequest req,
            @RequestParam(name = "ttlSeconds") long ttlSeconds,
            Authentication auth) {
        Set<String> grants = req.grantedPrincipals() == null || req.grantedPrincipals().isEmpty()
                ? Set.of(auth.getName())
                : req.grantedPrincipals();
        service.create(req.secretId(), SecretValue.of(req.value()), grants,
                java.time.Duration.ofSeconds(ttlSeconds));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("secretId", req.secretId(), "provisioned", true));
    }

    /** The provisioning principal is always granted, so {@link #atRest} can re-read the record. */
    private SecretRecord requireOwned(String secretId, Authentication auth) {
        // read() above already enforced the grant + audited; re-resolve the ciphertext shape here.
        // A separate accessor avoids exposing the record on the service's read() return.
        return service.snapshotForOwner(secretId, auth.getName());
    }
}
