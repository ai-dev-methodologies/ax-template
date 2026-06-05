package com.ax.template.authblueprint.apiversioning;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VERSION-DISCOVERY-001 — the unauthenticated, cacheable version-discovery endpoint ({@code
 * /api/versions}). Returns 200 with a JSON document listing every advertised major version and, per
 * version, its {@code status} ∈ {current, deprecated, sunset}; for deprecated versions the
 * {@code deprecationDate} + {@code sunsetDate} (RFC 3339) and the migration guide URL. It identifies
 * which version the default policy resolves to ({@code default: "v2"}). It sets {@code Cache-Control}
 * and NEVER leaks build identifiers, hostnames, or stack traces.
 *
 * <p>Spec: specs/api-versioning-l0.yaml#VERSION-DISCOVERY-001.
 */
@RestController
public class ApiVersionDiscoveryController {

    /** RFC 3339 instant (ISO-8601 with offset) for the discovery document's date fields. */
    private static final DateTimeFormatter RFC_3339 = DateTimeFormatter.ISO_INSTANT;

    private final ApiVersionCatalog catalog;

    public ApiVersionDiscoveryController(ApiVersionCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/versions")
    public ResponseEntity<Map<String, Object>> versions() {
        List<Map<String, Object>> versions = new ArrayList<>();
        for (ApiVersionCatalog.Version v : catalog.all()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("version", v.label());
            entry.put("status", v.status().name().toLowerCase());
            if (v.isDeprecated()) {
                entry.put("deprecationDate", RFC_3339.format(v.deprecatedAt()));
                entry.put("sunsetDate", RFC_3339.format(v.sunsetAt()));
                entry.put("migrationGuide", v.migrationGuide());
            }
            versions.add(entry);
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("default", catalog.resolveDefault().label());
        doc.put("versions", versions);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(doc);
    }
}
