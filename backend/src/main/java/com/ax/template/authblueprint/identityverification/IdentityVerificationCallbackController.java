package com.ax.template.authblueprint.identityverification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Identity verification callback receiver (PASS / KCB).
 *
 * <p>R54 — full surface for {@code specs/identity-verification-l0.yaml}:
 * <ul>
 *   <li>IDV-CALLBACK-001 — HMAC-SHA256 envelope verification (this layer).</li>
 *   <li>IDV-CALLBACK-002 — {@link IdentityVerificationService#processCallback}
 *       persists a {@link VerifiedIdentity}; controller returns
 *       {@code {status:"accepted", provider:"<name>"}}.</li>
 *   <li>IDV-CALLBACK-003 — response body never contains RRN (the body is a
 *       fixed two-field map; the entity has no rrn column).</li>
 *   <li>IDV-PROVIDER-001/002 — provider lookup + canonical extraction in service.</li>
 *   <li>IDV-AUDIT-001 — every callback attempt funnels through the service for
 *       structured AuditLog publish (SUCCESS / HMAC_FAIL / UNKNOWN_PROVIDER /
 *       EXTRACTION_FAIL).</li>
 * </ul>
 *
 * <p>HMAC verification stays at the controller because it needs the raw body
 * byte-for-byte before any JSON parsing. On verification miss, the controller
 * notifies the service via {@link IdentityVerificationService#recordHmacFailure}
 * so the audit hook still fires.
 */
@RestController
@RequestMapping("/api/identity-verification/callback")
public class IdentityVerificationCallbackController {

    private static final Logger log =
        LoggerFactory.getLogger(IdentityVerificationCallbackController.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String SIGNATURE_HEADER = "X-Identity-Signature";

    private final Map<String, byte[]> providerSecrets;
    private final IdentityVerificationService service;

    public IdentityVerificationCallbackController(
            @Value("${ax.identity-verification.pass.secret:}") String passSecret,
            @Value("${ax.identity-verification.kcb.secret:}") String kcbSecret,
            IdentityVerificationService service) {
        this.providerSecrets = Map.of(
            "pass", passSecret.getBytes(StandardCharsets.UTF_8),
            "kcb",  kcbSecret.getBytes(StandardCharsets.UTF_8)
        );
        this.service = service;
    }

    @PostMapping(value = "/{provider}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleCallback(
            @PathVariable String provider,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signatureHeader,
            @RequestBody byte[] rawBody) {

        byte[] secret = providerSecrets.get(provider);
        if (secret == null || secret.length == 0) {
            // IDV-PROVIDER-002: unknown provider never reaches the service for
            // parsing, but the audit hook still fires so dashboards see the
            // rejected attempt.
            service.recordUnknownProvider(provider);
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            pd.setTitle("Unknown identity verification provider");
            pd.setDetail("provider=" + provider);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
        }

        if (!verifyHmac(signatureHeader, rawBody, secret)) {
            log.warn("Identity verification callback rejected: HMAC mismatch (provider={})", provider);
            service.recordHmacFailure(provider);
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
            pd.setTitle("Identity verification signature failed");
            // IDV-CALLBACK-003: error body MUST NOT echo any payload field;
            // only a stable failure reason.
            pd.setDetail("HMAC verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
        }

        try {
            service.processCallback(provider, rawBody);
        } catch (IdentityVerificationException ex) {
            // EXTRACTION_FAIL etc — service already audited.
            HttpStatus status = ex.reason().status();
            ProblemDetail pd = ProblemDetail.forStatus(status);
            pd.setTitle("Identity verification callback failed");
            pd.setDetail(ex.reason().name());
            return ResponseEntity.status(status).body(pd);
        }

        // IDV-CALLBACK-002 / IDV-PROVIDER-001: unified response shape across
        // providers. Body fields are intentionally minimal — no CI, no DI, no
        // user_name echoed back. IDV-CALLBACK-003: no RRN field anywhere.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "accepted");
        body.put("provider", provider);
        return ResponseEntity.ok(body);
    }

    private static boolean verifyHmac(String signatureHeader, byte[] rawBody, byte[] secret) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        String receivedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());
        byte[] receivedDigest;
        try {
            receivedDigest = HexFormat.of().parseHex(receivedHex);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        byte[] expectedDigest = computeHmac(rawBody, secret);
        // Constant-time comparison — IDV-CALLBACK-001 anchor pattern.
        return MessageDigest.isEqual(expectedDigest, receivedDigest);
    }

    private static byte[] computeHmac(byte[] data, byte[] secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 initialisation failed", ex);
        }
    }
}
