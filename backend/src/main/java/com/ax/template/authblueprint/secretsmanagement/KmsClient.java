package com.ax.template.authblueprint.secretsmanagement;

/**
 * SECRET-ENCRYPTION-001 — the KMS SPI for envelope encryption. The KEY-ENCRYPTION KEY (KEK) never
 * leaves the KMS: a fork-receiver swaps {@link InMemoryKmsClient} for an AWS KMS / Vault Transit /
 * GCP KMS adapter behind this same two-method contract. Per-secret DATA-ENCRYPTION KEYs (DEKs) are
 * generated, then WRAPPED by the KEK so only the KMS can unwrap them.
 *
 * <p>Spec: specs/secrets-management-l0.yaml#SECRET-ENCRYPTION-001.
 */
public interface KmsClient {

    /** Generate a fresh 256-bit DEK and return BOTH its plaintext (use-then-discard) and KEK-wrapped form. */
    DataKey generateDataKey();

    /** Unwrap a previously-wrapped DEK using the KEK held inside the KMS — plaintext DEK lives only in memory. */
    byte[] unwrap(byte[] wrappedDek);

    /** A generated data key: plaintext DEK (caller MUST discard after use) + the KEK-wrapped DEK to persist. */
    record DataKey(byte[] plaintext, byte[] wrapped) {}
}
