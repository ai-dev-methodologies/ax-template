package com.ax.template.authblueprint.approvalworkflow;

import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WF-ROUTE-001/002 — attribute-resolved approval routing (P3-15, extends approval-workflow).
 * RestAssured black-box, same reference workload as {@link ApprovalComplianceTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("WORKFLOW")
class RoutingWorkflowComplianceTest {

    // P2-120: the field stays — com.ax.template.authblueprint.common.AxPort reads it by
    // reflection before every test and is the single writer of RestAssured.port. The manual
    // publish that used to live in a per-test setup method here is gone.
    @LocalServerPort int port;

    @Test
    @Tag("WF-ROUTE-001")
    void routing_resolvesChainAtSubmission_andSurvivesLaterRuleDeletion() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfr1-req"), "MEMBER");
        String adminToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfr1-admin"), "ADMIN");
        String category = "budget-" + UUID.randomUUID();

        String ruleId = given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body("{\"categoryOrDept\":\"" + category + "\",\"minAmount\":0,\"maxAmount\":1000000,"
                + "\"approverRoleChain\":[\"MANAGER\",\"DIRECTOR\"]}")
        .when().post("/api/approvals/routing-rules")
        .then().statusCode(201)
            .body("approverRoleChain", Matchers.contains("MANAGER", "DIRECTOR"))
        .extract().path("id");

        // Create in routing mode: no approverUserIds, category+amount instead.
        String requestId = given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body("{\"type\":\"budget\",\"category\":\"" + category + "\",\"amount\":500000}")
        .when().post("/api/approvals")
        .then().statusCode(201)
            .body("status", Matchers.equalTo("DRAFT"))
            .body("steps.size()", Matchers.equalTo(0))
        .extract().path("id");

        // Submit → chain resolves from the rule; steps created; snapshot recorded.
        given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().post("/api/approvals/" + requestId + "/submit")
        .then().statusCode(200)
            .body("status", Matchers.equalTo("SUBMITTED"))
            .body("steps.size()", Matchers.equalTo(2))
            .body("steps[0].approverUserId", Matchers.equalTo("MANAGER"))
            .body("steps[1].approverUserId", Matchers.equalTo("DIRECTOR"))
            .body("resolvedChain", Matchers.contains("MANAGER", "DIRECTOR"));

        // Delete the rule — an already-resolved, in-flight request is unaffected.
        given()
            .header("Authorization", "Bearer " + adminToken)
        .when().delete("/api/approvals/routing-rules/" + ruleId)
        .then().statusCode(204);

        given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().get("/api/approvals/" + requestId)
        .then().statusCode(200)
            .body("resolvedChain", Matchers.contains("MANAGER", "DIRECTOR"))
            .body("steps.size()", Matchers.equalTo(2));
    }

    @Test
    @Tag("WF-ROUTE-002")
    void routing_noMatchingRule_isFailClosed422_andRuleSetIsQueryable() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfr2-req"), "MEMBER");
        String adminToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfr2-admin"), "ADMIN");
        String category = "travel-" + UUID.randomUUID();

        String ruleId = given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body("{\"categoryOrDept\":\"" + category + "\",\"minAmount\":0,\"maxAmount\":1000,"
                + "\"approverRoleChain\":[\"MANAGER\"]}")
        .when().post("/api/approvals/routing-rules")
        .then().statusCode(201)
        .extract().path("id");

        // amount 9,999,999 falls outside the only band [0,1000) for this category → 422.
        String requestId = given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body("{\"type\":\"travel\",\"category\":\"" + category + "\",\"amount\":9999999}")
        .when().post("/api/approvals")
        .then().statusCode(201)
        .extract().path("id");

        given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().post("/api/approvals/" + requestId + "/submit")
        .then().statusCode(422)
            .body("code", Matchers.equalTo("NO_MATCHING_ROUTING_RULE"));

        // Request stays DRAFT — fail-closed, no silent default chain.
        given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().get("/api/approvals/" + requestId)
        .then().statusCode(200)
            .body("status", Matchers.equalTo("DRAFT"))
            .body("steps.size()", Matchers.equalTo(0));

        // The rule set is queryable by any authenticated caller.
        given()
            .header("Authorization", "Bearer " + requesterToken)
        .when().get("/api/approvals/routing-rules")
        .then().statusCode(200)
            .body("findAll { it.id == '" + ruleId + "' }.size()", Matchers.equalTo(1));
    }

    @Test
    @Tag("WF-ROUTE-001")
    void routing_neitherApproversNorRoutingAttributes_is400() {
        String requesterToken = ApprovalWorkflowTestSupport.obtainToken(
            ApprovalWorkflowTestSupport.freshEmail("wfr3-req"), "MEMBER");

        given()
            .header("Authorization", "Bearer " + requesterToken)
            .contentType(ContentType.JSON)
            .body("{\"type\":\"budget\"}")
        .when().post("/api/approvals")
        .then().statusCode(400)
            .body("code", Matchers.equalTo("ROUTING_ATTRIBUTES_REQUIRED"));
    }
}
