-- soft-delete-l0 reference workload (specs/soft-delete-l0.yaml).
-- Tombstoned parent + child with a deleted_at column (NULL = live). SOFTDELETE-UNIQUE-001 is
-- enforced by a PARTIAL unique index scoped to LIVE rows, so a soft-deleted email is freed for reuse.
CREATE TABLE soft_delete_accounts (
    id         UUID         NOT NULL PRIMARY KEY,
    owner_id   VARCHAR(255) NOT NULL,
    email      VARCHAR(320) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    deleted_at TIMESTAMP
);

CREATE INDEX ix_sda_owner ON soft_delete_accounts (owner_id);

-- SOFTDELETE-UNIQUE-001: unique among LIVE rows only (deleted_at IS NULL), per-owner.
CREATE UNIQUE INDEX uq_sda_email_live
    ON soft_delete_accounts (owner_id, email)
    WHERE deleted_at IS NULL;

CREATE TABLE soft_delete_notes (
    id         UUID          NOT NULL PRIMARY KEY,
    account_id UUID          NOT NULL,
    text       VARCHAR(1000) NOT NULL,
    deleted_at TIMESTAMP
);

CREATE INDEX ix_sdn_account ON soft_delete_notes (account_id);
