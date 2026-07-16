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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    tools.jackson.databind.ObjectMapper objectMapper;

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
    void oversizedBodyRejectedBeforeFingerprint() {
        // Jackson 3 raised the default stream max-string-length 20MB→100MB, and that cap does NOT
        // reach RequestFingerprint's hand-built standalone mapper. The controller therefore rejects a
        // body larger than 20MB with 413 BEFORE it can be parsed/reserialized. Marker must not echo.
        String marker = "OVERSIZEDBODYMARKER";
        String hugeBody = "{\"v\":\"" + marker + "Z".repeat(20_000_050) + "\"}";
        String body = post(token, UUID.randomUUID().toString(), hugeBody)
            .post("/api/idempotency-demo/resources")
            .then().statusCode(413).extract().asString();
        assertThat(body)
            .as("413 body must not echo the oversized request payload")
            .doesNotContain(marker);
    }

    @Test
    @Tag("IDEMPOTENCY")
    void transportFilterRejectsOverCapBodyAtEdge_boundedSwallowPosture() {
        // DEFENSE 1 (end-to-end wiring): a body FAR over the transport filter cap (20 MiB) is rejected
        // at the outermost edge by RequestBodySizeLimitFilter BEFORE any converter buffers it. This
        // body (21 MiB) exceeds the filter's byte cap, so the filter's fast path commits a 413 WITHOUT
        // reading it (unlike oversizedBodyRejectedBeforeFingerprint, < 20 MiB, which the controller belt
        // catches after a full read). The container is then left with ~21 MiB of unread body. With
        // server.tomcat.max-swallow-size at its BOUNDED default (2 MiB) — the fix for the unbounded
        // thread-hold DoS (an attacker declaring a 1 TB Content-Length and trickling forever) — Tomcat
        // REFUSES to drain 21 MiB and resets the connection instead of holding the request thread.
        // Correct secure outcome: EITHER a 413 (if the small committed response flushed first) OR a
        // connection reset — NEVER acceptance, NEVER an echo of the payload, NEVER an unbounded hold.
        String marker = "TRANSPORTFILTERMARKER";
        String hugeBody = "{\"v\":\"" + marker + "Z".repeat(21 * 1024 * 1024) + "\"}";

        // NON-VACUITY WITNESS (server-side, deterministic): the edge filter increments this counter
        // exactly once per 413 it commits BEFORE any converter buffers the body. Snapshot the delta
        // around the request. A hugely-oversized body seen client-side as a connection reset gives no
        // readable 413, and an edge 413 is indistinguishable from a downstream controller-belt 413 by
        // response bytes — so this counter, not client forensics, is what locks "the reject happened at
        // the transport edge." Revert the edge reject (raise the filter cap) → controller belt handles
        // it → this counter never moves → the assertion below fails RED.
        double rejectsBefore = rejectionCounter();

        // NO-UNBOUNDED-HOLD PROOF: measure wall-clock around ONLY the request. The bounded max-swallow
        // posture resets the connection promptly instead of holding the request thread to drain 21 MiB.
        // A stalled/held thread (the DoS this prevents) would blow past this bound.
        long startNanos = System.nanoTime();
        Exception thrown = null;
        io.restassured.response.Response resp = null;
        try {
            resp = post(token, UUID.randomUUID().toString(), hugeBody)
                .post("/api/idempotency-demo/resources").thenReturn();
        } catch (Exception e) {
            thrown = e;
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertThat(elapsedMs)
            .as("over-cap request must resolve promptly (bounded reset / prompt 413), never hold the "
                + "request thread to drain the body — a stall would exceed this bound (elapsed=%dms)", elapsedMs)
            .isLessThan(10_000L);

        if (thrown == null) {
            // A response was delivered: it MUST be the clean 413 rejection with no echo (never 2xx).
            assertThat(resp.statusCode())
                .as("if a response is delivered, it is the 413 rejection (never 2xx acceptance)")
                .isEqualTo(413);
            assertThat(resp.asString())
                .as("413 body must not echo the oversized request payload")
                .doesNotContain(marker)
                .contains("REQUEST_BODY_TOO_LARGE");
        } else {
            // No readable response: the ONLY acceptable transport outcome is a connection RESET /
            // broken pipe (a SocketException that is NOT an InterruptedIOException subtype). A timeout
            // (InterruptedIOException — covers SocketTimeoutException AND ConnectTimeoutException) or a
            // connect-establishment failure (ConnectException) signals a STALL / held thread, the exact
            // failure the bounded swallow prevents — those must FAIL, not pass.
            boolean stallOrConnectFailure = false;
            boolean connectionReset = false;
            for (Throwable c = thrown; c != null; c = (c.getCause() == c) ? null : c.getCause()) {
                if (c instanceof java.io.InterruptedIOException || c instanceof java.net.ConnectException) {
                    stallOrConnectFailure = true; // timeout / connect-establishment problem == a stall
                }
                if (c instanceof java.net.SocketException && !(c instanceof java.io.InterruptedIOException)) {
                    String msg = c.getMessage() == null ? "" : c.getMessage().toLowerCase(java.util.Locale.ROOT);
                    if (msg.contains("reset") || msg.contains("broken pipe") || msg.contains("connection")) {
                        connectionReset = true; // Tomcat reset mid-write rather than drain unbounded
                    }
                }
            }
            assertThat(stallOrConnectFailure)
                .as("outcome must NOT be a timeout/connect-stall (InterruptedIOException — incl. "
                    + "Socket/ConnectTimeoutException — or ConnectException): those signal a held/stalled "
                    + "request thread, the exact failure the bounded swallow prevents: " + thrown)
                .isFalse();
            assertThat(connectionReset)
                .as("hugely-oversized body rejected by a clean connection RESET / broken pipe (SocketException, "
                    + "NOT a timeout) under the bounded swallow — no unbounded thread-hold; payload never "
                    + "buffered: " + thrown)
                .isTrue();
        }

        // Edge-reject witness: the filter committed a 413 at the transport edge for this over-cap body.
        assertThat(rejectionCounter() - rejectsBefore)
            .as("RequestBodySizeLimitFilter must have rejected the over-cap body AT THE EDGE (metric "
                + "'%s' incremented) — reverting the edge reject makes this stay 0 (RED)",
                com.ax.template.authblueprint.common.RequestBodySizeLimitFilter.REJECTIONS_METRIC)
            .isGreaterThanOrEqualTo(1.0);
    }

    /** Current value of the edge-reject witness counter (0.0 until the filter first commits a 413). */
    private double rejectionCounter() {
        var counter = registry.find(
            com.ax.template.authblueprint.common.RequestBodySizeLimitFilter.REJECTIONS_METRIC).counter();
        return counter == null ? 0.0 : counter.count();
    }

    @Test
    @Tag("IDEMPOTENCY")
    void autoconfiguredMapperRejectsOversizedJsonString() {
        // DEFENSE 5 (regression guard): the auto-configured Spring MVC mapper is pinned to a 20M
        // max-string-length (application.yml). A JSON string field > 20M chars must be REJECTED on
        // the parse path (Jackson 3's default 100M would otherwise silently accept it). If a future
        // change removes the pin, this parse succeeds and the test fails.
        String oversized = "{\"s\":\"" + "a".repeat(20_000_050) + "\"}";
        assertThatThrownBy(() -> objectMapper.readValue(oversized, Object.class))
            .as("oversized JSON string is rejected by the pinned 20M max-string-length")
            .isInstanceOf(tools.jackson.core.JacksonException.class);
    }

    @Test
    @Tag("IDEMPOTENCY")
    void autoconfiguredMapperRejectsTokenDenseBody() {
        // DEFENSE 5b (token-count seal): the auto-configured Spring MVC mapper is pinned to a 1M
        // max-token-count (spring.jackson.factory.constraints.read.max-token-count). Jackson 3's
        // default is -1 (UNBOUNDED), so a sub-20MB body packed with millions of tiny STRUCTURAL
        // tokens — {"x":[{},{},…]} — would otherwise deserialize into millions of maps → heap
        // exhaustion, even though every individual string/number is tiny (max-string-length never
        // trips). The pinned bound aborts the parse with a JacksonException (NOT an OOM / 500). This
        // body is ~2.1MB (~1.4M structural tokens) — over the 1M token bound, under the 20M string
        // and 20M document bounds — so ONLY the token-count knob can reject it.
        StringBuilder sb = new StringBuilder("{\"x\":[");
        for (int i = 0; i < 700_000; i++) {
            sb.append("{},");
        }
        sb.append("{}]}");
        String tokenDense = sb.toString();
        assertThatThrownBy(() -> objectMapper.readValue(tokenDense, Object.class))
            .as("token-dense body rejected by the pinned 1M max-token-count (parse aborted, not OOM)")
            .isInstanceOf(tools.jackson.core.JacksonException.class);
    }

    @Test
    @Tag("IDEMPOTENCY")
    void tokenDenseBodyRejectedByControllerNotAcceptedOrOom() {
        // END-TO-END for the over-constraint reject path: a token-dense body (~2.1MB, ~1.4M
        // structural tokens) is UNDER the 20MB char cap (controller length check passes) but OVER
        // RequestFingerprint's 1M max-token-count. The fingerprint parse aborts → the request is
        // REJECTED with 413 (NOT accepted with a degraded raw-hash fingerprint, NOT an OOM/500). A
        // degraded fingerprint would let a same-key reorder retry get a false 422 instead of a replay.
        // The rejection must not echo the body.
        // A unique marker is embedded as the array's object KEY so the body carries a distinctive
        // substring: if the 413 ever reflected any request content, the marker would surface. The
        // marker does not alter the ~1.4M structural-token count that trips the bound.
        String marker = "TOKENDENSEBODYMARKER";
        StringBuilder sb = new StringBuilder("{\"" + marker + "\":[");
        for (int i = 0; i < 700_000; i++) {
            sb.append("{},");
        }
        sb.append("{}]}");
        String resp = post(token, UUID.randomUUID().toString(), sb.toString())
            .post("/api/idempotency-demo/resources")
            .then().statusCode(413).extract().asString();
        assertThat(resp)
            .as("413 rejection carries the bounded error code and never echoes the body")
            .contains("REQUEST_BODY_TOO_LARGE")
            .doesNotContain(marker);
    }

    @Test
    @Tag("IDEMPOTENCY")
    void legitimateKeyOrderReorderStillReplaysUnderTheCap() {
        // Control paired with the reject test above: a LEGITIMATE under-cap body whose retry differs
        // ONLY in object-key order STILL canonicalizes to the same fingerprint → replay (not 422).
        // Proves the reject path did not collateral-damage normal idempotency canonicalization.
        String key = UUID.randomUUID().toString();
        String id = post(token, key, "{\"a\":1,\"b\":2}").post("/api/idempotency-demo/resources")
            .then().statusCode(201).extract().jsonPath().getString("id");
        post(token, key, "{\"b\":2,\"a\":1}").post("/api/idempotency-demo/resources")
            .then().statusCode(201).header("Idempotency-Replayed", "true")
            .body("id", org.hamcrest.Matchers.equalTo(id));
    }

    @Test
    @Tag("IDEMPOTENCY")
    @Tag("IDEMPOTENCY-CACHE-001")
    void keysAreIsolatedPerTenant() {
        String key = UUID.randomUUID().toString();
        String body = "{\"v\":1}";
        String idA = post(token, key, body).post("/api/idempotency-demo/resources")
            .then().statusCode(201).extract().jsonPath().getString("id");
        // the store DOES replay within a tenant (so the cross-tenant difference below is real
        // isolation, not merely a store that always creates fresh)
        String idAReplay = post(token, key, body).post("/api/idempotency-demo/resources")
            .then().statusCode(201).header("Idempotency-Replayed", "true").extract().jsonPath().getString("id");
        assertThat(idAReplay).isEqualTo(idA);
        // a DIFFERENT tenant reusing the SAME key gets its OWN fresh resource (no cross-tenant replay)
        String tokenB = freshTenantToken("idem-b");
        String idB = post(tokenB, key, body).post("/api/idempotency-demo/resources")
            .then().statusCode(201).header("Idempotency-Replayed", "false").extract().jsonPath().getString("id");
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
        // The TIMING-INDEPENDENT write-once invariant: exactly ONE request creates a new resource
        // (201 + Replayed:false); every other request is EITHER rejected in-flight (409) OR replays
        // the winner verbatim (201 + Replayed:true) — never a second creation. A hard "conflict==9"
        // would be coupled to the WORK_LATENCY window; this invariant holds at any timing.
        AtomicInteger firstSeen = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger replay = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        java.util.Set<String> winnerIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    var resp = post(token, key, body).post("/api/idempotency-demo/resources").thenReturn();
                    int sc = resp.statusCode();
                    if (sc == 201 && "false".equals(resp.header("Idempotency-Replayed"))) {
                        firstSeen.incrementAndGet();
                        winnerIds.add(resp.jsonPath().getString("id"));
                    } else if (sc == 201) {
                        replay.incrementAndGet();
                        winnerIds.add(resp.jsonPath().getString("id"));
                    } else if (sc == 409) {
                        conflict.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        go.countDown(); // fire all 10 simultaneously
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(firstSeen.get()).as("exactly one winner created the resource").isEqualTo(1);
        assertThat(conflict.get() + replay.get()).as("the other nine neither created nor failed unexpectedly").isEqualTo(9);
        assertThat(conflict.get()).as("at least one concurrent duplicate was rejected in-flight").isGreaterThanOrEqualTo(1);
        assertThat(other.get()).as("no unexpected status").isZero();
        assertThat(winnerIds).as("every non-conflict response references the single winning resource").hasSize(1);

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

        // NESTED object key reorder MUST also canonicalize (proves recursive sorting, not just
        // top-level) — the dogfood flagged this as a potential gap; lock it in empirically.
        String nestedKey = UUID.randomUUID().toString();
        String nestedId = post(token, nestedKey, "{\"outer\":{\"a\":1,\"b\":2},\"z\":9}")
            .post("/api/idempotency-demo/resources").then().statusCode(201).extract().jsonPath().getString("id");
        post(token, nestedKey, "{\"z\":9,\"outer\":{\"b\":2,\"a\":1}}")
            .post("/api/idempotency-demo/resources").then().statusCode(201)
            .header("Idempotency-Replayed", "true")
            .body("id", org.hamcrest.Matchers.equalTo(nestedId));
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
        // conflict — a latch-gated burst on a fresh key. 8 simultaneous requests within the
        // in-flight window make >=1 in-flight rejection effectively certain (7 losers); we assert
        // >=1 (not a hard count) so the metric-recording check is not timing-coupled.
        String k2 = UUID.randomUUID().toString();
        int burst = 8;
        ExecutorService pool = Executors.newFixedThreadPool(burst);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger burstConflicts = new AtomicInteger();
        for (int i = 0; i < burst; i++) {
            pool.submit(() -> {
                try {
                    go.await();
                    if (post(token, k2, "{\"v\":1}").post("/api/idempotency-demo/resources")
                            .thenReturn().statusCode() == 409) {
                        burstConflicts.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        assertThat(burstConflicts.get()).as("the burst produced at least one in-flight conflict").isGreaterThanOrEqualTo(1);

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
