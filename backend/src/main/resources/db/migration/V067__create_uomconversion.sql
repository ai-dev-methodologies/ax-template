-- dimensional-uom-conversion reference workload — realizes specs/dimensional-uom-conversion-l0.yaml
-- (P1-53: cross-dimension unit conversion with a dimensional-compatibility precondition + a versioned
-- bridging material property (density / unit-weight) + a recorded reconstructible basis + deterministic
-- BigDecimal arithmetic). DISTINCT from order-multiple-quantization (rounds WITHIN one dimension).

CREATE TABLE uom_materials (
    id              UUID         NOT NULL PRIMARY KEY,
    material_ref    VARCHAR(200) NOT NULL,
    current_version BIGINT       NOT NULL DEFAULT 0,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT chk_uom_material CHECK (current_version >= 0)
);

-- UOMCONV-MATERIAL/VERSION-001 — one immutable bridging-property version per (material, version);
-- a corrected density is a NEW version (append-only), the prior versions preserved. The factor is the
-- to-dimension base per from-dimension base (VOLUME→MASS = density kg/L; COUNT→MASS = unit-weight kg/each).
CREATE TABLE uom_material_properties (
    id             UUID           NOT NULL PRIMARY KEY,
    material_id    UUID           NOT NULL REFERENCES uom_materials(id),
    version        BIGINT         NOT NULL,
    from_dimension VARCHAR(20)    NOT NULL,                -- LENGTH | MASS | VOLUME | COUNT
    to_dimension   VARCHAR(20)    NOT NULL,
    factor         NUMERIC(38,12) NOT NULL,
    recorded_at    TIMESTAMP      NOT NULL
);

CREATE UNIQUE INDEX uq_uom_material_version ON uom_material_properties (material_id, version);

-- UOMCONV-BASIS/DETERMINISM-001 — one immutable conversion record carrying its full reconstructible
-- basis (from_quantity / to_quantity — NEVER 'value'); idempotency_basis is unique so an identical
-- re-request returns the recorded conversion verbatim rather than computing a second drifting result.
CREATE TABLE uom_conversions (
    id                UUID           NOT NULL PRIMARY KEY,
    material_id       UUID,                                -- null for a SAME_DIMENSION conversion
    from_quantity     NUMERIC(38,12) NOT NULL,
    from_unit         VARCHAR(20)    NOT NULL,
    to_unit           VARCHAR(20)    NOT NULL,
    from_dimension    VARCHAR(20)    NOT NULL,
    to_dimension      VARCHAR(20)    NOT NULL,
    mode              VARCHAR(20)    NOT NULL,             -- SAME_DIMENSION | CROSS_DIMENSION
    factor            NUMERIC(38,12) NOT NULL,
    material_version  BIGINT         NOT NULL,
    result_scale      INTEGER        NOT NULL,
    to_quantity       NUMERIC(38,12) NOT NULL,
    idempotency_basis VARCHAR(300)   NOT NULL,
    occurred_at       TIMESTAMP      NOT NULL,
    actor             VARCHAR(200)   NOT NULL
);

CREATE UNIQUE INDEX uq_uom_conversion_basis ON uom_conversions (idempotency_basis);
