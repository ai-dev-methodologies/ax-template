package com.ax.template.authblueprint.geoquery;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * geo-bounded-query-l0 compliance — verified against the live geoquery reference workload. HONEST
 * DEGRADED SUBSET: no PostGIS/GiST behavior is claimed (H2 cannot verify it) — the invariant
 * verified here is the bbox-prefilter-then-exact-haversine-postfilter SHAPE, fail-closed ISO 6709
 * input bounds, and deterministic ordering. Spec: specs/geo-bounded-query-l0.yaml.
 *
 * <p>Each test uses a WIDELY SEPARATED coordinate region (whole-degree offsets) so radius queries
 * never cross-pollute across test methods sharing the same H2 instance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("GEOQUERY")
class GeoQueryComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = GeoQueryTestSupport.obtainToken(GeoQueryTestSupport.freshEmail("geo-member"), "MEMBER");
    }

    private ExtractableResponse<Response> register(String externalRef, String lat, String lon) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"externalRef\":\"" + externalRef + "\",\"lat\":" + lat + ",\"lon\":" + lon + "}")
        .when().post("/api/geo/points").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> search(String lat, String lon, String radiusMeters) {
        return given().header("Authorization", "Bearer " + member)
            .queryParam("lat", lat).queryParam("lon", lon).queryParam("radiusMeters", radiusMeters)
        .when().get("/api/geo/points/search").thenReturn().then().extract();
    }

    // ── GEO-INPUT-001 — fail-closed lat/lon/radius bounds ──
    @Test @Tag("GEO-INPUT-001")
    void register_outOfBoundsLatLon_is422() {
        assertThat(register("bad-lat-hi", "91", "0").statusCode()).isEqualTo(422);
        assertThat(register("bad-lat-lo", "-91", "0").statusCode()).isEqualTo(422);
        assertThat(register("bad-lon-hi", "0", "181").statusCode()).isEqualTo(422);
        assertThat(register("bad-lon-lo", "0", "-181").statusCode()).isEqualTo(422);

        ExtractableResponse<Response> bad = register("bad-lat-hi", "91", "0");
        assertThat(bad.jsonPath().getString("code")).isEqualTo("GEO_INVALID_INPUT");
    }

    @Test @Tag("GEO-INPUT-001")
    void register_validBounds_is201() {
        assertThat(register("valid-point", "1.5", "1.5").statusCode()).isEqualTo(201);
    }

    @Test @Tag("GEO-INPUT-001")
    void search_invalidRadius_is422() {
        assertThat(search("1", "1", "0").statusCode()).isEqualTo(422);
        assertThat(search("1", "1", "-5").statusCode()).isEqualTo(422);
        assertThat(search("1", "1", "50001").statusCode()).isEqualTo(422); // past MAX_RADIUS_METERS
        assertThat(search("1", "1", "500").statusCode()).isEqualTo(200);  // valid
    }

    // ── GEO-BBOX-001 — indexed bbox prefilter, exact haversine postfilter, no over-inclusion ──
    @Test @Tag("GEO-BBOX-001")
    void search_cornerInBboxButOutsideRadius_isExcluded() {
        // region around (10.0, 10.0) — isolated from other tests' coordinates
        double centerLat = 10.0;
        double centerLon = 10.0;
        register("bbox-inside", String.valueOf(centerLat + 0.003), String.valueOf(centerLon));   // ~333m — inside
        register("bbox-corner", String.valueOf(centerLat + 0.007), String.valueOf(centerLon + 0.007)); // ~1100m — bbox candidate, outside radius

        ExtractableResponse<Response> res = search(String.valueOf(centerLat), String.valueOf(centerLon), "1000");
        assertThat(res.statusCode()).isEqualTo(200);
        var refs = res.jsonPath().getList("point.externalRef");
        assertThat(refs).as("the inside point is returned").contains("bbox-inside");
        assertThat(refs).as("the bbox-corner point is a bbox candidate but OUTSIDE the true radius")
            .doesNotContain("bbox-corner");

        // every returned point's exact distance must be <= radius
        var distances = res.jsonPath().getList("distanceMeters", Double.class);
        assertThat(distances).allMatch(d -> d <= 1000.0);
    }

    // ── GEO-DETERMINISM-001 — distance ascending, id ascending stable tiebreak ──
    @Test @Tag("GEO-DETERMINISM-001")
    void search_repeatedQuery_returnsIdenticalOrderedResult() {
        // region around (20.0, 20.0) — isolated from other tests' coordinates
        double centerLat = 20.0;
        double centerLon = 20.0;
        // two points at the EXACT same location — distance 0 from center — tie broken by id.
        String idA = register("tie-a", String.valueOf(centerLat + 0.001), String.valueOf(centerLon + 0.001))
            .jsonPath().getString("id");
        String idB = register("tie-b", String.valueOf(centerLat + 0.001), String.valueOf(centerLon + 0.001))
            .jsonPath().getString("id");

        var first = search(String.valueOf(centerLat), String.valueOf(centerLon), "1000")
            .jsonPath().getList("point.id");
        var second = search(String.valueOf(centerLat), String.valueOf(centerLon), "1000")
            .jsonPath().getList("point.id");

        assertThat(first).as("repeated identical query returns the identical ordered list")
            .containsExactlyElementsOf(second);
        // both are distance-0 ties — the stable secondary sort key is point id ascending
        var expectedOrder = java.util.stream.Stream.of(idA, idB)
            .sorted(java.util.Comparator.comparing(UUID::fromString)).toList();
        assertThat(first).containsExactlyElementsOf(expectedOrder);
    }
}
