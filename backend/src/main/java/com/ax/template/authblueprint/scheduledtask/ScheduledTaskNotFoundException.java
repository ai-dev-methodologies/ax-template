package com.ax.template.authblueprint.scheduledtask;

import java.util.UUID;

/**
 * Thrown when an admin operation references a non-existent task id.
 * Mapped to 404 by {@link ScheduledTaskController}.
 */
public class ScheduledTaskNotFoundException extends RuntimeException {
    private final UUID id;

    public ScheduledTaskNotFoundException(UUID id) {
        super("Scheduled task not found: id=" + id);
        this.id = id;
    }

    public UUID getId() { return id; }
}
