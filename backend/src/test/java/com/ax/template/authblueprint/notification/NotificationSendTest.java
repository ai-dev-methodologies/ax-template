package com.ax.template.authblueprint.notification;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEND family (2 items): NOTIF-SEND-001, NOTIF-SEND-002.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(NotificationSendTest.SendTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationSendTest {

    @LocalServerPort int port;

    @Autowired NotificationRepository repository;
    @Autowired NotificationPreferencesRepository preferencesRepository;
    @Autowired RecordingEmailChannel email;

    @BeforeEach
    void setup() {
        NotificationTestSupport.useRandomPort(port);
        email.reset();
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-SEND-001")
    void send_001_adminCanSendAndRowIsPersisted() {
        String adminToken = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("send-admin"), "ADMIN");
        String recipientToken = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("send-recipient"), "MEMBER");
        String recipientId = NotificationTestSupport.resolveCallerUserId(recipientToken);

        long before = repository.count();

        UUID notifId = UUID.fromString(
            given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("{"
                    + "\"recipientUserId\":\"" + recipientId + "\","
                    + "\"type\":\"WELCOME\","
                    + "\"title\":\"Welcome\","
                    + "\"body\":\"Welcome to ax-template\""
                    + "}")
            .when().post("/api/notifications")
            .then().statusCode(201)
                .body("type", org.hamcrest.Matchers.equalTo("WELCOME"))
                .body("title", org.hamcrest.Matchers.equalTo("Welcome"))
                .body("status", org.hamcrest.Matchers.equalTo("UNREAD"))
                .extract().path("id"));

        assertThat(repository.count() - before)
            .as("send must persist exactly one row")
            .isEqualTo(1);

        Notification saved = repository.findById(notifId).orElseThrow();
        assertThat(saved.getRecipientUserId()).isEqualTo(recipientId);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        assertThat(saved.getType()).isEqualTo("WELCOME");
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-SEND-001")
    void send_001_nonAdminGets403() {
        String memberToken = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("send-member"), "MEMBER");

        given()
            .header("Authorization", "Bearer " + memberToken)
            .contentType(ContentType.JSON)
            .body("{\"recipientUserId\":\"" + UUID.randomUUID() + "\","
                + "\"type\":\"X\",\"title\":\"X\"}")
        .when().post("/api/notifications")
        .then().statusCode(403);
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-SEND-002")
    void send_002_emailChannelInvokedWhenPreferenceEnabled() {
        String adminToken = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("send2-admin"), "ADMIN");
        String recipientToken = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("send2-rcv"), "MEMBER");
        String recipientId = NotificationTestSupport.resolveCallerUserId(recipientToken);

        // Recipient has email enabled (default).
        // Send a notification.
        given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body("{"
                + "\"recipientUserId\":\"" + recipientId + "\","
                + "\"type\":\"INFO\","
                + "\"title\":\"Hi\""
                + "}")
        .when().post("/api/notifications")
        .then().statusCode(201);

        assertThat(email.deliveries())
            .as("email channel must be invoked once when preference enabled (NOTIF-SEND-002)")
            .isEqualTo(1);
    }

    @Test
    @Tag("NOTIFICATION")
    @Tag("NOTIF-SEND-002")
    void send_002_channelFailureIsNonBlocking() {
        String adminToken = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("send2f-admin"), "ADMIN");
        String recipientToken = NotificationTestSupport.obtainToken(
            NotificationTestSupport.freshEmail("send2f-rcv"), "MEMBER");
        String recipientId = NotificationTestSupport.resolveCallerUserId(recipientToken);

        email.setNextCallThrows(true);
        long before = repository.count();

        // POST still returns 201 even though the email channel throws.
        given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body("{"
                + "\"recipientUserId\":\"" + recipientId + "\","
                + "\"type\":\"INFO\","
                + "\"title\":\"Hi\""
                + "}")
        .when().post("/api/notifications")
        .then().statusCode(201);

        assertThat(repository.count() - before)
            .as("notification row must remain persisted despite channel failure")
            .isEqualTo(1);
    }

    @TestConfiguration
    static class SendTestConfig {
        @Bean
        @Primary
        RecordingEmailChannel recordingEmailChannel() {
            return new RecordingEmailChannel();
        }
    }

    @TestComponent
    static class RecordingEmailChannel implements NotificationChannel {
        private final AtomicInteger calls = new AtomicInteger();
        private volatile boolean nextCallThrows = false;

        public void reset() { calls.set(0); nextCallThrows = false; }
        public int deliveries() { return calls.get(); }
        public void setNextCallThrows(boolean v) { this.nextCallThrows = v; }

        @Override public String id() { return "email"; }

        @Override public void deliver(Notification notification) {
            calls.incrementAndGet();
            if (nextCallThrows) {
                nextCallThrows = false;
                throw new RuntimeException("simulated email gateway failure");
            }
        }

        @Override public boolean enabledFor(NotificationPreferences prefs) {
            return prefs != null && prefs.isEmailEnabled();
        }
    }
}
