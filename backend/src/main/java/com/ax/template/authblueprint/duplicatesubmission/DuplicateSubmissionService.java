package com.ax.template.authblueprint.duplicatesubmission;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * duplicate-submission-key-l0 sole orchestrator. An exact natural-key match against an ACTIVE
 * submission is a deterministic 409 (DUPKEY-NATURAL-001); a near (fuzzy-window) match is
 * accepted but flagged for review with a linkage to the suspect (DUPKEY-FUZZY-002); withdrawing
 * or rejecting a submission releases its key so a legitimate resubmission succeeds
 * (DUPKEY-WITHDRAWN-003).
 */
@Service
public class DuplicateSubmissionService {

    private final DuplicateKeyChannelRepository channels;
    private final SubmissionRepository submissions;
    private final SubmissionStateMachine stateMachine;
    private final DuplicateSubmissionMetrics metrics;
    private final Clock clock;

    public DuplicateSubmissionService(DuplicateKeyChannelRepository channels, SubmissionRepository submissions,
                                      SubmissionStateMachine stateMachine, DuplicateSubmissionMetrics metrics, Clock clock) {
        this.channels = channels;
        this.submissions = submissions;
        this.stateMachine = stateMachine;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public DuplicateKeyChannel defineChannel(String scopeLabel, int fuzzyWindowDays) {
        if (scopeLabel == null || scopeLabel.isBlank()) {
            metrics.record("defineChannel", "invalid");
            throw DuplicateSubmissionException.invalidChannel("scopeLabel must not be blank");
        }
        if (fuzzyWindowDays < 0) {
            metrics.record("defineChannel", "invalid");
            throw DuplicateSubmissionException.invalidChannel("fuzzyWindowDays must be >= 0");
        }
        DuplicateKeyChannel c = new DuplicateKeyChannel(UUID.randomUUID(), scopeLabel, fuzzyWindowDays, Instant.now(clock));
        DuplicateKeyChannel saved = channels.save(c);
        metrics.record("defineChannel", "ok");
        return saved;
    }

    /**
     * DUPKEY-NATURAL/FUZZY-001/002 — gate one submission. An exact ACTIVE natural-key match is
     * rejected 409 (pre-checked, with the UNIQUE(channel_id, active_key) constraint as the
     * concurrent-race backstop translated via {@link DataIntegrityViolationException}). Absent
     * an exact match, a fuzzy-window near-match on the same subject+type is flagged for review
     * (never silently accepted, never hard-rejected).
     */
    @Transactional
    public Submission submit(UUID channelId, String subjectRef, LocalDate lossDate, String lossType, String actor) {
        DuplicateKeyChannel channel = channels.findById(channelId).orElseThrow(DuplicateSubmissionException::notFound);
        String naturalKey = Submission.deriveNaturalKey(subjectRef, lossDate, lossType);

        submissions.findActiveByNaturalKey(channelId, naturalKey).ifPresent(existing -> {
            metrics.record("submit", "duplicate");
            throw DuplicateSubmissionException.duplicateSubmission(existing.getId());
        });

        List<Submission> fuzzyCandidates = submissions.findFuzzyCandidates(channelId, subjectRef, lossType,
            lossDate.minusDays(channel.getFuzzyWindowDays()), lossDate.plusDays(channel.getFuzzyWindowDays()));
        boolean flagged = !fuzzyCandidates.isEmpty();
        UUID suspectId = flagged ? fuzzyCandidates.get(0).getId() : null;

        try {
            Submission saved = submissions.saveAndFlush(new Submission(UUID.randomUUID(), channelId, subjectRef,
                lossDate, lossType, flagged, suspectId, Instant.now(clock)));
            metrics.record("submit", flagged ? "flagged" : "accepted");
            return saved;
        } catch (DataIntegrityViolationException race) {
            // a concurrent submission won the UNIQUE(channel_id, active_key) race — translate to the
            // same deterministic 409 as the pre-check (never an opaque 500). Deliberately NO follow-up
            // repository call here: a flush failure leaves this transaction's persistence context
            // poisoned (Hibernate marks the Session rollback-only), so a second query in the SAME
            // transaction can itself throw — silently losing the 409 translation. The race-loser's
            // 409 therefore carries no conflictingSubmissionId (only the pre-check path above does);
            // correctness of the rejection matters more than the id being populated in the rare race.
            metrics.record("submit", "duplicate");
            throw DuplicateSubmissionException.duplicateSubmission(null);
        }
    }

    @Transactional
    public Submission withdraw(UUID id) {
        Submission s = submissions.findById(id).orElseThrow(DuplicateSubmissionException::notFound);
        stateMachine.withdraw(s);
        metrics.record("withdraw", "ok");
        return s;
    }

    @Transactional
    public Submission reject(UUID id) {
        Submission s = submissions.findById(id).orElseThrow(DuplicateSubmissionException::notFound);
        stateMachine.reject(s);
        metrics.record("reject", "ok");
        return s;
    }

    @Transactional(readOnly = true)
    public Submission get(UUID id) {
        return submissions.findById(id).orElseThrow(DuplicateSubmissionException::notFound);
    }
}
