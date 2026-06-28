-- tax-application domain (TAX-EXEMPT-SKIP / TAX-IDEMPOTENT-RECOMPUTE / TAX-AUTHZ)
-- Order-level tax application with two portable correctness invariants:
--   1. EXEMPT-SKIP        — an exempt customer OR an exempt line contributes ZERO taxable base.
--   2. IDEMPOTENT-RECOMPUTE — re-pricing converges to EXACTLY ONE combined tax record per order.
-- Money is in integer minor units. The jurisdiction rate is injected by the caller (no rate table).

-- The declared taxable input (TaxableOrder aggregate root).
CREATE TABLE taxable_orders (
    id              UUID    NOT NULL,
    version         BIGINT  NOT NULL DEFAULT 0,
    customer_exempt BOOLEAN NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_taxable_orders PRIMARY KEY (id)
);

-- Order lines (@ElementCollection on the TaxableOrder aggregate root): taxable base + per-line exemption.
CREATE TABLE taxable_order_lines (
    order_id           UUID   NOT NULL,
    taxable_base_minor BIGINT NOT NULL,
    exempt             BOOLEAN NOT NULL,
    CONSTRAINT fk_taxable_order_lines_order
        FOREIGN KEY (order_id) REFERENCES taxable_orders(id)
);

CREATE INDEX idx_taxable_order_lines_order ON taxable_order_lines (order_id);

-- The single combined tax record DERIVED per order (TaxAssessment aggregate root). The UNIQUE
-- constraint on order_id makes a second tax row per order UNREPRESENTABLE; the CHECK makes a
-- negative tax amount unrepresentable.
CREATE TABLE tax_assessments (
    id                 UUID   NOT NULL,
    version            BIGINT NOT NULL DEFAULT 0,
    order_id           UUID   NOT NULL,
    tax_amount_minor   BIGINT NOT NULL,
    taxable_base_minor BIGINT NOT NULL,
    rate_basis_points  BIGINT NOT NULL,
    computed_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tax_assessments PRIMARY KEY (id),
    CONSTRAINT uq_tax_assessment_order UNIQUE (order_id),
    CONSTRAINT chk_tax_assessment_values
        CHECK (tax_amount_minor >= 0 AND taxable_base_minor >= 0 AND rate_basis_points >= 0)
);
