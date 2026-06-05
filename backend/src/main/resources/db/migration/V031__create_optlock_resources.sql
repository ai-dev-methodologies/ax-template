-- optimistic-locking-l0 reference workload (specs/optimistic-locking-l0.yaml).
-- A mutable, owner-scoped resource with a provider-managed JPA @Version column
-- (OPTLOCK-VERSION-001): non-null, DB default 0, incremented on every flush.
CREATE TABLE optlock_resources (
    id         UUID         NOT NULL PRIMARY KEY,
    owner_id   VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    quantity   INTEGER      NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX ix_optlock_owner ON optlock_resources (owner_id);
