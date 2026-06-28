-- offer-eligibility domain (OFFER-QUALIFIER-MINQTY / OFFER-SEGMENT-ELIGIBILITY / OFFER-FAIL-CLOSED / OFFER-AUTHZ)
-- The WHO/WHICH-ITEMS applicability gate for an offer/discount — distinct from discount MATH.
-- Eligibility is evaluated deterministically and FAIL-CLOSED from these declared criteria; an
-- ineligible offer can never reach the discount-application path. Any criterion may be NULL
-- (the evaluator denies by default on missing/unknown criteria).

CREATE TABLE eligibility_offers (
    id                    UUID         NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    name                  VARCHAR(200) NOT NULL,
    qualifier_sku         VARCHAR(100),
    qualifier_tag         VARCHAR(100),
    min_qualifier_qty     INT          NOT NULL,
    target_sku            VARCHAR(100),
    target_tag            VARCHAR(100),
    discount_basis_points BIGINT       NOT NULL,
    eligible_segment      VARCHAR(100),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_eligibility_offers PRIMARY KEY (id),
    CONSTRAINT uq_eligibility_offer_name UNIQUE (name),
    CONSTRAINT chk_eligibility_offer_values
        CHECK (min_qualifier_qty >= 1 AND discount_basis_points >= 0)
);

-- Customer-xref allow-list (@ElementCollection on the EligibilityOffer aggregate root).
CREATE TABLE eligibility_offer_customers (
    offer_id    UUID NOT NULL,
    customer_id UUID NOT NULL,
    CONSTRAINT fk_eligibility_offer_customers_offer
        FOREIGN KEY (offer_id) REFERENCES eligibility_offers(id)
);

CREATE INDEX idx_eligibility_offer_customers_offer ON eligibility_offer_customers (offer_id);
