package com.ax.template.authblueprint.thresholdfiling;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * threshold-filing-obligation-l0 compliance — verified against the live thresholdfiling reference
 * workload. The invariant: the accrual that crosses the threshold binds a filing obligation exactly
 * once, in the SAME transaction that flips the register to TRIGGERED (no re-trigger); the bound
 * record is immutable and self-describing; its due date is trigger + a fixed statutory window, an
 * overdue-but-open filing stays visible via a fail-closed query, and the ONLY terminal is an
 * explicit ack. Spec: specs/threshold-filing-obligation-l0.yaml
 * (31 CFR §1020.320(b)(3) SAR window + 14 CFR §43.10 crossing shape + PMC7510293 closed-loop).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("THRESHOLD_FILING")
class FilingComplianceTest {

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbcTemplate;
    String member;
    String admin;

    @BeforeEach
    void setup() {
        FilingTestSupport.useRandomPort(port);
        member = FilingTestSupport.obtainToken(FilingTestSupport.freshEmail("fil-member"), "MEMBER");
        admin = FilingTestSupport.obtainToken(FilingTestSupport.freshEmail("fil-admin"), "ADMIN");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createRegister(String subject, String threshold) {
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"subjectKey\":\"" + subject + "\",\"threshold\":" + threshold + "}")
        .when().post("/api/filing-registers").then().statusCode(201);
        return subject;
    }

