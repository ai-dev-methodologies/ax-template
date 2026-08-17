package com.ax.template.authblueprint.notification;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * PREFERENCES family (2 items): NOTIF-PREF-001, NOTIF-PREF-002.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationPreferencesTest {

    @LocalServerPort int port;

    @Autowired NotificationPreferencesRepository repository;

    private String token;
    private String userId;

    @BeforeEach
    void setup() {
        token = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("prefs"), "MEMBER");
        userId = NotificationTestSupport.resolveCallerUserId(token);
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-PREF-001")
    void pref_001_returnsDefaultsWhenNoRowExists() {
        // No row pre-seeded — defaults must be returned and no row created.
        assertThat(repository.findById(userId)).isEmpty();

        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().get("/api/notifications/preferences")
        .then().statusCode(200)
            .body("inAppEnabled", equalTo(true))
            .body("emailEnabled", equalTo(true));

        // GET must NOT lazily insert a row (NOTIF-PREF-001).
        assertThat(repository.findById(userId))
            .as("GET /preferences must not create a preferences row when none exists")
            .isEmpty();
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-PREF-001")
    void pref_001_returnsSavedValuesWhenPresent() {
        repository.save(new NotificationPreferences(userId, false, true));

        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().get("/api/notifications/preferences")
        .then().statusCode(200)
            .body("inAppEnabled", equalTo(false))
            .body("emailEnabled", equalTo(true));
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-PREF-002")
    void pref_002_partialUpdateMergesFields() {
        // First call: only emailEnabled provided.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"emailEnabled\":false}")
        .when().patch("/api/notifications/preferences")
        .then().statusCode(200)
            .body("inAppEnabled", equalTo(true))     // default kept
            .body("emailEnabled", equalTo(false));   // updated

        // Second call: both fields.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"inAppEnabled\":false,\"emailEnabled\":true}")
        .when().patch("/api/notifications/preferences")
        .then().statusCode(200)
            .body("inAppEnabled", equalTo(false))
            .body("emailEnabled", equalTo(true));

        // Persisted row reflects final state.
        NotificationPreferences saved = repository.findById(userId).orElseThrow();
        assertThat(saved.isInAppEnabled()).isFalse();
        assertThat(saved.isEmailEnabled()).isTrue();
    }
}
