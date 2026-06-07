-- transformation reference workload — realizes specs/transformation-conservation-l0.yaml
-- (IDW11 dogfood signature cluster: conserve-with-CLASSIFIED-LOSS, the dual of balanced-posting).
-- version = JPA @Version; BigDecimal money/qty columns NUMERIC(19,4) exact-decimal.

CREATE TABLE transformation_runs (
    id             UUID           NOT NULL PRIMARY KEY,
    created_by     VARCHAR(255)   NOT NULL,
    base_unit      VARCHAR(32)    NOT NULL,
    total_input    NUMERIC(19, 4) NOT NULL,
    total_good     NUMERIC(19, 4) NOT NULL,
    total_residual NUMERIC(19, 4) NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMP      NOT NULL,
    -- XFORM-ACCOUNTED-LOSS-001 — exact conservation backstop (never silent shrinkage).
    CONSTRAINT chk_xform_conserved CHECK (total_input = total_good + total_residual),
    CONSTRAINT chk_xform_totals_nonneg CHECK (total_input >= 0 AND total_good >= 0 AND total_residual >= 0)
);

CREATE TABLE transformation_legs (
    id            UUID           NOT NULL PRIMARY KEY,
    run_id        UUID           NOT NULL,
    role          VARCHAR(16)    NOT NULL,
    disposition   VARCHAR(16),
    material_code VARCHAR(120)   NOT NULL,
    qty           NUMERIC(19, 4) NOT NULL,
    unit          VARCHAR(32)    NOT NULL,
    CONSTRAINT chk_xform_leg_role CHECK (role IN ('INPUT', 'GOOD_OUTPUT', 'RESIDUAL')),
    CONSTRAINT chk_xform_leg_disposition
        CHECK (disposition IS NULL OR disposition IN ('SCRAP', 'REWORK', 'YIELD_LOSS', 'WIP_REMAINDER')),
    -- XFORM-RESIDUAL-CLASSIFIED-001 — a residual leg MUST carry a governed disposition (no misc bucket).
    CONSTRAINT chk_xform_residual_classified CHECK (role <> 'RESIDUAL' OR disposition IS NOT NULL),
    CONSTRAINT chk_xform_leg_qty_nonneg CHECK (qty >= 0)
);

CREATE INDEX ix_xform_legs_run ON transformation_legs (run_id);
