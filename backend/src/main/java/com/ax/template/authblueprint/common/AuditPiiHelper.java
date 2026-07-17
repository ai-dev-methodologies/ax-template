package com.ax.template.authblueprint.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Cross-cutting PII handling helper for audit logs and stored-error columns.
 *
 * <p>R67 — promoted from {@code emailoutbox.AuditPiiHelper} to the backend
 * {@code common} package after the rule of three+ was satisfied: seven
 * modules (emailoutbox, activityfeed, notification, scheduledtask,
 * reportexport, webhook, auditlog) all wire through this helper for
 * R61 enforcement.
 *
 * <h2>Two responsibilities</h2>
 * <ul>
 *   <li>{@link #piiHash} — hash an arbitrary PII identifier (email,
 *       userId, phone, RRN) to a short stable correlation token suitable
 *       for audit logs / metrics labels. Avoids writing raw values to
 *       log aggregators (ELK, Splunk, CloudWatch). SHA-256 hex truncated
 *       to 16 chars — collision risk is acceptable for an ops
 *       fingerprint; the goal is correlation, not identification.
 *       Anchors R61 {@code audit-log-pii-hash-required}.</li>
 *   <li>{@link #sanitizeReason} — scrub a provider / job / adapter
 *       exception message BEFORE persisting it in any stored-error
 *       column. Mirrors
 *       {@code templates/L0/fork-receiver-kit/parse-error.ts#sanitizeStoredError}
 *       at the JVM side. Anchors R61
 *       {@code server-side-stored-error-sanitize}.</li>
 * </ul>
 *
 * <p>Both methods are pure / static / no side effects.
 */
public final class AuditPiiHelper {

    private AuditPiiHelper() {}

    /**
     * Truncated SHA-256 hex of an arbitrary PII identifier. Returns
     * {@code "(none)"} for null/blank. Use in AUDIT log lines as a stable
     * correlation token that contains no recoverable PII:
     *
     * <pre>
     * log.info("verb=X recipientHash={}", piiHash(email));
     * log.info("verb=Y callerHash={}",    piiHash(userId));
     * log.info("verb=Z phoneHash={}",     piiHash(phone));
     * </pre>
     */
    public static String piiHash(String value) {
        if (value == null || value.isBlank()) return "(none)";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is guaranteed by every JVM; this path is unreachable.
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /**
     * Scrub a provider / job / adapter exception message before persisting
     * it in a stored-error column ({@code last_error}, {@code error_message},
     * {@code failure_reason}). Mirrors the render-layer scrubber but
     * applies at storage so the column itself never holds plain PII.
     *
     * <p>Patterns redacted:
     * <ul>
     *   <li>KR RRN — {@code \d{6}-?\d{7}} (P3-48 — hyphen optional; a 13-digit
     *       un-hyphenated 주민등록번호 must not leak)</li>
     *   <li>KR mobile — {@code 01[016789]-?\d{3,4}-?\d{4}}</li>
     *   <li>JWT shape — {@code eyJ[A-Za-z0-9._-]{20,}}</li>
     *   <li>Bearer header value</li>
     *   <li>{@code sk-...} secret prefix</li>
     *   <li>{@code ghp_...} GitHub PAT</li>
     *   <li>email address</li>
     *   <li>IPv4</li>
     *   <li>{@code *.internal}, {@code *.local} hostnames</li>
     * </ul>
     */
    public static String sanitizeReason(String raw) {
        if (raw == null) return null;
        String s = raw;
        s = s.replaceAll("\\d{6}-?\\d{7}", "[REDACTED]");
        s = s.replaceAll("01[016789]-?\\d{3,4}-?\\d{4}", "[REDACTED]");
        s = s.replaceAll("eyJ[A-Za-z0-9._-]{20,}", "[REDACTED]");
        s = s.replaceAll("(?i)Bearer\\s+[A-Za-z0-9._-]+", "[REDACTED]");
        s = s.replaceAll("sk-[A-Za-z0-9._-]{10,}", "[REDACTED]");
        s = s.replaceAll("ghp_[A-Za-z0-9]{20,}", "[REDACTED]");
        s = s.replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[REDACTED]");
        s = s.replaceAll("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b", "[REDACTED]");
        s = s.replaceAll("[\\w-]+\\.internal\\b", "[REDACTED]");
        s = s.replaceAll("[\\w-]+\\.local\\b", "[REDACTED]");
        return s;
    }
}
