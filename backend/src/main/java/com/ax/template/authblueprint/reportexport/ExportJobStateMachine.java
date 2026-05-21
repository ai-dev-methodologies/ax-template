package com.ax.template.authblueprint.reportexport;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link ExportJob#getStatus()}.
 *
 * <p>Trace: EXPORT-LIFECYCLE-004 — illegal transitions throw {@link IllegalStateException}
 * and are never persisted. Mirrors the Payment / Subscription / Order state-machine
 * pattern adopted across the catalog (see also {@code SubscriptionStateMachine},
 * {@code OrderStateMachine}, {@code PaymentStateMachine}).
 *
 * <p>Allowed transitions (manifest {@code lifecycle.state_machine.transitions}):
 * <pre>
 *   PENDING   → RUNNING, CANCELLED
 *   RUNNING   → COMPLETED, FAILED
 *   COMPLETED → ∅ (terminal)
 *   FAILED    → ∅ (terminal)
 *   CANCELLED → ∅ (terminal)
 * </pre>
 */
@Component
public class ExportJobStateMachine {

    private static final Map<ExportJobStatus, Set<ExportJobStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ExportJobStatus.class);
        ALLOWED.put(ExportJobStatus.PENDING, EnumSet.of(ExportJobStatus.RUNNING, ExportJobStatus.CANCELLED));
        ALLOWED.put(ExportJobStatus.RUNNING, EnumSet.of(ExportJobStatus.COMPLETED, ExportJobStatus.FAILED));
        ALLOWED.put(ExportJobStatus.COMPLETED, EnumSet.noneOf(ExportJobStatus.class));
        ALLOWED.put(ExportJobStatus.FAILED, EnumSet.noneOf(ExportJobStatus.class));
        ALLOWED.put(ExportJobStatus.CANCELLED, EnumSet.noneOf(ExportJobStatus.class));
    }

    private final Clock clock;

    public ExportJobStateMachine(Clock clock) {
        this.clock = clock;
    }

    /** PENDING → RUNNING; stamps {@code startedAt}. */
    public void markRunning(ExportJob job) {
        assertTransition(job.getStatus(), ExportJobStatus.RUNNING);
        job.setStatus(ExportJobStatus.RUNNING);
        job.setStartedAt(Instant.now(clock));
    }

    /** RUNNING → COMPLETED; stamps payload + size + completedAt. */
    public void markCompleted(ExportJob job, byte[] payload, long rowCount) {
        assertTransition(job.getStatus(), ExportJobStatus.COMPLETED);
        job.setStatus(ExportJobStatus.COMPLETED);
        job.setPayload(payload);
        job.setSizeBytes(payload == null ? 0L : payload.length);
        job.setRowCount(rowCount);
        job.setCompletedAt(Instant.now(clock));
    }

    /** RUNNING → FAILED; records the error message and clears any partial payload. */
    public void markFailed(ExportJob job, String reason) {
        assertTransition(job.getStatus(), ExportJobStatus.FAILED);
        job.setStatus(ExportJobStatus.FAILED);
        job.setPayload(null);
        job.setErrorMessage(reason);
        job.setCompletedAt(Instant.now(clock));
    }

    /** PENDING → CANCELLED. */
    public void markCancelled(ExportJob job) {
        assertTransition(job.getStatus(), ExportJobStatus.CANCELLED);
        job.setStatus(ExportJobStatus.CANCELLED);
        job.setCompletedAt(Instant.now(clock));
    }

    private static void assertTransition(ExportJobStatus from, ExportJobStatus to) {
        Set<ExportJobStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(ExportJobStatus.class));
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                "illegal transition: " + from + " → " + to + "; allowed = " + allowed);
        }
    }
}
