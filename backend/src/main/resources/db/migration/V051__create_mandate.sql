-- mandate-fanout reference workload — realizes specs/mandate-fanout-l0.yaml
-- (P1-37 one-mandate-fanout / P1-35 multi-check-battery / P1-31 deemed-election):
-- one directive fans out to EXACTLY N child tasks (completion is a DERIVED recall, never a flag),
-- gated by a pass-all check battery, with a deemed default on a child's silence past its deadline,
-- and a PESSIMISTIC_WRITE-serialized terminal so each child resolves exactly once (CWE-362).

CREATE TABLE mandates (
    id            UUID         NOT NULL PRIMARY KEY,
    directive     VARCHAR(500) NOT NULL,
    issued_count  INTEGER      NOT NULL,          -- the recorded fan-out target N (immutable)
    status        VARCHAR(20)  NOT NULL,          -- ISSUED | SATISFIED
    satisfied_by  VARCHAR(200),
    satisfied_at  TIMESTAMP,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL,
    -- MANDATE-FANOUT/BATTERY-001 — N positive (partial/empty fan-out unrepresentable);
    -- a SATISFIED mandate records who cleared the battery and when.
    CONSTRAINT chk_mandate CHECK (
        issued_count > 0
        AND (status <> 'SATISFIED' OR (satisfied_by IS NOT NULL AND satisfied_at IS NOT NULL))
    )
);

-- MANDATE-FANOUT/CONCURRENT-001 — one child task per (mandate, task_seq); a terminal task carries
-- its resolver / reason / instant (the @Check), so a bare un-recorded terminal is unrepresentable.
CREATE TABLE mandate_tasks (
    id              UUID         NOT NULL PRIMARY KEY,
    mandate_id      UUID         NOT NULL REFERENCES mandates(id),
    task_seq        INTEGER      NOT NULL,
    state           VARCHAR(20)  NOT NULL,        -- PENDING | DONE | DECLINED | DEEMED
    deemed_deadline TIMESTAMP    NOT NULL,        -- past this with no response → DEEMED
    resolved_by     VARCHAR(200),
    resolve_reason  VARCHAR(20),                  -- EXPLICIT | DEEMED
    resolved_at     TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT chk_mandate_task CHECK (
        state = 'PENDING'
        OR (resolved_by IS NOT NULL AND resolved_at IS NOT NULL AND resolve_reason IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_mandate_task_seq ON mandate_tasks (mandate_id, task_seq);

-- MANDATE-BATTERY-001 — one declared check per (mandate, check_key); the verdict supersedes on the
-- SAME row (idempotent on the key), never a duplicate.
CREATE TABLE mandate_checks (
    id          UUID         NOT NULL PRIMARY KEY,
    mandate_id  UUID         NOT NULL REFERENCES mandates(id),
    check_key   VARCHAR(100) NOT NULL,
    verdict     VARCHAR(20)  NOT NULL,            -- PENDING | PASSED | FAILED
    recorded_by VARCHAR(200),
    recorded_at TIMESTAMP,
    declared_at TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_mandate_check_key ON mandate_checks (mandate_id, check_key);
