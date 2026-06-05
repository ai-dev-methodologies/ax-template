package com.ax.template.authblueprint.caching;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * CACHE-ETAG-001 — strong ETag validator derived from the representation bytes.
 *
 * A strong validator is the quoted hex SHA-256 of the body. {@link #matches} implements the
 * If-None-Match comparison (RFC 7232 §3.2): a request whose If-None-Match equals the current ETag
 * yields 304; any other value (including a stale ETag after mutation) yields the full 200 body.
 * Spec: specs/caching-l0.yaml#CACHE-ETAG-001 (RFC 9110 §8.8.3 / RFC 7232).
 */
public final class EtagSupport {

    private EtagSupport() {}

    /** Strong validator: the SHA-256 hex of the body, double-quoted (e.g. "9f86d0...". */
    public static String strongEtag(byte[] body) {
        try {
            String hex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
            return "\"" + hex + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String strongEtag(String body) {
        return strongEtag(body.getBytes(StandardCharsets.UTF_8));
    }

    /** RFC 7232 §3.2 — true iff the client's If-None-Match matches the current ETag (→ 304). */
    public static boolean matches(String ifNoneMatch, String currentEtag) {
        if (ifNoneMatch == null || currentEtag == null) {
            return false;
        }
        if ("*".equals(ifNoneMatch.trim())) {
            return true;
        }
        for (String candidate : ifNoneMatch.split(",")) {
            String c = candidate.trim();
            if (c.startsWith("W/")) {
                c = c.substring(2).trim();
            }
            if (c.equals(currentEtag)) {
                return true;
            }
        }
        return false;
    }
}
