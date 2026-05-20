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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * LIST + READ + DISMISS families (4 items):
 *   NOTIF-LIST-001, NOTIF-LIST-002, NOTIF-READ-001, NOTIF-DISMISS-001.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationListReadDismissTest {

    @LocalServerPort int port;

    @Autowired NotificationRepository repository;

    private String token;
    private String userId;

    @BeforeEach
    void setup() {
        NotificationTestSupport.useRandomPort(port);
        token = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("list-read"), "MEMBER");
        userId = NotificationTestSupport.resolveCallerUserId(token);
    }

    private void seed(int count, NotificationStatus status) {
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            repository.save(Notification.builder()
                .id(UUID.randomUUID())
                .recipientUserId(userId)
                .type("SAMPLE")
                .title("title-" + i)
                .body("body-" + i)
                .status(status)
                .createdAt(now.minusSeconds(i))
                .updatedAt(now.minusSeconds(i))
                .build());
        }
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-LIST-001")
    void list_001_paginationDefaultsAndMaxSize() {
        seed(25, NotificationStatus.UNREAD);

        // Default page size 20.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().get("/api/notifications")
        .then().statusCode(200)
            .body("content.size()", equalTo(20))
            .body("totalElements", equalTo(25));

        // Page 1 size 5 -> 5 items (the second page of a 5-item-per-page paging).
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .queryParam("page", 1).queryParam("size", 5)
        .when().get("/api/notifications")
        .then().statusCode(200)
            .body("content.size()", equalTo(5))
            .body("totalElements", equalTo(25));

        // Size > 100 must be rejected.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .queryParam("size", 200)
        .when().get("/api/notifications")
        .then().statusCode(400);
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-LIST-002")
    void list_002_filterByStatusAndUnreadCountHeader() {
        seed(5, NotificationStatus.UNREAD);
        seed(3, NotificationStatus.READ);

        // UNREAD only.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .queryParam("status", "UNREAD")
        .when().get("/api/notifications")
        .then().statusCode(200)
            .header("X-Unread-Count", "5")
            .body("totalElements", equalTo(5));

        // READ only.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .queryParam("status", "READ")
        .when().get("/api/notifications")
        .then().statusCode(200)
            .header("X-Unread-Count", "5")
            .body("totalElements", equalTo(3));

        // ALL.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().get("/api/notifications")
        .then().statusCode(200)
            .header("X-Unread-Count", "5")
            .body("totalElements", equalTo(8));
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-READ-001")
    void read_001_markRead_isIdempotent() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        repository.save(Notification.builder()
            .id(id)
            .recipientUserId(userId)
            .type("X")
            .title("t")
            .status(NotificationStatus.UNREAD)
            .createdAt(now)
            .updatedAt(now)
            .build());

        // First call -> 200, READ.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().patch("/api/notifications/" + id + "/read")
        .then().statusCode(200)
            .body("status", equalTo("READ"));

        // Idempotent: second call -> still 200, still READ.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().patch("/api/notifications/" + id + "/read")
        .then().statusCode(200)
            .body("status", equalTo("READ"));

        Notification persisted = repository.findById(id).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.READ);
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-DISMISS-001")
    void dismiss_001_softDeleteExcludesFromSubsequentQueries() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        repository.save(Notification.builder()
            .id(id)
            .recipientUserId(userId)
            .type("X")
            .title("dismiss-me")
            .status(NotificationStatus.UNREAD)
            .createdAt(now)
            .updatedAt(now)
            .build());

        // DELETE -> 204
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().delete("/api/notifications/" + id)
        .then().statusCode(204);

        // Subsequent GET single -> 404
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().get("/api/notifications/" + id)
        .then().statusCode(404);

        // GET list filtered to this user -> notif must NOT appear.
        given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().get("/api/notifications")
        .then().statusCode(200)
            .body("content.findAll { it.id == '" + id + "' }.size()", equalTo(0));

        // Underlying row is still in the table but with deleted=true.
        Notification stored = repository.findById(id).orElseThrow();
        assertThat(stored.isDeleted()).isTrue();
    }
}
