-- CAT-INVENTORY-GATE-001: add tri-state inventory policy flag to catalog_skus.
-- Existing rows default to ALWAYS_AVAILABLE so no existing SKU changes purchasability.
ALTER TABLE catalog_skus
    ADD COLUMN inventory_type VARCHAR(20) NOT NULL DEFAULT 'ALWAYS_AVAILABLE'
        CONSTRAINT chk_sku_inventory_type
        CHECK (inventory_type IN ('UNAVAILABLE', 'ALWAYS_AVAILABLE', 'CHECK_QUANTITY'));
