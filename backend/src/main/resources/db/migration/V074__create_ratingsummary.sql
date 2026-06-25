-- ratingsummary reference workload — realizes derived-aggregate-consistency-l0.yaml
-- Invariants enforced at DB + service layers:
--   DERIVED-AGG-CONSISTENCY-001 — recomputed in the same transaction as any review state change
--   DERIVED-AGG-ELIGIBILITY-001 — only APPROVED reviews contribute; PENDING/REJECTED excluded
--   DERIVED-AGG-EMPTY-001 — empty eligible set yields sentinel (average=0, reviewCount=0)

CREATE TABLE rating_reviews (
    id         UUID         NOT NULL PRIMARY KEY,
    product_id UUID         NOT NULL,
    stars      INT          NOT NULL,
    status     VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_review_stars CHECK (stars >= 1 AND stars <= 5),
    CONSTRAINT chk_review_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX ix_rating_reviews_product_status ON rating_reviews (product_id, status);

CREATE TABLE rating_summaries (
    product_id    UUID           NOT NULL PRIMARY KEY,
    average       DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    review_count  INT            NOT NULL DEFAULT 0,
    version       BIGINT         NOT NULL DEFAULT 0
);
