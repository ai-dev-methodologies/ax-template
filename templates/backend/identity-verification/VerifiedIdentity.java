/**
 * @ax-template-meta
 * template_id: backend/identity-verification/VerifiedIdentity
 * layer: backend-domain
 * domain: identity-verification
 * anchors_rule: no-rrn-collection-without-legal-basis.md
 * provenance_class: locked_constraint
 * evidence:
 *   - source_type: external
 *     citation: "KISA 본인인증 가이드라인 — VerifiedIdentity stores only CI/DI tokens; storing the raw RRN is prohibited under 개인정보보호법 §24"
 *     url: "https://www.kisa.or.kr/2060301/form?postSeq=14&lang_type=KO"
 *   - source_type: external
 *     citation: "개인정보보호법 §24 — 고유식별정보 처리 제한: minimum necessary principle prohibits storing the RRN when CI/DI suffice"
 *     url: "https://www.law.go.kr/법령/개인정보보호법"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Entity mapping with @Entity, @Id, @GeneratedValue"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   VerifiedIdentity entity stores a successful 본인인증 outcome.
 *   Fields: ci, di (opaque tokens), name, dob, verifiedAt, providerName, metadata.
 *   CRITICAL: NO RRN field. Adding rrn / residentRegistrationNumber / 주민등록번호
 *   violates IDV-CALLBACK-003 and 개인정보보호법 §24.
 */
package com.example.app.identityverification;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.hibernate.annotations.SQLDelete;

/**
 * Verified identity record produced by a successful KISA 본인인증 callback.
 *
 * <p>Field contract (IDV-CALLBACK-003):
 * <ul>
 *   <li>{@code ci} — Connecting Information (64 hex chars); cross-service unique person token.
 *   <li>{@code di} — Duplicate Information (64 hex chars); per-service unique person token.
 *   <li>{@code name}, {@code dob} — verified legal name and date of birth.
 *   <li>{@code verifiedAt} — server-side persistence timestamp.
 *   <li>{@code providerName} — "pass" | "kcb".
 *   <li>{@code metadata} — provider-specific extras (never the raw RRN).
 * </ul>
 *
 * <p>RRN is NEVER stored. CI/DI replace the RRN for identity correlation (개인정보보호법 §24).
 *
 * <p>Extends {@link BaseEntity} (SP13) for: id (UUID), createdAt, updatedAt, deleted.
 */
@Entity
@SQLDelete(sql = "UPDATE verified_identity SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
    name = "verified_identity",
    indexes = {
        @Index(name = "idx_verified_identity_ci", columnList = "ci"),
        @Index(name = "idx_verified_identity_provider", columnList = "provider_name")
    }
)
public class VerifiedIdentity extends BaseEntity {

    /**
     * Connecting Information — 64-byte hex token.
     * Cross-service unique person identifier (not the RRN — IDV-CALLBACK-003).
     */
    @Column(name = "ci", nullable = false, length = 128)
    private String ci;

    /**
     * Duplicate Information — 64-byte hex token.
     * Per-service unique person identifier.
     */
    @Column(name = "di", nullable = false, length = 128)
    private String di;

    /** Verified legal name (Korean full name). */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Date of birth from verified identity. */
    @Column(name = "dob")
    private LocalDate dob;

    /** Server-side timestamp when this record was persisted. */
    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    /** Provider that produced this verification: "pass" | "kcb". */
    @Column(name = "provider_name", nullable = false, length = 20)
    private String providerName;

    /**
     * Provider-specific metadata (NOT the RRN).
     * Stored as JSON; use for provider-specific audit keys only.
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = com.example.app.common.JsonbConverter.class)
    private Map<String, String> metadata;

    protected VerifiedIdentity() {
        // JPA
    }

    /**
     * Factory method — creates a new VerifiedIdentity from extracted KISA CI/DI data.
     *
     * @param data canonical verified identity data from the provider adapter
     * @return a new (unsaved) VerifiedIdentity
     */
    public static VerifiedIdentity create(VerifiedIdentityData data) {
        VerifiedIdentity v = new VerifiedIdentity();
        v.ci           = data.ci();
        v.di           = data.di();
        v.name         = data.name();
        v.dob          = data.dob();
        v.verifiedAt   = data.verifiedAt();
        v.providerName = data.providerName();
        v.metadata     = data.metadata();
        return v;
    }

    public String getCi()           { return ci; }
    public String getDi()           { return di; }
    public String getName()         { return name; }
    public LocalDate getDob()       { return dob; }
    public Instant getVerifiedAt()  { return verifiedAt; }
    public String getProviderName() { return providerName; }
    public Map<String, String> getMetadata() { return metadata; }
}
