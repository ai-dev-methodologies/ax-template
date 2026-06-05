package com.ax.template.authblueprint.pagination;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * PAGE-CURSOR-001 — opaque, tamper-evident pagination cursor.
 *
 * A cursor encodes {@code {last_id, page_size}} + an HMAC-SHA256 signature, base64url-encoded, so it
 * is opaque to clients and cannot be forged or mutated to read outside the intended window. Decoding a
 * tampered cursor is rejected (constant-time signature comparison). A real deployment also folds in the
 * sort value + a tenant scope and rotates the key; this reference keeps a fixed demo key.
 * Spec: specs/pagination-l0.yaml#PAGE-CURSOR-001.
 */
public final class CursorCodec {

    private static final byte[] KEY = "ax-pagination-demo-hmac-key-v1".getBytes(StandardCharsets.UTF_8);

    private CursorCodec() {}

    public record Cursor(long lastId, int pageSize) {}

    public static String encode(Cursor c) {
        String payload = c.lastId() + ":" + c.pageSize();
        String token = payload + "." + hmac(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String token) {
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("PAGE-CURSOR-001: cursor is not valid base64url");
        }
        int dot = raw.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException("PAGE-CURSOR-001: malformed cursor");
        }
        String payload = raw.substring(0, dot);
        String sig = raw.substring(dot + 1);
        byte[] expected = hmac(payload).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, sig.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("PAGE-CURSOR-001: cursor signature invalid (tampered or wrong key)");
        }
        String[] parts = payload.split(":");
        return new Cursor(Long.parseLong(parts[0]), Integer.parseInt(parts[1]));
    }

    private static String hmac(String s) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(s.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
