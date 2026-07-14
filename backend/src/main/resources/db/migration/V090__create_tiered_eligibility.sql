-- tiered-eligibility reference workload — realizes specs/tiered-eligibility-l0.yaml (P3-29, backlog-100
-- Lane B energy wave, composing threshold-terminal-derivation-l0's crossing mechanism generalized to an
-- N-tier ladder). A ladder's currentTierIndex is ALWAYS the derived function of count against its own
-- ordered tier thresholds; the automatic accrual path can only increase count (monotone degrade-only),
-- and an explicit, audited restore is the ONLY path that may decrease it.

CREATE TABLE tier_ladders (
    id                 UUID         NOT NULL PRIMARY KEY,
    ladder_key         VARCHAR(200) NOT NULL,
    ladder_count       INTEGER      NOT NULL,
    current_tier_index INTEGER     NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL,
    CONSTRAINT chk_tier_ladder_nonnegative CHECK (ladder_count >= 0 AND current_tier_index >= 0)
);

CREATE UNIQUE INDEX uq_tier_ladder_key ON tier_ladders (ladder_key);

-- @ElementCollection on TierLadder — the ordered rungs (ordinal preserves ladder order; enter_at_count
-- for tier 0 is always 0).
CREATE TABLE tier_ladder_tiers (
    ladder_id      UUID         NOT NULL,
    ordinal        INTEGER      NOT NULL,
    tier_name      VARCHAR(100) NOT NULL,
    enter_at_count INTEGER      NOT NULL,
    CONSTRAINT fk_tier_ladder_tiers_ladder FOREIGN KEY (ladder_id) REFERENCES tier_ladders(id)
);

CREATE INDEX ix_tier_ladder_tiers_ladder ON tier_ladder_tiers (ladder_id, ordinal);

-- TIER-LADDER-001 — append-only accrual ledger (the AUTOMATIC, degrade-only path).
CREATE TABLE tier_accruals (
    id               UUID      NOT NULL PRIMARY KEY,
    ladder_id        UUID      NOT NULL,
    delta            INTEGER   NOT NULL,
    count_after      INTEGER   NOT NULL,
    tier_index_after INTEGER   NOT NULL,
    sequence_no      BIGINT    NOT NULL,
    recorded_at      TIMESTAMP NOT NULL,
    CONSTRAINT chk_tier_accrual_delta CHECK (delta > 0)
);

CREATE UNIQUE INDEX uq_tier_accrual_seq ON tier_accruals (ladder_id, sequence_no);

-- TIER-MONOTONE-001 — append-only restore audit ledger, SEPARATE from accruals (the ONLY path back up).
CREATE TABLE tier_restore_events (
    id               UUID         NOT NULL PRIMARY KEY,
    ladder_id        UUID         NOT NULL,
    count_after      INTEGER      NOT NULL,
    tier_index_after INTEGER      NOT NULL,
    reason           VARCHAR(500) NOT NULL,
    sequence_no      BIGINT       NOT NULL,
    recorded_at      TIMESTAMP    NOT NULL,
    CONSTRAINT chk_tier_restore_reason CHECK (reason <> '')
);

CREATE UNIQUE INDEX uq_tier_restore_seq ON tier_restore_events (ladder_id, sequence_no);
