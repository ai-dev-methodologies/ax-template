package com.ax.template.authblueprint.mececlassification;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * mece-classification-l0 compliance — verified against the live mececlassification reference
 * workload. The invariant: an item has EXACTLY one category per scheme (a second assignment is 409,
 * DB-backstopped); every scheme MUST declare a residual bucket, so rule-based classification never
 * fails open; reclassification is an append-only move record, current category derive-on-read from
 * the latest move. Spec: specs/mece-classification-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("MECE_CLASSIFICATION")
class MeceComplianceTest {

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbcTemplate;
    String member;

    @BeforeEach
    void setup() {
        member = MeceTestSupport.obtainToken(MeceTestSupport.freshEmail("mece-member"), "MEMBER");
    }

    private ExtractableResponse<Response> declareScheme(String key, String residual) {
        String body = residual == null
            ? "{\"schemeKey\":\"" + key + "\"}"
            : "{\"schemeKey\":\"" + key + "\",\"residualCategory\":\"" + residual + "\"}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/mece/schemes").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> addRule(String schemeKey, String matchValue, String category) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"matchValue\":\"" + matchValue + "\",\"category\":\"" + category + "\"}")
        .when().post("/api/mece/schemes/" + schemeKey + "/rules").thenReturn().then().extract();
    }

    private ExtractableResponse<Response> classify(String schemeKey, String itemRef, String category) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"category\":\"" + category + "\",\"actor\":\"tester\",\"reason\":\"initial\"}")
        .when().post("/api/mece/schemes/" + schemeKey + "/items/" + itemRef + "/classify")
            .thenReturn().then().extract();
    }

    private ExtractableResponse<Response> classifyByAttribute(String schemeKey, String itemRef, String attributeValue) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"attributeValue\":\"" + attributeValue + "\",\"actor\":\"tester\",\"reason\":\"auto\"}")
        .when().post("/api/mece/schemes/" + schemeKey + "/items/" + itemRef + "/classify-by-attribute")
            .thenReturn().then().extract();
    }

    private ExtractableResponse<Response> reclassify(String schemeKey, String itemRef, String category, String reason) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"category\":\"" + category + "\",\"actor\":\"tester\",\"reason\":\"" + reason + "\"}")
        .when().post("/api/mece/schemes/" + schemeKey + "/items/" + itemRef + "/reclassify")
            .thenReturn().then().extract();
    }

    private long countInCategory(String schemeKey, String category) {
        return Long.parseLong(given().header("Authorization", "Bearer " + member)
            .when().get("/api/mece/schemes/" + schemeKey + "/categories/" + category + "/count")
            .then().statusCode(200).extract().asString());
    }

    // ── MECE-EXCLUSIVE-001 — exactly one category per (scheme, item); a second assignment is 409 ──
    @Test @Tag("MECE-EXCLUSIVE-001")
    void secondInitialAssignment_rejected409_uniqueBackstopIndependentOfServicePath() {
        String scheme = "scheme-" + UUID.randomUUID();
        declareScheme(scheme, "UNCLASSIFIED");
        String item = "item-" + UUID.randomUUID();

        ExtractableResponse<Response> first = classify(scheme, item, "A");
        assertThat(first.statusCode()).isEqualTo(201);

        ExtractableResponse<Response> second = classify(scheme, item, "B");
        assertThat(second.statusCode())
            .as("MECE-EXCLUSIVE-001 — a second initial assignment is a 409, use reclassify instead")
            .isEqualTo(409);
        assertThat(second.jsonPath().getString("code")).isEqualTo("MECE_ALREADY_CLASSIFIED");

        // the DB backstop independently blocks a duplicate (scheme_key, item_ref) row
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO item_classifications (id, scheme_key, item_ref, created_at) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), scheme, item, java.sql.Timestamp.from(Instant.now())))
            .as("MECE-EXCLUSIVE-001 — uq(scheme_key, item_ref) blocks a native bypass")
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test @Tag("MECE-EXCLUSIVE-001")
    void classifyingUnderMissingScheme_is404() {
        assertThat(classify("scheme-" + UUID.randomUUID(), "item-x", "A").statusCode()).isEqualTo(404);
    }

    // ── MECE-EXHAUSTIVE-002 — every scheme must declare a residual bucket; classification never fails open ──
    @Test @Tag("MECE-EXHAUSTIVE-002")
    void schemeWithoutResidual_rejected422_matchingNoRule_landsInResidual_countVisible() {
        String noResidual = "scheme-" + UUID.randomUUID();
        assertThat(declareScheme(noResidual, "").statusCode())
            .as("MECE-EXHAUSTIVE-002 — a scheme without a residual category is 422")
            .isEqualTo(422);
        assertThat(declareScheme(noResidual, null).statusCode()).isEqualTo(422);

        String scheme = "scheme-" + UUID.randomUUID();
        assertThat(declareScheme(scheme, "UNCLASSIFIED").statusCode()).isEqualTo(201);
        assertThat(addRule(scheme, "electronics", "ELECTRONICS").statusCode()).isEqualTo(201);

        // an attribute value that matches NO rule lands in the residual bucket — never rejected
        String item = "item-" + UUID.randomUUID();
        ExtractableResponse<Response> classified = classifyByAttribute(scheme, item, "unmapped-widget");
        assertThat(classified.statusCode()).isEqualTo(201);
        assertThat(classified.jsonPath().getString("currentCategory"))
            .as("MECE-EXHAUSTIVE-002 — no rule match lands in the residual category")
            .isEqualTo("UNCLASSIFIED");

        assertThat(countInCategory(scheme, "UNCLASSIFIED")).isGreaterThanOrEqualTo(1);

        // a matching attribute resolves to its rule's category, not the residual
        String item2 = "item-" + UUID.randomUUID();
        ExtractableResponse<Response> matched = classifyByAttribute(scheme, item2, "electronics");
        assertThat(matched.jsonPath().getString("currentCategory")).isEqualTo("ELECTRONICS");
    }

    // ── MECE-RECLASS-003 — append-only move history; current category derive-on-read from the latest ──
    @Test @Tag("MECE-RECLASS-003")
    void reclassification_appendsMoves_currentDerivedFromLatest_historyImmutableAndOrdered() {
        String scheme = "scheme-" + UUID.randomUUID();
        declareScheme(scheme, "UNCLASSIFIED");
        String item = "item-" + UUID.randomUUID();

        classify(scheme, item, "A");
        reclassify(scheme, item, "B", "corrected");
        ExtractableResponse<Response> afterC = reclassify(scheme, item, "C", "re-corrected");
        assertThat(afterC.statusCode()).isEqualTo(200);
        assertThat(afterC.jsonPath().getString("currentCategory")).isEqualTo("C");

        ExtractableResponse<Response> current = given().header("Authorization", "Bearer " + member)
            .when().get("/api/mece/schemes/" + scheme + "/items/" + item).then().statusCode(200).extract();
        assertThat(current.jsonPath().getString("currentCategory")).isEqualTo("C");

        List<Map<String, Object>> history = given().header("Authorization", "Bearer " + member)
            .when().get("/api/mece/schemes/" + scheme + "/items/" + item + "/history")
            .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(history).hasSize(3);
        assertThat(history.get(0).get("fromCategory")).isNull();
        assertThat(history.get(0).get("toCategory")).isEqualTo("A");
        assertThat(history.get(1).get("fromCategory")).isEqualTo("A");
        assertThat(history.get(1).get("toCategory")).isEqualTo("B");
        assertThat(history.get(2).get("fromCategory")).isEqualTo("B");
        assertThat(history.get(2).get("toCategory")).isEqualTo("C");
    }

    @Test @Tag("MECE-RECLASS-003")
    void reclassifyingUnclassifiedItem_is404() {
        String scheme = "scheme-" + UUID.randomUUID();
        declareScheme(scheme, "UNCLASSIFIED");
        assertThat(reclassify(scheme, "never-classified", "X", "n/a").statusCode()).isEqualTo(404);
    }
}
