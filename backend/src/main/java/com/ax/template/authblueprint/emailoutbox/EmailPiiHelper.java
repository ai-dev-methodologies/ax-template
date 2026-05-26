package com.ax.template.authblueprint.emailoutbox;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * R60 — PII handling helper for email-outbox audit + lastError storage.
 *
 * <p>Two responsibilities:
 * <ul>
 *   <li>{@link #recipientHash} — hash an email address to a short stable
 *       fingerprint suitable for audit logs / metrics labels. Avoids
 *       writing the raw email to log aggregators (ELK, Splunk, CloudWatch).
 *       SHA-256 hex truncated to 16 chars — collision risk is acceptable
 *       for an ops fingerprint; the goal is correlation, not identification.</li>
 *   <li>{@link #sanitizeReason} — scrub provider-thrown error strings
 *       BEFORE persisting them in {@code email_outbox.last_error}. Mirrors
 *       templates/L0/fork-receiver-kit/parse-error.ts#sanitizeStoredError
 *       but at the JVM side. Refuses to store raw RRN / mobile / JWT /
 *       Bearer / email / internal hostname even when the sender adapter
 *       throws an exception whose message embeds them.</li>
 * </ul>
 *
 * <p>Both methods are pure / static / no side effects.
 */
final class EmailPiiHelper {

    private EmailPiiHelper() {}

    /**
     * Truncated SHA-256 hex of the recipient. Returns "(none)" for null/blank.
     * Use in AUDIT log lines: {@code log.info("verb=X recipientHash={}", recipientHash(email))}.
     */
    static String recipientHash(String recipient) {
        if (recipient == null || recipient.isBlank()) return "(none)";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(recipient.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is guaranteed by every JVM; this path is unreachable.
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /**
     * Scrub a sender-adapter exception message before persisting in the
     * {@code last_error} column. Mirrors the render-layer scrubber but
     * applies at storage so the column itself never holds plain PII.
     *
     * <p>Patterns redacted:
     * <ul>
     *   <li>KR RRN — {@code \d{6}-\d{7}}</li>
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
    static String sanitizeReason(String raw) {
        if (raw == null) return null;
        String s = raw;
        s = s.replaceAll("\\d{6}-\\d{7}", "[REDACTED]");
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
