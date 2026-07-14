-- additive-fact-ledger-l0 (specs/additive-fact-ledger-l0.yaml)
-- FACT-ADDITIVE-ACCUM: many small facts ACCUMULATE per period (Σ facts == period total).
-- FACT-LATE-DELTA-POST: a late fact for a CLOSED period posts a delta into the CURRENT open
--   period referencing the origin — the closed period's frozen_aggregate is never touched.
-- FACT-CLOSED-PERIOD-ADD: frozen_aggregate is set exactly once, at close, and never rewritten.
-- FACT-IDEMPOTENT: facts are unique on (source, external_fact_id) — duplicates accumulate once.

CREATE TABLE fact_periods (
    id UUID PRIMARY KEY,
    subject VARCHAR(200) NOT NULL,
    label VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    frozen_aggregate NUMERIC(15,4),
    closed_at TIMESTAMP,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_fact_period_frozen CHECK (status = 'OPEN' OR frozen_aggregate IS NOT NULL)
);

CREATE TABLE facts (
    id UUID PRIMARY KEY,
    period_id UUID NOT NULL,
    source VARCHAR(100) NOT NULL,
    external_fact_id VARCHAR(200) NOT NULL,
    amount NUMERIC(15,4) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_fact_period FOREIGN KEY (period_id) REFERENCES fact_periods(id)
);
CREATE UNIQUE INDEX uq_fact_source_external_id ON facts(source, external_fact_id);

CREATE TABLE late_delta_postings (
    id UUID PRIMARY KEY,
    current_period_id UUID NOT NULL,
    origin_period_id UUID NOT NULL,
    fact_id UUID NOT NULL,
    amount NUMERIC(15,4) NOT NULL,
    posted_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_posting_current_period FOREIGN KEY (current_period_id) REFERENCES fact_periods(id),
    CONSTRAINT fk_posting_origin_period FOREIGN KEY (origin_period_id) REFERENCES fact_periods(id),
    CONSTRAINT fk_posting_fact FOREIGN KEY (fact_id) REFERENCES facts(id)
);
