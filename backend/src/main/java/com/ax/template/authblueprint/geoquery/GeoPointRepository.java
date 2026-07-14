package com.ax.template.authblueprint.geoquery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * GEO-BBOX-001 — the indexed bounding-box PREFILTER: a plain lat/lon BETWEEN range predicate (no
 * spatial extension required — no PostGIS/GiST claim). {@link GeoQueryService} applies the exact
 * haversine POSTFILTER + deterministic ordering (GEO-DETERMINISM-001) on the candidates this
 * returns. Bind params only — no string-concatenated SQL (violation-proof asserted).
 */
public interface GeoPointRepository extends JpaRepository<GeoPoint, UUID> {

    @Query("SELECT p FROM GeoPoint p WHERE p.lat BETWEEN :minLat AND :maxLat AND p.lon BETWEEN :minLon AND :maxLon")
    List<GeoPoint> findBoundingBoxCandidates(@Param("minLat") BigDecimal minLat,
                                             @Param("maxLat") BigDecimal maxLat,
                                             @Param("minLon") BigDecimal minLon,
                                             @Param("maxLon") BigDecimal maxLon,
                                             Pageable pageable);
}
