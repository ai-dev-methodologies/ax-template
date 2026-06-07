package com.ax.template.authblueprint.announcement;

import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Compliance tests for announcement-l0 (ultragoal G007 dogfood — added end-to-end via /ax-plan).
 * RestAssured black-box per the catalog convention (no MockMvc / no @WithMockUser); every call
 * traverses the real auth + security pipeline. Window tests use windows relative to the real
 * clock (the injected auditLogClock singleton) so no context-dirtying clock mock is needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ANNOUNCEMENT")
class AnnouncementComplianceTest {

    @LocalServerPort int port;
    @Autowired MeterRegistry meterRegistry;

    String admin;
    String member;

    @BeforeEach
    void setup() {
        AnnouncementTestSupport.useRandomPort(port);
        admin = AnnouncementTestSupport.obtainToken(AnnouncementTestSupport.freshEmail("ann-admin"), "ADMIN");
        member = AnnouncementTestSupport.obtainToken(AnnouncementTestSupport.freshEmail("ann-member"), "MEMBER");
    }

    private static String body(String title, Instant startsAt, Instant endsAt) {
        return "{\"title\":\"" + title + "\",\"body\":\"hello body\",\"startsAt\":\""
            + startsAt + "\",\"endsAt\":\"" + endsAt + "\"}";
    }

    /** Create a DRAFT as admin; return its id. */
    private String createDraft(String title, Instant s, Instant e) {
        return given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body(body(title, s, e))
        .when().post("/api/admin/announcements")
        .then().statusCode(201).body("state", equalTo("DRAFT")).extract().path("id");
    }

    private void publish(String id) {
        given().header("Authorization", "Bearer " + admin)
        .when().post("/api/admin/announcements/" + id + "/publish")
        .then().statusCode(200).body("state", equalTo("PUBLISHED"));
    }

    // ── ANN-LIFECYCLE-001 ─────────────────────────────────────────────────────
    @Test @Tag("ANN-LIFECYCLE-001")
    void lifecycle_draftPublishArchive_andIllegalTransitionsAre409() {
        Instant now = Instant.now();
        String id = createDraft("lifecycle", now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));

        // archive a DRAFT -> 409 invalid-transition
        given().header("Authorization", "Bearer " + admin)
        .when().post("/api/admin/announcements/" + id + "/archive")
        .then().statusCode(409).body("type", equalTo("urn:problem:announcement-invalid-transition"));

        publish(id);                                  // DRAFT -> PUBLISHED

        // re-publish a PUBLISHED -> 409
        given().header("Authorization", "Bearer " + admin)
        .when().post("/api/admin/announcements/" + id + "/publish")
        .then().statusCode(409).body("type", equalTo("urn:problem:announcement-invalid-transition"));

