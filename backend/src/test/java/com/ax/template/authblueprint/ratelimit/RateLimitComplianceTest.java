package com.ax.template.authblueprint.ratelimit;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box compliance verification for ratelimit-l0 (RATELIMIT-1..4).
 * Uses RestAssured against random port — portable, no MockMvc, no @WithMockUser.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitComplianceTest {

    @LocalServerPort int port;
    @Autowired RateLimitProperties properties;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    @Tag("RATELIMIT")
    @Tag("RATELIMIT-1")
    void ratelimit_1_rejectsBeyondMaxPerWindow() {
        String key = freshKey();
        int max = properties.maxPerWindow();

        for (int i = 0; i < max; i++) {
            Response r = given().header(properties.keyHeader(), key)
                .when().get("/api/ratelimit/ping");
            assertThat(r.statusCode())
                .as("request #%d of max=%d should still succeed", i + 1, max)
                .isEqualTo(200);
        }

        Response over = given().header(properties.keyHeader(), key)
            .when().get("/api/ratelimit/ping");
        assertThat(over.statusCode())
            .as("request #%d (over max=%d) must be 429", max + 1, max)
            .isEqualTo(429);
    }

    @Test
    @Tag("RATELIMIT")
    @Tag("RATELIMIT-2")
    void ratelimit_2_retryAfterHeaderOn429() {
        String key = freshKey();
        // Burn through the quota
        for (int i = 0; i < properties.maxPerWindow(); i++) {
            given().header(properties.keyHeader(), key)
                .when().get("/api/ratelimit/ping").then().statusCode(200);
        }
        Response over = given().header(properties.keyHeader(), key)
            .when().get("/api/ratelimit/ping");
        assertThat(over.statusCode()).isEqualTo(429);

        String retryAfter = over.getHeader("Retry-After");
        assertThat(retryAfter)
            .as("RFC 6585 §4 — 429 response MUST include Retry-After")
            .isNotNull()
            .isNotBlank();
        assertThat(Integer.parseInt(retryAfter))
            .as("Retry-After must be a non-negative delta-seconds integer")
            .isGreaterThanOrEqualTo(0);
    }

    @Test
    @Tag("RATELIMIT")
    @Tag("RATELIMIT-3")
    void ratelimit_3_clientKeysIsolated() {
        String keyA = freshKey();
        String keyB = freshKey();

        // Exhaust client A
        for (int i = 0; i < properties.maxPerWindow(); i++) {
            given().header(properties.keyHeader(), keyA)
                .when().get("/api/ratelimit/ping").then().statusCode(200);
        }
        given().header(properties.keyHeader(), keyA)
            .when().get("/api/ratelimit/ping").then().statusCode(429);

        // Client B must still be allowed — quotas are per-key
        given().header(properties.keyHeader(), keyB)
            .when().get("/api/ratelimit/ping").then().statusCode(200);
    }

    @Test
    @Tag("RATELIMIT")
    @Tag("RATELIMIT-4")
    void ratelimit_4_quotaResetsAfterWindow() throws InterruptedException {
        String key = freshKey();

        for (int i = 0; i < properties.maxPerWindow(); i++) {
            given().header(properties.keyHeader(), key)
                .when().get("/api/ratelimit/ping").then().statusCode(200);
        }
        given().header(properties.keyHeader(), key)
            .when().get("/api/ratelimit/ping").then().statusCode(429);

        // Caffeine schedules eviction lazily — wait a bit more than the window
        // and let next access trigger the expireAfterWrite cleanup.
        Thread.sleep(properties.windowMillis() + 500L);

        given().header(properties.keyHeader(), key)
            .when().get("/api/ratelimit/ping").then().statusCode(200);
    }

    /** Generate a fresh key per test so previous test buckets do not leak. */
    private String freshKey() {
        return "test-" + UUID.randomUUID();
    }
}
