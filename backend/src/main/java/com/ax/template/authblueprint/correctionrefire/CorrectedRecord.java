package com.ax.template.authblueprint.correctionrefire;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * correction-refire-l0 — one IMMUTABLE, APPEND-ONLY published version of a subject
 * (CRF-SUPERSEDE-001). Version numbers per {@code subjectRef} are strictly increasing
 * (uq(subject_ref, version) backstops a concurrent-publish race deterministically); a
 * correction carries a forward {@code correctsVersion} pointer to the version it corrects,
 * while the PRIOR version row is never mutated or deleted. There is NO stored "current version"
 * pointer anywhere in this domain (CRF-CHAIN-004) — resolving current is always
 * {@code MAX(version)} derived on read via {@link CorrectedRecordRepository}.
 */
@AggregateRoot
@Entity
@Table(name = "corrected_records", uniqueConstraints = {
    @UniqueConstraint(name = "uq_corrected_record_subject_version", columnNames = {"subject_ref", "version"})
})
public class CorrectedRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_ref", nullable = false, updatable = false, length = 200)
    private String subjectRef;

    @Column(name = "version", nullable = false, updatable = false)
    private int version;

    @Column(name = "content", nullable = false, updatable = false, length = 4000)
    private String content;

    /** CRF-IDEMPOTENT-003 — SHA-256 hex of {@link #content}; a re-publish with the SAME hash is a no-op. */
    @Column(name = "content_hash", nullable = false, updatable = false, length = 64)
    private String contentHash;

    /** Null on the FIRST published version; set to the version this one corrects otherwise. */
    @Column(name = "corrects_version", updatable = false)
    private Integer correctsVersion;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;

    protected CorrectedRecord() {}

    CorrectedRecord(UUID id, String subjectRef, int version, String content, String contentHash,
                    Integer correctsVersion, Instant publishedAt) {
        this.id = id;
        this.subjectRef = subjectRef;
        this.version = version;
        this.content = content;
        this.contentHash = contentHash;
        this.correctsVersion = correctsVersion;
        this.publishedAt = publishedAt;
    }

    public static CorrectedRecord publish(UUID id, String subjectRef, int version, String content,
                                          String contentHash, Integer correctsVersion, Instant publishedAt) {
        return new CorrectedRecord(id, subjectRef, version, content, contentHash, correctsVersion, publishedAt);
    }

    public boolean isCorrection() {
        return correctsVersion != null;
    }

    public UUID getId() { return id; }
    public String getSubjectRef() { return subjectRef; }
    public int getVersion() { return version; }
    public String getContent() { return content; }
    public String getContentHash() { return contentHash; }
    public Integer getCorrectsVersion() { return correctsVersion; }
    public Instant getPublishedAt() { return publishedAt; }
}
