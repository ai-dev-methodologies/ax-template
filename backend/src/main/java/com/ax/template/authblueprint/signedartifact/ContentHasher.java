package com.ax.template.authblueprint.signedartifact;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SIGNED-ASYM-001 — SHA-256 lowercase hex digest of the artifact content; the JWS signs OVER this
 * hash (not the raw content directly), so verification re-hashes the presented content and checks
 * it against the signed claim — a tampered content payload no longer matches.
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
