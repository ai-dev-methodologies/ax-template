-- V092__create_tieredauthority.sql
-- amount-tiered-authority-l0 (전결 규정) — P3-18 (Lane C authority verticals).
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration a Flyway-enabled production deployment would execute.
--
-- Trace:
--   ATA-TIER-001     — authority_tier_bands.min_decider_level gates decide()
--   ATA-BOUNDARY-001 — bands validated to tile [lo,hi) at config time (app-level, not a DB check —
--                       the tiling rule spans MULTIPLE rows, which a single-row CHECK cannot express)
--   ATA-SNAPSHOT-001 — tiered_decision_records is fully append-only (every column immutable)

CREATE TABLE IF NOT EXISTS authority_tier_tables (
    id             UUID         PRIMARY KEY,
    table_version  INTEGER      NOT NULL UNIQUE,
    created_by     VARCHAR(200) NOT NULL,
    created_at     TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS authority_tier_bands (
    id                 UUID          PRIMARY KEY,
    table_id           UUID          NOT NULL,
    order_index        INTEGER       NOT NULL,
    min_amount         DECIMAL(15,2) NOT NULL,
    max_amount         DECIMAL(15,2),
    min_decider_level  INTEGER       NOT NULL,
    CONSTRAINT fk_authority_tier_bands_table
        FOREIGN KEY (table_id) REFERENCES authority_tier_tables(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_authority_tier_bands_table_order
    ON authority_tier_bands(table_id, order_index);

CREATE TABLE IF NOT EXISTS tiered_decision_records (
    id                     UUID          PRIMARY KEY,
    table_id               UUID          NOT NULL,
    table_version          INTEGER       NOT NULL,
    amount                 DECIMAL(15,2) NOT NULL,
    band_min_amount        DECIMAL(15,2) NOT NULL,
    band_max_amount        DECIMAL(15,2),
    band_min_decider_level INTEGER       NOT NULL,
    decider_level          INTEGER       NOT NULL,
    outcome                VARCHAR(500),
    decided_by             VARCHAR(200)  NOT NULL,
    decided_at             TIMESTAMP     NOT NULL,
    CONSTRAINT fk_tiered_decision_records_table
        FOREIGN KEY (table_id) REFERENCES authority_tier_tables(id)
);
