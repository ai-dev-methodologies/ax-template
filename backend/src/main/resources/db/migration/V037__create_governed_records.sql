-- attested-change-record reference workload — realizes specs/attested-change-record-l0.yaml
-- (IDW12 dogfood flagship: ALCOA / 21 CFR 11.10(e) attested-governed-mutation integrity).
-- A governed datum's value is mutated ONLY via an appended, immutable change record.

CREATE TABLE governed_data (
    id           UUID          NOT NULL PRIMARY KEY,
    name         VARCHAR(200)  NOT NULL,
    datum_value  VARCHAR(2000) NOT NULL,   -- "value" is a reserved word in H2/SQL
    version      BIGINT        NOT NULL DEFAULT 0,
    created_by VARCHAR(255)  NOT NULL,
    created_at TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_governed_data_name ON governed_data (name);

-- ACR-APPEND-ONLY-001 — append-only; rows are never UPDATEd or deleted (21 CFR 11.10(e):
-- "record changes shall not obscure previously recorded information"). The application never
-- issues an UPDATE/DELETE against this table; a correction is a NEW appended row.
CREATE TABLE governed_change_records (
    id                   UUID          NOT NULL PRIMARY KEY,
    datum_id             UUID          NOT NULL,
    field_name           VARCHAR(120)  NOT NULL,
    sequence_no          BIGINT        NOT NULL,
    old_value            VARCHAR(2000),
    new_value            VARCHAR(2000) NOT NULL,
    reason               VARCHAR(1000) NOT NULL,
    reason_vocab_version VARCHAR(40),
    actor                VARCHAR(255)  NOT NULL,
    occurred_at          TIMESTAMP     NOT NULL,
    CONSTRAINT chk_governed_change_seq CHECK (sequence_no >= 1),
    -- ACR-ENVELOPE-001 — reason is mandatory and non-blank on every change row.
    CONSTRAINT chk_governed_change_reason CHECK (LENGTH(TRIM(reason)) > 0)
);

-- one monotonic sequence per (datum, field) — causal order, and a duplicate is unrepresentable.
CREATE UNIQUE INDEX uq_governed_change_seq ON governed_change_records (datum_id, field_name, sequence_no);
CREATE INDEX ix_governed_change_datum ON governed_change_records (datum_id, sequence_no);
