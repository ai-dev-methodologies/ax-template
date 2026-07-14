package com.ax.template.authblueprint.eventingest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * monotonic-event-ingest-l0 dedup ledger row (INGEST-IDEMPOTENT-APPLY-001) — the Idempotent
 * Consumer pattern's processed-message record. A row exists once per (stream, event_id);
 * a redelivered event is recognized by the unique constraint and discarded before any second
 * side effect. Fully append-only — every column is immutable and there is no mutator.
 */
@AggregateMember(root = IngestState.class)
@Entity
@Table(name = "processed_events", uniqueConstraints = {
    @UniqueConstraint(name = "uq_processed_event_stream_eventid", columnNames = {"stream", "event_id"})
})
public class ProcessedEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ingest_state_id", nullable = false, updatable = false)
    private UUID ingestStateId;

    @Column(name = "stream", nullable = false, updatable = false, length = 40)
    private String stream;

    @Column(name = "event_id", nullable = false, updatable = false, length = 200)
    private String eventId;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(UUID id, UUID ingestStateId, String stream, String eventId, Instant appliedAt) {
        this.id = id;
        this.ingestStateId = ingestStateId;
        this.stream = stream;
        this.eventId = eventId;
        this.appliedAt = appliedAt;
    }

    public UUID getId() { return id; }
    public UUID getIngestStateId() { return ingestStateId; }
    public String getStream() { return stream; }
    public String getEventId() { return eventId; }
    public Instant getAppliedAt() { return appliedAt; }
}
