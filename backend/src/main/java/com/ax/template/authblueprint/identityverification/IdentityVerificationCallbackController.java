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
 * <p>Implements the callback surface required by
 * {@code specs/identity-verification-l0.yaml}:
 * <ul>
 *   <li>IDV-CALLBACK-001 — HMAC-SHA256 signature verification on
 *       {@code X-Identity-Signature} header; 401 on missing/invalid.</li>
 *   <li>IDV-CALLBACK-002 — valid HMAC returns 200 with
 *       {@code {status:"accepted", provider:"<name>"}}.</li>
 *   <li>IDV-CALLBACK-003 — response body never contains RRN
 *       (개인정보보호법 §24-1).</li>
 *   <li>IDV-PROVIDER-001 — PASS and KCB use the same response shape; per-provider
 *       payload decoding lives in the catalog-defined provider adapters
 *       (R3+ scope — this minimal controller only validates the HMAC envelope).</li>
 * </ul>
 *
 * <p>Persistence ({@code VerifiedIdentity} entity, repository, audit publisher,
 * admin list endpoint) is intentionally NOT included in this minimal
 * implementation. The catalog ships the contract surface; fork-receivers wire
 * persistence per their data store. The minimal controller exists so
 * {@code ./gradlew testIdentityVerification} is a binary pass/fail signal for
 * the HMAC envelope contract — the foundation every adapter relies on.
 *
 * <p>Security: SecurityConfig permits {@code /api/identity-verification/callback/**}
 * because authentication is via the provider HMAC signature, not a user JWT.
 *
 * @see WebhookReceiver (sibling pattern under integration/)
 * @see PaymentCallbackController (sibling pattern under payment/)
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

    public IdentityVerificationCallbackController(
            @Value("${ax.identity-verification.pass.secret:}") String passSecret,
            @Value("${ax.identity-verification.kcb.secret:}") String kcbSecret) {
        this.providerSecrets = Map.of(
            "pass", passSecret.getBytes(StandardCharsets.UTF_8),
            "kcb",  kcbSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    @PostMapping(value = "/{provider}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleCallback(
            @PathVariable String provider,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signatureHeader,
            @RequestBody byte[] rawBody) {

        byte[] secret = providerSecrets.get(provider);
        if (secret == null || secret.length == 0) {
            // Unknown provider — fork-receivers register additional providers via
            // ax.identity-verification.<provider>.secret. IDV-PROVIDER-002 (R3+):
            // surface as 400 with RFC 7807 ProblemDetail.
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            pd.setTitle("Unknown identity verification provider");
            pd.setDetail("provider=" + provider);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
        }

        if (!verifyHmac(signatureHeader, rawBody, secret)) {
            log.warn("Identity verification callback rejected: HMAC mismatch (provider={})", provider);
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
            pd.setTitle("Identity verification signature failed");
            // IDV-CALLBACK-003: error body must NOT echo any payload field; only
            // a stable failure reason. Avoid leaking provider-specific names.
            pd.setDetail("HMAC verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
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
