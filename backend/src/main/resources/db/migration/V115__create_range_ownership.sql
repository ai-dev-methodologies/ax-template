-- range-ownership-l0 reference workload — realizes specs/range-ownership-l0.yaml
-- (BACKLOG P3-22: number-range ownership governance — an identifier assignment is valid only
-- inside a range block owned by the assigning owner; range blocks never overlap across owners
-- (half-open [start,end), adjacency legal); porting is an append-only reassignment record with
-- containment re-validated against the new owner, current owner always derive-on-read).

-- RNG-NONOVERLAP-002 — a singleton lock row. H2 cannot express a true range-exclusion
-- constraint (unlike PostgreSQL's EXCLUDE USING gist + btree_gist), so this row's
-- PESSIMISTIC_WRITE lock is the authoritative backstop for the block-registration race.
CREATE TABLE range_registry_lock (
    id VARCHAR(20) NOT NULL PRIMARY KEY
);
INSERT INTO range_registry_lock (id) VALUES ('GLOBAL');

CREATE TABLE range_blocks (
    id          UUID         NOT NULL PRIMARY KEY,
    owner_ref   VARCHAR(200) NOT NULL,
    range_start BIGINT       NOT NULL,          -- inclusive lower bound
    range_end   BIGINT       NOT NULL,          -- exclusive upper bound
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT chk_range_block CHECK (range_start < range_end)
);
CREATE INDEX idx_range_blocks_owner ON range_blocks (owner_ref);

CREATE TABLE identifier_assignments (
    id               UUID      NOT NULL PRIMARY KEY,
    identifier_value BIGINT    NOT NULL UNIQUE,
    created_at       TIMESTAMP NOT NULL
);

-- RNG-PORT-003 — one immutable, append-only ownership event per (re)assignment. NO
-- current-owner column exists anywhere — it is always derived-on-read from the latest event.
CREATE TABLE ownership_events (
    id            UUID         NOT NULL PRIMARY KEY,
    assignment_id UUID         NOT NULL REFERENCES identifier_assignments(id),
    from_owner    VARCHAR(200),                -- null on the initial assignment event
    to_owner      VARCHAR(200) NOT NULL,
    reason        VARCHAR(200) NOT NULL,
    occurred_at   TIMESTAMP    NOT NULL
);
CREATE INDEX idx_ownership_events_assignment ON ownership_events (assignment_id, occurred_at);