    private ExtractableResponse<Response> accrue(String subject, String delta) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"delta\":" + delta + "}")
        .when().post("/api/filing-registers/" + subject + "/accruals").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> getFiling(String subject) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/filing-registers/" + subject + "/filing").then().statusCode(200).extract();
    }

    // ── TFO-TRIGGER-001 — crossing binds a filing exactly once, same-tx; no re-trigger ──
    @Test @Tag("TFO-TRIGGER-001")
    void crossingAccrual_bindsFilingExactlyOnce_inSameTransaction_andBlocksRetrigger() {
        String subject = createRegister("fil-" + UUID.randomUUID(), "100");

        ExtractableResponse<Response> below = accrue(subject, "60");
        assertThat(below.statusCode()).isEqualTo(200);
        assertThat(below.jsonPath().getString("status")).isEqualTo("ACTIVE");

        // no filing exists yet — 404
        int beforeCrossing = given().header("Authorization", "Bearer " + member)
            .when().get("/api/filing-registers/" + subject + "/filing").thenReturn().statusCode();
        assertThat(beforeCrossing).isEqualTo(404);

        // the crossing accrual is ACCEPTED and returns TRIGGERED immediately, WITH the filing bound
        ExtractableResponse<Response> crossing = accrue(subject, "45");
        assertThat(crossing.statusCode()).isEqualTo(200);
        assertThat(crossing.jsonPath().getString("status"))
            .as("TFO-TRIGGER-001 — the crossing response itself exposes TRIGGERED")
            .isEqualTo("TRIGGERED");

        ExtractableResponse<Response> filing = getFiling(subject);
        assertThat(filing.jsonPath().getString("subjectKey")).isEqualTo(subject);

        // a repeated/late accrual on TRIGGERED is a deterministic 409 — no re-trigger
        ExtractableResponse<Response> late = accrue(subject, "1");
        assertThat(late.statusCode()).isEqualTo(409);
        assertThat(late.jsonPath().getString("code")).isEqualTo("FILING_TRIGGERED");
    }

    // ── TFO-TRIGGER-001 — keystone: N racing accruals that jointly cross bind exactly one filing ──
    @Test @Tag("TFO-TRIGGER-001")
    void concurrentAccruals_exactlyOneFilingBound() throws Exception {
        String subject = createRegister("fil-" + UUID.randomUUID(), "100");
        int n = 8;
        BigDecimal delta = new BigDecimal("30");                       // 8 × 30 = 240 ≫ 100

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<ExtractableResponse<Response>> results = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                results.add(accrue(subject, delta.toPlainString()));
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        long triggeredResponses = results.stream()
            .filter(r -> r.statusCode() == 200 && "TRIGGERED".equals(r.jsonPath().getString("status")))
            .count();
        assertThat(triggeredResponses)
            .as("TFO-TRIGGER-001 — exactly ONE accrual is the crossing across 8 racing accruals")
            .isEqualTo(1);

        // exactly one filing exists and is reachable — the UNIQUE(register_id) backstop held
        assertThat(getFiling(subject).jsonPath().getString("subjectKey")).isEqualTo(subject);
    }

    // ── TFO-DEADLINE-001 — due date = trigger + fixed window; overdue-open stays visible; ack-only ──
    @Test @Tag("TFO-DEADLINE-001")
    void filingDueDate_isTriggerPlusFixedWindow_overdueVisibleUntilAcked_thenClosesOnce() {
        String subject = createRegister("fil-" + UUID.randomUUID(), "100");
        accrue(subject, "60");
        accrue(subject, "45");                                        // crossing

        ExtractableResponse<Response> filing = getFiling(subject);
        Instant trigger = Instant.parse(filing.jsonPath().getString("triggerInstant"));
        Instant due = Instant.parse(filing.jsonPath().getString("dueAt"));
        assertThat(due)
            .as("TFO-DEADLINE-001 — the fixed 31 CFR §1020.320(b)(3) 30-day window")
            .isEqualTo(trigger.plus(30, ChronoUnit.DAYS));

        // force the filing overdue via a native write (mirrors ThresholdTerminalComplianceTest's
        // DB-backstop technique) — the API never lets a caller backdate a due date directly
        jdbcTemplate.update("UPDATE filing_obligations SET due_at = ? WHERE subject_key = ?",
            java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)), subject);

        ExtractableResponse<Response> overdue = given().header("Authorization", "Bearer " + admin)
            .when().get("/api/filing-registers/overdue").then().statusCode(200).extract();
        assertThat(overdue.jsonPath().getList("data.subjectKey", String.class))
            .as("TFO-DEADLINE-001 — an overdue OPEN filing MUST stay visible, never silently filtered")
            .contains(subject);

        // a non-admin cannot see the operational overdue listing
        int forbidden = given().header("Authorization", "Bearer " + member)
            .when().get("/api/filing-registers/overdue").thenReturn().statusCode();
        assertThat(forbidden).isEqualTo(403);

        // ack closes the loop — it disappears because it was explicitly closed, not silently expired
        ExtractableResponse<Response> ack = given().header("Authorization", "Bearer " + member)
            .when().post("/api/filing-registers/" + subject + "/filing/ack").then().statusCode(200).extract();
        assertThat(ack.jsonPath().getString("status")).isEqualTo("ACKNOWLEDGED");
        assertThat(ack.jsonPath().getString("ackBy")).isNotBlank();
        assertThat(ack.jsonPath().getString("ackAt")).isNotBlank();

        ExtractableResponse<Response> afterAck = given().header("Authorization", "Bearer " + admin)
            .when().get("/api/filing-registers/overdue").then().statusCode(200).extract();
        assertThat(afterAck.jsonPath().getList("data.subjectKey", String.class)).doesNotContain(subject);

        // the loop closes exactly once
        int secondAck = given().header("Authorization", "Bearer " + member)
            .when().post("/api/filing-registers/" + subject + "/filing/ack").thenReturn().statusCode();
        assertThat(secondAck).isEqualTo(409);
    }

    // ── TFO-FILING-RECORD-001 — the bound record is immutable and self-describing ──
    @Test @Tag("TFO-FILING-RECORD-001")
    void filingRecord_isSelfDescribing_andImmutableAcrossReads() {
        String subject = createRegister("fil-" + UUID.randomUUID(), "250");
        accrue(subject, "300");                                       // single crossing accrual

        ExtractableResponse<Response> first = getFiling(subject);
        assertThat(new BigDecimal(first.jsonPath().getString("thresholdSnapshot")))
            .as("the record snapshots the threshold AT trigger time — independent of the register")
            .isEqualByComparingTo("250");
        assertThat(first.jsonPath().getString("subjectKey")).isEqualTo(subject);
        assertThat(first.jsonPath().getString("triggerInstant")).isNotBlank();

        // reading again yields the identical provenance — nothing here ever mutates
        ExtractableResponse<Response> second = getFiling(subject);
        assertThat(second.jsonPath().getString("triggerInstant")).isEqualTo(first.jsonPath().getString("triggerInstant"));
        assertThat(second.jsonPath().getString("dueAt")).isEqualTo(first.jsonPath().getString("dueAt"));
    }
}
