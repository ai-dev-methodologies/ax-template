package com.ax.template.authblueprint.requestvalidation;

import java.text.Normalizer;

/**
 * VALIDATION-SANITIZE-001 — the ONLY transforms applied to accepted input, as an EXPLICIT,
 * named, allowlist step (never an implicit silent mutation, never a denylist of "bad"
 * characters): Unicode NFC normalization + surrounding-whitespace trim.
 *
 * <p>This runs AFTER validation has already accepted the value — it is not a way to "clean"
 * malformed input into validity. Input that fails a declared constraint is rejected
 * upstream (reject-not-sanitize), it never reaches this normalizer.
 *
 * <p>Anchored to the OWASP Input Validation Cheat Sheet ("Allowlist validation is
 * appropriate for all input fields provided by the user").
 * Spec: specs/request-validation-l0.yaml#VALIDATION-SANITIZE-001.
 */
public final class NameNormalizer {

    private NameNormalizer() {}

    /** Allowlist transform: trim surrounding whitespace, then NFC-normalize. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        return Normalizer.normalize(raw.trim(), Normalizer.Form.NFC);
    }
}
