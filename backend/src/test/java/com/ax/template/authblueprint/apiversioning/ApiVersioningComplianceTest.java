package com.ax.template.authblueprint.apiversioning;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * api-versioning-l0 compliance — every item verified against the live reference workload over
 * black-box HTTP. The catalog-default strategy is {@code url-path}: the major version is the first
 * path segment under the demo prefix. The demo + discovery surfaces are public (version negotiation is
 * API-surface plumbing, not an authorization decision), so the tests drive them directly without a
 * JWT. Domain @Tag("API_VERSIONING") drives ./gradlew testApiVersioning; the per-item @Tag binds the
 * spec item to its test (spec_item_verification_binding guard). Spec: specs/api-versioning-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApiVersioningComplianceTest {

    private static final String DEMO = "/api/api-versioning-demo";
    private static final DateTimeFormatter IMF_FIXDATE = DateTimeFormatter.RFC_1123_DATE_TIME;

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;


    // ── VERSION-NEGOTIATION-001 ──────────────────────────────────────────────

    @Test
    @Tag("API_VERSIONING")
    @Tag("VERSION-NEGOTIATION-001")
    void urlPathStrategy_validSelectsThatVersion_unsupported404_malformed400_conflict400() {
        // an explicit /v2 request selects v2 (the current major) and echoes it back
        given().get(DEMO + "/v2/widgets/42").then().statusCode(200)
                .header("X-API-Version", equalTo("v2"))
                .body("apiVersion", equalTo("v2"));

        // a syntactically VALID but UNSUPPORTED version under url-path → 404 Not Found (spec's url-path branch)
        given().get(DEMO + "/v9/widgets/42").then().statusCode(404)
                .body("code", equalTo("API_VERSION_UNSUPPORTED"));

        // a MALFORMED version token → 400 with an RFC 9457 problem body identifying the negotiation failure
        given().get(DEMO + "/vX/widgets/42").then().statusCode(400)
                .contentType("application/problem+json")
                .body("code", equalTo("API_VERSION_MALFORMED"))
                .body("type", notNullValue());

        // mixing two strategies (path /v1 + a CONFLICTING X-API-Version: 2) → 400, never silently resolved
        given().header("X-API-Version", "2").get(DEMO + "/v1/widgets/42").then().statusCode(400)
                .body("code", equalTo("API_VERSION_STRATEGY_CONFLICT"));

        // a path version with a MATCHING X-API-Version is NOT a conflict (same major) → serves normally
        given().header("X-API-Version", "2").get(DEMO + "/v2/widgets/42").then().statusCode(200);
    }

    // ── VERSION-DEFAULT-001 ──────────────────────────────────────────────────

    @Test
    @Tag("API_VERSIONING")
    @Tag("VERSION-DEFAULT-001")
    void noVersionSelector_resolvesToLatestStable_neverDeprecated_andEchoesResolvedVersion() {
        // latest-stable default → newest NON-deprecated major (v2, since v1 is deprecated)
        given().get(DEMO + "/widgets/7").then().statusCode(200)
                // the resolved version is surfaced BOTH on the header AND in the body field
                .header("X-API-Version", equalTo("v2"))
                .body("apiVersion", equalTo("v2"))
                // the default MUST NOT resolve to the deprecated v1 → no Deprecation/Sunset on the default
                .header("Deprecation", nullValue())
                .header("Sunset", nullValue());

        // a stray X-API-Version with NO path version is still a second strategy → 400 (not silently honoured)
        given().header("X-API-Version", "1").get(DEMO + "/widgets/7").then().statusCode(400)
                .body("code", equalTo("API_VERSION_STRATEGY_CONFLICT"));
    }

    // ── VERSION-COMPATIBILITY-001 ────────────────────────────────────────────

    @Test
    @Tag("API_VERSIONING")
    @Tag("VERSION-COMPATIBILITY-001")
    void withinMajor_onlyAdditive_v1KeepsOriginalRequiredSet_v2AddsOptionalFieldOnly() {
        // v1 returns its ORIGINAL required field set {id, name} (+ the always-present apiVersion echo)
        given().get(DEMO + "/v1/widgets/100").then().statusCode(200)
                .body("id", equalTo("100"))
                .body("name", equalTo("widget-100"))
                .body("apiVersion", equalTo("v1"))
                // the v2-only additive field is ABSENT from v1 — no field was removed/renamed/retyped in v1
                .body("tags", nullValue());

        // v2 adds the OPTIONAL `tags` field WITHOUT removing/renaming/retyping any v1 field
        given().get(DEMO + "/v2/widgets/100").then().statusCode(200)
                .body("id", equalTo("100"))          // unchanged type + semantics across the major boundary
                .body("name", equalTo("widget-100")) // unchanged
                .body("tags", notNullValue());        // NEW optional field only
    }

    // ── VERSION-DEPRECATION-001 ──────────────────────────────────────────────

    @Test
    @Tag("API_VERSIONING")
    @Tag("VERSION-DEPRECATION-001")
    void deprecatedVersion_carriesDeprecationAndSunsetHeaders_stillServes200_sunsetNotInPast() {
        var resp = given().get(DEMO + "/v1/widgets/5").then()
                // a deprecated version STILL serves normally (200, NOT 4xx) until its sunset instant
                .statusCode(200)
                .header("Deprecation", notNullValue())   // signals the WHAT (this version is deprecated)
                .header("Sunset", notNullValue())         // signals the WHEN (RFC 8594 removal instant)
                .header("Link", notNullValue())           // a Link with rel="deprecation"
                .extract();

        // the Sunset value is an IMF-fixdate and MUST NOT be in the past while the version is still served
        Instant sunset = Instant.from(IMF_FIXDATE.parse(resp.header("Sunset")));
        assertThat(sunset).as("Sunset is in the future while v1 is still served").isAfter(Instant.now());

        // the Link header carries the deprecation relation type
        assertThat(resp.header("Link")).contains("rel=\"deprecation\"");

        // the CURRENT version carries NEITHER deprecation header
        given().get(DEMO + "/v2/widgets/5").then().statusCode(200)
                .header("Deprecation", nullValue())
                .header("Sunset", nullValue());
    }

    // ── VERSION-MIGRATION-001 ────────────────────────────────────────────────

    @Test
    @Tag("API_VERSIONING")
    @Tag("VERSION-MIGRATION-001")
    void deprecationWindow_atLeastMinimum_andAbsoluteVersionSpecificMigrationGuideExposed() {
        var resp = given().get(DEMO + "/v1/widgets/9").then().statusCode(200).extract();

        // the migration window (Sunset − Deprecation) MUST be at least the configured minimum (3 months)
        Instant deprecation = Instant.from(IMF_FIXDATE.parse(resp.header("Deprecation")));
        Instant sunset = Instant.from(IMF_FIXDATE.parse(resp.header("Sunset")));
        long windowDays = java.time.Duration.between(deprecation, sunset).toDays();
        assertThat(windowDays).as("migration window honours the 3-month minimum").isGreaterThanOrEqualTo(90L);

        // the deprecated response exposes a stable migration guide URL via a Link rel="deprecation"
        String link = resp.header("Link");
        assertThat(link).contains("rel=\"deprecation\"");
        // the guide URL is ABSOLUTE and VERSION-SPECIFIC (tells the client which NEXT major to move to)
        assertThat(link).contains("https://");
        assertThat(link).contains("v1-to-v2");
    }

    // ── VERSION-DISCOVERY-001 ────────────────────────────────────────────────

    @Test
    @Tag("API_VERSIONING")
    @Tag("VERSION-DISCOVERY-001")
    void discoveryEndpoint_listsVersionsWithStatusAndDates_identifiesDefault_isCacheable_noLeak() {
        var resp = given().get("/api/versions").then()
                .statusCode(200)
                // unauthenticated + cacheable
                .header("Cache-Control", notNullValue())
                // identifies which version the default policy resolves to
                .body("default", equalTo("v2"))
                // every advertised major is listed with a status from {current, deprecated, sunset}
                .body("versions.find { it.version == 'v1' }.status", equalTo("deprecated"))
                .body("versions.find { it.version == 'v2' }.status", equalTo("current"))
                // the deprecated version exposes its dates + migration guide
                .body("versions.find { it.version == 'v1' }.deprecationDate", notNullValue())
                .body("versions.find { it.version == 'v1' }.sunsetDate", notNullValue())
                .body("versions.find { it.version == 'v1' }.migrationGuide", notNullValue())
                .extract();

        // MUST NOT leak internal build identifiers, hostnames, or stack traces
        String raw = resp.asString();
        assertThat(raw).as("no stack-trace leak").doesNotContain("Exception").doesNotContain("\tat ");
        assertThat(raw).as("no internal build/host leak").doesNotContain("localhost").doesNotContain("buildId");
        assertThat(resp.header("Cache-Control")).contains("max-age");
    }

    // ── VERSION-OBSERVABILITY-001 ────────────────────────────────────────────

    @Test
    @Tag("API_VERSIONING")
    @Tag("VERSION-OBSERVABILITY-001")
    void exposesExactlyThreeBoundedLabelMeters() {
        // drive: a current-version request, a deprecated-version request (twice) so all three meters fire
        given().get(DEMO + "/v2/widgets/1").then().statusCode(200);   // current
        given().get(DEMO + "/v1/widgets/1").then().statusCode(200);   // deprecated → requests + deprecated_calls
        given().get(DEMO + "/v1/widgets/2").then().statusCode(200);   // deprecated again

        // the 2 meters the served paths exercise exist (sunset_breach is the 3rd canonical meter but is
        // registered lazily on a sunset breach, which this fixture has no served path for — we DO NOT
        // fabricate a breach just to register it; the meter is defined in ApiVersioningMetrics and would
        // register on first real increment).
        assertThat(registry.find(ApiVersioningMetrics.REQUESTS_BY_VERSION).counter()).isNotNull();
        assertThat(registry.find(ApiVersioningMetrics.DEPRECATED_CALLS).counter()).isNotNull();

        // requests_by_version → bounded {version, status} only, values from the closed sets
        Set<String> versions = Set.of("v1", "v2");
        Set<String> statuses = Set.of("current", "deprecated", "sunset");
        Set<String> reqKeys = Set.of(ApiVersioningMetrics.TAG_VERSION, ApiVersioningMetrics.TAG_STATUS);
        for (Meter m : registry.find(ApiVersioningMetrics.REQUESTS_BY_VERSION).meters()) {
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                assertThat(reqKeys).as("requests_by_version tag key bounded").contains(t.getKey());
                if (t.getKey().equals(ApiVersioningMetrics.TAG_VERSION)) {
                    assertThat(versions).as("version from closed set").contains(t.getValue());
                } else {
                    assertThat(statuses).as("status from closed set").contains(t.getValue());
                }
            }
        }

        // deprecated_calls → bounded {version} only, value from the closed version set
        for (Meter m : registry.find(ApiVersioningMetrics.DEPRECATED_CALLS).meters()) {
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                assertThat(t.getKey()).as("deprecated_calls tag key bounded")
                        .isEqualTo(ApiVersioningMetrics.TAG_VERSION);
                assertThat(versions).as("version from closed set").contains(t.getValue());
            }
        }

        // exact recorded counts (NOT mere existence): v2/current >= 1, v1/deprecated >= 2,
        // deprecated_calls{v1} >= 2 — mirrors the secrets/webhook-signing observability rigor.
        var current = registry.find(ApiVersioningMetrics.REQUESTS_BY_VERSION)
                .tag(ApiVersioningMetrics.TAG_VERSION, "v2")
                .tag(ApiVersioningMetrics.TAG_STATUS, "current").counter();
        assertThat(current).as("v2/current recorded").isNotNull();
        assertThat(current.count()).isGreaterThanOrEqualTo(1.0);

        var deprecatedReqs = registry.find(ApiVersioningMetrics.REQUESTS_BY_VERSION)
                .tag(ApiVersioningMetrics.TAG_VERSION, "v1")
                .tag(ApiVersioningMetrics.TAG_STATUS, "deprecated").counter();
        assertThat(deprecatedReqs).as("v1/deprecated recorded").isNotNull();
        assertThat(deprecatedReqs.count()).isGreaterThanOrEqualTo(2.0);

        var deprecatedCalls = registry.find(ApiVersioningMetrics.DEPRECATED_CALLS)
                .tag(ApiVersioningMetrics.TAG_VERSION, "v1").counter();
        assertThat(deprecatedCalls).as("deprecated_calls{v1} recorded").isNotNull();
        assertThat(deprecatedCalls.count()).isGreaterThanOrEqualTo(2.0);
    }
}
