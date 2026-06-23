package com.ax.template.authblueprint.recurringinterval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One completion of a recurring obligation's window (CRI-ONCE-001) — appended, immutable, and
 * exactly-once per window. The UNIQUE(obligation_id, closed_window_start) constraint is the
 * exactly-once DB backstop: because completing advances the obligation's windowStart, the closed
 * window's start uniquely identifies a window — a racing second complete on the SAME window cannot
 * append a duplicate even if the in-code lock slipped (CWE-362). There is no update or delete path:
 * the occurrence history is the full append-only trail.
 */
@AggregateMember(root = RecurringObligation.class)
@Entity
@Table(name = "recurring_occurrences", uniqueConstraints = {
    @UniqueConstraint(name = "uq_recurring_window", columnNames = {"obligation_id", "closed_window_start"})
})
public class Occurrence {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "obligation_id", nullable = false, updatable = false)
    private UUID obligationId;

    /** The start of the window this occurrence CLOSED — uniquely identifies the window. */
    @Column(name = "closed_window_start", nullable = false, updatable = false)
    private Instant closedWindowStart;

    @Column(name = "completed_by", nullable = false, updatable = false, length = 200)
    private String completedBy;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt;

    protected Occurrence() {}

    public Occurrence(UUID id, UUID obligationId, Instant closedWindowStart, String completedBy,
                      Instant completedAt) {
        this.id = id;
        this.obligationId = obligationId;
        this.closedWindowStart = closedWindowStart;
        this.completedBy = completedBy;
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public UUID getObligationId() { return obligationId; }
    public Instant getClosedWindowStart() { return closedWindowStart; }
    public String getCompletedBy() { return completedBy; }
    public Instant getCompletedAt() { return completedAt; }
}
