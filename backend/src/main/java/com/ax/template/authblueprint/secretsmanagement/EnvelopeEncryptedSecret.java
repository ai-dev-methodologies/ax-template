package com.ax.template.authblueprint.secretsmanagement;

import java.util.Base64;

/**
 * SECRET-ENCRYPTION-001 — the AT-REST representation of a secret version. Carries ONLY the AES-GCM
 * {@code ciphertext} and the KEK-{@code wrappedDek}; there is no field that ever holds the plaintext
 * secret or an unwrapped DEK. This is the only shape the persistence layer ever sees, which is why a
 * "plaintext-at-rest column" cannot exist by construction.
 *
 * <p>{@link #ciphertextB64()} exposes the stored bytes for the demo surface so a test can prove the
 * persisted form is ciphertext (not the plaintext), without ever revealing the secret.
 *
 * <p>Spec: specs/secrets-management-l0.yaml#SECRET-ENCRYPTION-001.
 */
public record EnvelopeEncryptedSecret(byte[] ciphertext, byte[] wrappedDek) {

    public String ciphertextB64() {
        return Base64.getEncoder().encodeToString(ciphertext);
    }
}
