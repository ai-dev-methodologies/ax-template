package com.ax.template.authblueprint.common;

import com.ax.template.authblueprint.AuthBlueprintBackendApplication;
import com.ax.template.authblueprint.emailoutbox.EmailOutbox;
import com.ax.template.authblueprint.emailoutbox.EmailOutboxRepository;
import com.ax.template.authblueprint.emailoutbox.EmailTemplate;
import com.ax.template.authblueprint.emailoutbox.EmailTemplateRepository;
import com.ax.template.authblueprint.scheduledtask.ScheduledTask;
import com.ax.template.authblueprint.scheduledtask.ScheduledTaskService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

/**
 * P2-35 — black-box regression for the sibling-contract {@code default: ALL} /
 * undeclared-parameter defect family.
 *
 * <p>{@code notification-openapi.yaml} is the CORRECT reference: its controller maps
 * {@code "ALL"} to a null filter. Two siblings had copied the contract shape without the
 * behaviour, and one had never bound its declared parameter at all:
 * <ul>
 *   <li><b>(b) email-outbox</b> — {@code status} was bound directly to the
 *       {@link com.ax.template.authblueprint.emailoutbox.EmailOutboxStatus} enum, so the
 *       contract's OWN documented default ({@code ?status=ALL}) produced a 400. The
 *       endpoint rejected the value its own contract told clients to send.</li>
 *   <li><b>(c) scheduled-task</b> — the contract declared a {@code status} query
 *       parameter but the handler took NO arguments, so Spring silently discarded it: a
 *       client filtering by status got the unfiltered list and no signal that the filter
 *       had been ignored. (Case (a), identity-verification, is covered in
 *       {@code IdentityVerificationFlowIT} where the domain's black-box IT already
 *       lives.)</li>
 * </ul>
 *
 * <p>Both cases are binding-layer defects, so they are only reproducible over real HTTP —
 * a direct controller call cannot observe Spring's argument resolution. RestAssured
 * black-box per PRACTICES-TEST-001.
 *
 * <p>{@code @DirtiesContext(BEFORE_CLASS)} follows the R22 ContextCache lever already
 * applied to BillingFlowIT / FeatureFlagFlowIT: this class boots with
 * {@code auth.signup.auto-verify=true}, a properties cache key shared with those classes,
 * and the LRU eviction that key suffered is what produced the dead-port 401 flake.
 */
