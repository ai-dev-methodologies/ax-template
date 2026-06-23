package com.ax.template.authblueprint.mandate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One fan-out child task of a {@link Mandate} (MANDATE-FANOUT-001). Created EXACTLY N at issue
 * time, each with a recorded {@code taskSeq} (unique per mandate — partial fan-out is
 * unrepresentable) and a {@code deemedDeadline}. A task reaches a TERMINAL state EXACTLY ONCE
 * (MANDATE-CONCURRENT-001): DONE / DECLINED explicitly, or DEEMED via the sweep on silence past
 * the deadline (MANDATE-DEEMED-001). The terminal resolution records its resolver, reason, and
 * instant — the @Check backstops that a terminal task carries who/why/when. The row carries
 * @Version so the explicit-complete path and the deemed sweep both take the same locked row.
 */
@AggregateMember(root = Mandate.class)
@Entity
@Table(name = "mandate_tasks", uniqueConstraints = {
    @UniqueConstraint(name = "uq_mandate_task_seq", columnNames = {"mandate_id", "task_seq"})
})
@Check(constraints =
    "state = 'PENDING'"
    + " OR (resolved_by IS NOT NULL AND resolved_at IS NOT NULL AND resolve_reason IS NOT NULL)")
public class MandateTask {

    /** An explicit response (DONE/DECLINED) by a real principal. */
    public static final String REASON_EXPLICIT = "EXPLICIT";
    /** A deemed default election on silence past the deadline (MANDATE-DEEMED-001). */
    public static final String REASON_DEEMED = "DEEMED";
    /** The resolver recorded for a deemed default — never a real principal. */
    public static final String SYSTEM_RESOLVER = "SYSTEM";

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mandate_id", nullable = false, updatable = false)
    private UUID mandateId;

    @Column(name = "task_seq", nullable = false, updatable = false)
    private int taskSeq;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private MandateTaskState state;

    /** The instant past which an unanswered task is DEEMED (MANDATE-DEEMED-001). */
    @Column(name = "deemed_deadline", nullable = false, updatable = false)
    private Instant deemedDeadline;

    @Column(name = "resolved_by", length = 200)
    private String resolvedBy;

    @Column(name = "resolve_reason", length = 20)
    private String resolveReason;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MandateTask() {}

    public MandateTask(UUID id, UUID mandateId, int taskSeq, Instant deemedDeadline, Instant createdAt) {
        this.id = id;
        this.mandateId = mandateId;
        this.taskSeq = taskSeq;
        this.state = MandateTaskState.PENDING;
        this.deemedDeadline = deemedDeadline;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook (service, under the task row lock) — reach a terminal state EXACTLY ONCE.
     *  Records the resolver, the reason (EXPLICIT or DEEMED), and the instant. Only a PENDING task
     *  is ever resolved — the caller gates on {@link #getState()} so a residual race loses
     *  deterministically (MANDATE-CONCURRENT-001). */
    void resolve(MandateTaskState terminal, String by, String reason, Instant at) {
        this.state = terminal;
        this.resolvedBy = by;
        this.resolveReason = reason;
        this.resolvedAt = at;
    }

    public boolean isPending() {
        return state == MandateTaskState.PENDING;
    }

    public UUID getId() { return id; }
    public UUID getMandateId() { return mandateId; }
    public int getTaskSeq() { return taskSeq; }
    public MandateTaskState getState() { return state; }
    public Instant getDeemedDeadline() { return deemedDeadline; }
    public String getResolvedBy() { return resolvedBy; }
    public String getResolveReason() { return resolveReason; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
