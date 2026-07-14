package com.ax.template.authblueprint.geoquery;

import jakarta.persistence.Column;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for geo-bounded-query-l0. Structural assertions a deliberate break cannot pass
 * silently: GeoPoint has no public setter + immutable columns + the ISO 6709 @Check backstop; the
 * bbox prefilter query uses ONLY named bind parameters (no raw SQL string concatenation anywhere
 * in the domain); and the migration carries the same @Check.
 */
@Tag("GEOQUERY")
class GeoQueryViolationProofTest {

    // ── GeoPoint has no public setter; every column is immutable (a point is re-registered, never updated) ──
    @Test @Tag("GEO-INPUT-001")
    void violation_geoPointNoPublicSetter_immutableColumns() throws Exception {
        for (Method m : GeoPoint.class.getMethods()) {
            assertThat(m.getName()).as("GeoPoint must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "externalRef", "lat", "lon", "createdAt"}) {
            Column col = GeoPoint.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("GeoPoint." + f + " must be immutable").isFalse();
        }
        Check check = GeoPoint.class.getAnnotation(Check.class);
        assertThat(check).as("the ISO 6709 range backstop must be present").isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("lat >= -90").contains("lat <= 90").contains("lon >= -180").contains("lon <= 180");
    }

    // ── GEO-BBOX-001 — no raw SQL string concatenation anywhere in this domain; bind params only ──
    @Test @Tag("GEO-BBOX-001")
    void violation_noRawSqlStringConcatenation_bindParamsOnly() throws Exception {
        String repo = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "geoquery", "GeoPointRepository.java"));
        assertThat(repo).as("the bbox prefilter must bind all four bounds by name")
            .contains(":minLat").contains(":maxLat").contains(":minLon").contains(":maxLon");

        for (String src : new String[]{"GeoQueryService", "GeoQueryController", "GeoPointRepository"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "geoquery", src + ".java"));
            assertThat(text).as(src + " must not build a native/raw query at runtime")
                .doesNotContain("createNativeQuery").doesNotContain("jdbcTemplate")
                .doesNotContain("Statement");
        }
    }

    // ── the migration carries the same ISO 6709 @Check backstop ──
    @Test @Tag("GEO-INPUT-001")
    void violation_migrationCarriesTheSameCheck() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V094__create_geoquery.sql")) {
            assertThat(in).as("V094__create_geoquery.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("lat >= -90").contains("lat <= 90").contains("lon >= -180").contains("lon <= 180");
        }
    }
}
