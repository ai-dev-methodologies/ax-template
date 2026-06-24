-- quorum-resolution reference workload — realizes specs/quorum-resolution-l0.yaml
-- A decision body resolves a motion by collecting immutable ballots from eligible weighted
-- voters, then resolving the outcome by a policy FROZEN at motion-open. Resolution is a
-- PURE reproducible function of the cast ballots + frozen policy.
--
-- CORRECTNESS TRAPS addressed in the schema:
-- QR-QUORUM-001: quorum is measured against total ELIGIBLE weight (the denominator), not
--   cast weight (which would trivially self-satisfy).
-- QR-DOUBLE-VOTE: UNIQUE(motion_id, voter_id) on ballots makes double-vote unrepresentable.
-- QR-IDEMPOTENT-001: UNIQUE(motion_id) on resolutions — second insert is mechanically blocked.
-- QR-FREEZE-001: all policy/roster/weight columns are NOT NULL and carry no UPDATE path in
--   application code; the only mutable column in motions is status.

CREATE TABLE motions (
    id                    UUID         NOT NULL PRIMARY KEY,
    convener_id           VARCHAR(200) NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    total_eligible_weight BIGINT       NOT NULL,
    -- Policy snapshot — frozen at open (application code: all updatable=false)
    rule_type             VARCHAR(20)  NOT NULL,
    threshold_numerator   BIGINT       NOT NULL,
    threshold_denominator BIGINT       NOT NULL,
    quorum_numerator      BIGINT       NOT NULL,
    quorum_denominator    BIGINT       NOT NULL,
    abstention_mode       VARCHAR(30)  NOT NULL,
    tie_break_mode        VARCHAR(20)  NOT NULL,
    tie_break_voter_id    VARCHAR(200),
    version               BIGINT       NOT NULL DEFAULT 0,
    created_at            TIMESTAMP    NOT NULL,
    CONSTRAINT chk_motion CHECK (
        status IN ('OPEN','TALLYING','RESOLVED')
        AND total_eligible_weight >= 0
        AND threshold_denominator > 0
        AND quorum_denominator > 0
        AND (tie_break_mode <> 'CHAIR_CASTING' OR tie_break_voter_id IS NOT NULL)
    )
);

CREATE TABLE quorum_eligible_voters (
    id        UUID         NOT NULL PRIMARY KEY,
    motion_id UUID         NOT NULL REFERENCES motions(id),
    voter_id  VARCHAR(200) NOT NULL,
    weight    BIGINT       NOT NULL,
    -- QR-ROSTER: one row per voter per motion — duplicate voter unrepresentable
    CONSTRAINT uq_quorum_eligible_voter UNIQUE (motion_id, voter_id),
    CONSTRAINT chk_eligible_voter CHECK (weight > 0)
);

CREATE INDEX ix_eligible_voter_motion ON quorum_eligible_voters (motion_id);

CREATE TABLE quorum_ballots (
    id             UUID        NOT NULL PRIMARY KEY,
    motion_id      UUID        NOT NULL REFERENCES motions(id),
    voter_id       VARCHAR(200) NOT NULL,
    choice         VARCHAR(10) NOT NULL,
    weight_at_cast BIGINT      NOT NULL,     -- copied from voter's frozen weight at cast time
    cast_at        TIMESTAMP   NOT NULL,
    -- QR-DOUBLE-VOTE: one ballot per voter per motion — duplicate vote unrepresentable
    CONSTRAINT uq_quorum_ballot_voter UNIQUE (motion_id, voter_id),
    CONSTRAINT chk_ballot CHECK (choice IN ('YES','NO','ABSTAIN') AND weight_at_cast > 0)
);

CREATE INDEX ix_ballot_motion ON quorum_ballots (motion_id);

CREATE TABLE quorum_resolutions (
    id                    UUID        NOT NULL PRIMARY KEY,
    motion_id             UUID        NOT NULL REFERENCES motions(id),
    outcome               VARCHAR(20) NOT NULL,
    yes_weight            BIGINT      NOT NULL,
    no_weight             BIGINT      NOT NULL,
    abstain_weight        BIGINT      NOT NULL,
    cast_eligible_weight  BIGINT      NOT NULL,
    total_eligible_weight BIGINT      NOT NULL,
    resolved_at           TIMESTAMP   NOT NULL,
    -- QR-IDEMPOTENT-001: one resolution per motion — re-resolve returns the existing row
    CONSTRAINT uq_quorum_resolution_motion UNIQUE (motion_id),
    CONSTRAINT chk_resolution CHECK (
        outcome IN ('PASSED','REJECTED','NO_DECISION')
        AND yes_weight + no_weight + abstain_weight <= total_eligible_weight
        AND cast_eligible_weight <= total_eligible_weight
    )
);
