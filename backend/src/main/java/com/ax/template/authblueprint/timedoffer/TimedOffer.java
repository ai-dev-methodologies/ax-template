package com.ax.template.authblueprint.timedoffer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * timed-offer-exclusive-assignment-l0 root: one offer extended to a candidate for a SUBJECT with a
 * recorded DEADLINE (TIMEDOFFER-LIFECYCLE-001). It stays OPEN until the candidate accepts/declines
 * or the deadline passes; the deadline sweep expires past-deadline OPEN offers EXACTLY ONCE,
 * recording who (SYSTEM) and when on this same row. Status moves ONLY through
 * {@link TimedOfferStateMachine} (no public setter). The re-offer ladder is append-only: a re-offer
 * is a NEW TimedOffer row whose {@code priorOfferId} points back at the offer it supersedes and
 * whose {@code attemptSeq} is strictly monotonic for the subject (TIMEDOFFER-LADDER-001). The
 * subject / candidate / deadline / attemptSeq / priorOfferId columns are immutable.
 */
@AggregateRoot
@Entity
@Table(name = "timed_offers")
@Check(constraints =
    "attempt_seq >= 1"
    + " AND (status <> 'OPEN' OR decided_at IS NULL)"
    + " AND (status = 'OPEN' OR decided_at IS NOT NULL)")
public class TimedOffer {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The thing being assigned — opaque external reference, recorded verbatim. Drives exclusivity. */
    @Column(name = "subject_id", nullable = false, updatable = false, length = 200)
    private String subjectId;

    /** The candidate this offer is extended to — opaque external reference. */
    @Column(name = "candidate", nullable = false, updatable = false, length = 200)
    private String candidate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OfferStatus status;

    /** TIMEDOFFER-LIFECYCLE-001 — the recorded deadline (an Instant on the injected Clock). */
    @Column(name = "deadline", nullable = false, updatable = false)
    private Instant deadline;

    /** TIMEDOFFER-LADDER-001 — strictly monotonic attempt position (1,2,3,…) for the subject. */
    @Column(name = "attempt_seq", nullable = false, updatable = false)
    private int attemptSeq;

    /** TIMEDOFFER-LADDER-001 — the prior offer this one re-offers (null for the first attempt). */
    @Column(name = "prior_offer_id", updatable = false)
    private UUID priorOfferId;

    /** Who decided this offer (the candidate on accept/decline, SYSTEM on sweep-expiry). */
    @Column(name = "decided_by", length = 200)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TimedOffer() {}

    public TimedOffer(UUID id, String subjectId, String candidate, Instant deadline,
                      int attemptSeq, UUID priorOfferId, Instant createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.candidate = candidate;
        this.status = OfferStatus.OPEN;
        this.deadline = deadline;
        this.attemptSeq = attemptSeq;
        this.priorOfferId = priorOfferId;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — package-private so only {@link TimedOfferStateMachine} advances status,
     *  recording who decided it and when in the SAME write (the basis for a bare status is unrepresentable). */
    void decide(OfferStatus next, String by, Instant at) {
        this.status = next;
        this.decidedBy = by;
        this.decidedAt = at;
    }

    /** TIMEDOFFER-LIFECYCLE-001 — acceptable only while OPEN and strictly before the deadline. */
    public boolean isAcceptableAt(Instant now) {
        return status == OfferStatus.OPEN && now.isBefore(deadline);
    }

    public boolean isOpen() {
        return status == OfferStatus.OPEN;
    }

    public boolean isPastDeadline(Instant now) {
        return !now.isBefore(deadline);
    }

    public UUID getId() { return id; }
    public String getSubjectId() { return subjectId; }
    public String getCandidate() { return candidate; }
    public OfferStatus getStatus() { return status; }
    public Instant getDeadline() { return deadline; }
    public int getAttemptSeq() { return attemptSeq; }
    public UUID getPriorOfferId() { return priorOfferId; }
    public String getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
