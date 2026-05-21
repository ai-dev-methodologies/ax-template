package com.ax.template.authblueprint.reportexport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit coverage for {@link ExportJobStateMachine}.
 *
 * <p>Trace: EXPORT-LIFECYCLE-004 — illegal transitions throw and are not persisted.
 */
@Tag("REPORT_EXPORT")
class ExportJobStateMachineTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-21T00:00:00Z"), ZoneOffset.UTC);
    private final ExportJobStateMachine sm = new ExportJobStateMachine(clock);

    @Test
    @Tag("EXPORT-LIFECYCLE-004")
    void pendingToRunningToCompleted_isAllowed() {
        ExportJob job = newJob();

        sm.markRunning(job);
        assertThat(job.getStatus()).isEqualTo(ExportJobStatus.RUNNING);
        assertThat(job.getStartedAt()).isNotNull();

        sm.markCompleted(job, new byte[]{1, 2, 3}, 3L);
        assertThat(job.getStatus()).isEqualTo(ExportJobStatus.COMPLETED);
        assertThat(job.getRowCount()).isEqualTo(3L);
        assertThat(job.getSizeBytes()).isEqualTo(3L);
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    @Tag("EXPORT-LIFECYCLE-004")
    void runningToFailed_isAllowed_andClearsPayload() {
        ExportJob job = newJob();
        sm.markRunning(job);
        sm.markFailed(job, "writer crashed");

        assertThat(job.getStatus()).isEqualTo(ExportJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("writer crashed");
        assertThat(job.getPayload()).isNull();
    }

    @Test
    @Tag("EXPORT-LIFECYCLE-004")
    void pendingToCancelled_isAllowed() {
        ExportJob job = newJob();

        sm.markCancelled(job);

        assertThat(job.getStatus()).isEqualTo(ExportJobStatus.CANCELLED);
    }

    @Test
    @Tag("EXPORT-LIFECYCLE-004")
    void runningToRunning_isRejected() {
        ExportJob job = newJob();
        sm.markRunning(job);

        assertThatThrownBy(() -> sm.markRunning(job))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("illegal transition");
    }

    @Test
    @Tag("EXPORT-LIFECYCLE-004")
    void pendingToCompleted_isRejected() {
        ExportJob job = newJob();

        assertThatThrownBy(() -> sm.markCompleted(job, new byte[]{}, 0L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Tag("EXPORT-LIFECYCLE-004")
    void completedToAnything_isRejected() {
        ExportJob job = newJob();
        sm.markRunning(job);
        sm.markCompleted(job, new byte[]{1}, 1L);

        assertThatThrownBy(() -> sm.markRunning(job)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sm.markFailed(job, "x")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sm.markCancelled(job)).isInstanceOf(IllegalStateException.class);
    }

    private static ExportJob newJob() {
        return ExportJob.builder()
            .ownerUserId("u-1")
            .format(ExportFormat.CSV)
            .status(ExportJobStatus.PENDING)
            .build();
    }
}
