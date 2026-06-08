package com.ax.template.authblueprint.activityfeed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Per-(event, user) read state. Idempotent — UNIQUE(event_id, user_id) backs
 * ACT-MARK-001 'second mark preserves the original timestamp'.
 */
@AggregateRoot
@Entity
@Table(
    name = "activity_reads",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_activity_reads_event_user",
        columnNames = {"event_id", "user_id"}
    ),
    indexes = {
        @Index(name = "ix_activity_reads_user", columnList = "user_id")
    }
)
public class ActivityRead {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Column(name = "read_at", nullable = false, updatable = false)
    private Instant readAt;

    protected ActivityRead() {}

    public ActivityRead(UUID eventId, String userId, Instant readAt) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.userId = userId;
        this.readAt = readAt;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getUserId() { return userId; }
    public Instant getReadAt() { return readAt; }
}
