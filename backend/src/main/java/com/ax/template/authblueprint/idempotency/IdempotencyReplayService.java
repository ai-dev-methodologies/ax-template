package com.ax.template.authblueprint.idempotency;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * IDEMPOTENCY-DEDUP-001 three-state replay machine over {@link IdempotencyReplayStore}:
 * never-seen → run the work + cache; seen-completed + same fingerprint → replay verbatim;
 * seen-completed + different fingerprint → 422; seen-in-flight → 409. Drives the bounded
 * {@link IdempotencyMetrics} outcomes. The work runs OUTSIDE the store's atomic claim so a
 * concurrent duplicate is rejected immediately (409) instead of blocking — this is what makes
 * "10 parallel → 1 success + 9 conflicts" hold (IDEMPOTENCY-CONCURRENT-001).
 *
 * <p>Spec: specs/idempotency-l0.yaml#IDEMPOTENCY-DEDUP-001 / -CONCURRENT-001.
 */
@Component
public class IdempotencyReplayService {

    /** The freshly-produced response a first-seen request caches for later replay. */
    public record Result(int status, String body) {}

    /** The resolved request: {@code label} is the metric outcome and drives the controller's mapping. */
    public record Outcome(int status, String body, String label) {}

    private final IdempotencyReplayStore store;
    private final IdempotencyMetrics metrics;

    public IdempotencyReplayService(IdempotencyReplayStore store, IdempotencyMetrics metrics) {
        this.store = store;
        this.metrics = metrics;
    }

    public Outcome process(String tenant, String key, String fingerprint, Supplier<Result> work) {
        String composite = IdempotencyReplayStore.composite(tenant, key);
        long claimedAt = System.nanoTime();
        IdempotencyReplayStore.Entry prev = store.claim(composite, fingerprint, claimedAt);

        if (prev == null) {                                    // winner — run the work once
            try {
                Result r = work.get();
                store.complete(composite, fingerprint, r.status(), r.body(), claimedAt);
                metrics.outcome(tenant, "first_seen");
                metrics.lockWait(tenant, Duration.ofNanos(System.nanoTime() - claimedAt));
                return new Outcome(r.status(), r.body(), "first_seen");
            } catch (RuntimeException ex) {
                store.release(composite);                      // never poison the key on transient failure
                throw ex;
            }
        }
        if (prev.state() == IdempotencyReplayStore.State.IN_FLIGHT) {
            metrics.outcome(tenant, "conflict");
            return new Outcome(409, null, "conflict");
        }
        if (prev.fingerprint().equals(fingerprint)) {          // completed + same payload → replay verbatim
            metrics.outcome(tenant, "replayed");
            return new Outcome(prev.status(), prev.body(), "replayed");
        }
        metrics.outcome(tenant, "fingerprint_mismatch");       // same key, different payload
        return new Outcome(422, null, "fingerprint_mismatch");
    }
}
