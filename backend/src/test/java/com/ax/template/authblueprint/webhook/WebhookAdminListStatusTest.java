package com.ax.template.authblueprint.webhook;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * P2-30 closure — pins the reconciled {@code GET /api/admin/webhook-deliveries}
 * {@code status} filter behavior against the (now-aligned) contract.
 * <p>
 * Before the fix, {@code contracts/webhook-openapi.yaml} advertised the query
 * value {@code SUCCESS} (the Java enum has {@link WebhookDeliveryStatus#SUCCEEDED}
 * instead) plus a synthetic {@code ALL} value with {@code default: ALL} that the
 * controller's {@code @RequestParam WebhookDeliveryStatus status} binding could
 * never satisfy — {@code Enum.valueOf("ALL")} throws, so the contract's own
 * documented default request (no {@code status} query param at all) 400s.
 * <p>
 * Decision (P2-30): reconcile the CONTRACT to the CODE rather than teach the
 * code a fake "ALL" status. Rationale:
 * <ul>
 *   <li>Smaller blast radius — zero production code changes; only the OpenAPI
 *       document (enum value name + dropped {@code ALL} + corrected default)
 *       needed to move.</li>
 *   <li>The frontend devconsole client ({@code webhookClient.ts},
 *       {@code webhooks-screen.tsx}) already used {@code SUCCEEDED} and already
 *       defaulted its query to {@code FAILED_PERMANENT} — i.e. the ACTUAL
 *       consumer of this endpoint already agreed with the code, not the
 *       contract. The contract was the outlier, not the code.</li>
 *   <li>The endpoint's own javadoc/blueprint framing
 *       (WEBHOOK-DEAD-LETTER-001 — "Dead-letter listing") never called for a
 *       cross-status "list everything" view; it is a single-status admin
 *       filter, so a real {@code ALL} branch would have been speculative
 *       scope growth, not a documented requirement.</li>
 * </ul>
 * This test proves: (1) the contract's now-correct enum value
 * ({@code status=SUCCEEDED}) round-trips 200 (previously {@code SUCCESS} would
 * have 400'd against the real enum), and (2) the contract's now-correct
 * documented default (omit {@code status}) resolves to {@code FAILED_PERMANENT}
 * and returns 200 — never binds to a non-existent {@code ALL} constant.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebhookAdminListStatusTest {

    @LocalServerPort int port;

    @Autowired WebhookDeliveryRepository deliveryRepository;

    private String adminToken;

    @BeforeEach
    void setup() {
        // P2-120: RestAssured.port is published by com.ax.template.authblueprint.common.AxPort
        // from the @LocalServerPort field above, before this method runs.
        adminToken = obtainAdminToken();
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-ADMIN-LIST-001")
    void statusFilter_succeededValueMatchesContract_andRoundTrips200() {
        UUID endpointId = UUID.randomUUID();
        WebhookDelivery delivery = WebhookDelivery.enqueue(endpointId, "order.created", "{}");
        delivery.markSucceeded(200, Instant.now());
        deliveryRepository.save(delivery);

        // contract now documents SUCCEEDED (not SUCCESS) — a contract-following
        // client sending this value must get 200, and the enum value name the
        // controller echoes back must be SUCCEEDED (matches WebhookDeliveryStatus).
        given()
            .header("Authorization", "Bearer " + adminToken)
            .accept(ContentType.JSON)
            .queryParam("status", "SUCCEEDED")
        .when().get("/api/admin/webhook-deliveries")
        .then().statusCode(200)
            .body("status", hasItem("SUCCEEDED"));

        // the OLD contract value SUCCESS is NOT a valid WebhookDeliveryStatus
        // constant and MUST still 400 — proves we reconciled by renaming the
        // contract value, not by adding a lenient alias in the binding.
        given()
            .header("Authorization", "Bearer " + adminToken)
            .accept(ContentType.JSON)
            .queryParam("status", "SUCCESS")
        .when().get("/api/admin/webhook-deliveries")
        .then().statusCode(400);
    }

    @Test
    @Tag("WEBHOOK")
    @Tag("WEBHOOK-ADMIN-LIST-002")
    void statusFilter_documentedDefault_omittedParam_resolvesToFailedPermanent_notAll() {
        UUID endpointId = UUID.randomUUID();
        WebhookDelivery deadLettered = WebhookDelivery.enqueue(endpointId, "order.created", "{}");
        deadLettered.markFailedPermanent(500, "boom", Instant.now());
        deliveryRepository.save(deadLettered);

        // the OLD contract-documented default was "no status param → ALL" and
        // "ALL" is not a WebhookDeliveryStatus constant, so this exact request
        // (the one the contract told callers was safe to make) used to 400.
        // Post-fix the contract's default is FAILED_PERMANENT (what the code
        // already did) — the omitted-param request now round-trips 200 and
        // is filtered exactly like an explicit ?status=FAILED_PERMANENT.
        given()
            .header("Authorization", "Bearer " + adminToken)
            .accept(ContentType.JSON)
        .when().get("/api/admin/webhook-deliveries")
        .then().statusCode(200)
            .body("status", hasItem("FAILED_PERMANENT"))
            .body("status", org.hamcrest.Matchers.everyItem(equalTo("FAILED_PERMANENT")));

        // there is deliberately no "ALL" value anymore — sending it is a 400,
        // not a silent fallback, so the contract can never re-drift back to
        // promising a status this binding cannot satisfy.
        given()
            .header("Authorization", "Bearer " + adminToken)
            .accept(ContentType.JSON)
            .queryParam("status", "ALL")
        .when().get("/api/admin/webhook-deliveries")
        .then().statusCode(400);
    }

    private String obtainAdminToken() {
        String email = "webhook-admin-" + UUID.randomUUID() + "@example.com";
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"ADMIN\"}")
        .when().post("/api/auth/email/signup");

        return given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }
}
