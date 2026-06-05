package com.ax.template.authblueprint.idempotency;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * idempotency-l0 compliance — every item verified against the live reference workload. Domain
 * @Tag("IDEMPOTENCY") drives ./gradlew testIdempotency; the per-item @Tag binds the spec item to its
 * test (spec_item_verification_binding guard). Spec: specs/idempotency-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IdempotencyComplianceTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;

    String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        token = freshTenantToken("idem");
    }

    private String freshTenantToken(String prefix) {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"MEMBER\"}")
            .when().post("/api/auth/email/signup");
        return given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
            .when().post("/api/auth/email/login").then().extract().path("accessToken");
    }

    private io.restassured.specification.RequestSpecification post(String tok, String key, String body) {
        var spec = given().header("Authorization", "Bearer " + tok).header("Content-Type", "application/json");
        if (key != null) {
            spec = spec.header("Idempotency-Key", key);
        }
        return spec.body(body);
    }

    @Test
    @Tag("IDEMPOTENCY")
    @Tag("IDEMPOTENCY-KEY-001")
    void invalidKeyFormatRejected() {
        post(token, "bad key with spaces", "{\"v\":1}")
            .post("/api/idempotency-demo/resources").then().statusCode(400)
            .body("code", org.hamcrest.Matchers.equalTo("IDEMPOTENCY_KEY_INVALID"));
        // a valid UUID key is accepted
        post(token, UUID.randomUUID().toString(), "{\"v\":1}")
            .post("/api/idempotency-demo/resources").then().statusCode(201);
    }

    @Test
    @Tag("IDEMPOTENCY")
    @Tag("IDEMPOTENCY-CACHE-001")
    void keysAreIsolatedPerTenant() {
        String key = UUID.randomUUID().toString();
        String body = "{\"v\":1}";
        String idA = post(token, key, body).post("/api/idempotency-demo/resources")
            .then().statusCode(201).extract().jsonPath().getString("id");
        // a DIFFERENT tenant reusing the SAME key gets its OWN fresh resource (no cross-tenant replay)
        String tokenB = freshTenantToken("idem-b");
        String idB = post(tokenB, key, body).post("/api/idempotency-demo/resources")
            .then().statusCode(201).extract().jsonPath().getString("id");
        assertThat(idB).isNotEqualTo(idA);
    }

    @Test
    @Tag("IDEMPOTENCY")
    @Tag("IDEMPOTENCY-DEDUP-001")
    void replaysCachedResponseAndRejectsPayloadChange() {
        String key = UUID.randomUUID().toString();
        String first = post(token, key, "{\"v\":1}").post("/api/idempotency-demo/resources")
            .then().statusCode(201).header("Idempotency-Replayed", "false")
            .extract().jsonPath().getString("id");
        // identical request → cached response replayed VERBATIM (same id), flagged replayed
        String replay = post(token, key, "{\"v\":1}").post("/api/idempotency-demo/resources")
            .then().statusCode(201).header("Idempotency-Replayed", "true")
            .extract().jsonPath().getString("id");
        assertThat(replay).isEqualTo(first);
        // same key, DIFFERENT payload → 422 (key reused with different payload)
        post(token, key, "{\"v\":999}").post("/api/idempotency-demo/resources")
            .then().statusCode(422)
            .body("code", org.hamcrest.Matchers.equalTo("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"));
    }

    @Test
    @Tag("IDEMPOTENCY")
    @Tag("IDEMPOTENCY-SCOPE-001")
    void keyRejectedOnGetAndAbsentKeyDoesNotDedup() {
        // SCOPE-001: Idempotency-Key on an inherently-idempotent GET → 400
        given().header("Authorization", "Bearer " + token).header("Idempotency-Key", UUID.randomUUID().toString())
            .get("/api/idempotency-demo/resources").then().statusCode(400)
            .body("code", org.hamcrest.Matchers.equalTo("IDEMPOTENCY_KEY_NOT_ALLOWED"));
        // absent key → no dedup: two identical POSTs create two distinct resources
        String id1 = post(token, null, "{\"v\":1}").post("/api/idempotency-demo/resources")
            .then().statusCode(201).extract().jsonPath().getString("id");
        String id2 = post(token, null, "{\"v\":1}").post("/api/idempotency-demo/resources")
            .then().statusCode(201).extract().jsonPath().getString("id");
        assertThat(id2).isNotEqualTo(id1);
    }

    @Test
    @Tag("IDEMPOTENCY")
    @Tag("IDEMPOTENCY-CONCURRENT-001")
    void tenParallelSameKeyYieldOneSuccessNineConflicts() throws InterruptedException {
        String key = UUID.randomUUID().toString();
        String body = "{\"v\":1}";
        int n = 10;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    int sc = post(token, key, body).post("/api/idempotency-demo/resources").thenReturn().statusCode();
                    if (sc == 201) success.incrementAndGet();
                    else if (sc == 409) conflict.incrementAndGet();
                    else other.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        go.countDown(); // fire all 10 simultaneously
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).as("exactly one winner").isEqualTo(1);
        assertThat(conflict.get()).as("nine 409 conflicts").isEqualTo(9);
        assertThat(other.get()).isZero();

        // after the winner completes, a subsequent identical request replays the cached response
        post(token, key, body).post("/api/idempotency-demo/resources")
            .then().statusCode(201).header("Idempotency-Replayed", "true");
    }

    @Test
    @Tag("IDEMPOTENCY")
    @Tag("IDEMPOTENCY-PAYLOAD-001")
    void fingerprintCanonicalizesJsonKeyOrderButDetectsValueChange() {
        String key = UUID.randomUUID().toString();
        String id = post(token, key, "{\"a\":1,\"b\":2}").post("/api/idempotency-demo/resources")
            .then().statusCode(201).extract().jsonPath().getString("id");
        // SAME semantic payload, different KEY ORDER → same fingerprint → replay (not 422)
        String replay = post(token, key, "{\"b\":2,\"a\":1}").post("/api/idempotency-demo/resources")
            .then().statusCode(201).header("Idempotency-Replayed", "true")
            .extract().jsonPath().getString("id");
        assertThat(replay).isEqualTo(id);
        // a genuine VALUE change → different fingerprint → 422
        post(token, key, "{\"a\":1,\"b\":3}").post("/api/idempotency-demo/resources")
            .then().statusCode(422);
    }

    @Test
    @Tag("IDEMPOTENCY")
    @Tag("IDEMPOTENCY-OBSERVABILITY-001")
    void exposesBoundedLabelMeters() throws InterruptedException {
        // first_seen + replayed
        String k1 = UUID.randomUUID().toString();
        post(token, k1, "{\"v\":1}").post("/api/idempotency-demo/resources").then().statusCode(201);
        post(token, k1, "{\"v\":1}").post("/api/idempotency-demo/resources").then().statusCode(201);
        // fingerprint_mismatch
        post(token, k1, "{\"v\":2}").post("/api/idempotency-demo/resources").then().statusCode(422);
        // conflict — a 2-parallel burst on a fresh key
        String k2 = UUID.randomUUID().toString();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    go.await();
                    post(token, k2, "{\"v\":1}").post("/api/idempotency-demo/resources");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(registry.find(IdempotencyMetrics.REQUESTS).counter()).isNotNull();
        assertThat(registry.find(IdempotencyMetrics.HIT_RATE).gauge()).isNotNull();
        assertThat(registry.find(IdempotencyMetrics.LOCK_WAIT).timer()).isNotNull();

        Set<String> outcomes = Set.of("first_seen", "replayed", "conflict", "fingerprint_mismatch");
        Set<String> allowedKeys = Set.of(IdempotencyMetrics.TAG_TENANT, IdempotencyMetrics.TAG_OUTCOME);
        for (Meter m : registry.find(IdempotencyMetrics.REQUESTS).meters()) {
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                assertThat(allowedKeys).as("requests_total tag key bounded").contains(t.getKey());
                if (t.getKey().equals(IdempotencyMetrics.TAG_OUTCOME)) {
                    assertThat(outcomes).as("outcome from closed set").contains(t.getValue());
                }
            }
        }
        // each of the four outcomes was actually exercised
        for (String oc : outcomes) {
            assertThat(registry.find(IdempotencyMetrics.REQUESTS)
                .tag(IdempotencyMetrics.TAG_OUTCOME, oc).counter())
                .as("outcome %s recorded", oc).isNotNull();
        }
    }
}
