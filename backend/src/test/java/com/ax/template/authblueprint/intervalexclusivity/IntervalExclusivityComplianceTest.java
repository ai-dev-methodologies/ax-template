package com.ax.template.authblueprint.intervalexclusivity;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * interval-exclusivity-l0 compliance — verified against the live intervalexclusivity reference
 * workload. The invariant: half-open overlap rejection with back-to-back legality in both
 * directions; concurrent overlapping bookings serialize to exactly one winner (H2-honest row-lock
 * mechanism, not a GiST exclusion constraint); shrink is unconditional, extend re-validates
 * atomically, cancel frees the window immediately. Spec: specs/interval-exclusivity-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("INTERVAL_EXCLUSIVITY")
class IntervalExclusivityComplianceTest {

    private static final Instant BASE = Instant.parse("2030-01-01T00:00:00Z");
    private static Instant t(int minutesFromBase) { return BASE.plusSeconds(minutesFromBase * 60L); }

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = IntervalExclusivityTestSupport.obtainToken(
            IntervalExclusivityTestSupport.freshEmail("ivx-member"), "MEMBER");
    }

    private String registerResource() {
        String key = "res-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"resourceKey\":\"" + key + "\"}")
        .when().post("/api/interval-exclusivity/resources").then().statusCode(201);
        return key;
    }

    private ExtractableResponse<Response> book(String resourceKey, int startMin, int endMin) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"resourceKey\":\"" + resourceKey + "\",\"startAt\":\"" + t(startMin) + "\",\"endAt\":\"" + t(endMin) + "\"}")
        .when().post("/api/interval-exclusivity/bookings").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> resize(String bookingId, int startMin, int endMin) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"startAt\":\"" + t(startMin) + "\",\"endAt\":\"" + t(endMin) + "\"}")
        .when().put("/api/interval-exclusivity/bookings/" + bookingId).thenReturn().then().extract();
    }

    private ExtractableResponse<Response> cancel(String bookingId) {
        return given().header("Authorization", "Bearer " + member)
        .when().post("/api/interval-exclusivity/bookings/" + bookingId + "/cancel").thenReturn().then().extract();
    }

    // ── IVX-OVERLAP-001 — half-open overlap rejection; back-to-back legal both directions; exact boundaries ──
    @Test @Tag("IVX-OVERLAP-001")
    void overlap_rejected_backToBack_legalBothDirections_exactBoundaries() {
        String resource = registerResource();
        assertThat(book(resource, 600, 660).statusCode()).isEqualTo(201);     // [10:00,11:00)

        assertThat(book(resource, 630, 690).statusCode())
            .as("IVX-OVERLAP-001 — a partially-overlapping window is rejected")
            .isEqualTo(409);
        assertThat(book(resource, 615, 645).statusCode())
            .as("a fully-contained sub-interval is still an overlap")
            .isEqualTo(409);

        assertThat(book(resource, 660, 720).statusCode())
            .as("half-open semantics — [11:00,12:00) immediately AFTER [10:00,11:00) is legal")
            .isEqualTo(201);
        assertThat(book(resource, 540, 600).statusCode())
            .as("half-open semantics — [09:00,10:00) immediately BEFORE [10:00,11:00) is legal")
            .isEqualTo(201);
    }

    @Test @Tag("IVX-OVERLAP-001")
    void bookingUnknownResource_is404() {
        assertThat(given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"resourceKey\":\"nope\",\"startAt\":\"" + t(0) + "\",\"endAt\":\"" + t(60) + "\"}")
            .when().post("/api/interval-exclusivity/bookings").thenReturn().statusCode())
            .isEqualTo(404);
    }

    // ── IVX-CONCURRENT-002 — 2-thread simultaneous overlapping booking → exactly one wins ──
    @Test @Tag("IVX-CONCURRENT-002")
    void concurrentOverlappingBookings_exactlyOneWins() throws Exception {
        String resource = registerResource();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> results = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                start.await();
                results.add(book(resource, 600, 660).statusCode());
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        long won = results.stream().filter(code -> code == 201).count();
        long lost = results.stream().filter(code -> code == 409).count();
        assertThat(won).as("IVX-CONCURRENT-002 — exactly one concurrent overlapping booking wins").isEqualTo(1);
        assertThat(lost).isEqualTo(1);
    }

    // ── IVX-MUTATE-003 — shrink unconditional; extend re-validates atomically; cancel frees the window ──
    @Test @Tag("IVX-MUTATE-003")
    void shrinkUnconditional_extendRevalidates_cancelFreesWindowImmediately() {
        String resource = registerResource();
        ExtractableResponse<Response> a = book(resource, 600, 720);           // A = [10:00,12:00)
        assertThat(a.statusCode()).isEqualTo(201);
        String aId = a.jsonPath().getString("id");
        ExtractableResponse<Response> b = book(resource, 720, 780);           // B = [12:00,13:00)
        String bId = b.jsonPath().getString("id");
        assertThat(b.statusCode()).isEqualTo(201);

        // shrink A to [11:00,11:30) — always allowed
        ExtractableResponse<Response> shrunk = resize(aId, 660, 690);
        assertThat(shrunk.statusCode()).as("IVX-MUTATE-003 — shrinking is unconditional").isEqualTo(200);

        // extend A to [11:00,12:30) — overlaps B, rejected
        assertThat(resize(aId, 660, 750).statusCode())
            .as("IVX-MUTATE-003 — extending re-validates overlap")
            .isEqualTo(409);

        // extend A to [10:30,12:00) — fits the gap before B, allowed
        assertThat(resize(aId, 630, 720).statusCode()).isEqualTo(200);

        // cancel B, then extend A into B's former window — succeeds immediately
        assertThat(cancel(bId).statusCode()).isEqualTo(200);
        ExtractableResponse<Response> extended = resize(aId, 630, 780);
        assertThat(extended.statusCode())
            .as("IVX-MUTATE-003 — cancel frees the window immediately for a new/extended booking")
            .isEqualTo(200);

        // cancelling again is a deterministic conflict — zero outgoing edges from CANCELLED
        assertThat(cancel(bId).statusCode()).isEqualTo(409);
    }

    @Test @Tag("IVX-MUTATE-003")
    void resizeOrCancelUnknownBooking_is404() {
        assertThat(resize(UUID.randomUUID().toString(), 0, 60).statusCode()).isEqualTo(404);
        assertThat(cancel(UUID.randomUUID().toString()).statusCode()).isEqualTo(404);
    }
}
