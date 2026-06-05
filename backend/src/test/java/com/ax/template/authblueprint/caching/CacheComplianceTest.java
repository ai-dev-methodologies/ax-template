package com.ax.template.authblueprint.caching;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Tag;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * caching-l0 compliance — every item verified against the live reference workload. Each test carries
 * the domain @Tag("CACHING") (drives ./gradlew testCaching) plus its per-item @Tag so the
 * spec_item_verification_binding guard resolves the spec item to this test. Spec: specs/caching-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CacheComplianceTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;

    String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        token = obtainToken("cache-" + UUID.randomUUID() + "@example.com");
    }

    private static String obtainToken(String email) {
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"MEMBER\"}")
            .when().post("/api/auth/email/signup");
        return given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
            .when().post("/api/auth/email/login")
            .then().extract().path("accessToken");
    }

    private io.restassured.specification.RequestSpecification auth(String t) {
        return given().header("Authorization", "Bearer " + t);
    }

    @Test
    @Tag("CACHING")
    @Tag("CACHE-CONTROL-001")
    void cacheableResponsesEmitExplicitCacheControl() {
        String id = UUID.randomUUID().toString();
        auth(token).queryParam("visibility", "public").get("/api/cache/items/" + id)
            .then().statusCode(200).header("Cache-Control", "public, max-age=60");
        auth(token).queryParam("visibility", "no-store").get("/api/cache/items/" + id)
            .then().statusCode(200).header("Cache-Control", "no-store");
        auth(token).get("/api/cache/items/" + id)
            .then().statusCode(200).header("Cache-Control", "private, max-age=60");
    }

    @Test
    @Tag("CACHING")
    @Tag("CACHE-ETAG-001")
    void conditionalGetHonoursIfNoneMatch() {
        String id = UUID.randomUUID().toString();
        String etag = auth(token).get("/api/cache/items/" + id).then().statusCode(200)
            .extract().header("ETag");
        assertThat(etag).startsWith("\"").endsWith("\"");

        // matching If-None-Match -> 304 with no body
        String body = auth(token).header("If-None-Match", etag).get("/api/cache/items/" + id)
            .then().statusCode(304).extract().body().asString();
        assertThat(body).isEmpty();

        // after a mutation the representation changes -> the stale validator no longer matches -> 200
        auth(token).post("/api/cache/items/" + id).then().statusCode(204);
        String newEtag = auth(token).header("If-None-Match", etag).get("/api/cache/items/" + id)
            .then().statusCode(200).extract().header("ETag");
        assertThat(newEtag).isNotEqualTo(etag);
    }

    @Test
    @Tag("CACHING")
    @Tag("CACHE-INVALIDATION-001")
    void writeInvalidatesCachedRepresentation() {
        String id = UUID.randomUUID().toString();
        String before = auth(token).get("/api/cache/items/" + id).then().statusCode(200)
            .extract().body().asString();
        auth(token).post("/api/cache/items/" + id).then().statusCode(204);
        String after = auth(token).get("/api/cache/items/" + id).then().statusCode(200)
            .extract().body().asString();
        assertThat(after).isNotEqualTo(before); // read-your-writes: never serve the stale value
    }

    @Test
    @Tag("CACHING")
    @Tag("CACHE-KEY-001")
    void keysAreTenantIsolated() {
        String id = UUID.randomUUID().toString();
        String tokenB = obtainToken("cache-b-" + UUID.randomUUID() + "@example.com");
        String bodyA = auth(token).get("/api/cache/items/" + id).then().statusCode(200).extract().body().asString();
        String bodyB = auth(tokenB).get("/api/cache/items/" + id).then().statusCode(200).extract().body().asString();
        assertThat(bodyA).isNotEqualTo(bodyB); // tenant B must not read tenant A's cached representation

        // a tenant-agnostic key in multi-tenant mode is a contract REJECT
        assertThatThrownBy(() -> new CacheKeyBuilder(true).build("  ", "item", id, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CACHE-KEY-001");
    }

    @Test
    @Tag("CACHING")
    @Tag("CACHE-STAMPEDE-001")
    void concurrentMissesRecomputeExactlyOnce() throws Exception {
        String id = UUID.randomUUID().toString();
        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                Callable<Integer> task = () -> {
                    start.await();
                    return auth(token).get("/api/cache/items/" + id).then().extract().statusCode();
                };
                futures.add(pool.submit(task));
            }
            start.countDown(); // release all threads at once
            for (Future<Integer> f : futures) {
                assertThat(f.get()).isEqualTo(200);
            }
        } finally {
            pool.shutdownNow();
        }
        int recomputes = auth(token).get("/api/cache/items/" + id + "/recomputes")
            .then().statusCode(200).extract().path("recomputes");
        assertThat(recomputes).isEqualTo(1); // single-flight: N parallel misses -> 1 origin recompute
    }

    @Test
    @Tag("CACHING")
    @Tag("CACHE-TTL-001")
    void ttlIsBoundedAndJittered() {
        // unbounded TTL is a contract REJECT
        assertThatThrownBy(() -> TtlPolicy.effectiveTtl(Duration.ZERO, 10))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CACHE-TTL-001");
        assertThatThrownBy(() -> TtlPolicy.effectiveTtl(Duration.ofSeconds(60), 99))
            .isInstanceOf(IllegalArgumentException.class);
        // jittered within ±10% of the 60s base
        for (int i = 0; i < 200; i++) {
            Duration ttl = TtlPolicy.effectiveTtl(Duration.ofSeconds(60), 10);
            assertThat(ttl.toMillis()).isBetween(54_000L, 66_000L);
        }
    }

    @Test
    @Tag("CACHING")
    @Tag("CACHE-OBSERVABILITY-001")
    void exposesExactlyThreeBoundedLabelMeters() {
        String id = UUID.randomUUID().toString();
        auth(token).get("/api/cache/items/" + id).then().statusCode(200); // miss -> hit_rate + latency
        auth(token).get("/api/cache/items/" + id).then().statusCode(200); // hit
        auth(token).post("/api/cache/items/" + id).then().statusCode(204); // eviction

        assertThat(registry.find(CacheMetrics.HIT_RATE).gauge()).isNotNull();
        assertThat(registry.find(CacheMetrics.EVICTION_TOTAL).counter()).isNotNull();
        assertThat(registry.find(CacheMetrics.OP_LATENCY).timer()).isNotNull();

        // labels MUST stay bounded — no key value / resource_id / user_id / payload dimensions
        Set<String> allowed = Set.of("tenant", "cache_name", "reason", "op");
        for (String name : List.of(CacheMetrics.HIT_RATE, CacheMetrics.EVICTION_TOTAL, CacheMetrics.OP_LATENCY)) {
            for (Meter m : registry.find(name).meters()) {
                for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                    assertThat(allowed).as("meter %s tag %s must be bounded-cardinality", name, t.getKey())
                        .contains(t.getKey());
                }
            }
        }
    }
}
