package com.ax.template.authblueprint.correctionrefire;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * CRF-IDEMPOTENT-003 — SHA-256 lowercase hex digest of published content, used to detect a
 * no-op re-publish (identical content) versus a real correction (changed content). Mirrors the
 * {@code apikey.ApiKeyHasher} / {@code provisionalattestation.ContentHasher} SHA-256 pattern.
 */
final class ContentHasher {

    private static final HexFormat HEX = HexFormat.of();

    private ContentHasher() {}

    static String sha256Hex(String content) {
        if (content == null) {
            throw new IllegalArgumentException("content is required");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
