package com.ax.template.authblueprint.provisionalattestation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * provisional-attestation-l0 root. Created PROVISIONAL by {@code authoredBy}; becomes ATTESTED
 * ONLY through {@link ProvisionalRecordStateMachine#attest} (sole mutator of {@code status} /
 * {@code attestedBy} / {@code attestedAt} / {@code attestedContentHash}) — PATT-LIFECYCLE-001.
 * The DB {@code @Check} backstops PATT-DISTINCT-002 even against a direct-SQL or ORM-bypassing
 * write: {@code attested_by} can never equal {@code authored_by}. {@code content} stays a plain
 * mutable column (not {@code updatable=false}) because the AUTHOR may legitimately edit it while
 * PROVISIONAL (PATT-FREEZE-003); the freeze is enforced in {@link ProvisionalRecordService}, not
 * at the JPA-column level, since a genuine pre-attestation edit path exists.
 */
@AggregateRoot
@Entity
@Table(name = "provisional_records")
@Check(constraints = "attested_by IS NULL OR attested_by <> authored_by")
public class ProvisionalRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "authored_by", nullable = false, updatable = false, length = 200)
    private String authoredBy;

    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProvisionalRecordStatus status;

    @Column(name = "attested_by", length = 200)
    private String attestedBy;

    @Column(name = "attested_at")
    private Instant attestedAt;

    /** PATT-FREEZE-003 — SHA-256 hex of the content AS IT EXISTED at the moment of attestation. */
    @Column(name = "attested_content_hash", length = 64)
    private String attestedContentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProvisionalRecord() {}

    ProvisionalRecord(UUID id, String authoredBy, String content, Instant createdAt) {
        this.id = id;
        this.authoredBy = authoredBy;
        this.content = content;
        this.status = ProvisionalRecordStatus.PROVISIONAL;
        this.createdAt = createdAt;
    }

    public static ProvisionalRecord author(String authoredBy, String content, Instant createdAt) {
        return new ProvisionalRecord(UUID.randomUUID(), authoredBy, content, createdAt);
    }

    /** PATT-FREEZE-003 — only legal while PROVISIONAL; the service enforces the frozen gate. */
    void editContent(String newContent) {
        this.content = newContent;
    }

    /** Sole-mutator hook — called ONLY by {@link ProvisionalRecordStateMachine#attest}. */
    void markAttested(String attestedBy, String attestedContentHash, Instant attestedAt) {
        this.status = ProvisionalRecordStatus.ATTESTED;
        this.attestedBy = attestedBy;
        this.attestedContentHash = attestedContentHash;
        this.attestedAt = attestedAt;
    }

    public UUID getId() { return id; }
    public String getAuthoredBy() { return authoredBy; }
    public String getContent() { return content; }
    public ProvisionalRecordStatus getStatus() { return status; }
    public String getAttestedBy() { return attestedBy; }
    public Instant getAttestedAt() { return attestedAt; }
    public String getAttestedContentHash() { return attestedContentHash; }
    public Instant getCreatedAt() { return createdAt; }
}
