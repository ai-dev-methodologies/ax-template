package com.ax.template.authblueprint.geoquery;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * geo-bounded-query-l0 sole orchestrator — a HONEST DEGRADED SUBSET (no PostGIS/GiST claim; see
 * specs/geo-bounded-query-l0.yaml scope). GEO-INPUT-001: every registration/query is validated
 * against the ISO 6709 lat/lon range and the policy radius cap BEFORE any query runs.
 * GEO-BBOX-001: {@link #searchRadius} first runs the indexed lat/lon bounding-box PREFILTER
 * ({@link GeoPointRepository#findBoundingBoxCandidates}), then applies an exact haversine
 * POSTFILTER, discarding any bbox candidate outside the true radius (a corner point can sit in
 * the box while outside the circle it approximates). GEO-DETERMINISM-001: results are ordered by
 * exact distance ascending, then point id ascending for a stable tiebreak.
 */
@Service
public class GeoQueryService {

    static final double EARTH_RADIUS_METERS = 6_371_000.0;
    static final double MAX_RADIUS_METERS = 50_000.0;
    /** Bounded candidate cap for the bbox prefilter — the search radius itself is already policy-capped. */
    static final int BBOX_CANDIDATE_CAP = 2_000;

    private final GeoPointRepository points;
    private final GeoQueryMetrics metrics;
    private final Clock clock;

    public GeoQueryService(GeoPointRepository points, GeoQueryMetrics metrics, Clock clock) {
        this.points = points;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** GEO-INPUT-001 — register a point; lat/lon MUST be within ISO 6709 range or 422. */
    @Transactional
    public GeoPoint register(String externalRef, BigDecimal lat, BigDecimal lon) {
        validateLat(lat);
        validateLon(lon);
        GeoPoint saved = points.save(new GeoPoint(UUID.randomUUID(), externalRef, lat, lon, Instant.now(clock)));
        metrics.record("register", "ok");
        return saved;
    }

    /**
     * GEO-BBOX/DETERMINISM-001 — radius query around (centerLat, centerLon). The bbox prefilter
     * runs an indexed lat/lon range predicate; the haversine postfilter then discards any
     * candidate whose EXACT distance exceeds radiusMeters. Ordered distance-ascending, id-ascending.
     */
    @Transactional(readOnly = true)
    public List<Result> searchRadius(BigDecimal centerLat, BigDecimal centerLon, double radiusMeters) {
        validateLat(centerLat);
        validateLon(centerLon);
        if (radiusMeters <= 0 || radiusMeters > MAX_RADIUS_METERS) {
            metrics.record("search", "invalid");
            throw GeoQueryException.invalidInput(
                "radiusMeters must be in (0, " + MAX_RADIUS_METERS + "] meters");
        }
        double lat = centerLat.doubleValue();
        double lon = centerLon.doubleValue();
        double metersPerDegreeLat = (EARTH_RADIUS_METERS * Math.PI) / 180.0;
        double latDeltaDeg = radiusMeters / metersPerDegreeLat;
        // guard the poles: cos(lat) → 0 would blow up the longitude delta.
        double cosLat = Math.max(Math.cos(Math.toRadians(lat)), 1e-9);
        double lonDeltaDeg = radiusMeters / (metersPerDegreeLat * cosLat);

        BigDecimal minLat = BigDecimal.valueOf(lat - latDeltaDeg);
        BigDecimal maxLat = BigDecimal.valueOf(lat + latDeltaDeg);
        BigDecimal minLon = BigDecimal.valueOf(lon - lonDeltaDeg);
        BigDecimal maxLon = BigDecimal.valueOf(lon + lonDeltaDeg);

        List<GeoPoint> candidates = points.findBoundingBoxCandidates(
            minLat, maxLat, minLon, maxLon, PageRequest.of(0, BBOX_CANDIDATE_CAP));

        metrics.record("search", "ok");
        return candidates.stream()
            .map(p -> new Result(p, haversineMeters(lat, lon, p.getLat().doubleValue(), p.getLon().doubleValue())))
            .filter(r -> r.distanceMeters() <= radiusMeters)                 // exact postfilter — GEO-BBOX-001
            .sorted(Comparator.comparingDouble(Result::distanceMeters)
                .thenComparing(r -> r.point().getId()))                     // GEO-DETERMINISM-001
            .toList();
    }

    /** The great-circle distance between two lat/lon points, in meters (Sinnott 1984 haversine). */
    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private static void validateLat(BigDecimal lat) {
        if (lat == null || lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw GeoQueryException.invalidInput("lat must be in [-90, 90] (ISO 6709)");
        }
    }

    private static void validateLon(BigDecimal lon) {
        if (lon == null || lon.compareTo(BigDecimal.valueOf(-180)) < 0 || lon.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw GeoQueryException.invalidInput("lon must be in [-180, 180] (ISO 6709)");
        }
    }

    /** A candidate point paired with its exact haversine distance from the query center. */
    public record Result(GeoPoint point, double distanceMeters) {}
}
