package com.ax.template.authblueprint.eventingest;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Attempts a brand-new {@link IngestState} row in its OWN independent transaction
 * (INGEST-IDEMPOTENT-APPLY-001). {@code findByStreamAndSubjectIdForUpdate} has nothing to LOCK
 * when the row does not exist yet, so two concurrent first-ever deliveries for the SAME
 * (stream, subject) can both reach the create path and race on {@code uq_ingest_state_stream_subject}.
 *
 * <p>The insert MUST run in a {@code REQUIRES_NEW} transaction, not merely a
 * {@code try { flush } catch} inside the caller's own transaction: after a flush fails on a
 * unique-constraint violation, Hibernate's persistence context still holds the failed transient
 * entity enlisted for insert, and the NEXT auto-flush-triggering query in that SAME session
 * (the caller's fallback read) re-attempts the identical doomed insert — a SECOND, uncaught
 * violation that surfaces as an unmapped 500 (a bug found and fixed via
 * {@code concurrentExactDuplicateDelivery_bothAck200_exactlyOneDedupRow}). Running the attempt
 * in its own transaction (its own EntityManager/session, per {@code REQUIRES_NEW}) means a
 * failure there rolls back ONLY that isolated attempt — the caller's own transaction and session
 * are never touched, so its fallback read afterward runs against a pristine persistence context.
 *
 * <p>Separate bean deliberately (mirrors {@code authzparity.BlockedAttemptRecorder}): a
 * {@code REQUIRES_NEW} call to a sibling method on the same proxied service bean would bypass
 * the proxy via the Spring self-invocation trap and run in the caller's own transaction,
 * defeating the whole point.
 */
@Component
public class IngestStateCreator {

    private final IngestStateRepository states;

    public IngestStateCreator(IngestStateRepository states) {
        this.states = states;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestState tryCreate(IngestState candidate) {
        return states.saveAndFlush(candidate);
    }
}
