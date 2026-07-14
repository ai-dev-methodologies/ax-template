-- monotonic-event-ingest-l0 (specs/monotonic-event-ingest-l0.yaml)
-- INGEST-WATERMARK: the row's last_applied_occurred_at watermark only ever advances.
-- INGEST-REJECT-STALE: an event at/behind the watermark never mutates this row.
-- INGEST-IDEMPOTENT-APPLY: processed_events is the dedup ledger on (stream, event_id).
-- INGEST-CAPTURE: last_applied_recorded_at is always server-assigned, never client-supplied.

CREATE TABLE ingest_states (
    id UUID PRIMARY KEY,
    stream VARCHAR(40) NOT NULL,
    subject_id VARCHAR(200) NOT NULL,
    state_value VARCHAR(200),
    last_applied_event_id VARCHAR(200) NOT NULL,
    last_applied_occurred_at TIMESTAMP NOT NULL,
    last_applied_captured_at TIMESTAMP NOT NULL,
    last_applied_recorded_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX uq_ingest_state_stream_subject ON ingest_states(stream, subject_id);

CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    ingest_state_id UUID NOT NULL,
    stream VARCHAR(40) NOT NULL,
    event_id VARCHAR(200) NOT NULL,
    applied_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_processed_event_state FOREIGN KEY (ingest_state_id) REFERENCES ingest_states(id)
);
CREATE UNIQUE INDEX uq_processed_event_stream_eventid ON processed_events(stream, event_id);
