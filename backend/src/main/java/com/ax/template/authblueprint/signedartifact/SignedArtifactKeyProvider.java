package com.ax.template.authblueprint.signedartifact;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;

import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.UUID;

/**
 * SIGNED-ASYM-001 — the ONE asymmetric (EC P-256 / ES256) signing key for the signed-artifact
 * reference workload. The private key never leaves this bean (used only by
 * {@link SignedArtifactService#issue}); the PUBLIC key is distributable to every relying party
 * via {@link #publicJwkSet()} (the {@code /jwks} endpoint) — the third-party-verifiable posture
 * this spec exists to enforce (a symmetric HMAC secret could never be published this way).
 * Mirrors the {@code security.JwtConfig} RSA key-pair pattern, swapped to EC for ES256.
 */
@Component
public class SignedArtifactKeyProvider {

    private final ECKey ecKey;

    public SignedArtifactKeyProvider() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair keyPair = gen.generateKeyPair();
            this.ecKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
                .privateKey((ECPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate EC (P-256) key pair for signed-artifact", e);
        }
    }

    /** SIGNED-ALG-ALLOWLIST-001 — the verifier resolves the key from server config keyed by kid,
     *  never from the token's own header, so {@link #keyId()} is the ONLY valid signing key id. */
    public String keyId() {
        return ecKey.getKeyID();
    }

    /** The FULL key (private + public) — used ONLY by {@link SignedArtifactService#issue} to sign. */
    ECKey signingKey() {
        return ecKey;
    }

    /** The PUBLIC-only key — used by {@link SignedArtifactService#verify} and the {@code /jwks} endpoint. */
    ECKey verifyingKey() {
        return ecKey.toPublicJWK();
    }

    /** SIGNED-ASYM-001 — the published verifying-key set (public-only JWKS, no issuer secret). */
    public JWKSet publicJwkSet() {
        return new JWKSet(verifyingKey());
    }
}
