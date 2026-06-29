-- V072: commerceorder domain — reference e-commerce cart/order spine
-- Tables: commerce_orders, commerce_order_items, commerce_fulfillment_groups,
--         commerce_fulfillment_group_items
-- Prefix 'commerce_' avoids collision with the toy 'orders'/'order_items' tables.

CREATE TABLE commerce_orders (
    id          UUID         NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    currency    CHAR(3)      NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    total       BIGINT       NOT NULL DEFAULT 0,
    sub_total   BIGINT       NOT NULL DEFAULT 0,
    tax         BIGINT       NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_commerce_orders PRIMARY KEY (id),
    CONSTRAINT chk_commerce_orders_totals CHECK (total >= 0 AND sub_total >= 0 AND tax >= 0),
    CONSTRAINT chk_commerce_orders_status CHECK (status IN ('IN_PROCESS', 'SUBMITTED', 'CANCELLED'))
);

CREATE INDEX ix_commerce_orders_user_created ON commerce_orders (user_id, created_at);
CREATE INDEX ix_commerce_orders_status ON commerce_orders (status);

CREATE TABLE commerce_order_items (
    id                  UUID         NOT NULL,
    order_id            UUID         NOT NULL,
    sku_id              VARCHAR(255) NOT NULL,
    name_at_add         VARCHAR(400) NOT NULL,
    unit_price_at_add   BIGINT       NOT NULL,
    quantity            INT          NOT NULL,
    CONSTRAINT pk_commerce_order_items PRIMARY KEY (id),
    CONSTRAINT fk_coi_order FOREIGN KEY (order_id) REFERENCES commerce_orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_coi_quantity CHECK (quantity > 0),
    CONSTRAINT chk_coi_price CHECK (unit_price_at_add >= 0)
);

CREATE INDEX ix_commerce_order_items_order ON commerce_order_items (order_id);

CREATE TABLE commerce_fulfillment_groups (
    id                  UUID         NOT NULL,
    order_id            UUID         NOT NULL,
    address             VARCHAR(500) NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'UNFULFILLED',
    merchandise_total   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_commerce_fulfillment_groups PRIMARY KEY (id),
    CONSTRAINT fk_cfg_order FOREIGN KEY (order_id) REFERENCES commerce_orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_cfg_status CHECK (status IN ('UNFULFILLED', 'PARTIALLY_FULFILLED', 'FULFILLED'))
);

CREATE INDEX ix_commerce_fulfillment_groups_order ON commerce_fulfillment_groups (order_id);

-- order_id: the root (CommerceOrder) owns this collection directly (HG-AGG-REF DDD-006):
-- a member (CommerceFulfillmentGroup) must NOT hold a typed collection of a sibling member,
-- so the FGI references its aggregate ROOT by id and the root cascades both collections.
CREATE TABLE commerce_fulfillment_group_items (
    id                      UUID    NOT NULL,
    order_id                UUID    NOT NULL,
    fulfillment_group_id    UUID    NOT NULL,
    order_item_id           UUID    NOT NULL,
    quantity                INT     NOT NULL,
    CONSTRAINT pk_commerce_fulfillment_group_items PRIMARY KEY (id),
    CONSTRAINT fk_cfgi_order FOREIGN KEY (order_id) REFERENCES commerce_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_cfgi_group FOREIGN KEY (fulfillment_group_id) REFERENCES commerce_fulfillment_groups (id) ON DELETE CASCADE,
    CONSTRAINT chk_cfgi_qty CHECK (quantity > 0)
);

CREATE INDEX ix_cfgi_order ON commerce_fulfillment_group_items (order_id);
CREATE INDEX ix_cfgi_group ON commerce_fulfillment_group_items (fulfillment_group_id);
CREATE INDEX ix_cfgi_order_item ON commerce_fulfillment_group_items (order_item_id);
