package com.ax.template.authblueprint.signedartifact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * signed-artifact-l0 — one IMMUTABLE, APPEND-ONLY issuance record (SIGNED-ASYM-001). An artifact
 * issuance is a fact: the JWS compact serialization, the {@code kid} pinning the published
 * verifying key, the {@code alg} (always an asymmetric value here — ES256), and the SHA-256
 * {@code contentHash} the signature actually covers are all recorded verbatim and never edited.
 */
@AggregateRoot
@Entity
@Table(name = "signed_artifacts")
public class SignedArtifact {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_ref", nullable = false, updatable = false, length = 200)
    private String subjectRef;

    /** SHA-256 hex of the content the signature covers (sign-over-content-hash binding). */
    @Column(name = "content_hash", nullable = false, updatable = false, length = 64)
    private String contentHash;

    @Column(name = "kid", nullable = false, updatable = false, length = 100)
    private String kid;

    @Column(name = "alg", nullable = false, updatable = false, length = 20)
    private String alg;

    @Column(name = "jws", nullable = false, updatable = false, length = 4000)
    private String jws;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected SignedArtifact() {}

    SignedArtifact(UUID id, String subjectRef, String contentHash, String kid, String alg, String jws,
                   Instant issuedAt) {
        this.id = id;
        this.subjectRef = subjectRef;
        this.contentHash = contentHash;
        this.kid = kid;
        this.alg = alg;
        this.jws = jws;
        this.issuedAt = issuedAt;
    }

    public static SignedArtifact issue(UUID id, String subjectRef, String contentHash, String kid, String alg,
                                       String jws, Instant issuedAt) {
        return new SignedArtifact(id, subjectRef, contentHash, kid, alg, jws, issuedAt);
    }

    public UUID getId() { return id; }
    public String getSubjectRef() { return subjectRef; }
    public String getContentHash() { return contentHash; }
    public String getKid() { return kid; }
    public String getAlg() { return alg; }
    public String getJws() { return jws; }
    public Instant getIssuedAt() { return issuedAt; }
}
