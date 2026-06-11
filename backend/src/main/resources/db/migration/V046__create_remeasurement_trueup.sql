-- remeasurement-trueup-l0 (specs/remeasurement-trueup-l0.yaml)
-- TUP-SUPERSEDE: readings append-only; supersession = new row + forward pointer.
-- TUP-RUNVERSION: runs versioned per period, recording their input basis.
-- TUP-DELTA: closed-period corrections post forward as net-delta true-ups.
-- TUP-SEALED: OPEN→CLOSED→SEALED one-way; closed/sealed implies run-of-record.

CREATE TABLE settlement_periods (
    id UUID PRIMARY KEY,
    subject VARCHAR(200) NOT NULL,
    label VARCHAR(100) NOT NULL,
    grid_slots INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    run_of_record_id UUID,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_period_run_of_record CHECK (status = 'OPEN' OR run_of_record_id IS NOT NULL),
    CONSTRAINT ck_period_grid CHECK (grid_slots >= 1)
);

-- column is reading_value, never value (SQL reserved word)
CREATE TABLE meter_readings (
    id UUID PRIMARY KEY,
    period_id UUID NOT NULL,
    slot_index INT NOT NULL,
    slot_version INT NOT NULL,
    reading_value NUMERIC(15,4) NOT NULL,
    source VARCHAR(20) NOT NULL,
    estimation_method VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    superseded_by_id UUID,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_reading_period FOREIGN KEY (period_id) REFERENCES settlement_periods(id),
    CONSTRAINT ck_reading_supersede CHECK (status <> 'SUPERSEDED' OR superseded_by_id IS NOT NULL),
    CONSTRAINT ck_reading_method CHECK ((source = 'ESTIMATED' AND estimation_method IS NOT NULL)
        OR (source = 'ACTUAL' AND estimation_method IS NULL)),
    CONSTRAINT ck_reading_slot_version CHECK (slot_version >= 1)
);
CREATE UNIQUE INDEX uq_reading_slot_version ON meter_readings(period_id, slot_index, slot_version);

CREATE TABLE settlement_runs (
    id UUID PRIMARY KEY,
    period_id UUID NOT NULL,
    run_version INT NOT NULL,
    basis_json VARCHAR(4000) NOT NULL,
    basis_hash VARCHAR(64) NOT NULL,
    total_value NUMERIC(15,4) NOT NULL,
    computed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_run_period FOREIGN KEY (period_id) REFERENCES settlement_periods(id)
);
CREATE UNIQUE INDEX uq_run_version ON settlement_runs(period_id, run_version);

CREATE TABLE trueup_postings (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    source_period_id UUID NOT NULL,
    target_period_id UUID NOT NULL,
    from_run_version INT NOT NULL,
    to_run_version INT NOT NULL,
    amount NUMERIC(15,4) NOT NULL,
    posted_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_posting_run FOREIGN KEY (run_id) REFERENCES settlement_runs(id),
    CONSTRAINT fk_posting_source FOREIGN KEY (source_period_id) REFERENCES settlement_periods(id),
    CONSTRAINT fk_posting_target FOREIGN KEY (target_period_id) REFERENCES settlement_periods(id)
);
CREATE UNIQUE INDEX uq_trueup_run ON trueup_postings(run_id);
