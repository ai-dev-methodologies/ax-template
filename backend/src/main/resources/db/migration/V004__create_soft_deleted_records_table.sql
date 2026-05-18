-- Fixture table for PRACTICES-PERS-005: soft-delete via @SQLDelete + @Where.
-- Demonstrates the correct pattern: deleted_at TIMESTAMP (not a boolean flag).
-- A partial index on (id) WHERE deleted_at IS NULL mirrors the @Where clause for
-- the planner, keeping active-record lookups O(log n) on large tables.
CREATE TABLE soft_deleted_records (
    id         UUID          NOT NULL PRIMARY KEY,
    label      VARCHAR(255)  NOT NULL,
    created_at TIMESTAMP     NOT NULL,
    updated_at TIMESTAMP     NOT NULL,
    version    BIGINT        NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP     NULL
);

CREATE INDEX idx_soft_deleted_records_not_deleted
    ON soft_deleted_records (id)
    WHERE deleted_at IS NULL;
