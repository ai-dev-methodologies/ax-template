-- two-axis-inventory-reservation reference workload — realizes specs/two-axis-inventory-reservation-l0.yaml
-- (P1-46: a two-axis available/reserved inventory with a two-phase reserve → commit → release hold.
-- An InventoryItem persists TWO quantities, on_hand and reserved; AVAILABLE = on_hand − reserved is
-- a DERIVED quantity, never a stored column. A Reservation is a HELD hold that commits (both axes
-- fall) or releases (reserved alone falls) exactly once. @Check reserved >= 0 AND reserved <= on_hand
-- backstops the conservation reserved == Σ(HELD reservation quantities).)

CREATE TABLE inventory_items (
    id          UUID         NOT NULL PRIMARY KEY,
    sku         VARCHAR(200) NOT NULL,
    on_hand     BIGINT       NOT NULL,
    reserved    BIGINT       NOT NULL DEFAULT 0,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL,
    -- INVRES-CONSERVE-001 — quantities non-negative; reserved can never exceed on_hand (so the
    -- DERIVED available = on_hand − reserved is always >= 0); available is NOT a stored column.
    CONSTRAINT chk_inventory_item CHECK (
        on_hand >= 0 AND reserved >= 0 AND reserved <= on_hand
    )
);

-- INVRES-RESERVE/COMMIT/RELEASE-001 — one hold per row; status moves HELD → (COMMITTED | RELEASED)
-- exactly once. quantity + item_id are immutable; the committed/released terminal is the goods
-- leaving / the hold freeing.
CREATE TABLE inventory_reservations (
    id          UUID         NOT NULL PRIMARY KEY,
    item_id     UUID         NOT NULL REFERENCES inventory_items(id),
    quantity    BIGINT       NOT NULL,
    status      VARCHAR(20)  NOT NULL,                  -- HELD | COMMITTED | RELEASED
    actor       VARCHAR(200) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT chk_inventory_reservation_qty CHECK (quantity > 0)
);

-- conservation + listing support: the HELD rows of an item are summed to verify reserved.
CREATE INDEX idx_inventory_reservations_item ON inventory_reservations (item_id, status);
