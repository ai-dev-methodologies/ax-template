package com.ax.template.authblueprint.secretsmanagement;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * SECRET-ENCRYPTION-001 — envelope encryption. Seals a {@link SecretValue} under a fresh
 * per-secret DEK (AES-GCM-256), where the DEK is itself KEK-wrapped by the {@link KmsClient}. The
 * persisted form ({@link EnvelopeEncryptedSecret}) carries ONLY ciphertext + the WRAPPED DEK —
 * never the plaintext secret and never the unwrapped DEK. Opening unwraps the DEK in-memory via the
 * KMS and decrypts; the plaintext lives only transiently inside {@link #open}.
 *
 * <p>Spec: specs/secrets-management-l0.yaml#SECRET-ENCRYPTION-001.
 */
@Component
public class EnvelopeCrypto {

    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;

    private final KmsClient kms;
    private final SecureRandom random = new SecureRandom();

    public EnvelopeCrypto(KmsClient kms) {
        this.kms = kms;
    }

    /** Seal plaintext: generate a DEK, AES-GCM encrypt, return {ciphertext, wrappedDek} — no plaintext kept. */
    public EnvelopeEncryptedSecret seal(SecretValue plaintext) {
        KmsClient.DataKey dataKey = kms.generateDataKey();
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, InMemoryKmsClient.toAesKey(dataKey.plaintext()),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] ct = cipher.doFinal(plaintext.reveal().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] envelope = ByteBuffer.allocate(nonce.length + ct.length).put(nonce).put(ct).array();
            return new EnvelopeEncryptedSecret(envelope, dataKey.wrapped());
        } catch (Exception e) {
            throw new IllegalStateException("envelope seal failed", e);
        } finally {
            java.util.Arrays.fill(dataKey.plaintext(), (byte) 0); // discard the plaintext DEK
        }
    }

    /** Open ciphertext: unwrap the DEK via KMS, AES-GCM decrypt → plaintext (in-memory only). */
    public SecretValue open(EnvelopeEncryptedSecret sealed) {
        byte[] dek = kms.unwrap(sealed.wrappedDek());
        try {
            ByteBuffer buf = ByteBuffer.wrap(sealed.ciphertext());
            byte[] nonce = new byte[NONCE_BYTES];
            buf.get(nonce);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, InMemoryKmsClient.toAesKey(dek),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return SecretValue.of(new String(cipher.doFinal(ct), java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("envelope open failed", e);
        } finally {
            java.util.Arrays.fill(dek, (byte) 0); // discard the unwrapped DEK
        }
    }
}
