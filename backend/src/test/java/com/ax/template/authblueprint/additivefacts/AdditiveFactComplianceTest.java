package com.ax.template.authblueprint.additivefacts;

import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * additive-fact-ledger-l0 compliance — verified against the live additivefacts reference
 * workload. The invariant: a period's total is Σ of its facts (never a single replaced value);
 * a late fact for a CLOSED period posts forward as a delta rather than mutating the frozen
 * aggregate (conservation holds); duplicate delivery accumulates once.
 * Spec: specs/additive-fact-ledger-l0.yaml (Fowler event-sourcing + telecom CDR late-arrival).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ADDITIVEFACTS")
class AdditiveFactComplianceTest {

    @LocalServerPort int port;
    String member;

    @BeforeEach
    void setup() {
        member = AdditiveFactTestSupport.obtainToken(AdditiveFactTestSupport.freshEmail("af-member"), "MEMBER");
    }

    private String createPeriod(String label) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"label\":\"" + label + "\"}")
        .when().post("/api/additive-facts/periods").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> addFact(String periodId, String source, String externalFactId,
                                                   String amount, String currentOpenPeriodId) {
        String body = "{\"source\":\"" + source + "\",\"externalFactId\":\"" + externalFactId
            + "\",\"amount\":" + amount
            + (currentOpenPeriodId == null ? "" : ",\"currentOpenPeriodId\":\"" + currentOpenPeriodId + "\"")
            + "}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/additive-facts/periods/" + periodId + "/facts").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> post(String path) {
        return given().header("Authorization", "Bearer " + member)
            .when().post("/api/additive-facts/periods/" + path).thenReturn().then().extract();
    }

    private ExtractableResponse<Response> get(String path) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/additive-facts/periods/" + path).then().statusCode(200).extract();
    }

    /**
     * RestAssured's default JsonPath parses JSON numbers as Double — fine for the coarse
     * round-number assertions elsewhere in this class, but NOT for a conservation proof over
     * NUMERIC(15,4) values, where a Double round-trip can silently mask a real cent-level drift.
     * BIG_DECIMAL is the exact number-return type (precedent: ThresholdTerminalComplianceTest /
     * CashInLieuComplianceTest).
     */
    private static JsonPath exact(ExtractableResponse<Response> resp) {
        return resp.body().jsonPath(new JsonPathConfig(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    // ── FACT-ADDITIVE-ACCUM-001 — period total == Σ facts; facts are append-only ──
    @Test @Tag("FACT-ADDITIVE-ACCUM-001")
    void periodTotal_isSumOfFacts_factsAppendOnly() {
        String period = createPeriod("2026-05");
        String src = "meter-" + UUID.randomUUID();
        assertThat(addFact(period, src, "ext-1", "10.0000", null).statusCode()).isEqualTo(201);
        assertThat(addFact(period, src, "ext-2", "20.0000", null).statusCode()).isEqualTo(201);
        assertThat(addFact(period, src, "ext-3", "5.5000", null).statusCode()).isEqualTo(201);

        assertThat(get(period).jsonPath().getDouble("total")).isEqualTo(35.5);
        assertThat(get(period + "/facts").jsonPath().getList("$")).hasSize(3);
    }

    // ── FACT-IDEMPOTENT-004 — duplicate delivery accumulates once ──
    @Test @Tag("FACT-IDEMPOTENT-004")
    void duplicateDelivery_accumulatesOnce() {
        String period = createPeriod("2026-06");
        String src = "meter-" + UUID.randomUUID();
        ExtractableResponse<Response> first = addFact(period, src, "ext-dup", "12.0000", null);
        ExtractableResponse<Response> retry = addFact(period, src, "ext-dup", "12.0000", null);

        assertThat(retry.jsonPath().getString("id")).isEqualTo(first.jsonPath().getString("id"));
        assertThat(get(period).jsonPath().getDouble("total")).isEqualTo(12.0);
        assertThat(get(period + "/facts").jsonPath().getList("$")).hasSize(1);
    }

    // ── FACT-CLOSED-PERIOD-ADD-003 — closed aggregate has no rewrite path ──
    @Test @Tag("FACT-CLOSED-PERIOD-ADD-003")
    void closedPeriod_aggregateHasNoRewritePath() {
        String period = createPeriod("2026-07");
        String src = "meter-" + UUID.randomUUID();
        addFact(period, src, "ext-a", "40.0000", null);

        ExtractableResponse<Response> closed = post(period + "/close");
        assertThat(closed.jsonPath().getString("status")).isEqualTo("CLOSED");
        assertThat(closed.jsonPath().getDouble("frozenAggregate")).isEqualTo(40.0);

        // closing again — one-way, deterministic conflict
        ExtractableResponse<Response> closeAgain = post(period + "/close");
        assertThat(closeAgain.statusCode()).isEqualTo(409);
        assertThat(closeAgain.jsonPath().getString("code")).isEqualTo("FACT_INVALID_STATE");

        // the frozen total is unaffected by anything except the forward-posting path
        assertThat(get(period).jsonPath().getDouble("frozenAggregate")).isEqualTo(40.0);
    }

    // ── FACT-LATE-DELTA-POST-002 — late fact posts forward; conservation holds ──
    @Test @Tag("FACT-LATE-DELTA-POST-002")
    void lateFact_postsForward_conservationHolds() {
        String origin = createPeriod("2026-08");
        String src = "meter-" + UUID.randomUUID();
        addFact(origin, src, "ext-1", "100.0000", null);
        addFact(origin, src, "ext-2", "50.0000", null);
        post(origin + "/close");                                   // frozen at 150
        assertThat(new BigDecimal(exact(get(origin)).getString("frozenAggregate")))
            .isEqualByComparingTo("150.0000");

        String current = createPeriod("2026-09");

        // a late fact arrives assigned to the now-CLOSED origin period, with no target → 422
        ExtractableResponse<Response> noTarget = addFact(origin, src, "ext-late", "30.0000", null);
        assertThat(noTarget.statusCode()).isEqualTo(422);
        assertThat(noTarget.jsonPath().getString("code")).isEqualTo("FACT_CURRENT_PERIOD_REQUIRED");

        // naming the current OPEN period as the correction target succeeds
        ExtractableResponse<Response> late = addFact(origin, src, "ext-late", "30.0000", current);
        assertThat(late.statusCode()).isEqualTo(201);

        // the origin's frozen aggregate is untouched
        assertThat(new BigDecimal(exact(get(origin)).getString("frozenAggregate")))
            .isEqualByComparingTo("150.0000");

        // exactly one posting, referencing the origin, for the fact's full amount
        ExtractableResponse<Response> postingsResp = get(origin + "/postings");
        assertThat(postingsResp.jsonPath().getList("$")).hasSize(1);
        assertThat(postingsResp.jsonPath().getString("[0].originPeriodId")).isEqualTo(origin);
        BigDecimal postedAmount = new BigDecimal(exact(postingsResp).getString("[0].amount"));
        assertThat(postedAmount).isEqualByComparingTo("30.0000");

        // conservation: origin.frozenAggregate + Σ postings == Σ every fact ever assigned to
        // origin — exact BigDecimal arithmetic, not the lossy Double round-trip.
        List<BigDecimal> factAmounts = exact(get(origin + "/facts")).getList("amount", String.class)
            .stream().map(BigDecimal::new).toList();
        BigDecimal allFacts = factAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(allFacts).isEqualByComparingTo("180.0000");     // 100 + 50 + 30
        BigDecimal frozenAggregate = new BigDecimal(exact(get(origin)).getString("frozenAggregate"));
        assertThat(frozenAggregate.add(postedAmount)).isEqualByComparingTo(allFacts);

        // posting into a non-OPEN target is rejected
        post(current + "/close");
        ExtractableResponse<Response> badTarget = addFact(origin, src, "ext-late-2", "5.0000", current);
        assertThat(badTarget.statusCode()).isEqualTo(422);
        assertThat(badTarget.jsonPath().getString("code")).isEqualTo("FACT_CURRENT_PERIOD_NOT_OPEN");
    }
}
