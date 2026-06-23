-- reproducible-procedure reference workload — realizes specs/reproducible-procedure-l0.yaml
-- (P1-7~9: an auditable deterministic procedure — a DRAW records a server-generated SEED +
-- algorithm + canonical input hash + selected ids (replayable from the recorded seed); a
-- CLASSIFICATION records the input hash + pinned classifier version + resolved class (the same
-- input under the same version is idempotent via uq(input_hash, classifier_version, kind), a
-- newer version records a SEPARATE result); a sensitive subject is stored raw but role-blinded).

CREATE TABLE procedures (
    id                 UUID         NOT NULL PRIMARY KEY,
    kind               VARCHAR(20)  NOT NULL,            -- DRAW | CLASSIFICATION
    input_set_ref      VARCHAR(200) NOT NULL,
    input_hash         VARCHAR(64)  NOT NULL,            -- canonical SHA-256 of the input set
    -- DRAW basis (PROC-DRAW-001 / PROC-REPLAY-001)
    seed               BIGINT,                           -- server-generated; the recorded replay basis
    algorithm          VARCHAR(60),
    draw_k             INTEGER      NOT NULL DEFAULT 0,
    candidates         VARCHAR(4000),                    -- canonical (sorted) candidate list, csv
    selected_ids       VARCHAR(4000),                    -- the recorded selection a replay reproduces
    -- CLASSIFICATION basis (PROC-CLASS-001)
    classifier_version VARCHAR(60),
    resolved_class     VARCHAR(120),
    -- BLINDING (PROC-BLIND-001) — raw subject stored, never serialized; ADMIN-only unmask
    raw_subject        VARCHAR(400),
    actor              VARCHAR(200) NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL,
    -- PROC-DRAW/CLASS-001 — a DRAW must carry its seed/algorithm/selection basis; a
    -- CLASSIFICATION must carry its version + resolved class. A bare result is unrepresentable.
    CONSTRAINT chk_procedure CHECK (
        draw_k >= 0
        AND (kind <> 'DRAW' OR (seed IS NOT NULL AND algorithm IS NOT NULL AND selected_ids IS NOT NULL))
        AND (kind <> 'CLASSIFICATION' OR (classifier_version IS NOT NULL AND resolved_class IS NOT NULL))
    )
);

-- PROC-CLASS-001 — the same input under the same classifier version is one row (idempotent);
-- a newer version is a SEPARATE row, so history is never silently re-labeled.
CREATE UNIQUE INDEX uq_procedure_class ON procedures (input_hash, classifier_version, kind);
