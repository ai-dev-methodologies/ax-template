package com.ax.template.authblueprint.favoritesbookmarks;

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
@Tag("FAVORITES")
class FavoriteComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        FavoritesTestSupport.useRandomPort(port);
    }

    // ─── CRUD family ─────────────────────────────────────────────────────────

    @Test
    @Tag("FAV-CRUD-001")
    void crud_001_addIsIdempotent() {
        String token = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("c1"), "MEMBER");
        String entityId = "prod-" + UUID.randomUUID();

        // First add → 201.
        given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"product\",\"entityId\":\"" + entityId + "\"}")
        .when().post("/api/favorites").then().statusCode(201);

        // Second add → 200 (idempotent).
        given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"product\",\"entityId\":\"" + entityId + "\"}")
        .when().post("/api/favorites").then().statusCode(200);

        // List shows exactly 1 row for this entity.
        given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/favorites?entityType=product")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(1));
    }

    @Test
    @Tag("FAV-CRUD-002")
    void crud_002_deleteIsIdempotent() {
        String token = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("c2"), "MEMBER");
        addFavorite(token, "product", "prod-c2");

        given().header("Authorization", "Bearer " + token)
        .when().delete("/api/favorites/product/prod-c2").then().statusCode(204);
        given().header("Authorization", "Bearer " + token)
        .when().delete("/api/favorites/product/prod-c2").then().statusCode(204);
    }

    @Test
    @Tag("FAV-CRUD-003")
    void crud_003_listOrderedNewestFirst() {
        String token = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("c3"), "MEMBER");
        addFavorite(token, "product", "p-c3-a");
        sleepMs(15);
        addFavorite(token, "product", "p-c3-b");
        sleepMs(15);
        addFavorite(token, "product", "p-c3-c");

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/favorites")
        .then().statusCode(200)
            .body("items[0].entityId", Matchers.equalTo("p-c3-c"))   // newest first
            .body("items[2].entityId", Matchers.equalTo("p-c3-a"));
    }

    // ─── QUERY family ────────────────────────────────────────────────────────

    @Test
    @Tag("FAV-QUERY-001")
    void query_001_checkReturnsBoolean() {
        String token = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("q1"), "MEMBER");

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/favorites/check/product/prod-q1")
        .then().statusCode(200).body("favorited", Matchers.equalTo(false));

        addFavorite(token, "product", "prod-q1");

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/favorites/check/product/prod-q1")
        .then().statusCode(200).body("favorited", Matchers.equalTo(true));
    }

    @Test
    @Tag("FAV-QUERY-002")
    void query_002_countReturnsGlobalAcrossUsers() {
        String tokenA = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("q2-a"), "MEMBER");
        String tokenB = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("q2-b"), "MEMBER");

        String entityId = "prod-q2-" + UUID.randomUUID();

        // Initial count = 0.
        given().header("Authorization", "Bearer " + tokenA)
        .when().get("/api/favorites/count/product/" + entityId)
        .then().statusCode(200).body("count", Matchers.equalTo(0));

        // Both users favorite the same entity → count = 2.
        addFavorite(tokenA, "product", entityId);
        addFavorite(tokenB, "product", entityId);
        given().header("Authorization", "Bearer " + tokenA)
        .when().get("/api/favorites/count/product/" + entityId)
        .then().statusCode(200).body("count", Matchers.equalTo(2));

        // A revokes → count = 1.
        given().header("Authorization", "Bearer " + tokenA)
        .when().delete("/api/favorites/product/" + entityId).then().statusCode(204);
        given().header("Authorization", "Bearer " + tokenA)
        .when().get("/api/favorites/count/product/" + entityId)
        .then().statusCode(200).body("count", Matchers.equalTo(1));
    }

    @Test
    @Tag("FAV-QUERY-003")
    void query_003_listFilteredByEntityType() {
        String token = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("q3"), "MEMBER");
        addFavorite(token, "product", "p-q3-1");
        addFavorite(token, "product", "p-q3-2");
        addFavorite(token, "article", "a-q3-1");

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/favorites?entityType=product")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(2));

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/favorites?entityType=article")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(1));

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/favorites")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(3));
    }

    // ─── AUTHZ family ────────────────────────────────────────────────────────

    @Test
    @Tag("FAV-AUTHZ-001")
    void authz_001_unauthenticatedReturns401() {
        given().contentType(ContentType.JSON).body("{}")
        .when().post("/api/favorites").then().statusCode(401);
        given().when().get("/api/favorites").then().statusCode(401);
        given().when().delete("/api/favorites/product/x").then().statusCode(401);
        given().when().get("/api/favorites/check/product/x").then().statusCode(401);
        given().when().get("/api/favorites/count/product/x").then().statusCode(401);
    }

    @Test
    @Tag("FAV-AUTHZ-002")
    void authz_002_listScopedToCaller() {
        String tokenA = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("az2-a"), "MEMBER");
        String tokenB = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("az2-b"), "MEMBER");
        addFavorite(tokenA, "product", "p-az2");
        addFavorite(tokenA, "product", "p-az2-b");

        given().header("Authorization", "Bearer " + tokenB)
        .when().get("/api/favorites")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(0));
    }

    @Test
    @Tag("FAV-AUTHZ-003")
    void authz_003_crossUserDeleteIsNoop() {
        String tokenA = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("az3-a"), "MEMBER");
        String tokenB = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("az3-b"), "MEMBER");
        addFavorite(tokenA, "product", "p-az3");

        // B's DELETE matches 0 rows (still 204 — idempotent contract).
        given().header("Authorization", "Bearer " + tokenB)
        .when().delete("/api/favorites/product/p-az3").then().statusCode(204);

        // A's row still present.
        given().header("Authorization", "Bearer " + tokenA)
        .when().get("/api/favorites/check/product/p-az3")
        .then().statusCode(200).body("favorited", Matchers.equalTo(true));
    }

    // ─── VALIDATION family ───────────────────────────────────────────────────

    @Test
    @Tag("FAV-VALID-001")
    void valid_001_blankFieldsReturn400() {
        String token = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("v1"), "MEMBER");

        given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"\",\"entityId\":\"x\"}")
        .when().post("/api/favorites").then().statusCode(400);

        given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"product\",\"entityId\":\"\"}")
        .when().post("/api/favorites").then().statusCode(400);
    }

    // FAV-VALID-002 covered by FavoriteQuotaTest (lowered cap via @TestPropertySource).

    @Test
    @Tag("FAV-VALID-003")
    void valid_003_oversizedNoteReturns400() {
        String token = FavoritesTestSupport.obtainToken(
            FavoritesTestSupport.freshEmail("v3"), "MEMBER");

        String longNote = "x".repeat(257);
        given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"product\",\"entityId\":\"p-v3\",\"note\":\"" + longNote + "\"}")
        .when().post("/api/favorites").then().statusCode(400);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void addFavorite(String token, String entityType, String entityId) {
        given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"" + entityType + "\",\"entityId\":\"" + entityId + "\"}")
        .when().post("/api/favorites")
        .then().statusCode(Matchers.anyOf(Matchers.equalTo(201), Matchers.equalTo(200)));
    }

    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
