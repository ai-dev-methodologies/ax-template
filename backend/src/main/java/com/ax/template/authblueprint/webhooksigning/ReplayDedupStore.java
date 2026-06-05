package com.ax.template.authblueprint.webhooksigning;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * WHSIGN-REPLAY-001 — the per-event seen-set that closes the window-only replay gap. The
 * WHSIGN-TIMESTAMP-001 tolerance window alone still admits a captured-and-still-fresh request; this
 * store records each {@code event_id} the FIRST time it is verified, with a TTL equal to the tolerance
 * window, so a repeat within the window is rejected (→ 409 WEBHOOK_EVENT_REPLAYED) and an event whose
 * id has aged past the window is naturally allowed again (and would by then fail the timestamp check).
 *
 * <p>Atomic first-claim via {@link java.util.concurrent.ConcurrentMap#putIfAbsent}: exactly one caller
 * registers a given id; concurrent duplicates observe the existing entry. The label-free
 * {@link WebhookSigningMetrics#replayRejected} (no event_id dimension) is the spec's bounded form.
 * In-memory reference — a fork-receiver delegates to idempotency-l0 (event_id → IDEMPOTENCY-KEY-001).
 * Spec: specs/webhook-signing-l0.yaml#WHSIGN-REPLAY-001.
 */
@Component
public class ReplayDedupStore {

    private final ConcurrentHashMap<String, Long> seen = new ConcurrentHashMap<>();

    /**
     * Record {@code eventId} as freshly seen, scoped to {@code endpoint} so two integrations cannot
     * collide on the same id. Returns {@code true} when THIS call is the first within the window
     * (accept), {@code false} when the id is a still-fresh repeat (reject as a replay).
     */
    boolean firstSeen(String endpoint, String eventId, long nowNanos, Duration window) {
        String composite = endpoint + ' ' + eventId; // event ids are token-charset; space cannot appear
        long ttlNanos = window.toNanos();
        Long prev = seen.putIfAbsent(composite, nowNanos);
        if (prev == null) {
            return true; // first claim within the window
        }
        if (nowNanos - prev > ttlNanos) {
            // the prior sighting aged out of the window — atomically take over the slot
            return seen.replace(composite, prev, nowNanos);
        }
        return false; // still-fresh repeat → replay
    }
}
