-- cash-in-lieu reference workload — realizes specs/cash-in-lieu-l0.yaml
-- (Lane G capital-markets wave: a fractional entitlement splits into integer units-in-kind + a
-- fractional remainder; the fraction is NEVER allocated in kind — it is converted to cash at a rate
-- snapshot recorded immutably. Allocation is idempotent per (subject, event)).

CREATE TABLE cash_in_lieu_allocations (
    id                   UUID          NOT NULL PRIMARY KEY,
    subject_ref          VARCHAR(200)  NOT NULL,
    event_ref            VARCHAR(200)  NOT NULL,
    raw_entitlement      NUMERIC(24,6) NOT NULL,
    units_in_kind        BIGINT        NOT NULL,
    fractional_remainder NUMERIC(24,6) NOT NULL,
    cash_rate            NUMERIC(19,6) NOT NULL,
    cash_value           NUMERIC(19,2) NOT NULL,
    allocated_at         TIMESTAMP     NOT NULL,
    CONSTRAINT chk_cash_in_lieu CHECK (
        units_in_kind >= 0 AND fractional_remainder >= 0 AND fractional_remainder < 1
        AND cash_rate > 0 AND cash_value >= 0
    )
);

CREATE UNIQUE INDEX uq_cil_subject_event ON cash_in_lieu_allocations (subject_ref, event_ref);
