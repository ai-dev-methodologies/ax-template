package com.ax.template.authblueprint.eventingest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * monotonic-event-ingest-l0 authoritative current-state row (INGEST-WATERMARK-001): the
 * projection an out-of-order, at-least-once external event stream folds into. The
 * {@code lastAppliedOccurredAt} watermark only ever ADVANCES via the package-private
 * {@link #apply} sole-mutator hook — never rewound by a late-arriving older event
 * (INGEST-REJECT-STALE-001 is enforced in {@link EventIngestService}, not here). One row
 * per (stream, subject).
 */
@AggregateRoot
@Entity
@Table(name = "ingest_states", uniqueConstraints = {
    @UniqueConstraint(name = "uq_ingest_state_stream_subject", columnNames = {"stream", "subject_id"})
})
public class IngestState {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "stream", nullable = false, updatable = false, length = 40)
    private String stream;

    @Column(name = "subject_id", nullable = false, updatable = false, length = 200)
    private String subjectId;

    @Column(name = "state_value", length = 200)
    private String stateValue;

    @Column(name = "last_applied_event_id", nullable = false, length = 200)
    private String lastAppliedEventId;

    /** The watermark (INGEST-WATERMARK-001) — event-time, never the receive-order proxy. */
    @Column(name = "last_applied_occurred_at", nullable = false)
    private Instant lastAppliedOccurredAt;

    @Column(name = "last_applied_captured_at", nullable = false)
    private Instant lastAppliedCapturedAt;

    /** Server-assigned at apply time — never client-supplied (INGEST-CAPTURE-001). */
    @Column(name = "last_applied_recorded_at", nullable = false)
    private Instant lastAppliedRecordedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IngestState() {}

    public IngestState(UUID id, String stream, String subjectId, String stateValue, String eventId,
                       Instant occurredAt, Instant capturedAt, Instant recordedAt, Instant createdAt) {
        this.id = id;
        this.stream = stream;
        this.subjectId = subjectId;
        this.stateValue = stateValue;
        this.lastAppliedEventId = eventId;
        this.lastAppliedOccurredAt = occurredAt;
        this.lastAppliedCapturedAt = capturedAt;
        this.lastAppliedRecordedAt = recordedAt;
        this.createdAt = createdAt;
    }

    /**
     * Sole-mutator hook. Callers (see {@link EventIngestService#apply}) MUST have already
     * verified {@code occurredAt} is strictly ahead of the current watermark — this method
     * performs no ordering check itself, it only records the advance.
     */
    void apply(String stateValue, String eventId, Instant occurredAt, Instant capturedAt, Instant recordedAt) {
        this.stateValue = stateValue;
        this.lastAppliedEventId = eventId;
        this.lastAppliedOccurredAt = occurredAt;
        this.lastAppliedCapturedAt = capturedAt;
        this.lastAppliedRecordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public String getStream() { return stream; }
    public String getSubjectId() { return subjectId; }
    public String getStateValue() { return stateValue; }
    public String getLastAppliedEventId() { return lastAppliedEventId; }
    public Instant getLastAppliedOccurredAt() { return lastAppliedOccurredAt; }
    public Instant getLastAppliedCapturedAt() { return lastAppliedCapturedAt; }
    public Instant getLastAppliedRecordedAt() { return lastAppliedRecordedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
