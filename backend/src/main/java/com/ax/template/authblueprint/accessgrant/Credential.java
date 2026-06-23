package com.ax.template.authblueprint.accessgrant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * time-bounded-access-grant-l0 root: a single CREDENTIAL a subject holds, of a named class
 * (e.g. LICENSE, INSURANCE, BACKGROUND_CHECK), carrying its OWN half-open validity window
 * [validFrom, validUntil) (AGRANT-ELIGIBILITY-001). A credential is non-expired at {@code now}
 * iff now ∈ [validFrom, validUntil) — the SAME recomputed-over-the-Clock predicate a grant uses;
 * there is NO stored {@code valid}/{@code expired} flag. The multi-credential eligibility gate
 * passes only when EVERY required class is held by a credential that is valid at now. Credentials
 * are append-only (immutable once issued); there is NO delete path.
 */
@AggregateRoot
@Entity
@Table(name = "access_credentials")
@Check(constraints = "valid_until > valid_from")
public class Credential {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_id", nullable = false, updatable = false, length = 200)
    private String subjectId;

    /** The credential class (e.g. LICENSE / INSURANCE / BACKGROUND_CHECK) — opaque, verbatim. */
    @Column(name = "credential_class", nullable = false, updatable = false, length = 100)
    private String credentialClass;

    @Column(name = "valid_from", nullable = false, updatable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false, updatable = false)
    private Instant validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Credential() {}

    public Credential(UUID id, String subjectId, String credentialClass,
                      Instant validFrom, Instant validUntil, Instant createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.credentialClass = credentialClass;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.createdAt = createdAt;
    }

    /** AGRANT-ELIGIBILITY-001 — non-expired iff now ∈ [validFrom, validUntil) (half-open), the
     *  same recomputed-over-the-Clock predicate a grant uses. Pure function — no stored flag. */
    public boolean isValidAt(Instant now) {
        return !now.isBefore(validFrom) && now.isBefore(validUntil);
    }

    public UUID getId() { return id; }
    public String getSubjectId() { return subjectId; }
    public String getCredentialClass() { return credentialClass; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public Instant getCreatedAt() { return createdAt; }
}
