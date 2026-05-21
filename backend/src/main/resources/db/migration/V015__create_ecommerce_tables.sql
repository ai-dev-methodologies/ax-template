-- R23 e-commerce capstone — recipes/e-commerce/RECIPE.md
--
-- Composition: crud + payment + notification + audit-log + search
-- Invariants (recipes/e-commerce/RECIPE.md):
--   ECOM-INV-001 — order.total_amount == sum(items.unit_price × items.quantity)
--   ECOM-INV-002 — payment.captured ⇒ order.confirmed (same transaction)
--   ECOM-INV-003 — all mutating endpoints require Idempotency-Key
--   ECOM-INV-004 — cancellation/refund actions logged via @Audited

CREATE TABLE products (
    id              VARCHAR(36)   PRIMARY KEY,
    owner_user_id   VARCHAR(36)   NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    description     VARCHAR(2000),
    price           BIGINT        NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    stock           INT           NOT NULL DEFAULT 0,
    image_file_id   VARCHAR(36),
    status          VARCHAR(16)   NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_products_owner ON products(owner_user_id);
CREATE INDEX idx_products_status ON products(status);

CREATE TABLE carts (
    id              VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL,
    total_amount    BIGINT        NOT NULL DEFAULT 0,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'KRW',
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    CONSTRAINT uk_carts_user UNIQUE (user_id)
);

CREATE TABLE cart_items (
    id                          VARCHAR(36)   PRIMARY KEY,
    cart_id                     VARCHAR(36)   NOT NULL,
    product_id                  VARCHAR(36)   NOT NULL,
    quantity                    INT           NOT NULL,
    unit_price_at_added_time    BIGINT        NOT NULL,
    line_total                  BIGINT        NOT NULL,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);

CREATE TABLE orders (
    id              VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL,
    total_amount    BIGINT        NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    status          VARCHAR(16)   NOT NULL,
    payment_id      VARCHAR(36),
    idempotency_key VARCHAR(120),
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    CONSTRAINT uk_orders_idem UNIQUE (idempotency_key)
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
    id                          VARCHAR(36)   PRIMARY KEY,
    order_id                    VARCHAR(36)   NOT NULL,
    product_id                  VARCHAR(36)   NOT NULL,
    product_name_at_purchase    VARCHAR(200)  NOT NULL,
    quantity                    INT           NOT NULL,
    unit_price                  BIGINT        NOT NULL,
    line_total                  BIGINT        NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
