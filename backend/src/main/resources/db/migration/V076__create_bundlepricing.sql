-- bundlepricing reference workload — realizes bundle-pricing-l0.yaml
-- Absorbed from Broadleaf BundleOrderItemImpl (independent implementation; not ported).
-- Invariants enforced at DB + service + derivation layers:
--   BUNDLE-ITEMSUM-001 — ITEM_SUM price = Σ child.unitPrice×qty + Σ fees (derived, never stored)
--   BUNDLE-FIXED-001   — BUNDLE price = fixed base price, NOT summed; mode/base-price exclusive (@Check)
--   BUNDLE-DERIVED-001 — taxability derived from children; NO stored total / taxable column

CREATE TABLE composite_items (
    id                UUID         NOT NULL PRIMARY KEY,
    version           BIGINT       NOT NULL DEFAULT 0,
    pricing_model     VARCHAR(16)  NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    base_retail_price BIGINT,
    base_sale_price   BIGINT,
    CONSTRAINT chk_composite_pricing_model CHECK (pricing_model IN ('ITEM_SUM', 'BUNDLE')),
    -- mode/base-price exclusivity (BUNDLE-FIXED-001): a BUNDLE composite MUST carry a fixed
    -- base price; an ITEM_SUM composite MUST NOT (its price is summed from children).
    CONSTRAINT chk_composite_mode_base CHECK (
        ((pricing_model = 'BUNDLE'   AND base_retail_price IS NOT NULL)
      OR (pricing_model = 'ITEM_SUM' AND base_retail_price IS NULL))
      AND (base_sale_price IS NULL OR base_sale_price <= base_retail_price)
      -- close the NULL-base hole: an ITEM_SUM row must not carry a stray base_sale_price
      -- (base_sale_price <= NULL evaluates to UNKNOWN, which a CHECK admits)
      AND (pricing_model = 'BUNDLE' OR base_sale_price IS NULL)
    )
);

-- Note: composite_items carries NO rolled-up total column. The conserving total is a pure
-- derivation of the children + fees (ITEM_SUM) or the immutable base (BUNDLE) — there is
-- nowhere to store a total that contradicts the children (BUNDLE-DERIVED-001).

CREATE TABLE composite_components (
    id                UUID         NOT NULL PRIMARY KEY,
    composite_item_id UUID         NOT NULL,
    name              VARCHAR(200),
    quantity          INT          NOT NULL,
    unit_retail_price BIGINT       NOT NULL,
    unit_sale_price   BIGINT,
    taxable           BOOLEAN      NOT NULL,
    CONSTRAINT fk_composite_component_item FOREIGN KEY (composite_item_id)
        REFERENCES composite_items (id),
    CONSTRAINT chk_composite_component_amounts CHECK (
        quantity > 0 AND unit_retail_price >= 0
        AND (unit_sale_price IS NULL OR (unit_sale_price >= 0 AND unit_sale_price <= unit_retail_price))
    )
);

CREATE INDEX ix_composite_components_item ON composite_components (composite_item_id);

-- @ElementCollection of immutable bundle-level fees (no identity of their own).
CREATE TABLE composite_item_fees (
    composite_item_id UUID         NOT NULL,
    label             VARCHAR(200),
    amount            BIGINT       NOT NULL,
    taxable           BOOLEAN      NOT NULL,
    CONSTRAINT fk_composite_fee_item FOREIGN KEY (composite_item_id)
        REFERENCES composite_items (id),
    CONSTRAINT chk_composite_fee_amount CHECK (amount >= 0)
);

CREATE INDEX ix_composite_item_fees_item ON composite_item_fees (composite_item_id);
