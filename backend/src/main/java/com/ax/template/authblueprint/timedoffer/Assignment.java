package com.ax.template.authblueprint.timedoffer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * timed-offer-exclusive-assignment-l0 exclusivity backstop (TIMEDOFFER-EXCLUSIVE-001). One immutable
 * row per assigned subject: the {@code uq(subject_id)} index makes a SECOND accept for the same
 * subject — even via a DIFFERENT competing offer — a deterministic constraint violation the service
 * maps to 409 (CWE-362, the suspenders for any residual race past the subject row lock). The row
 * records WHICH offer and WHICH candidate won, and WHEN. It is a separate aggregate root (the
 * subject's exclusivity boundary), referencing the winning {@link TimedOffer} by IDENTITY (a
 * {@code UUID offerId}, never an object pointer — HG-AGG-REF). No mutator + no delete path: a
 * subject, once assigned, stays assigned for the life of this primitive.
 */
@AggregateRoot
@Entity
@Table(name = "timed_offer_assignments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_timed_offer_subject", columnNames = {"subject_id"})
})
public class Assignment {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The assigned subject — uq(subject_id) makes a second assignment a deterministic 409. */
    @Column(name = "subject_id", nullable = false, updatable = false, length = 200)
    private String subjectId;

    /** The winning offer, referenced by IDENTITY (HG-AGG-REF — never an object pointer). */
    @Column(name = "offer_id", nullable = false, updatable = false)
    private UUID offerId;

    /** The candidate who won the subject. */
    @Column(name = "candidate", nullable = false, updatable = false, length = 200)
    private String candidate;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected Assignment() {}

    public Assignment(UUID id, String subjectId, UUID offerId, String candidate, Instant assignedAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.offerId = offerId;
        this.candidate = candidate;
        this.assignedAt = assignedAt;
    }

    public UUID getId() { return id; }
    public String getSubjectId() { return subjectId; }
    public UUID getOfferId() { return offerId; }
    public String getCandidate() { return candidate; }
    public Instant getAssignedAt() { return assignedAt; }
}
