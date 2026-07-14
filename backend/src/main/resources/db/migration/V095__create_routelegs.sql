-- route-leg-contiguity reference workload — realizes specs/route-leg-contiguity-l0.yaml
-- (P3-4 NEW domain). A route's legs form an ordered, contiguous chain: leg N's
-- destination MUST equal leg N+1's origin (LEG-SEQUENCE-001), the ordinal sequence
-- MUST be gapless 1..N (LEG-GAP-001), and any mutation is serialized by the root's
-- @Version optimistic lock, with a uq(route_id, ordinal) backstop against any
-- residual race (LEG-MUTATE-001).

CREATE TABLE routes (
    id           UUID      NOT NULL PRIMARY KEY,
    mutation_seq BIGINT    NOT NULL DEFAULT 0,     -- dirties this row on every structural leg mutation
    version      BIGINT    NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL
);

CREATE TABLE route_legs (
    id          UUID         NOT NULL PRIMARY KEY,
    route_id    UUID         NOT NULL REFERENCES routes(id),
    ordinal     INTEGER      NOT NULL,             -- 1..N contiguous position — mutated via bulk JPQL only
    origin_code VARCHAR(200) NOT NULL,              -- mutated via bulk JPQL only (replace)
    dest_code   VARCHAR(200) NOT NULL,              -- mutated via bulk JPQL only (replace)
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT chk_route_leg_ordinal CHECK (ordinal >= 1)
);

-- LEG-GAP-001 — a duplicate ordinal for one route is a deterministic constraint violation
-- (the two-phase park-then-land shift in RouteRepository exists specifically to never trip this
-- mid-mutation on a legitimate single-request renumbering).
CREATE UNIQUE INDEX uq_route_leg_ordinal ON route_legs (route_id, ordinal);
