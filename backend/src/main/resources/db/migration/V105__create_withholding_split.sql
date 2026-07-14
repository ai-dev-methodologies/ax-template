-- withholding-split reference workload — realizes specs/withholding-split-l0.yaml
-- (Lane G capital-markets wave: a gross payment posting splits into EXACTLY withholding + net legs
-- in one transaction, sum == gross to the cent; the rate that produced the split is snapshotted
-- immutably on the posting; a per-period remittance run collects the withholding legs idempotently).

CREATE TABLE withholding_postings (
    id                      UUID          NOT NULL PRIMARY KEY,
    gross_amount            NUMERIC(19,2) NOT NULL,
    rate                    NUMERIC(9,6)  NOT NULL,
    period                  VARCHAR(7)    NOT NULL,       -- YYYY-MM
    correction_of_posting_id UUID,
    created_at              TIMESTAMP     NOT NULL,
    CONSTRAINT chk_withholding_posting CHECK (gross_amount <> 0 AND rate >= 0 AND rate < 1)
);

CREATE TABLE withholding_legs (
    id          UUID          NOT NULL PRIMARY KEY,
    posting_id  UUID          NOT NULL,
    leg_type    VARCHAR(20)   NOT NULL,       -- WITHHOLDING | NET
    amount      NUMERIC(19,2) NOT NULL,
    created_at  TIMESTAMP     NOT NULL
);

CREATE INDEX idx_withholding_legs_posting ON withholding_legs (posting_id);

CREATE TABLE remittance_runs (
    id             UUID          NOT NULL PRIMARY KEY,
    period         VARCHAR(7)    NOT NULL,
    total_withheld NUMERIC(19,2) NOT NULL,
    posting_count  INTEGER       NOT NULL,
    collected_at   TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX uq_remittance_period ON remittance_runs (period);
