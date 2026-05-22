-- V022__create_activity_feed.sql
-- Activity-feed domain schema (R35).
--
-- Trace:
--   ACT-PUBLISH-003 — UNIQUE(actor_user_id, idempotency_key) backs idempotent publish
--   ACT-MARK-001    — UNIQUE(event_id, user_id) on activity_reads backs idempotent mark

CREATE TABLE IF NOT EXISTS activity_events (
    id                 UUID         PRIMARY KEY,
    actor_user_id      VARCHAR(255) NOT NULL,
    verb               VARCHAR(64)  NOT NULL,
    object_type        VARCHAR(64)  NOT NULL,
    object_id          VARCHAR(255) NOT NULL,
    subject_type       VARCHAR(64),
    subject_id         VARCHAR(255),
    metadata_json      VARCHAR(4096),
    idempotency_key    VARCHAR(128),
    created_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uq_activity_events_actor_idempotency UNIQUE (actor_user_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS ix_activity_events_actor_created
    ON activity_events(actor_user_id, created_at);
CREATE INDEX IF NOT EXISTS ix_activity_events_object
    ON activity_events(object_type, object_id);
CREATE INDEX IF NOT EXISTS ix_activity_events_created
    ON activity_events(created_at);

CREATE TABLE IF NOT EXISTS activity_event_audience (
    event_id          UUID         NOT NULL,
    audience_user_id  VARCHAR(255) NOT NULL,
    PRIMARY KEY (event_id, audience_user_id),
    CONSTRAINT fk_activity_event_audience_event
        FOREIGN KEY (event_id) REFERENCES activity_events(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_activity_event_audience_user
    ON activity_event_audience(audience_user_id, event_id);

CREATE TABLE IF NOT EXISTS activity_reads (
    id          UUID         PRIMARY KEY,
    event_id    UUID         NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    read_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uq_activity_reads_event_user UNIQUE (event_id, user_id),
    CONSTRAINT fk_activity_reads_event
        FOREIGN KEY (event_id) REFERENCES activity_events(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_activity_reads_user
    ON activity_reads(user_id);
