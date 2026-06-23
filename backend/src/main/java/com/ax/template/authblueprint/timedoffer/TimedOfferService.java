package com.ax.template.authblueprint.timedoffer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * timed-offer-exclusive-assignment-l0 sole orchestrator. A TimedOffer is OPEN until the candidate
 * accepts/declines or the deadline passes (TIMEDOFFER-LIFECYCLE-001). The accept path serializes on
 * the SUBJECT — it locks every offer row for the subject (PESSIMISTIC_WRITE) so concurrent accepts
 * across COMPETING offers for the same subject converge, and a uq(subject_id) Assignment row is the
 * deterministic backstop loser-selector for any residual race (TIMEDOFFER-EXCLUSIVE/CONCURRENT-001,
 * CWE-362). A declined/expired offer is re-offered as a NEW append-only row referencing the prior
 * with a monotonic attemptSeq (TIMEDOFFER-LADDER-001). The deadline sweep lives in
 * {@link TimedOfferSweeper} — a separate concurrent writer that must LOSE to a live accept. The
 * acting actor is ALWAYS the authenticated caller (the controller passes {@code auth.getName()}).
 */
@Service
public class TimedOfferService {

    static final String SYSTEM_ACTOR = "SYSTEM";

    private final TimedOfferRepository offers;
    private final AssignmentRepository assignments;
    private final TimedOfferStateMachine sm;
    private final TimedOfferMetrics metrics;
    private final Clock clock;

    public TimedOfferService(TimedOfferRepository offers, AssignmentRepository assignments,
                             TimedOfferStateMachine sm, TimedOfferMetrics metrics, Clock clock) {
        this.offers = offers;
        this.assignments = assignments;
        this.sm = sm;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** TIMEDOFFER-LIFECYCLE-001 — extend a fresh offer for a subject to a candidate with a deadline. */
    @Transactional
    public TimedOffer extend(String subjectId, String candidate, Instant deadline) {
        int attemptSeq = (int) offers.countBySubjectId(subjectId) + 1;   // monotonic per subject
        TimedOffer o = new TimedOffer(UUID.randomUUID(), subjectId, candidate, deadline,
            attemptSeq, null, Instant.now(clock));
        TimedOffer saved = offers.save(o);
        metrics.record("extend", "ok");
        return saved;
    }

    /**
     * TIMEDOFFER-EXCLUSIVE/CONCURRENT-001 — accept an offer, claiming the subject exclusively. Locks
     * EVERY offer row for the subject so concurrent accepts (this offer twice, or two competing
     * offers for one subject) serialize: the first locker reads no Assignment, creates it, and wins;
     * the rest, behind the lock, re-read the now-assigned subject and 409. The uq(subject_id) on the
     * Assignment row is the suspenders — even a residual race past the lock makes the second insert a
     * DataIntegrityViolationException the catch maps to the same deterministic 409.
     */
    @Transactional
    public TimedOffer accept(UUID offerId, String candidate) {
        TimedOffer o = offers.findById(offerId).orElseThrow(TimedOfferException::notFound);
        // Take the subject-wide lock (serializes competing accepts on the SAME subject).
        offers.findBySubjectIdForUpdate(o.getSubjectId());
        TimedOffer fresh = offers.findById(offerId).orElseThrow(TimedOfferException::notFound); // re-read under lock
        Instant now = Instant.now(clock);
        if (fresh.getStatus() != OfferStatus.OPEN) {
            metrics.record("accept", "not_open");
            throw TimedOfferException.notOpen(fresh.getStatus().name());        // 409 — terminal offer
        }
        if (fresh.isPastDeadline(now)) {
            metrics.record("accept", "expired_deadline");
            throw TimedOfferException.offerExpired();                           // 409 — deadline lapsed
        }
        if (assignments.findBySubjectId(fresh.getSubjectId()).isPresent()) {
            metrics.record("accept", "subject_taken");
            throw TimedOfferException.subjectAlreadyAssigned();                 // 409 — exclusivity loser
        }
        try {
            // uq(subject_id) — a second assignment for the subject is the deterministic 409 backstop.
            assignments.saveAndFlush(new Assignment(UUID.randomUUID(), fresh.getSubjectId(),
                fresh.getId(), candidate, now));
        } catch (DataIntegrityViolationException dup) {
            metrics.record("accept", "subject_taken");
            throw TimedOfferException.subjectAlreadyAssigned();                 // residual-race loser
        }
        sm.accept(fresh, candidate, now);          // status mutated through the machine (dirty-check)
        metrics.record("accept", "accepted");
        return fresh;
    }

    /** TIMEDOFFER-LIFECYCLE-001 — the candidate declines the offer; the subject stays unassigned. */
    @Transactional
    public TimedOffer decline(UUID offerId, String candidate) {
        TimedOffer o = offers.findByIdForUpdate(offerId).orElseThrow(TimedOfferException::notFound);
        if (o.getStatus() != OfferStatus.OPEN) {
            metrics.record("decline", "not_open");
            throw TimedOfferException.notOpen(o.getStatus().name());
        }
        sm.decline(o, candidate, Instant.now(clock));
        metrics.record("decline", "declined");
        return o;
    }

    /**
     * TIMEDOFFER-LADDER-001 — re-offer a declined/expired offer to the NEXT candidate as a NEW
     * append-only row referencing the prior, with a strictly monotonic attemptSeq for the subject.
     * Refuses an OPEN offer (422) and an already-assigned subject (409).
     */
    @Transactional
    public TimedOffer reoffer(UUID priorOfferId, String nextCandidate, Instant deadline) {
        TimedOffer prior = offers.findById(priorOfferId).orElseThrow(TimedOfferException::notFound);
        if (prior.getStatus() == OfferStatus.OPEN || prior.getStatus() == OfferStatus.ACCEPTED) {
            metrics.record("reoffer", "not_reofferable");
            throw TimedOfferException.notReofferable(prior.getStatus().name());  // 422 — only declined/expired
        }
        if (assignments.findBySubjectId(prior.getSubjectId()).isPresent()) {
            metrics.record("reoffer", "subject_taken");
            throw TimedOfferException.subjectAlreadyAssigned();                  // 409 — already assigned
        }
        int attemptSeq = (int) offers.countBySubjectId(prior.getSubjectId()) + 1; // monotonic
        TimedOffer next = offers.save(new TimedOffer(UUID.randomUUID(), prior.getSubjectId(),
            nextCandidate, deadline, attemptSeq, prior.getId(), Instant.now(clock)));
        metrics.record("reoffer", "reoffered");
        return next;
    }

    // ── read side ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TimedOffer get(UUID offerId) {
        return offers.findById(offerId).orElseThrow(TimedOfferException::notFound);
    }

    @Transactional(readOnly = true)
    public List<TimedOffer> ladder(String subjectId) {
        return offers.findLadderBySubjectId(subjectId, PageRequest.of(0, 500));
    }

    /** TIMEDOFFER-LIFECYCLE-001 — bounded batch of due OPEN offer ids (the sweeper loops over these). */
    @Transactional(readOnly = true)
    public List<UUID> dueOfferIds(int batchSize) {
        return offers.findDueOfferIds(OfferStatus.OPEN, Instant.now(clock), PageRequest.of(0, batchSize));
    }
}
