package com.ax.template.authblueprint.recurringinterval;

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
 * completion-reset-recurring-interval-l0 root: a RECURRING obligation whose interval RESETS ON
 * COMPLETION (CRI-RESET-001). The CURRENT window is {@code [windowStart, windowStart +
 * intervalSeconds)}. {@code intervalSeconds} is immutable. {@code windowStart} is advanced ONLY by
 * {@link #completeAndAdvance} (the package-private sole-mutator hook the service calls under the
 * row lock) — completing slides the window FORWARD to the completion instant, so doing the task
 * early shifts the whole future schedule forward (the defining property vs a fixed calendar grid).
 *
 * <p>Due/overdue is NEVER stored authoritatively (CRI-DUE-001): the service recomputes it from the
 * clock and {@code windowStart} on every read. The {@code sweptOverdue} column is a NON-authoritative
 * operational flag the @Scheduled sweep may record (CRI-SWEEP-001); it is not the source of truth.
 * The @Check backstops a positive interval.
 */
@AggregateRoot
@Entity
@Table(name = "recurring_obligations")
@Check(constraints = "interval_seconds > 0")
public class RecurringObligation {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "obligation_key", nullable = false, updatable = false, length = 200, unique = true)
    private String obligationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecurringObligationStatus status;

    /** Immutable — the recurring interval length in seconds (the window width). */
    @Column(name = "interval_seconds", nullable = false, updatable = false)
    private long intervalSeconds;

    /** The CURRENT window's start. Advanced to the completion instant on every completion
     *  (CRI-RESET-001) — measured FROM the completion, never a fixed grid. Mutable, but ONLY
     *  through {@link #completeAndAdvance} under the obligation's PESSIMISTIC_WRITE row lock. */
    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    /** The instant the most recent occurrence closed its window (null until the first completion). */
    @Column(name = "last_completed_at")
    private Instant lastCompletedAt;

    /** NON-authoritative swept overdue flag (CRI-SWEEP-001) — operational visibility only. The
     *  authoritative due/overdue is ALWAYS recomputed from the clock + windowStart (CRI-DUE-001). */
    @Column(name = "swept_overdue", nullable = false)
    private boolean sweptOverdue;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecurringObligation() {}

    public RecurringObligation(UUID id, String obligationKey, long intervalSeconds,
                               Instant windowStart, Instant createdAt) {
        this.id = id;
        this.obligationKey = obligationKey;
        this.status = RecurringObligationStatus.OPEN;
        this.intervalSeconds = intervalSeconds;
        this.windowStart = windowStart;
        this.sweptOverdue = false;
        this.createdAt = createdAt;
    }

    /** CRI-RESET-001 sole-mutator hook (service, under the row lock) — slide the window forward
     *  to the completion instant. A completion clears any stale swept-overdue flag (the new window
     *  is fresh). Returns the windowStart that was just CLOSED, for the occurrence snapshot. */
    Instant completeAndAdvance(Instant completedAt) {
        Instant closed = this.windowStart;
        this.windowStart = completedAt;
        this.lastCompletedAt = completedAt;
        this.sweptOverdue = false;
        return closed;
    }

    /** CRI-SWEEP-001 sole-mutator hook — record the NON-authoritative swept overdue flag. */
    void recordSweptOverdue(boolean overdue) {
        this.sweptOverdue = overdue;
    }

    /** The end of the CURRENT window — the instant this obligation becomes due/overdue. */
    public Instant nextDueAt() {
        return windowStart.plusSeconds(intervalSeconds);
    }

    public UUID getId() { return id; }
    public String getObligationKey() { return obligationKey; }
    public RecurringObligationStatus getStatus() { return status; }
    public long getIntervalSeconds() { return intervalSeconds; }
    public Instant getWindowStart() { return windowStart; }
    public Instant getLastCompletedAt() { return lastCompletedAt; }
    public boolean isSweptOverdue() { return sweptOverdue; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
