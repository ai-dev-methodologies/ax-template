-- commercecatalog reference workload — realizes reference-inspired catalog feature set
-- with stricter invariants (INV-1~6) enforced at DB + service layers.

-- ── CatalogProduct aggregate root ────────────────────────────────────────────────────
CREATE TABLE catalog_products (
    id                     UUID         NOT NULL PRIMARY KEY,
    version                BIGINT       NOT NULL DEFAULT 0,
    name                   VARCHAR(400) NOT NULL,
    description            VARCHAR(2000),
    default_sku_id         UUID,
    can_sell_without_options BOOLEAN    NOT NULL DEFAULT FALSE,
    active_start_date      TIMESTAMP,
    active_end_date        TIMESTAMP,
    archived               BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Window well-formedness: end must be strictly after start when both present.
    CONSTRAINT chk_product_dates CHECK (
        active_end_date IS NULL OR active_start_date IS NULL OR active_end_date > active_start_date
    )
);

-- ── Sku (@AggregateMember of CatalogProduct) ─────────────────────────────────────────
-- INV-1 backstop: (product_id, option_signature) UNIQUE where option_signature IS NOT NULL.
-- INV: sale_price <= retail_price (@Check).
CREATE TABLE catalog_skus (
    id                UUID         NOT NULL PRIMARY KEY,
    product_id        UUID         NOT NULL REFERENCES catalog_products(id),
    version           BIGINT       NOT NULL DEFAULT 0,
    is_default        BOOLEAN      NOT NULL,
    retail_price      BIGINT,
    sale_price        BIGINT,
    currency          CHAR(3),
    active_start_date TIMESTAMP,
    active_end_date   TIMESTAMP,
    external_id       VARCHAR(200),
    upc               VARCHAR(50),
    option_signature  VARCHAR(4000),
    CONSTRAINT chk_sku_sale_price CHECK (sale_price IS NULL OR sale_price <= retail_price)
);

-- INV-1: DB-level uniqueness backstop for (product_id, option_signature) when non-null.
CREATE UNIQUE INDEX uq_sku_product_option_sig
    ON catalog_skus (product_id, option_signature)
    WHERE option_signature IS NOT NULL;

-- ── ProductOption (@AggregateMember) ─────────────────────────────────────────────────
CREATE TABLE catalog_product_options (
    id                   UUID         NOT NULL PRIMARY KEY,
    product_id           UUID         NOT NULL REFERENCES catalog_products(id),
    attribute_name       VARCHAR(200) NOT NULL,
    required             BOOLEAN      NOT NULL,
    use_in_sku_generation BOOLEAN     NOT NULL,
    CONSTRAINT uq_product_option_attr UNIQUE (product_id, attribute_name)
);

-- ── ProductOptionValue (@AggregateMember) ────────────────────────────────────────────
CREATE TABLE catalog_option_values (
    id               UUID         NOT NULL PRIMARY KEY,
    option_id        UUID         NOT NULL REFERENCES catalog_product_options(id),
    attribute_value  VARCHAR(200) NOT NULL,
    price_adjustment BIGINT,
    CONSTRAINT uq_option_value_attr UNIQUE (option_id, attribute_value)
);

-- ── SkuOptionValueXref (@AggregateMember) — composite PK, both columns immutable ─────
CREATE TABLE catalog_sku_option_values (
    sku_id          UUID NOT NULL REFERENCES catalog_skus(id),
    option_value_id UUID NOT NULL REFERENCES catalog_option_values(id),
    CONSTRAINT pk_sku_option_value PRIMARY KEY (sku_id, option_value_id),
    CONSTRAINT uq_sku_option_value UNIQUE (sku_id, option_value_id)
);

-- ── Category aggregate root (separate consistency boundary) ──────────────────────────
CREATE TABLE catalog_categories (
    id               UUID         NOT NULL PRIMARY KEY,
    version          BIGINT       NOT NULL DEFAULT 0,
    name             VARCHAR(400) NOT NULL,
    archived         BOOLEAN      NOT NULL DEFAULT FALSE,
    active_start_date TIMESTAMP,
    active_end_date  TIMESTAMP,
    parent_id        UUID         REFERENCES catalog_categories(id),
    CONSTRAINT chk_category_dates CHECK (
        active_end_date IS NULL OR active_start_date IS NULL OR active_end_date > active_start_date
    )
);

-- ── CategoryProductXref (@AggregateMember of CatalogProduct) — ID-ref only, no FK to Category ──
-- Cross-aggregate by ID: category_id is an identity reference, not an FK to catalog_categories,
-- preserving aggregate boundary (the Category aggregate is a separate consistency unit).
CREATE TABLE catalog_category_products (
    product_id    UUID    NOT NULL REFERENCES catalog_products(id),
    category_id   UUID    NOT NULL,
    display_order INT     NOT NULL,
    CONSTRAINT pk_cat_product PRIMARY KEY (product_id, category_id),
    CONSTRAINT uq_cat_product UNIQUE (product_id, category_id)
);

CREATE INDEX ix_catalog_product_default_sku ON catalog_products (default_sku_id);
CREATE INDEX ix_catalog_sku_product ON catalog_skus (product_id);
CREATE INDEX ix_catalog_option_product ON catalog_product_options (product_id);
CREATE INDEX ix_catalog_optval_option ON catalog_option_values (option_id);
CREATE INDEX ix_catalog_category_parent ON catalog_categories (parent_id);
