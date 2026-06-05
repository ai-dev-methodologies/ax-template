package com.ax.template.authblueprint.secretsmanagement;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * SECRET-ENCRYPTION-001 — the TEMPLATE KMS: an in-process KEK that generates and AES-GCM-wraps DEKs.
 * The KEK is created once at startup and is NEVER persisted nor exposed — it stands in for the
 * KMS-held key-encryption key. A fork-receiver replaces this whole bean with an AWS KMS / Vault
 * Transit adapter and the rest of the workload is unchanged (the {@link KmsClient} contract holds).
 *
 * <p>Wrapping uses AES-GCM-256 with a per-wrap random 96-bit nonce prepended to the ciphertext, so
 * the same DEK never wraps to the same bytes twice.
 *
 * <p>Spec: specs/secrets-management-l0.yaml#SECRET-ENCRYPTION-001.
 */
@Component
public class InMemoryKmsClient implements KmsClient {

    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;
    private static final int DEK_BITS = 256;

    private final SecretKey kek = generateAesKey();
    private final SecureRandom random = new SecureRandom();

    @Override
    public DataKey generateDataKey() {
        byte[] dek = generateAesKey().getEncoded();
        return new DataKey(dek, wrapWithKek(dek));
    }

    @Override
    public byte[] unwrap(byte[] wrappedDek) {
        return decryptWithKek(wrappedDek);
    }

    private byte[] wrapWithKek(byte[] dek) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, kek, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] ct = cipher.doFinal(dek);
            return ByteBuffer.allocate(nonce.length + ct.length).put(nonce).put(ct).array();
        } catch (Exception e) {
            throw new IllegalStateException("KEK wrap failed", e);
        }
    }

    private byte[] decryptWithKek(byte[] wrapped) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(wrapped);
            byte[] nonce = new byte[NONCE_BYTES];
            buf.get(nonce);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, kek, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(ct);
        } catch (Exception e) {
            throw new IllegalStateException("KEK unwrap failed", e);
        }
    }

    private static SecretKey generateAesKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(DEK_BITS);
            return kg.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException("AES key generation failed", e);
        }
    }

    /** Re-materialize a raw DEK as an AES key for the data-layer cipher (used by {@link EnvelopeCrypto}). */
    static SecretKey toAesKey(byte[] raw) {
        return new SecretKeySpec(raw, "AES");
    }
}
