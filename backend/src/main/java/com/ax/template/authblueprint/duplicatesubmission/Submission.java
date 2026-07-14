package com.ax.template.authblueprint.duplicatesubmission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * duplicate-submission-key-l0 root. Its own aggregate — references {@link DuplicateKeyChannel}
 * by id, never by object pointer. The NATURAL SAME-LOSS KEY (subject + loss-date + loss-type,
 * DUPKEY-NATURAL-001) is derived once at construction and immutable. {@code activeKey} mirrors
 * {@code naturalKey} while {@link #getStatus()} is ACTIVE and is cleared to {@code null} the
 * moment the submission is withdrawn or rejected (DUPKEY-WITHDRAWN-003) — a NULL is excluded
 * from the {@code UNIQUE(channel_id, active_key)} backstop by ordinary SQL semantics, so a
 * released key is immediately available to a fresh submission without any partial/filtered
 * index syntax. Lifecycle moves ONLY via the package-private {@link #release} hook, called by
 * {@link SubmissionStateMachine}.
 */
@AggregateRoot
@Entity
@Table(name = "duplicate_submissions",
    uniqueConstraints = @UniqueConstraint(name = "uq_duplicate_submission_active_key",
        columnNames = {"channel_id", "active_key"}))
@Check(constraints = "(status = 'ACTIVE') = (active_key IS NOT NULL)")
public class Submission {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "channel_id", nullable = false, updatable = false)
    private UUID channelId;

    @Column(name = "subject_ref", nullable = false, updatable = false, length = 200)
    private String subjectRef;

    @Column(name = "loss_date", nullable = false, updatable = false)
    private LocalDate lossDate;

    @Column(name = "loss_type", nullable = false, updatable = false, length = 100)
    private String lossType;

    /** DUPKEY-NATURAL-001 — subjectRef|lossDate|lossType, immutable, derived once at construction. */
    @Column(name = "natural_key", nullable = false, updatable = false, length = 320)
    private String naturalKey;

    /** Mirrors naturalKey while ACTIVE; null once WITHDRAWN/REJECTED (DUPKEY-WITHDRAWN-003). */
    @Column(name = "active_key", length = 320)
    private String activeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status;

    /** DUPKEY-FUZZY-002 — set at construction when a near-match band hit; never mutated after. */
    @Column(name = "flagged_for_review", nullable = false, updatable = false)
    private boolean flaggedForReview;

    /** The suspect submission this one was flagged against (null when not flagged). */
    @Column(name = "suspect_submission_id", updatable = false)
    private UUID suspectSubmissionId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Submission() {}

    public Submission(UUID id, UUID channelId, String subjectRef, LocalDate lossDate, String lossType,
                      boolean flaggedForReview, UUID suspectSubmissionId, Instant createdAt) {
        this.id = id;
        this.channelId = channelId;
        this.subjectRef = subjectRef;
        this.lossDate = lossDate;
        this.lossType = lossType;
        this.naturalKey = deriveNaturalKey(subjectRef, lossDate, lossType);
        this.activeKey = this.naturalKey;
        this.status = SubmissionStatus.ACTIVE;
        this.flaggedForReview = flaggedForReview;
        this.suspectSubmissionId = suspectSubmissionId;
        this.createdAt = createdAt;
    }

    public static String deriveNaturalKey(String subjectRef, LocalDate lossDate, String lossType) {
        return subjectRef + "|" + lossDate + "|" + lossType;
    }

    /** Sole-mutator hook — DUPKEY-WITHDRAWN-003: a terminal status releases the key. */
    void release(SubmissionStatus terminalStatus) {
        this.status = terminalStatus;
        this.activeKey = null;
    }

    public UUID getId() { return id; }
    public UUID getChannelId() { return channelId; }
    public String getSubjectRef() { return subjectRef; }
    public LocalDate getLossDate() { return lossDate; }
    public String getLossType() { return lossType; }
    public String getNaturalKey() { return naturalKey; }
    public String getActiveKey() { return activeKey; }
    public SubmissionStatus getStatus() { return status; }
    public boolean isFlaggedForReview() { return flaggedForReview; }
    public UUID getSuspectSubmissionId() { return suspectSubmissionId; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
