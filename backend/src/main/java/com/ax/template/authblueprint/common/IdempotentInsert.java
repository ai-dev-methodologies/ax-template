package com.ax.template.authblueprint.common;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Runs a racy unique-constraint insert in its OWN independent ({@code REQUIRES_NEW}) transaction
 * so that a constraint violation aborts ONLY the isolated inner transaction — leaving the caller's
 * OUTER transaction (and its persistence context / DB connection) pristine for the idempotent
 * replay requery (P1-64 / P1-65).
 *
 * <h2>Why this exists</h2>
 * The catalog's idempotency idiom is: pre-check, then {@code saveAndFlush(new Entity(...))} inside
 * a {@code try}, and on {@link org.springframework.dao.DataIntegrityViolationException} re-query the
 * winner's row and return it. On H2 that works, but on PostgreSQL a unique-constraint violation
 * puts the transaction into the aborted state (SQLSTATE {@code 25P02}); every subsequent statement
 * in that SAME transaction — including the catch-block requery — fails with
 * {@code "current transaction is aborted"}, surfacing as an unmapped 500 instead of the intended
 * idempotent replay.
 *
 * <p>Isolating the racy insert in a {@code REQUIRES_NEW} boundary fixes this: Spring suspends the
 * outer transaction and gives the insert its own {@link jakarta.persistence.EntityManager}/session
 * bound to a fresh transaction. A violation there rolls back ONLY that inner transaction; the
 * exception propagates to the caller, whose own transaction and connection are untouched, so the
 * catch-block requery runs cleanly.
 *
 * <h2>Why a separate bean (not a private method)</h2>
 * Spring's declarative transactions are proxy-based: a {@code REQUIRES_NEW} call to a sibling method
 * on the SAME service bean bypasses the proxy (the self-invocation trap) and silently runs in the
 * caller's own transaction — defeating the isolation entirely. Crossing a bean boundary into this
 * dedicated collaborator is mandatory. Precedent in this catalog:
 * {@code eventingest.IngestStateCreator} and {@code authzparity.BlockedAttemptRecorder}.
 *
 * <p>Usage — the caller keeps the pre-check and the catch/requery in its own transaction; only the
 * racy insert crosses the boundary:
 * <pre>
 * try {
 *     return idempotentInsert.insert(() -&gt; repo.saveAndFlush(new Entity(...)));
 * } catch (DataIntegrityViolationException e) {   // ran in the OUTER (unpoisoned) tx
 *     return repo.findByBusinessKey(...).orElseThrow(...);
 * }
 * </pre>
 *
 * <p>The supplier MUST perform the flushing insert itself ({@code saveAndFlush} /
 * {@code persistAndFlush}) so the constraint violation fires INSIDE this method's transaction, not
 * at the outer commit after this method returns.
 */
@Component
public class IdempotentInsert {

    /**
     * Execute {@code insert} in a fresh {@code REQUIRES_NEW} transaction. The supplier must issue a
     * flushing insert; a {@link org.springframework.dao.DataIntegrityViolationException} it raises
     * rolls back only this inner transaction and propagates to the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T insert(Supplier<T> insert) {
        return insert.get();
    }
}
