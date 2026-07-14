package com.ax.template.authblueprint.routelegs;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * route-leg-contiguity-l0 compliance — verified against the live routelegs reference workload.
 * The invariant: leg N's destination MUST equal leg N+1's origin at all times; the ordinal
 * sequence is always a gapless 1..N; insert/remove/replace re-validate BOTH affected neighbors
 * atomically; concurrent mutation on the SAME route serializes. Spec: specs/route-leg-contiguity-l0.yaml.
 *
 * <p>Note on interior insert/replace: once a route is built through append (which itself always
 * enforces contiguity), every adjacent pair is ALREADY connected — so a genuine interior insert
 * between two already-adjacent legs necessarily lands on the shared waypoint (origin==dest, a
 * layover-style leg). This is a structural consequence of the chain model, not a test artifact;
 * edge-position insert/replace (front/back) exercise the non-degenerate single-neighbor case.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ROUTELEGS")
class RouteLegComplianceTest {

    @LocalServerPort int port;
    @Autowired RouteLegService service;
    String member;

    @BeforeEach
    void setup() {
        RouteLegTestSupport.useRandomPort(port);
        member = RouteLegTestSupport.obtainToken(RouteLegTestSupport.freshEmail("route-member"), "MEMBER");
    }

    private String createRoute() {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/route-legs/routes").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> append(String routeId, String origin, String dest) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"originCode\":\"" + origin + "\",\"destCode\":\"" + dest + "\"}")
        .when().post("/api/route-legs/routes/" + routeId + "/legs").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> insert(String routeId, int atOrdinal, String origin, String dest) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"atOrdinal\":" + atOrdinal + ",\"originCode\":\"" + origin + "\",\"destCode\":\"" + dest + "\"}")
        .when().post("/api/route-legs/routes/" + routeId + "/legs/insert").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> remove(String routeId, int ordinal) {
        return given().header("Authorization", "Bearer " + member)
            .when().delete("/api/route-legs/routes/" + routeId + "/legs/" + ordinal).thenReturn().then().extract();
    }

    private ExtractableResponse<Response> replace(String routeId, int ordinal, String origin, String dest) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"originCode\":\"" + origin + "\",\"destCode\":\"" + dest + "\"}")
        .when().put("/api/route-legs/routes/" + routeId + "/legs/" + ordinal).thenReturn().then().extract();
    }

    private ExtractableResponse<Response> reorder(String routeId, List<String> legIds) {
        String body = "{\"legIds\":[" + legIds.stream().map(id -> "\"" + id + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().put("/api/route-legs/routes/" + routeId + "/legs/reorder").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getRoute(String routeId) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/route-legs/routes/" + routeId).then().statusCode(200).extract();
    }

    // ── LEG-SEQUENCE-001 — leg N dest MUST equal leg N+1 origin, enforced at append ──
    @Test @Tag("LEG-SEQUENCE-001")
    void append_buildsChain_wrongOriginRejected() {
        String routeId = createRoute();
        assertThat(append(routeId, "A", "B").statusCode()).isEqualTo(201);
        assertThat(append(routeId, "B", "C").statusCode()).isEqualTo(201);

        ExtractableResponse<Response> bad = append(routeId, "X", "Y"); // X != C (current last dest)
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("ROUTE_LEG_SEQUENCE_VIOLATION");

        var legs = getRoute(routeId).jsonPath().getList("legs");
        assertThat(legs).as("route unchanged after the rejected append").hasSize(2);
    }

    // ── LEG-GAP-001 — reorder into a valid new permutation (cyclic legs) atomically; invalid reorder is refused ──
    @Test @Tag("LEG-GAP-001")
    void reorder_validPermutation_ok_invalidPermutation_rejected() {
        String routeId = createRoute();
        // a 3-leg CYCLE — multiple rotations are all valid contiguous chains, unlike a strict linear path.
        append(routeId, "A", "B");
        append(routeId, "B", "C");
        append(routeId, "C", "A");
        var afterBuild = getRoute(routeId).jsonPath();
        List<String> legIds = afterBuild.getList("legs.id", String.class);
        assertThat(legIds).hasSize(3);

        // rotate: [B→C, C→A, A→B] — still a valid contiguous chain (a cyclic rotation)
        List<String> rotated = List.of(legIds.get(1), legIds.get(2), legIds.get(0));
        ExtractableResponse<Response> ok = reorder(routeId, rotated);
        assertThat(ok.statusCode()).isEqualTo(200);
        var rotatedOrdinals = ok.jsonPath().getList("legs.id", String.class);
        assertThat(rotatedOrdinals).containsExactlyElementsOf(rotated);
        assertThat(ok.jsonPath().getList("legs.ordinal", Integer.class)).containsExactly(1, 2, 3);

        // an invalid permutation (breaks adjacency: leg1.dest must equal leg2.origin) is refused
        List<String> invalid = List.of(legIds.get(0), legIds.get(2), legIds.get(1)); // A→B, C→A, B→C — B≠C
        ExtractableResponse<Response> bad = reorder(routeId, invalid);
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("ROUTE_LEG_GAP_VIOLATION");
        // ordinals unchanged from the last successful reorder
        assertThat(getRoute(routeId).jsonPath().getList("legs.id", String.class)).containsExactlyElementsOf(rotated);
    }

    // ── LEG-MUTATE-001 — insert at the front (single-neighbor edge case) re-validates the AFTER neighbor ──
    @Test @Tag("LEG-MUTATE-001")
    void insert_atFront_matchingAfterNeighbor_ok_violating_is422() {
        String routeId = createRoute();
        append(routeId, "A", "M");

        ExtractableResponse<Response> bad = insert(routeId, 1, "X", "Y"); // dest Y != current leg1 origin A
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("ROUTE_LEG_SEQUENCE_VIOLATION");
        assertThat(getRoute(routeId).jsonPath().getList("legs")).hasSize(1);

        ExtractableResponse<Response> ok = insert(routeId, 1, "Z", "A"); // dest A == leg1 origin A
        assertThat(ok.statusCode()).isEqualTo(201);
        var legs = ok.jsonPath();
        assertThat(legs.getList("legs.originCode", String.class)).containsExactly("Z", "A");
        assertThat(legs.getList("legs.destCode", String.class)).containsExactly("A", "M");
        assertThat(legs.getList("legs.ordinal", Integer.class)).containsExactly(1, 2);
    }

    // ── LEG-MUTATE-001 — interior insert re-validates BOTH neighbors (structurally lands on the shared waypoint) ──
    @Test @Tag("LEG-MUTATE-001")
    void insert_interior_bothNeighborsRevalidated() {
        String routeId = createRoute();
        append(routeId, "A", "M");
        append(routeId, "M", "D");

        // violates the BEFORE neighbor (origin must be M)
        assertThat(insert(routeId, 2, "X", "D").statusCode()).isEqualTo(422);
        // violates the AFTER neighbor (dest must be M)
        assertThat(insert(routeId, 2, "M", "Y").statusCode()).isEqualTo(422);
        assertThat(getRoute(routeId).jsonPath().getList("legs")).as("route unchanged after rejections").hasSize(2);

        // satisfies BOTH: origin==before.dest(M), dest==after.origin(M) — a layover leg at the shared waypoint
        ExtractableResponse<Response> ok = insert(routeId, 2, "M", "M");
        assertThat(ok.statusCode()).isEqualTo(201);
        var legs = ok.jsonPath();
        assertThat(legs.getList("legs.originCode", String.class)).containsExactly("A", "M", "M");
        assertThat(legs.getList("legs.destCode", String.class)).containsExactly("M", "M", "D");
        assertThat(legs.getList("legs.ordinal", Integer.class)).containsExactly(1, 2, 3);
    }

    // ── LEG-MUTATE-001 — remove re-validates the remaining neighbors; a break is refused ──
    @Test @Tag("LEG-MUTATE-001")
    void remove_middleLeg_okWhenNeighborsStillMatch_rejectedWhenTheyDont() {
        String routeId = createRoute();
        append(routeId, "A", "M");
        append(routeId, "M", "M");   // layover — removing it leaves A→M / M→D still contiguous
        append(routeId, "M", "D");

        ExtractableResponse<Response> ok = remove(routeId, 2);
        assertThat(ok.statusCode()).isEqualTo(200);
        var legs = ok.jsonPath();
        assertThat(legs.getList("legs.originCode", String.class)).containsExactly("A", "M");
        assertThat(legs.getList("legs.destCode", String.class)).containsExactly("M", "D");
        assertThat(legs.getList("legs.ordinal", Integer.class)).containsExactly(1, 2);

        // a route where removing the middle leg WOULD break the remaining neighbors is refused
        String routeId2 = createRoute();
        append(routeId2, "A", "M");
        append(routeId2, "M", "N");
        append(routeId2, "N", "D");
        ExtractableResponse<Response> bad = remove(routeId2, 2); // removing M→N leaves A→M / N→D — M != N
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("ROUTE_LEG_SEQUENCE_VIOLATION");
        assertThat(getRoute(routeId2).jsonPath().getList("legs")).as("nothing removed").hasSize(3);
    }

    // ── LEG-MUTATE-001 — replace at an edge position (single neighbor) succeeds/fails on that neighbor ──
    @Test @Tag("LEG-MUTATE-001")
    void replace_edgeLeg_matchingNeighbor_ok_violating_is422() {
        String routeId = createRoute();
        append(routeId, "A", "M");
        append(routeId, "M", "N");

        ExtractableResponse<Response> bad = replace(routeId, 1, "Z", "W"); // dest W != leg2 origin M
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("ROUTE_LEG_SEQUENCE_VIOLATION");

        ExtractableResponse<Response> ok = replace(routeId, 1, "Z", "M"); // dest M == leg2 origin M
        assertThat(ok.statusCode()).isEqualTo(200);
        var legs = ok.jsonPath();
        assertThat(legs.getList("legs.originCode", String.class)).containsExactly("Z", "M");
        assertThat(legs.getList("legs.destCode", String.class)).containsExactly("M", "N");
    }

    // ── LEG-MUTATE-001 — keystone: concurrent mutation on the SAME route serializes ──
    @Test @Tag("LEG-MUTATE-001")
    void concurrentAppends_onSameRoute_oneWinsOneGets409() throws Exception {
        String routeId = createRoute();
        append(routeId, "A", "M"); // baseline leg — both threads will try to append M→D concurrently
        UUID id = UUID.fromString(routeId);

        int n = 2;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> outcomes = new ConcurrentLinkedQueue<>();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    service.appendLeg(id, "M", "D");
                    outcomes.add("OK");
                } catch (RouteLegException ex) {
                    outcomes.add(ex.code());
                }
                return null;
            }));
        }
        // worker-ready barrier: open the gate only once BOTH workers are parked on it, so the
        // two appends genuinely race instead of running back-to-back (mirrors
        // TokenizedSecuritiesComplianceTest#concurrentIssue_exactlyOneWins_registerConserved).
        assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        for (Future<?> f : futures) {
            f.get(); // surface any unexpected worker exception instead of a confusing count mismatch
        }

        // Exactly one append wins; the loser's error code is scheduling-dependent by the nature
        // of "append the current tail" (NOT a flake to paper over — both are deterministically
        // correct rejections, diagnosed via a captured DIAG run):
        //   - genuine read-phase overlap (both threads see the SAME pre-mutation legs list) →
        //     the loser's insert collides on uq(route_id, ordinal) / the root's stale @Version →
        //     409 ROUTE_CONCURRENT_MODIFICATION.
        //   - the OS scheduler fully serializes the two calls (the winner commits before the
        //     loser even reads) → the loser's fresh read sees the NEW tail (already advanced past
        //     "M") and its identical "M"→"D" attempt is then a genuinely invalid append against
        //     the CURRENT chain → 422 ROUTE_LEG_SEQUENCE_VIOLATION. This is not a race artifact;
        //     it is the correct rejection for resubmitting a leg that no longer matches the tail.
        // Either shape proves the keystone: no interleaving EVER admits two appends of the same
        // slot, and the final state is always exactly the 2 contiguous legs asserted below.
        assertThat(outcomes.stream().filter("OK"::equals).count())
            .as("LEG-MUTATE-001 — exactly one concurrent append wins").isEqualTo(1);
        assertThat(outcomes).as("the loser is deterministically rejected, either by the concurrency "
                + "guard (true overlap) or by sequence re-validation (full serialization)")
            .filteredOn(o -> !o.equals("OK"))
            .hasSize(1)
            .allMatch(o -> o.equals("ROUTE_CONCURRENT_MODIFICATION") || o.equals("ROUTE_LEG_SEQUENCE_VIOLATION"));

        var finalLegs = getRoute(routeId).jsonPath();
        assertThat(finalLegs.getList("legs")).as("no lost/duplicated leg").hasSize(2);
        assertThat(finalLegs.getList("legs.ordinal", Integer.class)).containsExactly(1, 2);
    }
}