@Tag("ALL_FILTER_SENTINEL")
@SpringBootTest(
        classes = AuthBlueprintBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"auth.signup.auto-verify=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AllFilterSentinelFlowIT {

    private static final String EMAIL_OUTBOX_ENDPOINT = "/api/admin/email-outbox";
    private static final String SCHEDULED_TASK_ENDPOINT = "/api/admin/scheduled-tasks";

    @LocalServerPort int port;

    @Autowired EmailOutboxRepository outboxRepository;
    @Autowired EmailTemplateRepository templateRepository;
    @Autowired ScheduledTaskService scheduledTaskService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        adminToken = obtainAdminToken();
    }

    private String obtainAdminToken() {
        String email = "all-filter-admin-" + UUID.randomUUID() + "@example.com";
        given().contentType(ContentType.JSON)
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"ADMIN\"}")
            .when().post("/api/auth/email/signup")
            .then().statusCode(org.hamcrest.Matchers.lessThan(300));
        return given().contentType(ContentType.JSON)
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
            .when().post("/api/auth/email/login")
            .then().extract().path("accessToken");
    }

    // ── (b) email-outbox ────────────────────────────────────────────────────

    private String seedOutboxRow() {
        String code = "p2-35-" + UUID.randomUUID();
        templateRepository.save(new EmailTemplate(code, "subject", "body"));
        EmailOutbox row = EmailOutbox.create(
            "p2-35-" + UUID.randomUUID() + "@example.com", code, "subject", "body", Instant.now());
        return outboxRepository.save(row).getId().toString();
    }

    @Test
    @Tag("ALL-SENTINEL-EMAIL-OUTBOX")
    @DisplayName("P2-35(b): GET /admin/email-outbox?status=ALL returns the unfiltered page (was 400)")
    void emailOutbox_statusAll_returns200Unfiltered() {
        String seededId = seedOutboxRow();

        // the contract's OWN default value — previously a 400 from enum binding
        given().header("Authorization", "Bearer " + adminToken)
            .queryParam("status", "ALL")
            .when().get(EMAIL_OUTBOX_ENDPOINT)
            .then().statusCode(200)
            .body("content.id", hasItem(seededId));

        // case-insensitive, matching the notification family reference
        given().header("Authorization", "Bearer " + adminToken)
            .queryParam("status", "all")
            .when().get(EMAIL_OUTBOX_ENDPOINT)
            .then().statusCode(200)
            .body("content.id", hasItem(seededId));

        // absent parameter behaves identically
        given().header("Authorization", "Bearer " + adminToken)
            .when().get(EMAIL_OUTBOX_ENDPOINT)
            .then().statusCode(200)
            .body("content.id", hasItem(seededId));
    }

    @Test
    @Tag("ALL-SENTINEL-EMAIL-OUTBOX")
    @DisplayName("P2-35(b): a REAL status value still filters, and an unknown token is a 400 ProblemDetail")
    void emailOutbox_realValueFilters_unknownIs400() {
        String seededId = seedOutboxRow();   // created PENDING

        given().header("Authorization", "Bearer " + adminToken)
            .queryParam("status", "PENDING")
            .when().get(EMAIL_OUTBOX_ENDPOINT)
            .then().statusCode(200)
            .body("content.id", hasItem(seededId));

        given().header("Authorization", "Bearer " + adminToken)
            .queryParam("status", "SENT")
            .when().get(EMAIL_OUTBOX_ENDPOINT)
            .then().statusCode(200)
            .body("content.id", not(hasItem(seededId)));

        given().header("Authorization", "Bearer " + adminToken)
            .queryParam("status", "NOT_A_STATUS")
            .when().get(EMAIL_OUTBOX_ENDPOINT)
            .then().statusCode(400)
            .body("code", equalTo("EMAIL_OUTBOX_BAD_REQUEST"));
    }

    // ── (c) scheduled-task ──────────────────────────────────────────────────

    @Test
    @Tag("ALL-SENTINEL-SCHEDULED-TASK")
    @DisplayName("P2-35(c): GET /admin/scheduled-tasks?status=… actually filters (the param was silently ignored)")
    void scheduledTask_statusParamIsBoundAndFilters() {
        String registeredName = "p2-35-registered-" + UUID.randomUUID();
        String enabledName = "p2-35-enabled-" + UUID.randomUUID();
        scheduledTaskService.register(registeredName, "0 0 * * * *");
        ScheduledTask toEnable = scheduledTaskService.register(enabledName, "0 0 * * * *");
        scheduledTaskService.enable(toEnable.getId());

        // REGISTERED: the registered task is in, the enabled one is out.
        // Before the fix BOTH appeared for every value — the filter was discarded.
        given().header("Authorization", "Bearer " + adminToken)
            .queryParam("status", "REGISTERED")
            .when().get(SCHEDULED_TASK_ENDPOINT)
            .then().statusCode(200)
            .body("name", hasItem(registeredName))
            .body("name", not(hasItem(enabledName)));

        given().header("Authorization", "Bearer " + adminToken)
            .queryParam("status", "ENABLED")
            .when().get(SCHEDULED_TASK_ENDPOINT)
            .then().statusCode(200)
            .body("name", hasItem(enabledName))
            .body("name", not(hasItem(registeredName)));

        // ALL — the contract's declared default — is the no-filter sentinel
        given().header("Authorization", "Bearer " + adminToken)
            .queryParam("status", "ALL")
            .when().get(SCHEDULED_TASK_ENDPOINT)
            .then().statusCode(200)
            .body("name", hasItem(registeredName))
            .body("name", hasItem(enabledName));

        // ... and is identical to omitting the parameter
        given().header("Authorization", "Bearer " + adminToken)
            .when().get(SCHEDULED_TASK_ENDPOINT)
            .then().statusCode(200)
            .body("name", hasItem(registeredName))
            .body("name", hasItem(enabledName));
    }

    @Test
    @Tag("ALL-SENTINEL-SCHEDULED-TASK")
    @DisplayName("P2-35(c): an unknown status token is a 400 ProblemDetail, not a silently unfiltered 200")
    void scheduledTask_unknownStatusIs400() {
        String name = "p2-35-unknown-" + UUID.randomUUID();
        scheduledTaskService.register(name, "0 0 * * * *");

        given().header("Authorization", "Bearer " + adminToken)
            .queryParam("status", "PAUSED")   // the vocabulary the contract used to claim
            .when().get(SCHEDULED_TASK_ENDPOINT)
            .then().statusCode(400)
            .body("code", equalTo("SCHEDULED_TASK_BAD_REQUEST"));

        // sanity: the seeded row IS visible unfiltered, so the 400 above is the
        // parameter's doing and not an empty dataset
        given().header("Authorization", "Bearer " + adminToken)
            .when().get(SCHEDULED_TASK_ENDPOINT)
            .then().statusCode(200)
            .body("name", hasItem(name))
            .body("$", not(hasSize(0)));
    }
}
