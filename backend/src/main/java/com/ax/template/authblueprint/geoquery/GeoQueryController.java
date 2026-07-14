package com.ax.template.authblueprint.geoquery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * geo-bounded-query-l0 thin controller. Delegates to {@link GeoQueryService}. HONEST DEGRADED
 * SUBSET — no PostGIS/GiST claim (specs/geo-bounded-query-l0.yaml scope + GEO-GIST-REVIEW-001).
 */
@RestController
public class GeoQueryController {

    public record RegisterReq(@NotBlank @Size(max = 200) String externalRef,
                              @NotNull BigDecimal lat, @NotNull BigDecimal lon) {}

    public record PointDto(UUID id, String externalRef, BigDecimal lat, BigDecimal lon, Instant createdAt) {
        static PointDto of(GeoPoint p) {
            return new PointDto(p.getId(), p.getExternalRef(), p.getLat(), p.getLon(), p.getCreatedAt());
        }
    }

    public record SearchResultDto(PointDto point, double distanceMeters) {}

    private final GeoQueryService service;

    public GeoQueryController(GeoQueryService service) {
        this.service = service;
    }

    /** GEO-INPUT-001 — register a point. */
    @PostMapping("/api/geo/points")
    public ResponseEntity<PointDto> register(@Valid @RequestBody RegisterReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(PointDto.of(service.register(req.externalRef(), req.lat(), req.lon())));
    }

    /** GEO-BBOX/DETERMINISM-001 — radius query: bbox prefilter, exact haversine postfilter, deterministic order. */
    @GetMapping("/api/geo/points/search")
    public List<SearchResultDto> search(@RequestParam BigDecimal lat, @RequestParam BigDecimal lon,
                                        @RequestParam double radiusMeters) {
        return service.searchRadius(lat, lon, radiusMeters).stream()
            .map(r -> new SearchResultDto(PointDto.of(r.point()), r.distanceMeters()))
            .toList();
    }

    @ExceptionHandler(GeoQueryException.class)
    public ResponseEntity<ProblemDetail> handle(GeoQueryException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
