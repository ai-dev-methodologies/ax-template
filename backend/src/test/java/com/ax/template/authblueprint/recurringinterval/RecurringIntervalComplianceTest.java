package com.ax.template.authblueprint.recurringinterval;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * completion-reset-recurring-interval-l0 compliance — verified against the live recurringinterval
 * reference workload. The invariant: completing advances the window FROM the completion instant
 * (reset, not a fixed grid); each window carries at most one append-only occurrence (exactly-once);
 * due/overdue is recomputed from the clock (never a stored boolean); the @Lazy-self sweep records
 * only a non-authoritative overdue flag and never auto-completes; concurrent completes serialize so
 * exactly one advances the window. Spec: specs/completion-reset-recurring-interval-l0.yaml
 * (14 CFR §91.409(b) completion-reset interval + CWE-362). Relative-time tests anchor windowStart
 * in the past via the create contract's anchorAt against the system UTC clock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("RECURRINGINTERVAL")
class RecurringIntervalComplianceTest {

    @LocalServerPort int port;
    @Autowired RecurringIntervalSweeper sweeper;
    @Autowired RecurringObligationService service;
    String member;
    String admin;

    @BeforeEach
    void setup() {
        RecurringIntervalTestSupport.useRandomPort(port);
        member = RecurringIntervalTestSupport.obtainToken(RecurringIntervalTestSupport.freshEmail("cri-member"), "MEMBER");
        admin = RecurringIntervalTestSupport.obtainToken(RecurringIntervalTestSupport.freshEmail("cri-admin"), "ADMIN");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String create(String key, long intervalSeconds, String anchorAt) {
        String body = anchorAt == null
            ? "{\"obligationKey\":\"" + key + "\",\"intervalSeconds\":" + intervalSeconds + "}"
            : "{\"obligationKey\":\"" + key + "\",\"intervalSeconds\":" + intervalSeconds
                + ",\"anchorAt\":\"" + anchorAt + "\"}";
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/recurring-interval").then().statusCode(201);
        return key;
    }

    private ExtractableResponse<Response> get(String key) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/recurring-interval/" + key).then().statusCode(200).extract();
    }

