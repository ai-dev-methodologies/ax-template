package com.ax.template.authblueprint.rangeownership;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * range-ownership-l0 compliance — verified against the live rangeownership reference workload.
 * The invariant: an identifier assignment is valid only inside a range block owned by the
 * assigning owner (fail-closed); range blocks never overlap across owners (half-open, adjacency
 * legal, serialized via a registry-row lock); porting appends an immutable reassignment record
 * whose destination must be a recognized plan participant, current owner always derive-on-read.
 * Spec: specs/range-ownership-l0.yaml (ITU-T E.164 numbering-plan administration + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("RANGEOWNERSHIP")
class RangeOwnershipComplianceTest {

    @LocalServerPort int port;
    @Autowired RangeOwnershipService service;
    String member;

    @BeforeEach
    void setup() {
        RangeOwnershipTestSupport.useRandomPort(port);
        member = RangeOwnershipTestSupport.obtainToken(RangeOwnershipTestSupport.freshEmail("rng-member"), "MEMBER");
    }

    /** A fresh, unused numeric base per test-scenario so concurrent tests never collide. */
    private static long freshBase() {
        return ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L);
    }

    private static String freshOwner(String label) {
        return label + "-" + UUID.randomUUID();
    }

    private ExtractableResponse<Response> registerBlock(String ownerRef, long start, long end) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"ownerRef\":\"" + ownerRef + "\",\"rangeStart\":" + start + ",\"rangeEnd\":" + end + "}")
        .when().post("/api/range-ownership/blocks").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> assign(long identifierValue, String ownerRef) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"identifierValue\":" + identifierValue + ",\"ownerRef\":\"" + ownerRef + "\"}")
        .when().post("/api/range-ownership/assignments").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> port(long identifierValue, String toOwner, String reason) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"toOwner\":\"" + toOwner + "\",\"reason\":\"" + reason + "\"}")
        .when().post("/api/range-ownership/assignments/" + identifierValue + "/port").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> history(long identifierValue) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/range-ownership/assignments/" + identifierValue + "/history").then().statusCode(200).extract();
    }

    // ── RNG-CONTAINMENT-001 — an assignment is valid only inside a block the assigning owner owns ──
    @Test @Tag("RNG-CONTAINMENT-001")
    void containment_insideOwnBlockAccepted_outsideOrWrongOwnerRejected_alreadyAssignedConflicts() {
        long base = freshBase();
        String ownerA = freshOwner("carrierA");
        String ownerB = freshOwner("carrierB");
        assertThat(registerBlock(ownerA, base, base + 1000).statusCode()).isEqualTo(201);
        assertThat(registerBlock(ownerB, base + 2000, base + 3000).statusCode()).isEqualTo(201);

        // inside ownerA's own block → accepted
        assertThat(assign(base + 500, ownerA).statusCode()).isEqualTo(201);

        // inside ownerB's block, but assigned AS ownerA → fail-closed 422 (not ownerA's block)
        ExtractableResponse<Response> wrongOwner = assign(base + 2500, ownerA);
        assertThat(wrongOwner.statusCode()).isEqualTo(422);
        assertThat(wrongOwner.jsonPath().getString("code")).isEqualTo("RANGE_NOT_OWNED");

        // inside no block at all → 422
        ExtractableResponse<Response> unallocated = assign(base + 1500, ownerA);
        assertThat(unallocated.statusCode()).isEqualTo(422);
        assertThat(unallocated.jsonPath().getString("code")).isEqualTo("RANGE_NOT_OWNED");

        // re-assigning an already-assigned identifier → 409
        ExtractableResponse<Response> dup = assign(base + 500, ownerA);
        assertThat(dup.statusCode()).isEqualTo(409);
        assertThat(dup.jsonPath().getString("code")).isEqualTo("IDENTIFIER_ALREADY_ASSIGNED");
    }

    // ── RNG-NONOVERLAP-002 — keystone: overlap rejected across owners; adjacency legal; concurrent-safe ──
    @Test @Tag("RNG-NONOVERLAP-002")
    void nonOverlap_overlapRejected_adjacencyLegal_concurrentRegistrationSerializes() throws Exception {
        long base = freshBase();
        String ownerX = freshOwner("carrierX");
        String ownerY = freshOwner("carrierY");
        assertThat(registerBlock(ownerX, base, base + 1000).statusCode()).isEqualTo(201);

        // an overlapping block from a DIFFERENT owner → 409
        ExtractableResponse<Response> overlap = registerBlock(ownerY, base + 500, base + 1500);
        assertThat(overlap.statusCode()).isEqualTo(409);
        assertThat(overlap.jsonPath().getString("code")).isEqualTo("RANGE_BLOCK_OVERLAP");

        // a block that exactly TOUCHES (adjacent, half-open) → legal, no overlap
        assertThat(registerBlock(ownerY, base + 1000, base + 2000).statusCode()).isEqualTo(201);

        // KEYSTONE — 2-thread concurrent registration of OVERLAPPING ranges: exactly one wins,
        // serialized on the RangeRegistryLock row (CWE-362 backstop, no bare pre-insert SELECT).
        long raceBase = freshBase();
        int n = 2;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            String owner = freshOwner("racer" + i);
            pool.submit(() -> {
                start.await();
                try {
                    service.registerBlock(owner, raceBase, raceBase + 1000);   // both submit the SAME overlapping range
                    codes.add(201);
                } catch (RangeOwnershipException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 201).count())
            .as("exactly one concurrent registration of an overlapping range must win").isEqualTo(1L);
        assertThat(codes.stream().filter(c -> c == 409).count())
            .as("the loser must see a deterministic 409, never both persisted").isEqualTo(1L);
    }

    // ── RNG-PORT-003 — append-only reassignment; destination must be a recognized plan participant; derive-on-read ──
    @Test @Tag("RNG-PORT-003")
    void port_appendsImmutableRecord_destinationMustBeRecognized_currentOwnerDeriveOnRead() {
        long base = freshBase();
        String ownerA = freshOwner("originatorA");
        String ownerB = freshOwner("recipientB");
        String unregistered = freshOwner("unregisteredC");
        assertThat(registerBlock(ownerA, base, base + 1000).statusCode()).isEqualTo(201);
        assertThat(registerBlock(ownerB, base + 5000, base + 6000).statusCode()).isEqualTo(201);   // ownerB's OWN, disjoint block

        long identifier = base + 500;
        assertThat(assign(identifier, ownerA).statusCode()).isEqualTo(201);

        // port to ownerB — ownerB owns SOME block (a recognized plan participant), even though it
        // does NOT cover this exact identifier (impossible under global non-overlap — ownerA's
        // block permanently covers this point).
        ExtractableResponse<Response> portResp = port(identifier, ownerB, "operator-transfer");
        assertThat(portResp.statusCode()).isEqualTo(201);
        assertThat(portResp.jsonPath().getString("fromOwner")).isEqualTo(ownerA);
        assertThat(portResp.jsonPath().getString("toOwner")).isEqualTo(ownerB);

        // current owner is derive-on-read from the latest event
        ExtractableResponse<Response> currentAfterPort = given().header("Authorization", "Bearer " + member)
            .when().get("/api/range-ownership/assignments/" + identifier).then().statusCode(200).extract();
        assertThat(currentAfterPort.jsonPath().getString("currentOwner")).isEqualTo(ownerB);

        // history is append-only and immutable — both records still readable
        var history = history(identifier).jsonPath().getList("toOwner");
        assertThat(history).containsExactly(ownerA, ownerB);

        // port to an owner with NO registered block anywhere → 422, history unchanged
        ExtractableResponse<Response> rejected = port(identifier, unregistered, "invalid-transfer");
        assertThat(rejected.statusCode()).isEqualTo(422);
        assertThat(rejected.jsonPath().getString("code")).isEqualTo("RANGE_NOT_OWNED");
        assertThat(history(identifier).jsonPath().getList("toOwner")).containsExactly(ownerA, ownerB);
    }
}
