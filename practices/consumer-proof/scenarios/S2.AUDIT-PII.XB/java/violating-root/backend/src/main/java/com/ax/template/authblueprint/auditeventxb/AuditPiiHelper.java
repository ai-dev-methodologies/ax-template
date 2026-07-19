package com.ax.template.authblueprint.auditeventxb;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Scenario-local copy of the catalog's real
 * {@code com.ax.template.authblueprint.common.AuditPiiHelper#piiHash}
 * (mirrored, not imported, so this fixture tree compiles standalone —
 * same isolation convention as the sibling S3.b2b-admin scenario copying
 * UserRole.java). Hashes a PII identifier to a short stable correlation
 * token instead of ever letting the raw value cross the BE->FE boundary.
 */
public final class AuditPiiHelper {

    private AuditPiiHelper() {}

    public static String piiHash(String value) {
        if (value == null || value.isBlank()) return "(none)";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
