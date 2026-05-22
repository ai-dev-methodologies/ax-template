package com.ax.template.authblueprint.activityfeed;

import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ACTIVITY")
class ActivityComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        ActivityFeedTestSupport.useRandomPort(port);
    }

    // ─── PUBLISH family ─────────────────────────────────────────────────────

    @Test
    @Tag("ACT-PUBLISH-001")
    void publish_001_serverStampsActor() {
        String token = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("p1"), "MEMBER");
        String userId = ActivityFeedTestSupport.resolveUserId(token);

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"verb\":\"liked\",\"objectType\":\"post\",\"objectId\":\"p-1\"}")
        .when().post("/api/activities")
        .then().statusCode(201)
            .body("verb", Matchers.equalTo("liked"))
            .body("actorUserId", Matchers.equalTo(userId))
            .body("createdAt", Matchers.notNullValue());
    }

    @Test
    @Tag("ACT-PUBLISH-002")
    void publish_002_emptyAudienceDefaultsToActor() {
        String token = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("p2"), "MEMBER");

        publish(token, "viewed", "post", "p-p2", null, null);

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/activities")
        .then().statusCode(200)
            .body("items.objectId", Matchers.hasItem("p-p2"));
    }

    @Test
    @Tag("ACT-PUBLISH-003")
    void publish_003_idempotencyKeyDeduplicates() {
        String token = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("p3"), "MEMBER");
        String body = "{\"verb\":\"created\",\"objectType\":\"post\",\"objectId\":\"p-p3\",\"idempotencyKey\":\"evt-p3\"}";

        String firstId = given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/activities").then().statusCode(201).extract().path("id");

        String secondId = given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body)
        .when().post("/api/activities").then().statusCode(200).extract().path("id");

        org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);
    }

    // ─── READ family ────────────────────────────────────────────────────────

    @Test
    @Tag("ACT-READ-001")
    void read_001_feedScopedToActorOrAudience() {
        String tokenA = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("r1-a"), "MEMBER");
        String tokenB = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("r1-b"), "MEMBER");
        String tokenC = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("r1-c"), "MEMBER");
        String bId = ActivityFeedTestSupport.resolveUserId(tokenB);

        // A publishes 2 events: one targets B, one doesn't (defaults to actor A only).
        publish(tokenA, "shared", "post", "r1-targeted", java.util.List.of(bId), null);
        publish(tokenA, "viewed", "post", "r1-private", null, null);

        // B sees only the targeted event.
        given().header("Authorization", "Bearer " + tokenB)
        .when().get("/api/activities")
        .then().statusCode(200)
            .body("items.objectId", Matchers.hasItem("r1-targeted"))
            .body("items.objectId", Matchers.not(Matchers.hasItem("r1-private")));

        // C sees neither.
        given().header("Authorization", "Bearer " + tokenC)
        .when().get("/api/activities")
        .then().statusCode(200)
            .body("totalElements", Matchers.equalTo(0));
    }

    @Test
    @Tag("ACT-READ-002")
    void read_002_orderNewestFirst() {
        String token = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("r2"), "MEMBER");
        publish(token, "v", "post", "r2-a", null, null);
        sleepMs(15);
        publish(token, "v", "post", "r2-b", null, null);
        sleepMs(15);
        publish(token, "v", "post", "r2-c", null, null);

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/activities")
        .then().statusCode(200)
            .body("items[0].objectId", Matchers.equalTo("r2-c"))
            .body("items[2].objectId", Matchers.equalTo("r2-a"));
    }

    @Test
    @Tag("ACT-READ-003")
    void read_003_paginationWithSizeClamp() {
        String token = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("r3"), "MEMBER");
        for (int i = 0; i < 5; i++) {
            publish(token, "v", "post", "r3-" + i, null, null);
        }

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/activities?page=0&size=2")
        .then().statusCode(200)
            .body("items.size()", Matchers.equalTo(2))
            .body("totalElements", Matchers.greaterThanOrEqualTo(5))
            .body("page", Matchers.equalTo(0))
            .body("size", Matchers.equalTo(2));

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/activities?size=500")
        .then().statusCode(200)
            .body("size", Matchers.equalTo(100));   // clamped
    }

    // ─── MARK family ────────────────────────────────────────────────────────

    @Test
    @Tag("ACT-MARK-001")
    void mark_001_markReadIsIdempotentAndScoped() {
        String tokenA = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("m1-a"), "MEMBER");
        String tokenB = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("m1-b"), "MEMBER");
        String tokenC = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("m1-c"), "MEMBER");
        String bId = ActivityFeedTestSupport.resolveUserId(tokenB);

        UUID eventId = UUID.fromString(publish(tokenA, "shared", "post", "m1", java.util.List.of(bId), null));

        // B marks read → 204.
        given().header("Authorization", "Bearer " + tokenB)
        .when().post("/api/activities/" + eventId + "/read")
        .then().statusCode(204);

        // Idempotent: second mark → 204.
        given().header("Authorization", "Bearer " + tokenB)
        .when().post("/api/activities/" + eventId + "/read")
        .then().statusCode(204);

        // C (not in audience) → 404.
        given().header("Authorization", "Bearer " + tokenC)
        .when().post("/api/activities/" + eventId + "/read")
        .then().statusCode(404);
    }

    @Test
    @Tag("ACT-MARK-002")
    void mark_002_unreadOnlyFilter() {
        String token = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("m2"), "MEMBER");

        String e1 = publish(token, "v", "post", "m2-a", null, null);
        publish(token, "v", "post", "m2-b", null, null);
        publish(token, "v", "post", "m2-c", null, null);

        // Mark one as read.
        given().header("Authorization", "Bearer " + token)
        .when().post("/api/activities/" + e1 + "/read").then().statusCode(204);

        // Unread-only feed → excludes the marked event.
        given().header("Authorization", "Bearer " + token)
        .when().get("/api/activities?unreadOnly=true")
        .then().statusCode(200)
            .body("totalElements", Matchers.equalTo(2));

        // All feed → all 3.
        given().header("Authorization", "Bearer " + token)
        .when().get("/api/activities")
        .then().statusCode(200)
            .body("totalElements", Matchers.greaterThanOrEqualTo(3));
    }

    @Test
    @Tag("ACT-MARK-003")
    void mark_003_markAllReadReturnsCount() {
        String token = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("m3"), "MEMBER");
        publish(token, "v", "post", "m3-a", null, null);
        publish(token, "v", "post", "m3-b", null, null);
        publish(token, "v", "post", "m3-c", null, null);

        given().header("Authorization", "Bearer " + token)
        .when().post("/api/activities/mark-all-read")
        .then().statusCode(200)
            .body("markedCount", Matchers.equalTo(3));

        // Second call → 0.
        given().header("Authorization", "Bearer " + token)
        .when().post("/api/activities/mark-all-read")
        .then().statusCode(200)
            .body("markedCount", Matchers.equalTo(0));
    }

    // ─── AUTHZ family ───────────────────────────────────────────────────────

    @Test
    @Tag("ACT-AUTHZ-001")
    void authz_001_unauthenticatedReturns401() {
        given().contentType(ContentType.JSON).body("{}").when().post("/api/activities").then().statusCode(401);
        given().when().get("/api/activities").then().statusCode(401);
        given().when().get("/api/activities/" + UUID.randomUUID()).then().statusCode(401);
        given().when().post("/api/activities/" + UUID.randomUUID() + "/read").then().statusCode(401);
        given().when().post("/api/activities/mark-all-read").then().statusCode(401);
    }

    @Test
    @Tag("ACT-AUTHZ-002")
    void authz_002_privateEventReturns404ToOthers() {
        String tokenA = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("az2-a"), "MEMBER");
        String tokenB = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("az2-b"), "MEMBER");

        String eventId = publish(tokenA, "private", "post", "az2", null, null);

        given().header("Authorization", "Bearer " + tokenB)
        .when().get("/api/activities/" + eventId)
        .then().statusCode(404);
    }

    @Test
    @Tag("ACT-AUTHZ-003")
    void authz_003_actorAlwaysSeesOwnEvenIfNotInAudience() {
        String tokenA = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("az3-a"), "MEMBER");
        String tokenB = ActivityFeedTestSupport.obtainToken(
            ActivityFeedTestSupport.freshEmail("az3-b"), "MEMBER");
        String bId = ActivityFeedTestSupport.resolveUserId(tokenB);

        // A publishes with audience=[B only] (no A).
        String eventId = publish(tokenA, "shared", "post", "az3", java.util.List.of(bId), null);

        // Actor A still sees it.
        given().header("Authorization", "Bearer " + tokenA)
        .when().get("/api/activities/" + eventId)
        .then().statusCode(200);

        // B sees it.
        given().header("Authorization", "Bearer " + tokenB)
        .when().get("/api/activities/" + eventId)
        .then().statusCode(200);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String publish(String token, String verb, String objectType, String objectId,
                           java.util.List<String> audience, String idempotencyKey) {
        StringBuilder body = new StringBuilder("{\"verb\":\"").append(verb).append("\",")
            .append("\"objectType\":\"").append(objectType).append("\",")
            .append("\"objectId\":\"").append(objectId).append("\"");
        if (audience != null) {
            body.append(",\"audienceUserIds\":[");
            for (int i = 0; i < audience.size(); i++) {
                if (i > 0) body.append(',');
                body.append('"').append(audience.get(i)).append('"');
            }
            body.append(']');
        }
        if (idempotencyKey != null) {
            body.append(",\"idempotencyKey\":\"").append(idempotencyKey).append('"');
        }
        body.append('}');

        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(body.toString())
        .when().post("/api/activities")
        .then().statusCode(Matchers.anyOf(Matchers.equalTo(201), Matchers.equalTo(200)))
            .extract().path("id");
    }

    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
