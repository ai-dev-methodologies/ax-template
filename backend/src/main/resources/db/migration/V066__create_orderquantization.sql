-- order-multiple-quantization reference workload — realizes specs/order-multiple-quantization-l0.yaml
-- (P1-52: NON-CONSERVING MOQ / order-multiple quantization — a required net quantity is rounded UP
-- to the supplier lot constraint: order_quantity = max(moq, ceil(required / multiple) * multiple),
-- and the deliberate surplus overage = order_quantity - required_quantity is RECORDED, never hidden).

CREATE TABLE order_quantizations (
    id                UUID         NOT NULL PRIMARY KEY,
    item_ref          VARCHAR(200) NOT NULL,
    required_quantity BIGINT       NOT NULL,        -- the required net quantity (quantizer input)
    moq               BIGINT       NOT NULL,        -- supplier minimum order quantity (>= 1)
    order_multiple    BIGINT       NOT NULL,        -- supplier order multiple / lot / pack size (>= 1)
    order_quantity    BIGINT       NOT NULL,        -- max(moq, ceil(required / multiple) * multiple)
    overage           BIGINT       NOT NULL,        -- NON-CONSERVING surplus = order_quantity - required_quantity
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL,
    -- ORDERQUANT-CONSTRAINT/OVERAGE-001 — constraints positive, required non-negative, the order
    -- quantity at/above both required and the MOQ, and the overage bound EXACTLY to the surplus so a
    -- record that fakes or drops the non-conserving excess is unrepresentable.
    CONSTRAINT chk_order_quantization CHECK (
        required_quantity >= 0 AND moq >= 1 AND order_multiple >= 1
        AND order_quantity >= required_quantity AND order_quantity >= moq
        AND overage >= 0 AND overage = order_quantity - required_quantity
    )
);

CREATE INDEX idx_order_quantizations_item_ref ON order_quantizations (item_ref, created_at DESC);
