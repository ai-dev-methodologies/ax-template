-- V093__create_appeal_independence.sql
-- appeal-decider-independence-l0 — P3-17 (Lane C authority verticals).
--
-- NOTE: Flyway is not on the runtime classpath in this template — Hibernate
-- ddl-auto=create-drop manages the H2 reference workload. This file documents
-- the migration a Flyway-enabled production deployment would execute.
--
-- Trace:
--   APPEAL-DISTINCT-001 — CHECK backstops the immediate-parent decider-distinctness case
--   APPEAL-CHAIN-001    — UNIQUE(parent_decision_id) enforces one appeal per level
--   APPEAL-OUTCOME-001  — every column is immutable (append-only; no UPDATE ever issued)

CREATE TABLE IF NOT EXISTS appeal_decisions (
    id                   UUID         PRIMARY KEY,
    parent_decision_id   UUID,
    chain_root_id        UUID         NOT NULL,
    level                INTEGER      NOT NULL,
    kind                 VARCHAR(16)  NOT NULL,
    decided_by           VARCHAR(200) NOT NULL,
    appealed_decider_by  VARCHAR(200),
    outcome              VARCHAR(32)  NOT NULL,
    decided_at           TIMESTAMP    NOT NULL,
    CONSTRAINT uq_appeal_parent_decision UNIQUE (parent_decision_id),
    CONSTRAINT fk_appeal_decisions_parent
        FOREIGN KEY (parent_decision_id) REFERENCES appeal_decisions(id),
    CONSTRAINT ck_appeal_decider_independent
        CHECK ((kind = 'ORIGINAL') OR (appealed_decider_by IS NOT NULL AND decided_by <> appealed_decider_by))
);

CREATE INDEX IF NOT EXISTS ix_appeal_decisions_chain_root_level
    ON appeal_decisions(chain_root_id, level);
