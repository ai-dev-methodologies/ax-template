package com.ax.template.authblueprint.notification;

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

/**
 * AUTHZ family (3 items): NOTIF-AUTHZ-001, NOTIF-AUTHZ-002, NOTIF-AUTHZ-003.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationAuthZTest {

    @LocalServerPort int port;

    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationPreferencesRepository preferencesRepository;

    @BeforeEach
    void setup() {
        NotificationTestSupport.useRandomPort(port);
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-AUTHZ-001")
    void authz_001_unauthenticatedRequestsAre401() {
        // GET list — no JWT
        given().accept(ContentType.JSON)
            .when().get("/api/notifications")
            .then().statusCode(401);

        // GET single — no JWT
        given().accept(ContentType.JSON)
            .when().get("/api/notifications/" + UUID.randomUUID())
            .then().statusCode(401);

        // PATCH mark read — no JWT
        given().accept(ContentType.JSON)
            .when().patch("/api/notifications/" + UUID.randomUUID() + "/read")
            .then().statusCode(401);

        // DELETE dismiss — no JWT
        given().accept(ContentType.JSON)
            .when().delete("/api/notifications/" + UUID.randomUUID())
            .then().statusCode(401);

        // GET preferences — no JWT
        given().accept(ContentType.JSON)
            .when().get("/api/notifications/preferences")
            .then().statusCode(401);

        // PATCH preferences — no JWT
        given().accept(ContentType.JSON).contentType(ContentType.JSON).body("{}")
            .when().patch("/api/notifications/preferences")
            .then().statusCode(401);

        // POST send — no JWT
        given().accept(ContentType.JSON).contentType(ContentType.JSON).body("{}")
            .when().post("/api/notifications")
            .then().statusCode(401);
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-AUTHZ-002")
    void authz_002_crossUserAccessReturns404() {
        String tokenA = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("authz-a"), "MEMBER");
        String tokenB = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("authz-b"), "MEMBER");
        String userIdA = NotificationTestSupport.resolveCallerUserId(tokenA);

        // Seed a notification owned by userA.
        UUID notifId = UUID.randomUUID();
        Instant now = Instant.now();
        notificationRepository.save(Notification.builder()
            .id(notifId)
            .recipientUserId(userIdA)
            .type("SAMPLE")
            .title("Hello A")
            .body("body")
            .createdAt(now)
            .updatedAt(now)
            .build());

        // userB attempts to read userA's notification — must return 404.
        given()
            .header("Authorization", "Bearer " + tokenB)
            .accept(ContentType.JSON)
        .when().get("/api/notifications/" + notifId)
        .then().statusCode(404);

        // userB attempts to mark userA's notification as read — 404.
        given()
            .header("Authorization", "Bearer " + tokenB)
            .accept(ContentType.JSON)
        .when().patch("/api/notifications/" + notifId + "/read")
        .then().statusCode(404);

        // userB attempts to delete userA's notification — 404.
        given()
            .header("Authorization", "Bearer " + tokenB)
            .accept(ContentType.JSON)
        .when().delete("/api/notifications/" + notifId)
        .then().statusCode(404);

        // userA's notification must still be intact (not dismissed by userB).
        Notification still = notificationRepository.findById(notifId).orElseThrow();
        // Owner can still see it
        org.assertj.core.api.Assertions.assertThat(still.isDeleted()).isFalse();
        org.assertj.core.api.Assertions.assertThat(still.getStatus())
            .isEqualTo(NotificationStatus.UNREAD);
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-AUTHZ-003")
    void authz_003_preferencesAreIsolatedPerCaller() {
        String tokenA = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("pref-iso-a"), "MEMBER");
        String tokenB = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("pref-iso-b"), "MEMBER");
        String userIdA = NotificationTestSupport.resolveCallerUserId(tokenA);
        String userIdB = NotificationTestSupport.resolveCallerUserId(tokenB);

        // userA sets emailEnabled=false explicitly.
        preferencesRepository.save(new NotificationPreferences(userIdA, true, false));

        // userB calls GET /preferences — must NOT see userA's values.
        // userB has never set preferences, so defaults (in_app=true, email=true)
        // must be returned.
        given()
            .header("Authorization", "Bearer " + tokenB)
            .accept(ContentType.JSON)
        .when().get("/api/notifications/preferences")
        .then().statusCode(200)
            .body("inAppEnabled", org.hamcrest.Matchers.equalTo(true))
            .body("emailEnabled", org.hamcrest.Matchers.equalTo(true));

        // NOTIF-AUTHZ-003 — no URL accepts a userId path parameter. The
        // preferences endpoint is /api/notifications/preferences without any
        // cross-user variant. We reflect over NotificationController to assert
        // that no method exposes a userId in the URL — this is the structural
        // guarantee the spec requires.
        java.lang.reflect.Method[] methods = NotificationController.class.getDeclaredMethods();
        for (java.lang.reflect.Method m : methods) {
            for (java.lang.reflect.Parameter p : m.getParameters()) {
                org.springframework.web.bind.annotation.PathVariable pv =
                    p.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
                if (pv != null) {
                    String name = pv.value().isEmpty() ? p.getName() : pv.value();
                    org.assertj.core.api.Assertions.assertThat(name.toLowerCase())
                        .as("NOTIF-AUTHZ-003 — no controller @PathVariable may carry a userId — found %s", name)
                        .doesNotContain("user");
                }
            }
        }

        // userB updates their own preferences — must not affect userA.
        given()
            .header("Authorization", "Bearer " + tokenB)
            .contentType(ContentType.JSON)
            .body("{\"emailEnabled\":false}")
        .when().patch("/api/notifications/preferences")
        .then().statusCode(200);

        NotificationPreferences a = preferencesRepository.findById(userIdA).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(a.isEmailEnabled()).isFalse(); // unchanged
        org.assertj.core.api.Assertions.assertThat(a.isInAppEnabled()).isTrue();

        NotificationPreferences b = preferencesRepository.findById(userIdB).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(b.isEmailEnabled()).isFalse();
        org.assertj.core.api.Assertions.assertThat(b.isInAppEnabled()).isTrue();
    }
}
