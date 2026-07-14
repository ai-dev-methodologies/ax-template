package com.ax.template.authblueprint.provisionalattestation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * PATT-FREEZE-003 — SHA-256 lowercase hex digest of the content attested at attest-time, mirroring
 * the {@code apikey.ApiKeyHasher} SHA-256 pattern. Binding a hash of what was attested makes a
 * post-attestation, out-of-band content drift detectable (the recomputed hash of the currently
 * stored content will no longer match the hash recorded at attestation).
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