        // PUBLISHED -> ARCHIVED ok
        given().header("Authorization", "Bearer " + admin)
        .when().post("/api/admin/announcements/" + id + "/archive")
        .then().statusCode(200).body("state", equalTo("ARCHIVED"));
    }

    // ── ANN-WINDOW-001 ────────────────────────────────────────────────────────
    @Test @Tag("ANN-WINDOW-001")
    void window_activeOnlyWhenPublishedAndWithinHalfOpenWindow() {
        Instant now = Instant.now();
        String activeId = createDraft("win-active", now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        publish(activeId);
        String futureId = createDraft("win-future", now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));
        publish(futureId);
        String expiredId = createDraft("win-expired", now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));
        publish(expiredId);

        java.util.List<String> activeIds = given().header("Authorization", "Bearer " + member)
        .when().get("/api/announcements/active")
        .then().statusCode(200).extract().path("id");

        assertThat(activeIds).contains(activeId);                 // PUBLISHED + within window
        assertThat(activeIds).doesNotContain(futureId);           // not started
        assertThat(activeIds).doesNotContain(expiredId);          // ended (exclusive upper)
    }

    // ── ANN-AUTHZ-001 ─────────────────────────────────────────────────────────
    @Test @Tag("ANN-AUTHZ-001")
    void authz_memberCannotWrite_readableByMember_unknownIs404_unauthIs401() {
        Instant now = Instant.now();
        // member create -> 403
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body("nope", now, now.plus(1, ChronoUnit.HOURS)))
        .when().post("/api/admin/announcements").then().statusCode(403);

        // active read by any authenticated user -> 200
        given().header("Authorization", "Bearer " + member)
        .when().get("/api/announcements/active").then().statusCode(200);

        // unknown id -> 404 (IDOR-safe), admin path
        given().header("Authorization", "Bearer " + admin)
        .when().get("/api/admin/announcements/" + UUID.randomUUID())
        .then().statusCode(404).body("type", equalTo("urn:problem:not-found"));

        // unauthenticated active read -> 401
        given().when().get("/api/announcements/active").then().statusCode(401);
    }

    // ── ANN-LIST-001 ──────────────────────────────────────────────────────────
    @Test @Tag("ANN-LIST-001")
    void list_activeReturnsOnlyActive_adminReturnsAll() {
        Instant now = Instant.now();
        String draftId = createDraft("list-draft", now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        String activeId = createDraft("list-active", now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        publish(activeId);
        String expiredId = createDraft("list-expired", now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));
        publish(expiredId);

        java.util.List<String> active = given().header("Authorization", "Bearer " + member)
        .when().get("/api/announcements/active").then().statusCode(200).extract().path("id");
        assertThat(active).contains(activeId);
        assertThat(active).doesNotContain(draftId);
        assertThat(active).doesNotContain(expiredId);

        java.util.List<String> all = given().header("Authorization", "Bearer " + admin)
        .when().get("/api/admin/announcements").then().statusCode(200).extract().path("id");
        assertThat(all).contains(draftId, activeId, expiredId);   // admin sees all regardless of state/window
    }

    // ── ANN-VALIDATION-001 ────────────────────────────────────────────────────
    @Test @Tag("ANN-VALIDATION-001")
    void validation_blankTitleIs400_andNonPositiveWindowIs400() {
        Instant now = Instant.now();
        // blank title -> 400 (bean-validation via GlobalProblemDetailAdvice)
        given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body(body("", now, now.plus(1, ChronoUnit.HOURS)))
        .when().post("/api/admin/announcements").then().statusCode(400);

        // over-length title (> manifest title_max 200) -> 400 at the validation boundary (@Size),
        // NOT a DataIntegrityViolation fall-through to /error (adversarial-review fix).
        given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body(body("x".repeat(201), now, now.plus(1, ChronoUnit.HOURS)))
        .when().post("/api/admin/announcements").then().statusCode(400);

        // endsAt <= startsAt -> 400 invalid-window
        given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body(body("bad-window", now.plus(1, ChronoUnit.HOURS), now))
        .when().post("/api/admin/announcements")
        .then().statusCode(400).body("type", equalTo("urn:problem:announcement-invalid-window"));
    }

    // ── ANN-OBSERVABILITY-001 ─────────────────────────────────────────────────
    @Test @Tag("ANN-OBSERVABILITY-001")
    void observability_boundedCounter_noIdOrAuthorLabel() {
        Instant now = Instant.now();
        String id = createDraft("obs", now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        publish(id);
        // a rejected archive is not reachable post-publish via DRAFT; trigger a rejected transition:
        String d2 = createDraft("obs2", now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        given().header("Authorization", "Bearer " + admin)
        .when().post("/api/admin/announcements/" + d2 + "/archive").then().statusCode(409); // rejected archive

        // the canonical counter exists, and NO meter carries an id/author/title label (bounded cardinality)
        assertThat(meterRegistry.find("announcement_total").counter()).isNotNull();
        boolean hasUnboundedLabel = meterRegistry.find("announcement_total").meters().stream()
            .flatMap(m -> m.getId().getTags().stream())
            .anyMatch(tag -> tag.getKey().equals("id") || tag.getKey().equals("author") || tag.getKey().equals("title"));
        assertThat(hasUnboundedLabel).isFalse();
        // every tag key is one of the two declared dimensions
        boolean onlyDeclaredKeys = meterRegistry.find("announcement_total").meters().stream()
            .flatMap(m -> m.getId().getTags().stream())
            .allMatch(tag -> tag.getKey().equals("transition") || tag.getKey().equals("outcome"));
        assertThat(onlyDeclaredKeys).isTrue();
    }
}
