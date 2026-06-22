-- settlement-finality reference workload — realizes specs/settlement-finality-l0.yaml
-- (IDW15 capital-markets residual P1-24~27: post-trade delivery-versus-payment finality —
--  DvP both legs commit atomically; novation replaces a counterparty conserving the net
--  obligation before finality; SETTLED is the irrevocable final state; a failed settlement
--  walks the exactly-once fail ladder PENDING→FAILED→RETRY→BUYIN). No delete path exists.

CREATE TABLE settlement_instructions (
    id               UUID          NOT NULL PRIMARY KEY,
    trade_ref        VARCHAR(100)  NOT NULL,
    delivery_party   VARCHAR(200)  NOT NULL,         -- replaceable by novation before finality
    payment_party    VARCHAR(200)  NOT NULL,         -- replaceable by novation before finality
    net_obligation   NUMERIC(19,4) NOT NULL,         -- CONSERVED across novation (never changes)
    delivery_settled BOOLEAN       NOT NULL,
    payment_settled  BOOLEAN       NOT NULL,
    status           VARCHAR(20)   NOT NULL,         -- PENDING | SETTLED | FAILED | RETRY | BUYIN
    final_at         TIMESTAMP,                       -- the recorded instant of finality
    version          BIGINT        NOT NULL DEFAULT 0,
    created_at       TIMESTAMP     NOT NULL,
    -- SETTLE-DVP-001 — both legs settle atomically or neither (partial settlement unrepresentable);
    -- SETTLE-FINAL-001 — SETTLED implies both legs settled + a recorded final instant, and a
    -- non-final instruction carries no finality instant.
    CONSTRAINT chk_settlement_dvp_finality CHECK (
        delivery_settled = payment_settled
        AND (status <> 'SETTLED' OR (delivery_settled = TRUE AND final_at IS NOT NULL))
        AND (status = 'SETTLED' OR final_at IS NULL)
    )
);

-- SETTLE-NOVATE-001 — one immutable record per counterparty replacement; the conserved
-- obligation is recorded verbatim per row; the released and assuming parties must differ.
CREATE TABLE novation_records (
    id                 UUID          NOT NULL PRIMARY KEY,
    instruction_id     UUID          NOT NULL REFERENCES settlement_instructions(id),
    leg                VARCHAR(20)   NOT NULL,        -- DELIVERY | PAYMENT
    released_party     VARCHAR(200)  NOT NULL,
    assuming_party     VARCHAR(200)  NOT NULL,
    assumed_obligation NUMERIC(19,4) NOT NULL,        -- == the released party's discharged obligation
    novated_by         VARCHAR(200)  NOT NULL,
    novated_at         TIMESTAMP     NOT NULL,
    CONSTRAINT chk_novation_substitution CHECK (released_party <> assuming_party AND assumed_obligation >= 0)
);

CREATE INDEX ix_novation_instruction ON novation_records (instruction_id);
