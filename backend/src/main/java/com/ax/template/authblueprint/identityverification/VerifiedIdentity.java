package com.ax.template.authblueprint.identityverification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * R54 — IDV-CALLBACK-002 / IDV-CALLBACK-003 persistence entity.
 *
 * <p>Stores the canonical correlation tokens (CI / DI) plus minimal user
 * descriptors (name, dob) extracted from a verified callback. Provider-specific
 * extras live in {@link #metadata} as opaque key/value pairs.
 *
 * <h2>No-RRN invariant (IDV-CALLBACK-003)</h2>
 * The entity intentionally has NO field whose name would carry the source
 * resident registration number — no {@code rrn}, no
 * {@code residentRegistrationNumber}, no {@code 주민등록번호}. The
 * {@link VerifiedIdentityViolationProofTest} guards this via reflection so a
 * future refactor cannot silently re-introduce the column.
 *
 * <h2>Immutability</h2>
 * Every content column is {@code @Column(updatable=false)}. The entity has no
 * public setters — once a callback is recorded, it MUST reflect the original
 * provider response. Re-attributing a verified row to a different user / CI /
 * DI would falsify the §24-1 audit trail.
 *
 * <h2>Metadata</h2>
 * {@link #metadata} is read-only after construction (returned as
 * {@link Collections#unmodifiableMap(Map)}). Provider adapters fill it with
 * their native keys; the catalog promises no schema on these keys — fork-
 * receivers index/query them as they like.
 */
@AggregateRoot
@Entity
@Table(name = "verified_identity")
public class VerifiedIdentity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ci", nullable = false, updatable = false, length = 128)
    private String ci;

    @Column(name = "di", nullable = false, updatable = false, length = 128)
    private String di;

    @Column(name = "name", nullable = false, updatable = false, length = 128)
    private String name;

    /**
     * Date-of-birth as ISO yyyy-MM-dd string. Stored as VARCHAR not DATE because
     * providers occasionally return partial DOBs (yyyy-MM with missing day);
     * normalising would require provider-specific defaults that the catalog
     * intentionally refuses to choose.
     */
    @Column(name = "dob", nullable = false, updatable = false, length = 16)
    private String dob;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private Instant verifiedAt;

    @Column(name = "provider_name", nullable = false, updatable = false, length = 32)
    private String providerName;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "verified_identity_metadata",
        joinColumns = @JoinColumn(name = "verified_identity_id"))
    @MapKeyColumn(name = "meta_key", length = 64)
    @Column(name = "meta_value", length = 1024)
    @JsonIgnore
    private Map<String, String> metadata;

    protected VerifiedIdentity() {
        // JPA
    }

    private VerifiedIdentity(UUID id, String ci, String di, String name, String dob,
                              Instant verifiedAt, String providerName,
                              Map<String, String> metadata) {
        this.id = id;
        this.ci = ci;
        this.di = di;
        this.name = name;
        this.dob = dob;
        this.verifiedAt = verifiedAt;
        this.providerName = providerName;
        this.metadata = metadata;
    }

    /** Factory matching the canonical {@link VerifiedIdentityData} shape. */
    public static VerifiedIdentity create(VerifiedIdentityData data) {
        if (data == null) {
            throw new IllegalArgumentException("data must be non-null");
        }
        return new VerifiedIdentity(
            UUID.randomUUID(),
            data.ci(),
            data.di(),
            data.name() == null ? "" : data.name(),
            data.dob()  == null ? "" : data.dob(),
            data.verifiedAt() == null ? Instant.now() : data.verifiedAt(),
            data.providerName(),
            // ElementCollection is owned by JPA and must be mutable internally,
            // but the entity never exposes the live reference — readers see an
            // unmodifiableMap() in {@link #getMetadata()}.
            new java.util.LinkedHashMap<>(data.metadata())
        );
    }

    public UUID getId() { return id; }
    public String getCi() { return ci; }
    public String getDi() { return di; }
    public String getName() { return name; }
    public String getDob() { return dob; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public String getProviderName() { return providerName; }

    public Map<String, String> getMetadata() {
        return metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }
}
