package com.ax.template.authblueprint.favoritesbookmarks;

import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;

/**
 * FAV-VALID-002 — quota enforcement.
 * Property override keeps the cap low so the test runs fast.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "favorites-bookmarks.max-favorites-per-user=2")
@Tag("FAVORITES")
class FavoriteQuotaTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        FavoritesTestSupport.useRandomPort(port);
    }

    @Test
    @Tag("FAV-VALID-002")
    void valid_002_quotaExceededReturns400() {
        String token = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("quota"), "MEMBER");

        post(token, "product", "p-1").statusCode(201);
        post(token, "product", "p-2").statusCode(201);

        // 3rd add over cap=2 → 400 FAVORITES_QUOTA_EXCEEDED.
        post(token, "product", "p-3")
            .statusCode(400)
            .body("code", Matchers.equalTo("FAVORITES_QUOTA_EXCEEDED"));
    }

    private io.restassured.response.ValidatableResponse post(String token, String entityType, String entityId) {
        return given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"" + entityType + "\",\"entityId\":\"" + entityId + "\"}")
        .when().post("/api/favorites")
        .then();
    }
}
