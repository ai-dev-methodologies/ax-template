package com.ax.template.authblueprint.scheduledtask;

import java.time.Instant;
import java.util.Objects;

/**
 * Minimal context handed to a {@link TaskHandler} on each invocation.
 */
public final class TaskExecutionContext {
    private final String taskName;
    private final Instant startedAt;

    public TaskExecutionContext(String taskName) {
        this.taskName = Objects.requireNonNull(taskName, "taskName");
        this.startedAt = Instant.now();
    }

    public String getTaskName() { return taskName; }
    public Instant getStartedAt() { return startedAt; }
}
