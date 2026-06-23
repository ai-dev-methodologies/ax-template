-- query-field-allowlist reference workload — realizes specs/query-field-allowlist-l0.yaml
-- (P1-38: a per-resource allowlist of the EXACT fields a caller may sort/filter on, mapping
-- the public field name to an internal entity property, rejecting any non-allowlisted field
-- by NAME — the IDW8 sort/filter field-allowlist security follow-up).
--
-- CatalogItem is the demonstrating resource the list endpoint pages over. Its FOUR exposed
-- columns (name, status, price_minor, created_at) are the only sortable/filterable surface;
-- internal_notes is the keystone target — present on the row but never sortable/filterable
-- and never carried out to a DTO.

CREATE TABLE catalog_items (
    id             UUID         NOT NULL PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    status         VARCHAR(20)  NOT NULL,          -- DRAFT | ACTIVE | ARCHIVED
    price_minor    BIGINT       NOT NULL,          -- money amount in MINOR units (cents)
    internal_notes VARCHAR(500),                   -- internal-only; NOT in the sort/filter allowlist
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL,
    -- QUERY-ALLOWLIST — a money amount is non-negative.
    CONSTRAINT chk_catalog_item CHECK (price_minor >= 0)
);
