package com.ax.template.authblueprint.optlock;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * optimistic-locking-l0 compliance — every item verified against the live reference workload.
 * Domain @Tag("OPTLOCK") drives ./gradlew testOptimisticLocking; the per-item @Tag binds the spec
 * item to its test (spec_item_verification_binding guard). Spec: specs/optimistic-locking-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OptlockComplianceTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;

    String token;

    @BeforeEach
    void setUp() {
        String email = "optlock-" + UUID.randomUUID() + "@example.com";
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"MEMBER\"}")
            .when().post("/api/auth/email/signup");
        token = given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
            .when().post("/api/auth/email/login").then().statusCode(200).extract().path("accessToken");
    }

    private io.restassured.specification.RequestSpecification auth() {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
    }

    /** Create a resource; returns {id, etag}. */
    private String[] create() {
        var r = auth().body("{\"name\":\"init\",\"quantity\":1}")
            .post("/api/optlock/resources").then().statusCode(201).extract();
        return new String[]{r.jsonPath().getString("id"), r.header("ETag")};
    }

    @Test
    @Tag("OPTLOCK")
    @Tag("OPTLOCK-ETAG-001")
    void emitsStrongVersionDerivedEtagThatChangesOnMutation() {
        String[] created = create();
        String id = created[0];
        String etag0 = auth().get("/api/optlock/resources/" + id).then().statusCode(200).extract().header("ETag");
        // strong validator: quoted, NO weak W/ prefix; deterministic "<entityType>-<id>-<version>"
        assertThat(etag0).startsWith("\"").doesNotStartWith("W/").isEqualTo(expectedEtag(id, 0));
        // a field change MUST change the ETag
        String etag1 = auth().header("If-Match", etag0).body("{\"name\":\"changed\",\"quantity\":2}")
            .put("/api/optlock/resources/" + id).then().statusCode(200).extract().header("ETag");
        assertThat(etag1).isEqualTo(expectedEtag(id, 1)).isNotEqualTo(etag0);
    }

    /** OPTLOCK-ETAG-001 3-part strong validator format: "<entityType>-<id>-<version>". */
    private static String expectedEtag(String id, long version) {
        return "\"optlock-resource-" + id + "-" + version + "\"";
    }

    @Test
    @Tag("OPTLOCK")
    @Tag("OPTLOCK-IFMATCH-001")
    void mutationWithoutIfMatchIsRejected428() {
        String id = create()[0];
        auth().body("{\"name\":\"x\",\"quantity\":2}")
            .put("/api/optlock/resources/" + id).then().statusCode(428)
            .body("code", org.hamcrest.Matchers.equalTo("PRECONDITION_REQUIRED"));

        // RFC 7232 §3.2: a WEAK validator — even the weak form of the CORRECT current ETag — can
        // never satisfy an If-Match (strong comparison) → 412, never a silent match.
        String[] fresh = create();
        auth().header("If-Match", "W/" + fresh[1]).body("{\"name\":\"x\",\"quantity\":2}")
            .put("/api/optlock/resources/" + fresh[0]).then().statusCode(412)
            .body("code", org.hamcrest.Matchers.equalTo("PRECONDITION_FAILED"));
    }

    @Test
    @Tag("OPTLOCK")
    @Tag("OPTLOCK-VERSION-001")
    void versionIncrementsOnEachWriteAndIsNotClientWritable() {
        String[] created = create();
        String id = created[0];
        String etag0 = created[1];
        String etag1 = auth().header("If-Match", etag0).body("{\"name\":\"a\",\"quantity\":2}")
            .put("/api/optlock/resources/" + id).then().statusCode(200).extract().header("ETag");
        assertThat(etag1).isEqualTo(expectedEtag(id, 1));
        // a client-supplied "version" field is IGNORED — the provider owns the version
        var r = auth().header("If-Match", etag1).body("{\"name\":\"b\",\"quantity\":3,\"version\":999}")
            .put("/api/optlock/resources/" + id).then().statusCode(200).extract();
        assertThat(r.header("ETag")).isEqualTo(expectedEtag(id, 2));     // provider version, not 999
        assertThat(r.jsonPath().getLong("version")).isEqualTo(2L);
    }

    @Test
    @Tag("OPTLOCK")
    @Tag("OPTLOCK-CONFLICT-001")
    void staleValidatorIs412AndConcurrentFlushRaceIs409() {
        // 412: a validator that was already stale at request entry
        String[] created = create();
        String id = created[0];
        String etag0 = created[1];
        auth().header("If-Match", etag0).body("{\"name\":\"first\",\"quantity\":2}")
            .put("/api/optlock/resources/" + id).then().statusCode(200);     // now at version 1
        auth().header("If-Match", etag0).body("{\"name\":\"stale\",\"quantity\":9}")
            .put("/api/optlock/resources/" + id).then().statusCode(412)      // etag0 is now stale
            .body("code", org.hamcrest.Matchers.equalTo("PRECONDITION_FAILED"));

        // 409: two writers both pass If-Match (same fresh version) then race the flush. The exact
        // loser status (409 vs 412) is timing-dependent, so retry the race until a genuine 409
        // surfaces — proving the flush-race path is reachable AND distinct from the 412 stale path.
        assertThat(raceUntil409())
            .as("the flush-race conflict is a 409, distinct from the 412 stale path").isEqualTo(409);
    }

    @Test
    @Tag("OPTLOCK")
    @Tag("OPTLOCK-RETRY-001")
    void conflictBodyCarriesCurrentEtagAndNoRetryAfter() {
        String[] created = create();
        String id = created[0];
        String etag0 = created[1];
        String etag1 = auth().header("If-Match", etag0).body("{\"name\":\"first\",\"quantity\":2}")
            .put("/api/optlock/resources/" + id).then().statusCode(200).extract().header("ETag");
        var r = auth().header("If-Match", etag0).body("{\"name\":\"stale\",\"quantity\":9}")
            .put("/api/optlock/resources/" + id).then().statusCode(412).extract();
        // RETRY-001: authoritative current_etag for read-modify-write; conditional retry, NO Retry-After
        assertThat(r.jsonPath().getString("current_etag")).isEqualTo(etag1);
        assertThat(r.header("Retry-After")).isNull();
        // the documented recovery actually works: re-GET the fresh representation, then re-PUT with
        // the new If-Match → 200 (read-modify-write succeeds against the current version)
        String freshEtag = auth().get("/api/optlock/resources/" + id).then().statusCode(200).extract().header("ETag");
        assertThat(freshEtag).isEqualTo(etag1);
        auth().header("If-Match", freshEtag).body("{\"name\":\"merged\",\"quantity\":9}")
            .put("/api/optlock/resources/" + id).then().statusCode(200);
    }

    @Test
    @Tag("OPTLOCK")
    @Tag("OPTLOCK-LOSTUPDATE-001")
    void concurrentStaleWritesYieldOneWinnerNoLostUpdate() {
        ConflictResult race = concurrentRace();
        assertThat(race.success).as("exactly one writer succeeds").isEqualTo(1);
        assertThat(race.conflict).as("exactly one writer is rejected").isEqualTo(1);
        assertThat(race.conflictStatus).as("the loser is a conflict — 412 stale OR 409 flush-race").isIn(409, 412);
        // final persisted state reflects the WINNER only — the loser's value did NOT clobber it
        var finalState = auth().get("/api/optlock/resources/" + race.id).then().statusCode(200).extract();
        assertThat(finalState.jsonPath().getLong("version")).isEqualTo(1L);
        assertThat(finalState.jsonPath().getString("name"))
            .isEqualTo(race.winnerName).isNotEqualTo(race.loserName);
    }

    @Test
    @Tag("OPTLOCK")
    @Tag("OPTLOCK-OBSERVABILITY-001")
    void exposesBoundedLabelMeters() {
        String[] created = create();
        String id = created[0];
        String etag0 = created[1];
        auth().header("If-Match", etag0).body("{\"name\":\"a\",\"quantity\":2}")
            .put("/api/optlock/resources/" + id).then().statusCode(200);   // applied
        auth().body("{\"name\":\"x\",\"quantity\":2}").put("/api/optlock/resources/" + id).then().statusCode(428); // precond_required
        auth().header("If-Match", etag0).body("{\"name\":\"y\",\"quantity\":2}")
            .put("/api/optlock/resources/" + id).then().statusCode(412);   // precondition_failed
        concurrentRace();                                                   // lock_conflict (409)

        assertThat(registry.find(OptlockMetrics.CONFLICTS).counter()).isNotNull();
        assertThat(registry.find(OptlockMetrics.PRECONDITION_REQUIRED).counter()).isNotNull();
        assertThat(registry.find(OptlockMetrics.WRITE_TIME).timer()).isNotNull();

        Set<String> outcomes = Set.of("precondition_failed", "lock_conflict");
        Set<String> results = Set.of("applied", "conflict");
        for (Meter m : registry.find(OptlockMetrics.CONFLICTS).meters()) {
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                assertThat(Set.of(OptlockMetrics.TAG_RESOURCE, OptlockMetrics.TAG_OUTCOME)).contains(t.getKey());
                if (t.getKey().equals(OptlockMetrics.TAG_OUTCOME)) {
                    assertThat(outcomes).contains(t.getValue());
                } else {
                    assertThat(t.getValue()).isEqualTo(OptlockMetrics.RESOURCE);
                }
            }
        }
        for (Meter m : registry.find(OptlockMetrics.WRITE_TIME).meters()) {
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                assertThat(Set.of(OptlockMetrics.TAG_RESOURCE, OptlockMetrics.TAG_RESULT)).contains(t.getKey());
                if (t.getKey().equals(OptlockMetrics.TAG_RESULT)) {
                    assertThat(results).contains(t.getValue());
                }
            }
        }
    }

    @Test
    @Tag("OPTLOCK")
    @Tag("OPTLOCK-NULLPRIMITIVE-001")
    void nullForPrimitiveQuantityIsRejected400NotCoercedToZero() {
        // Contract pin (Jackson 2→3 migration): an explicit JSON null for the primitive `int quantity`
        // now fails binding (Jackson 3 fail-on-null-for-primitives is ON by default) → 400. Jackson 2
        // silently coerced null→0. The 400 is the DESIRED safer behavior: a null quantity is a client
        // error, not a request for quantity 0. This ADDS a pin; it changes no existing assertion.
        var resp = auth().body("{\"name\":\"nullqty\",\"quantity\":null}")
            .post("/api/optlock/resources").then().extract();
        assertThat(resp.statusCode())
            .as("explicit null for primitive int quantity must be rejected 400, not coerced to 0")
            .isEqualTo(400);
    }

    // ── concurrency helper ───────────────────────────────────────────────────

    private record ConflictResult(String id, int success, int conflict, int conflictStatus,
                                  String winnerName, String loserName) {}

    /**
     * Repeat the race until a genuine 409 (flush-race) loser is observed, proving that outcome is
     * reachable and distinct from the 412 stale-at-entry path. The in-tx write window makes 409 the
     * usual outcome; the bounded loop removes any residual scheduling flakiness without a hard
     * timing assumption. Returns the conflict status of the first 409 (or the last status seen).
     */
    private int raceUntil409() {
        int last = 0;
        for (int attempt = 0; attempt < 8; attempt++) {
            ConflictResult r = concurrentRace();
            last = r.conflictStatus();
            if (last == 409) {
                return 409;
            }
        }
        return last;
    }

    /**
     * Two writers GET version 0, both PUT with the SAME If-Match "id-0". The service's in-transaction
     * write window makes both pass the precondition (both load v0), then the flush race lets exactly
     * one win (v1) and the other lose with 409 (its stale @Version UPDATE bumps zero rows).
     */
    private ConflictResult concurrentRace() {
        String[] created = create();
        String id = created[0];
        String etag0 = created[1];
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger conflictStatus = new AtomicInteger();
        AtomicReference<String> winnerName = new AtomicReference<>();
        AtomicReference<String> loserName = new AtomicReference<>();
        for (int i = 0; i < 2; i++) {
            String name = "writer-" + i;
            pool.submit(() -> {
                try {
                    go.await();
                    ExtractableResponse<Response> resp = auth().header("If-Match", etag0)
                        .body("{\"name\":\"" + name + "\",\"quantity\":5}")
                        .put("/api/optlock/resources/" + id).thenReturn().then().extract();
                    if (resp.statusCode() == 200) {
                        success.incrementAndGet();
                        winnerName.set(name);
                    } else {
                        conflict.incrementAndGet();
                        conflictStatus.set(resp.statusCode());
                        loserName.set(name);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        go.countDown();
        pool.shutdown();
        try {
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new ConflictResult(id, success.get(), conflict.get(), conflictStatus.get(),
                winnerName.get(), loserName.get());
    }
}
