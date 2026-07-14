-- geo-bounded-query reference workload — realizes specs/geo-bounded-query-l0.yaml
-- (P3-1 DEGRADED-IMPL — HONEST DEGRADED SUBSET, no PostGIS/GiST claim). Plain lat/lon
-- NUMERIC columns; the indexed bounding-box prefilter (GEO-BBOX-001) runs as a plain
-- BETWEEN range predicate over these columns, backstopped by the @Check ISO 6709 range
-- (GEO-INPUT-001) already enforced in application code before any row is written.

CREATE TABLE geo_points (
    id           UUID          NOT NULL PRIMARY KEY,
    external_ref VARCHAR(200)  NOT NULL,                 -- opaque subject reference
    lat          NUMERIC(9,6)  NOT NULL,
    lon          NUMERIC(9,6)  NOT NULL,
    created_at   TIMESTAMP     NOT NULL,
    CONSTRAINT chk_geo_point_bounds CHECK (
        lat >= -90 AND lat <= 90 AND lon >= -180 AND lon <= 180
    )
);

-- GEO-BBOX-001 — a plain B-tree-indexable range predicate on lat/lon is the prefilter;
-- no spatial extension is required or claimed.
CREATE INDEX idx_geo_points_lat_lon ON geo_points (lat, lon);
