-- commerce-promotion domain (PROMO-CONSERVE/STACK/ORDER/MAXSELECT/CLAMP/MAXUSES/IDEMPOTENT-001)
-- PROMO-MAXUSES-001 (reference TOCTOU strengthening): UNIQUE(offer_id, order_ref) on promo_redemptions
-- is the atomic backstop — the service's PESSIMISTIC_WRITE lock is the advisory gate,
-- this constraint is the hard guarantee even under concurrent writes.

CREATE TABLE promo_offers (
    id                    UUID         NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    name                  VARCHAR(200) NOT NULL,
    discount_type         VARCHAR(20)  NOT NULL,
    discount_value        BIGINT       NOT NULL,
    scope                 VARCHAR(10)  NOT NULL,
    priority              INT          NOT NULL,
    combinable            BOOLEAN      NOT NULL,
    stackable             BOOLEAN      NOT NULL,
    apply_to_sale_price   BOOLEAN,
    max_uses              BIGINT       NOT NULL DEFAULT 0,
    max_uses_per_customer BIGINT       NOT NULL DEFAULT 0,
    active_start          TIMESTAMP WITH TIME ZONE NOT NULL,
    active_end            TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_promo_offers PRIMARY KEY (id),
    CONSTRAINT uq_promo_offer_name UNIQUE (name),
    CONSTRAINT chk_promo_offer_values
        CHECK (discount_value >= 0 AND priority >= 0 AND max_uses >= 0 AND max_uses_per_customer >= 0)
);

CREATE TABLE promo_offer_codes (
    id        UUID          NOT NULL,
    offer_id  UUID          NOT NULL,
    code      VARCHAR(100)  NOT NULL,
    max_uses  BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_promo_offer_codes PRIMARY KEY (id),
    CONSTRAINT uq_promo_offer_code UNIQUE (code),
    CONSTRAINT fk_promo_offer_codes_offer FOREIGN KEY (offer_id) REFERENCES promo_offers(id)
);

CREATE INDEX idx_promo_offer_codes_offer_id ON promo_offer_codes (offer_id);

-- PROMO-MAXUSES-001: UNIQUE(offer_id, order_ref) is the atomic cap backstop.
-- The service acquires PESSIMISTIC_WRITE on the offer row before inserting here,
-- but this constraint ensures correctness even if the advisory check is bypassed.
CREATE TABLE promo_redemptions (
    id          UUID         NOT NULL,
    offer_id    UUID         NOT NULL,
    customer_id VARCHAR(200) NOT NULL,
    order_ref   VARCHAR(200) NOT NULL,
    redeemed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_promo_redemptions PRIMARY KEY (id),
    CONSTRAINT uq_promo_redemption_offer_order UNIQUE (offer_id, order_ref),
    CONSTRAINT fk_promo_redemptions_offer FOREIGN KEY (offer_id) REFERENCES promo_offers(id)
);

CREATE INDEX idx_promo_redemptions_offer_id ON promo_redemptions (offer_id);
CREATE INDEX idx_promo_redemptions_customer  ON promo_redemptions (offer_id, customer_id);