    private ExtractableResponse<Response> complete(String key) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/recurring-interval/" + key + "/complete").then().statusCode(200).extract();
    }

    private int occurrenceCount(String key) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/recurring-interval/" + key + "/occurrences")
            .then().statusCode(200).extract().jsonPath().getList("data").size();
    }

    // ── CRI-RESET-001 — completing advances the window FROM the completion instant (reset, not grid) ──
    @Test @Tag("CRI-RESET-001")
    void complete_advancesWindowFromCompletionInstant_earlyCompletionSlidesScheduleForward() {
        // anchor the window 1 day ago, interval 7 days → fixed grid would put the 2nd due at anchor+14d
        String anchor = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = create("cri-" + UUID.randomUUID(), 7L * 86400L, anchor);

        Instant firstDue = Instant.parse(get(k).jsonPath().getString("nextDueAt"));
        assertThat(firstDue).isEqualTo(Instant.parse(anchor).plus(7, ChronoUnit.DAYS));

        // complete EARLY (1 day into a 7-day window) — the window must slide FORWARD to ~now
        ExtractableResponse<Response> done = complete(k);
        Instant completedAt = Instant.parse(done.jsonPath().getString("lastCompletedAt"));
        Instant newWindowStart = Instant.parse(done.jsonPath().getString("windowStart"));
        assertThat(newWindowStart)
            .as("CRI-RESET-001 — windowStart slides forward to the completion instant, not a fixed grid")
            .isEqualTo(completedAt);

        // the next due is completedAt + interval — STRICTLY earlier than a fixed grid's anchor+2*interval
        Instant nextDue = Instant.parse(get(k).jsonPath().getString("nextDueAt"));
        assertThat(nextDue).isEqualTo(completedAt.plus(7, ChronoUnit.DAYS));
        Instant fixedGridSecondDue = Instant.parse(anchor).plus(14, ChronoUnit.DAYS);
        assertThat(nextDue)
            .as("early completion moved the next due EARLIER than a fixed cadence would")
            .isBefore(fixedGridSecondDue);

        // an Occurrence recorded the CLOSED window (the original anchor) + completedBy + completedAt
        ExtractableResponse<Response> occ = given().header("Authorization", "Bearer " + member)
            .when().get("/api/recurring-interval/" + k + "/occurrences").then().statusCode(200).extract();
        assertThat(occ.jsonPath().getString("data[0].closedWindowStart")).isEqualTo(anchor);
        assertThat(occ.jsonPath().getString("data[0].completedBy")).isNotBlank();
    }

    // ── CRI-ONCE-001 — at most one completion per window occupancy; a re-complete before the new
    //     window elapses is a deterministic 409, append-only history never duplicates ──
    @Test @Tag("CRI-ONCE-001")
    void completionIsExactlyOncePerWindow_reCompleteWithinTheWindowIs409() {
        String anchor = Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = create("cri-" + UUID.randomUUID(), 3L * 86400L, anchor);

        Instant closedFirst = Instant.parse(complete(k).jsonPath().getString("windowStart"));   // == completion1
        assertThat(occurrenceCount(k)).isEqualTo(1);
        assertThat(closedFirst)
            .as("completing advanced the window forward to the completion instant").isAfter(Instant.parse(anchor));

        // the just-opened window has NOT elapsed (3-day interval) → a re-complete is a duplicate → 409
        int second = given().header("Authorization", "Bearer " + member)
            .when().post("/api/recurring-interval/" + k + "/complete").thenReturn().statusCode();
        assertThat(second)
            .as("CRI-ONCE-001 — a second completion before the new window is due again is 409")
            .isEqualTo(409);
        given().header("Authorization", "Bearer " + member)
            .when().post("/api/recurring-interval/" + k + "/complete")
            .then().statusCode(409)
            .body("code", org.hamcrest.Matchers.equalTo("RECURRING_INTERVAL_WINDOW_ALREADY_COMPLETED"));

        // the append-only history never duplicated — still exactly one occurrence
        assertThat(occurrenceCount(k)).isEqualTo(1);
        java.util.List<String> closed = given().header("Authorization", "Bearer " + member)
            .when().get("/api/recurring-interval/" + k + "/occurrences")
            .then().statusCode(200).extract().jsonPath().getList("data.closedWindowStart", String.class);
        assertThat(closed).containsExactly(anchor);     // the very first window was the anchor
    }

    // ── CRI-DUE-001 — due/overdue is RECOMPUTED from the clock + windowStart, never a stored boolean ──
    @Test @Tag("CRI-DUE-001")
    void overdueIsRecomputedFromTheClock_notAStoredBoolean() {
        // a window anchored well in the past with a short interval reads overdue PURELY from the clock —
        // no write ever set an 'overdue' flag (swept_overdue is still its default false)
        String pastAnchor = Instant.now().minus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String overdueKey = create("cri-" + UUID.randomUUID(), 86400L, pastAnchor);   // 1-day window, 40d old
        ExtractableResponse<Response> overdue = get(overdueKey);
        assertThat(overdue.jsonPath().getBoolean("overdue"))
            .as("CRI-DUE-001 — overdue is recomputed true purely because the clock passed windowStart+interval")
            .isTrue();
        assertThat(overdue.jsonPath().getBoolean("sweptOverdue"))
            .as("no sweep ran — the authoritative overdue came from recomputation, NOT the stored flag")
            .isFalse();

        // a window anchored now with a long interval reads not-overdue — same recomputation, no write
        String freshAnchor = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        String freshKey = create("cri-" + UUID.randomUUID(), 365L * 86400L, freshAnchor);
        assertThat(get(freshKey).jsonPath().getBoolean("overdue")).isFalse();
    }

    // ── CRI-SWEEP-001 — the sweep records a non-authoritative flag and NEVER auto-completes ──
    @Test @Tag("CRI-SWEEP-001")
    void sweepRecordsOverdueFlag_butNeverAutoCompletes() {
        String pastAnchor = Instant.now().minus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = create("cri-" + UUID.randomUUID(), 86400L, pastAnchor);
        assertThat(occurrenceCount(k)).isZero();

        // ADMIN-only deterministic sweep trigger
        ExtractableResponse<Response> swept = given().header("Authorization", "Bearer " + admin)
            .when().post("/api/recurring-interval/" + k + "/sweep").then().statusCode(200).extract();
        assertThat(swept.jsonPath().getBoolean("sweptOverdue"))
            .as("the sweep DERIVED + recorded the non-authoritative overdue flag").isTrue();
        assertThat(swept.jsonPath().getString("status")).isEqualTo("OPEN");

        // the sweep auto-completed NOTHING — still zero occurrences, window NOT advanced
        assertThat(occurrenceCount(k))
            .as("CRI-SWEEP-001 — the sweep never auto-completes; only a real completion advances the window")
            .isZero();
        assertThat(get(k).jsonPath().getString("windowStart")).isEqualTo(pastAnchor);
    }

    // ── CRI-CONCURRENT-001 — keystone: N racing completes → exactly one advances, the rest 409 ──
    @Test @Tag("CRI-CONCURRENT-001")
    void concurrentCompletes_exactlyOneAdvancesTheWindow_theRest409() throws Exception {
        String anchor = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = create("cri-" + UUID.randomUUID(), 30L * 86400L, anchor);

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                int code = given().header("Authorization", "Bearer " + member)
                    .when().post("/api/recurring-interval/" + k + "/complete").thenReturn().statusCode();
                if (code == 200) ok.incrementAndGet();
                else if (code == 409) conflict.incrementAndGet();
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(ok.get())
            .as("CRI-CONCURRENT-001 — exactly ONE of 8 racing completes advanced the window")
            .isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(n - 1);
        assertThat(occurrenceCount(k))
            .as("exactly one occurrence appended across 8 racing completes").isEqualTo(1);
    }

    // ── CRI-AUTHZ-001 — completer is the caller; sweep is ADMIN-only; unknown key 404 ──
    @Test @Tag("CRI-AUTHZ-001")
    void completerIsTheCaller_sweepIsAdminOnly_unknownKeyIs404() {
        // unauthenticated → 401
        given().header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"cri-unauth\",\"intervalSeconds\":86400}")
        .when().post("/api/recurring-interval").then().statusCode(401);

        String anchor = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String k = create("cri-" + UUID.randomUUID(), 30L * 86400L, anchor);

        // completedBy is the authenticated principal (the caller's id), NOT any body field. Proven
        // by two DIFFERENT callers completing two obligations → two DISTINCT, non-blank completers.
        complete(k);                                          // member completes k
        String memberCompleter = given().header("Authorization", "Bearer " + member)
            .when().get("/api/recurring-interval/" + k + "/occurrences")
            .then().statusCode(200).extract().jsonPath().getString("data[0].completedBy");
        assertThat(memberCompleter).isNotBlank();

        String adminKey = "cri-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"obligationKey\":\"" + adminKey + "\",\"intervalSeconds\":" + (30L * 86400L)
                + ",\"anchorAt\":\"" + anchor + "\"}")
        .when().post("/api/recurring-interval").then().statusCode(201);
        given().header("Authorization", "Bearer " + admin)
            .when().post("/api/recurring-interval/" + adminKey + "/complete").then().statusCode(200);
        String adminCompleter = given().header("Authorization", "Bearer " + admin)
            .when().get("/api/recurring-interval/" + adminKey + "/occurrences")
            .then().statusCode(200).extract().jsonPath().getString("data[0].completedBy");
        assertThat(adminCompleter)
            .as("CRI-AUTHZ-001 — completedBy is each caller's own principal, never a shared body field")
            .isNotBlank().isNotEqualTo(memberCompleter);

        // the sweep trigger is ADMIN-only
        given().header("Authorization", "Bearer " + member)
            .when().post("/api/recurring-interval/" + k + "/sweep").then().statusCode(403);
        given().header("Authorization", "Bearer " + admin)
            .when().post("/api/recurring-interval/" + k + "/sweep").then().statusCode(200);

        // unknown key → 404 problem+json with the canonical code
        given().header("Authorization", "Bearer " + member)
            .when().get("/api/recurring-interval/does-not-exist-" + UUID.randomUUID())
            .then().statusCode(404).body("code", org.hamcrest.Matchers.equalTo("RESOURCE_NOT_FOUND"));
    }
}
