package com.ax.template.authblueprint.geoquery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * geo-bounded-query-l0 root: one registered point (an opaque external subject reference plus a
 * lat/lon location). Immutable after registration — a point is re-registered as a new row, never
 * updated in place. The @Check backstop is the SAME ISO 6709 bound {@link GeoQueryService}
 * validates before ever persisting (GEO-INPUT-001) — a DB-level defense-in-depth, not the primary
 * gate. No PostGIS/GiST claim is made anywhere on this entity (plain NUMERIC columns only) —
 * see specs/geo-bounded-query-l0.yaml scope + GEO-GIST-REVIEW-001.
 */
@AggregateRoot
@Entity
@Table(name = "geo_points")
@Check(constraints = "lat >= -90 AND lat <= 90 AND lon >= -180 AND lon <= 180")
public class GeoPoint {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Opaque external subject reference (e.g. driver/asset/store id) — recorded verbatim. */
    @Column(name = "external_ref", nullable = false, updatable = false, length = 200)
    private String externalRef;

    @Column(name = "lat", nullable = false, updatable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(name = "lon", nullable = false, updatable = false, precision = 9, scale = 6)
    private BigDecimal lon;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GeoPoint() {}

    public GeoPoint(UUID id, String externalRef, BigDecimal lat, BigDecimal lon, Instant createdAt) {
        this.id = id;
        this.externalRef = externalRef;
        this.lat = lat;
        this.lon = lon;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getExternalRef() { return externalRef; }
    public BigDecimal getLat() { return lat; }
    public BigDecimal getLon() { return lon; }
    public Instant getCreatedAt() { return createdAt; }
}
