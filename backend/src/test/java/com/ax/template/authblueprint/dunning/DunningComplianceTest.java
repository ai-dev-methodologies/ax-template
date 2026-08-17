package com.ax.template.authblueprint.dunning;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * dunning-collections-l0 compliance — verified against the live dunning reference workload.
 * The invariant: a one-way ladder REMINDER→NOTICE→FINAL_NOTICE→SUSPENDED with exactly-once
 * stage transitions; an aging bucket computed deterministically from days-overdue at a RECORDED
 * as-of instant; a cure window that resets to CURRENT + halts the ladder on full cure and
 * resumes on lapse; concurrent advances serialize so exactly one wins.
 * Spec: specs/dunning-collections-l0.yaml (FDCPA staged-notice + 17 CFR 210.5-02 allowance + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("DUNNING")
class DunningComplianceTest {

    @LocalServerPort int port;
    @Autowired DunningService service;
    String member;

    @BeforeEach
    void setup() {
        member = DunningTestSupport.obtainToken(DunningTestSupport.freshEmail("dun-member"), "MEMBER");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String openCase(String ref, LocalDate dueDate, String overdueAmount) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"receivableRef\":\"" + ref + "\",\"dueDate\":\"" + dueDate + "\","
                + "\"overdueAmount\":" + overdueAmount + "}")
        .when().post("/api/dunning/cases").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> advance(String id, String fromStage) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"fromStage\":\"" + fromStage + "\"}")
        .when().post("/api/dunning/cases/" + id + "/advance").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> pay(String id, String amount) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"amount\":" + amount + "}")
        .when().post("/api/dunning/cases/" + id + "/payments").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> cure(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/dunning/cases/" + id + "/cure").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getCase(String id) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/dunning/cases/" + id).then().statusCode(200).extract();
    }

    // ── DUNNING-LADDER-001 — one-way exactly-once ladder; SUSPENDED is terminal ──
    @Test @Tag("DUNNING-LADDER-001")
    void ladder_advancesOneWay_exactlyOnce_suspendedTerminal() {
        String id = openCase("INV-LADDER", LocalDate.now(ZoneOffset.UTC).minusDays(45), "100.00");

        ExtractableResponse<Response> toNotice = advance(id, "REMINDER");
        assertThat(toNotice.statusCode()).isEqualTo(200);
        assertThat(toNotice.jsonPath().getString("stage")).isEqualTo("NOTICE");

        // re-emitting an already-reached rung (stale fromStage, case not yet terminal)
        // → 409 DUNNING_STAGE_ALREADY_REACHED
        ExtractableResponse<Response> stale = advance(id, "REMINDER");
        assertThat(stale.statusCode()).isEqualTo(409);
        assertThat(stale.jsonPath().getString("code")).isEqualTo("DUNNING_STAGE_ALREADY_REACHED");

        ExtractableResponse<Response> toFinalNotice = advance(id, "NOTICE");
        assertThat(toFinalNotice.statusCode()).isEqualTo(200);
        assertThat(toFinalNotice.jsonPath().getString("stage")).isEqualTo("FINAL_NOTICE");
        ExtractableResponse<Response> toSuspended = advance(id, "FINAL_NOTICE");
        assertThat(toSuspended.statusCode()).isEqualTo(200);
        assertThat(toSuspended.jsonPath().getString("stage")).isEqualTo("SUSPENDED");

        // advancing past SUSPENDED → 409 DUNNING_LADDER_TERMINAL
        ExtractableResponse<Response> terminal = advance(id, "SUSPENDED");
        assertThat(terminal.statusCode()).isEqualTo(409);
        assertThat(terminal.jsonPath().getString("code")).isEqualTo("DUNNING_LADDER_TERMINAL");

        // the transition trail is append-only one-per-rung
        var transitions = given().header("Authorization", "Bearer " + member)
            .when().get("/api/dunning/cases/" + id + "/transitions")
            .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(transitions).hasSize(3);   // NOTICE, FINAL_NOTICE, SUSPENDED (REMINDER is the open state)
    }

    // ── DUNNING-AGING-001 — deterministic bucket from days-overdue, with recorded basis ──
    @Test @Tag("DUNNING-AGING-001")
    void aging_isDeterministic_withRecordedBasis() {
        // not yet due → CURRENT
        ExtractableResponse<Response> current = getCase(openCase("INV-CUR", LocalDate.now(ZoneOffset.UTC).plusDays(10), "50.00"));
        assertThat(current.jsonPath().getString("agingBucket")).isEqualTo("CURRENT");

        // 15 days overdue → B1_30
        ExtractableResponse<Response> b1 = getCase(openCase("INV-B1", LocalDate.now(ZoneOffset.UTC).minusDays(15), "50.00"));
        assertThat(b1.jsonPath().getString("agingBucket")).isEqualTo("B1_30");

        // 45 days overdue → B2_60
        ExtractableResponse<Response> b2 = getCase(openCase("INV-B2", LocalDate.now(ZoneOffset.UTC).minusDays(45), "50.00"));
        assertThat(b2.jsonPath().getString("agingBucket")).isEqualTo("B2_60");

        // 120 days overdue → B3_90_PLUS, and the basis is recorded
        ExtractableResponse<Response> b3 = getCase(openCase("INV-B3", LocalDate.now(ZoneOffset.UTC).minusDays(120), "50.00"));
        assertThat(b3.jsonPath().getString("agingBucket")).isEqualTo("B3_90_PLUS");
        assertThat(b3.jsonPath().getString("agingAsOf")).as("the as-of basis is recorded").isNotBlank();
        assertThat(b3.jsonPath().getLong("daysOverdue")).as("the days-overdue basis is recorded").isGreaterThanOrEqualTo(120L);
        assertThat(b3.jsonPath().getString("dueDate")).isNotBlank();
    }

    // ── DUNNING-CURE-001 — full cure resets to CURRENT + halts the ladder; idempotent ──
    @Test @Tag("DUNNING-CURE-001")
    void cure_fullPaymentWithinWindow_resetsToCurrent_andHalts_idempotent() {
        String id = openCase("INV-CURE", LocalDate.now(ZoneOffset.UTC).minusDays(45), "100.00");
        advance(id, "REMINDER");                              // NOTICE; aging B2_60

        // a partial payment opens the window but does NOT reset the case
        ExtractableResponse<Response> partial = pay(id, "40.00");
        assertThat(partial.statusCode()).isEqualTo(200);
        assertThat(partial.jsonPath().getString("cureDeadline")).as("cure window opened").isNotBlank();
        assertThat(partial.jsonPath().getString("agingBucket")).isEqualTo("B2_60");   // unchanged
        assertThat(partial.jsonPath().getBoolean("ladderHalted")).isFalse();

        // full payment within the window → cure
        assertThat(pay(id, "60.00").statusCode()).isEqualTo(200);
        ExtractableResponse<Response> cured = cure(id);
        assertThat(cured.statusCode()).isEqualTo(200);
        assertThat(cured.jsonPath().getString("agingBucket")).isEqualTo("CURRENT");
        assertThat(cured.jsonPath().getBoolean("ladderHalted")).isTrue();
        assertThat(cured.jsonPath().getString("cureDeadline")).as("window closed on cure").isNull();

        // the halt is recorded as a CURED transition
        var transitions = given().header("Authorization", "Bearer " + member)
            .when().get("/api/dunning/cases/" + id + "/transitions")
            .then().statusCode(200).extract().jsonPath().getList("kind");
        assertThat(transitions).contains("CURED");

        // a second cure is idempotent — never a double reset
        ExtractableResponse<Response> again = cure(id);
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat(again.jsonPath().getString("agingBucket")).isEqualTo("CURRENT");
    }

    // ── DUNNING-CURE-001 — curing with no open window → 422 ──
    @Test @Tag("DUNNING-CURE-001")
    void cure_withNoOpenWindow_is422() {
        String id = openCase("INV-NOWIN", LocalDate.now(ZoneOffset.UTC).minusDays(15), "100.00");
        ExtractableResponse<Response> bad = cure(id);              // never paid → no window
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("DUNNING_NO_CURE_WINDOW");
    }

    // ── DUNNING-CURE-001 — a partial payment alone does not enable cure (422) ──
    @Test @Tag("DUNNING-CURE-001")
    void cure_partialPaymentOnly_is422() {
        String id = openCase("INV-PARTIAL", LocalDate.now(ZoneOffset.UTC).minusDays(15), "100.00");
        assertThat(pay(id, "30.00").statusCode()).isEqualTo(200);   // window open, but not full
        ExtractableResponse<Response> bad = cure(id);
        assertThat(bad.statusCode()).isEqualTo(422);
        assertThat(bad.jsonPath().getString("code")).isEqualTo("DUNNING_NO_CURE_WINDOW");
    }

    // ── DUNNING-CONCURRENT-001 — keystone: N concurrent advances → exactly one wins ──
    @Test @Tag("DUNNING-CONCURRENT-001")
    void concurrentAdvances_exactlyOneWins() throws Exception {
        String id = openCase("INV-RACE", LocalDate.now(ZoneOffset.UTC).minusDays(20), "100.00");
        UUID caseId = UUID.fromString(id);

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Integer> codes = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    service.advance(caseId, DunningStage.REMINDER, "racer");   // all from REMINDER
                    codes.add(200);
                } catch (DunningException ex) {
                    codes.add(ex.status().value());
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(codes.stream().filter(c -> c == 200).count())
            .as("DUNNING-CONCURRENT-001 — exactly one advance wins").isEqualTo(1);
        assertThat(codes.stream().filter(c -> c == 409).count()).isEqualTo(n - 1);

        // the case advanced by exactly one rung, with exactly one NOTICE transition
        assertThat(getCase(id).jsonPath().getString("stage")).isEqualTo("NOTICE");
        var notices = given().header("Authorization", "Bearer " + member)
            .when().get("/api/dunning/cases/" + id + "/transitions")
            .then().statusCode(200).extract().jsonPath().getList("stage");
        assertThat(notices.stream().filter("NOTICE"::equals).count())
            .as("exactly one transition row for the reached rung").isEqualTo(1L);
    }
}
