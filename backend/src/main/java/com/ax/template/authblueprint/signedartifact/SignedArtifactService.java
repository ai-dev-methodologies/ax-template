package com.ax.template.authblueprint.signedartifact;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * signed-artifact-l0 sole orchestrator. {@link #issue} signs a canonical payload (subjectRef +
 * SHA-256 content-hash) with the ONE asymmetric EC/ES256 key (SIGNED-ASYM-001) — never HMAC.
 * {@link #verify} enforces SIGNED-ALG-ALLOWLIST-001: the alg + verifying key are resolved from
 * THIS service's server-side configuration keyed by {@code kid} — NEVER from the token's own
 * (attacker-controlled) {@code alg} header — so {@code alg:none} and an HS*-over-public-key
 * algorithm-confusion forgery are both rejected before any signature check runs. A verified
 * signature over a content-hash that no longer matches the presented content is reported as a
 * content mismatch (tamper), distinct from a bad signature.
 */
@Service
public class SignedArtifactService {

    /** SIGNED-ASYM-001 — the only algorithm this service ever signs or accepts. */
    private static final JWSAlgorithm SERVER_ALGORITHM = JWSAlgorithm.ES256;

    private final SignedArtifactRepository repository;
    private final SignedArtifactKeyProvider keyProvider;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SignedArtifactService(SignedArtifactRepository repository, SignedArtifactKeyProvider keyProvider,
                                 ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.keyProvider = keyProvider;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public SignedArtifact issue(String subjectRef, String content) {
        String contentHash = ContentHasher.sha256Hex(content);
        ObjectNode payloadNode = objectMapper.createObjectNode();
        payloadNode.put("subjectRef", subjectRef);
        payloadNode.put("contentHash", contentHash);

        JWSHeader header = new JWSHeader.Builder(SERVER_ALGORITHM).keyID(keyProvider.keyId()).build();
        JWSObject jwsObject = new JWSObject(header, new Payload(payloadNode.toString()));
        try {
            jwsObject.sign(new ECDSASigner(keyProvider.signingKey()));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign artifact", ex);
        }

        SignedArtifact artifact = SignedArtifact.issue(UUID.randomUUID(), subjectRef, contentHash,
            keyProvider.keyId(), SERVER_ALGORITHM.getName(), jwsObject.serialize(), Instant.now(clock));
        return repository.save(artifact);
    }

    /**
     * SIGNED-ALG-ALLOWLIST-001 — the caller presents the JWS compact serialization AND the
     * original content; verification succeeds only if (1) the alg is the one THIS server
     * configured for the resolved kid (never the token's own claim), (2) the signature verifies
     * against the published public key, and (3) the content's recomputed hash matches the hash
     * the signature covers.
     */
    @Transactional(readOnly = true)
    public void verify(String compactJws, String content) {
        // The Unsecured JWS (alg:none, RFC 7518 §3.6) carries an EMPTY signature segment, which
        // com.nimbusds.jose.JWSObject refuses to parse at all (it requires a non-empty JWS
        // signature) — so alg:none must be caught by inspecting the RAW header BEFORE handing the
        // token to JWSObject, or it would be misclassified as merely malformed (400) instead of
        // the SIGNED-ALG-ALLOWLIST-001 rejection (401) it actually is: an empty signature is
        // never a verified signature, full stop.
        if ("none".equalsIgnoreCase(headerAlgOf(compactJws))) {
            throw SignedArtifactException.unsupportedAlgorithm();
        }

        JWSObject jwsObject;
        try {
            jwsObject = JWSObject.parse(compactJws);
        } catch (ParseException ex) {
            throw SignedArtifactException.malformed();
        }

        String kid = jwsObject.getHeader().getKeyID();
        // The alg + verifying key are resolved from SERVER-SIDE config keyed by kid — this is
        // the ONLY kid this server ever issues, and its expected alg is fixed at SERVER_ALGORITHM.
        // The token's OWN alg claim is never trusted to select the verifier.
        if (kid == null || !keyProvider.keyId().equals(kid)) {
            throw SignedArtifactException.unknownKid();
        }
        Algorithm presentedAlg = jwsObject.getHeader().getAlgorithm();
        if (presentedAlg == null || !SERVER_ALGORITHM.equals(presentedAlg)) {
            // Catches alg:none (Algorithm.NONE) AND an HS256-over-public-key confusion forgery —
            // both present an alg other than the server's configured ES256 for this kid.
            throw SignedArtifactException.unsupportedAlgorithm();
        }

        boolean signatureValid;
        try {
            JWSVerifier verifier = new ECDSAVerifier(keyProvider.verifyingKey());
            signatureValid = jwsObject.verify(verifier);
        } catch (Exception ex) {
            throw SignedArtifactException.signatureInvalid();
        }
        if (!signatureValid) {
            throw SignedArtifactException.signatureInvalid();
        }

        String signedContentHash = readPayload(jwsObject).get("contentHash").asText();
        String presentedHash = ContentHasher.sha256Hex(content);
        if (!signedContentHash.equals(presentedHash)) {
            throw SignedArtifactException.contentMismatch();
        }
    }

    private tools.jackson.databind.JsonNode readPayload(JWSObject jwsObject) {
        try {
            return objectMapper.readTree(jwsObject.getPayload().toString());
        } catch (Exception ex) {
            throw SignedArtifactException.malformed();
        }
    }

    /** Reads {@code alg} straight off the raw base64url header segment — a structural check that
     *  works even on inputs {@link JWSObject#parse} itself would refuse (e.g. alg:none's empty
     *  signature segment). Returns null if the token isn't even 3 dot-separated segments or the
     *  header isn't valid base64url JSON — callers fall through to the normal parse/malformed path. */
    private String headerAlgOf(String compactJws) {
        String[] parts = compactJws.split("\\.", -1);
        if (parts.length != 3) {
            return null;
        }
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            return objectMapper.readTree(headerBytes).path("alg").asText(null);
        } catch (Exception ex) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public SignedArtifact getOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(SignedArtifactException::notFound);
    }
}
