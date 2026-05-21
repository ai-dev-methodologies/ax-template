package com.ax.template.authblueprint.tagcategorization;

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

/**
 * Compliance tests for the tag-categorization domain (R32). RestAssured black-box.
 * Unit coverage for TAG-CRUD-001 / TAG-CRUD-003 / TAG-HIER-001 lives in
 * {@link TagSluggerTest} + {@link TagEntityShapeTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("TAGGING")
class TagComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        TagCategorizationTestSupport.useRandomPort(port);
    }

    // ─── CRUD family ─────────────────────────────────────────────────────────

    @Test
    @Tag("TAG-CRUD-001")
    void crud_001_postAutoGeneratesSlug() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("crud1"), "ADMIN");

        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"New Product Line\"}")
        .when().post("/api/tags")
        .then()
            .statusCode(201)
            .body("slug", Matchers.equalTo("new-product-line"))
            .body("name", Matchers.equalTo("New Product Line"));

        // Duplicate slug rejected.
        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"new-product-line\"}")
        .when().post("/api/tags")
        .then()
            .statusCode(400)
            .body("code", Matchers.equalTo("DUPLICATE_SLUG"));
    }

    @Test
    @Tag("TAG-CRUD-002")
    void crud_002_listScopedByParent() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("crud2"), "ADMIN");

        UUID parent = createTag(admin, "crud2 parent " + UUID.randomUUID(), null);
        createTag(admin, "crud2 child " + UUID.randomUUID() + " a", parent);
        createTag(admin, "crud2 child " + UUID.randomUUID() + " b", parent);

        // Top-level (parent=null) — must include the parent and any other top-level tags
        // (from other tests) but MUST NOT include the children.
        given()
            .header("Authorization", "Bearer " + admin)
        .when().get("/api/tags")
        .then()
            .statusCode(200)
            .body("items.id", Matchers.hasItem(parent.toString()));

        // Children-only listing.
        given()
            .header("Authorization", "Bearer " + admin)
        .when().get("/api/tags?parent=" + parent)
        .then()
            .statusCode(200)
            .body("totalElements", Matchers.equalTo(2));
    }

    @Test
    @Tag("TAG-CRUD-003")
    void crud_003_putRenamesButSlugStable() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("crud3"), "ADMIN");

        UUID tagId = createTag(admin, "Original Name", null);
        String originalSlug = given()
            .header("Authorization", "Bearer " + admin)
        .when().get("/api/tags/" + tagId)
        .then().extract().path("slug");

        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Renamed\",\"color\":\"#FF00FF\"}")
        .when().put("/api/tags/" + tagId)
        .then()
            .statusCode(200)
            .body("name", Matchers.equalTo("Renamed"))
            .body("color", Matchers.equalTo("#FF00FF"))
            .body("slug", Matchers.equalTo(originalSlug));
    }

    @Test
    @Tag("TAG-CRUD-004")
    void crud_004_deleteCascadesAttachments() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("crud4-admin"), "ADMIN");
        String member = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("crud4-mem"), "MEMBER");

        UUID tagId = createTag(admin, "crud4 " + UUID.randomUUID(), null);
        attach(member, tagId, "product", "prod-1");
        attach(member, tagId, "product", "prod-2");

        // Delete tag.
        given()
            .header("Authorization", "Bearer " + admin)
        .when().delete("/api/tags/" + tagId)
        .then().statusCode(204);

        // Both entities have no tags.
        given()
            .header("Authorization", "Bearer " + member)
        .when().get("/api/tags/by-entity/product/prod-1")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(0));
        given()
            .header("Authorization", "Bearer " + member)
        .when().get("/api/tags/by-entity/product/prod-2")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(0));
    }

    // ─── ATTACHMENT family ───────────────────────────────────────────────────

    @Test
    @Tag("TAG-ATTACH-001")
    void attach_001_isIdempotent() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("att1-a"), "ADMIN");
        String member = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("att1-m"), "MEMBER");

        UUID tagId = createTag(admin, "att1 " + UUID.randomUUID(), null);

        // First attach → 201.
        given()
            .header("Authorization", "Bearer " + member).contentType(ContentType.JSON)
            .body("{\"entityType\":\"product\",\"entityId\":\"p-att1\"}")
        .when().post("/api/tags/" + tagId + "/attach")
        .then().statusCode(201);

        // Second attach with same pair → 200, NOT 409.
        given()
            .header("Authorization", "Bearer " + member).contentType(ContentType.JSON)
            .body("{\"entityType\":\"product\",\"entityId\":\"p-att1\"}")
        .when().post("/api/tags/" + tagId + "/attach")
        .then().statusCode(200);

        // by-entity returns exactly one tag (no duplicate).
        given()
            .header("Authorization", "Bearer " + member)
        .when().get("/api/tags/by-entity/product/p-att1")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(1));
    }

    @Test
    @Tag("TAG-ATTACH-002")
    void attach_002_detachIsIdempotent() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("att2-a"), "ADMIN");
        String member = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("att2-m"), "MEMBER");

        UUID tagId = createTag(admin, "att2 " + UUID.randomUUID(), null);
        attach(member, tagId, "product", "p-att2");

        // First detach → 204.
        given()
            .header("Authorization", "Bearer " + member)
        .when().delete("/api/tags/" + tagId + "/attach/product/p-att2")
        .then().statusCode(204);

        // Second detach → still 204 (idempotent per HTTP RFC 9110).
        given()
            .header("Authorization", "Bearer " + member)
        .when().delete("/api/tags/" + tagId + "/attach/product/p-att2")
        .then().statusCode(204);
    }

    @Test
    @Tag("TAG-ATTACH-003")
    void attach_003_byEntityReturnsOrderedTagsAndEmptyForUnknown() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("att3-a"), "ADMIN");
        String member = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("att3-m"), "MEMBER");

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        UUID tagA = createTag(admin, "att3 zebra " + suffix, null);
        UUID tagB = createTag(admin, "att3 apple " + suffix, null);

        attach(member, tagA, "product", "p-att3");
        attach(member, tagB, "product", "p-att3");

        // By name ASC: "att3 apple <suffix>" before "att3 zebra <suffix>".
        given()
            .header("Authorization", "Bearer " + member)
        .when().get("/api/tags/by-entity/product/p-att3")
        .then()
            .statusCode(200)
            .body("totalElements", Matchers.equalTo(2))
            .body("items[0].id", Matchers.equalTo(tagB.toString()))
            .body("items[1].id", Matchers.equalTo(tagA.toString()));

        // Unknown entity → 200 empty.
        given()
            .header("Authorization", "Bearer " + member)
        .when().get("/api/tags/by-entity/never-heard-of/whatever")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(0));
    }

    // ─── HIERARCHY family ────────────────────────────────────────────────────

    @Test
    @Tag("TAG-HIER-002")
    void hier_002_missingParentReturns400() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("hier2"), "ADMIN");

        given()
            .header("Authorization", "Bearer " + admin)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"orphan child\",\"parentTagId\":\"" + UUID.randomUUID() + "\"}")
        .when().post("/api/tags")
        .then()
            .statusCode(400)
            .body("code", Matchers.equalTo("PARENT_NOT_FOUND"));
    }

    @Test
    @Tag("TAG-HIER-003")
    void hier_003_deleteWithChildrenIs409UnlessCascade() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("hier3"), "ADMIN");
        String member = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("hier3-m"), "MEMBER");

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        UUID parent = createTag(admin, "hier3 parent " + suffix, null);
        UUID child = createTag(admin, "hier3 child " + suffix, parent);
        attach(member, child, "product", "p-hier3");

        // Without cascade → 409.
        given()
            .header("Authorization", "Bearer " + admin)
        .when().delete("/api/tags/" + parent)
        .then()
            .statusCode(409)
            .body("code", Matchers.equalTo("TAG_HAS_CHILDREN"));

        // With cascade=true → 204, both gone, attachment gone.
        given()
            .header("Authorization", "Bearer " + admin)
        .when().delete("/api/tags/" + parent + "?cascade=true")
        .then().statusCode(204);

        given()
            .header("Authorization", "Bearer " + admin)
        .when().get("/api/tags/" + parent)
        .then().statusCode(404);
        given()
            .header("Authorization", "Bearer " + admin)
        .when().get("/api/tags/" + child)
        .then().statusCode(404);
        given()
            .header("Authorization", "Bearer " + member)
        .when().get("/api/tags/by-entity/product/p-hier3")
        .then().statusCode(200).body("totalElements", Matchers.equalTo(0));
    }

    // ─── AUTHZ family ────────────────────────────────────────────────────────

    @Test
    @Tag("TAG-AUTHZ-001")
    void authz_001_unauthenticatedReturns401() {
        given().contentType(ContentType.JSON).body("{\"name\":\"x\"}")
        .when().post("/api/tags").then().statusCode(401);

        given().when().get("/api/tags").then().statusCode(401);
        given().when().get("/api/tags/" + UUID.randomUUID()).then().statusCode(401);
        given().when().delete("/api/tags/" + UUID.randomUUID()).then().statusCode(401);
    }

    @Test
    @Tag("TAG-AUTHZ-002")
    void authz_002_memberCannotMutateButCanAttach() {
        String admin = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("authz2-a"), "ADMIN");
        String member = TagCategorizationTestSupport.obtainToken(
            TagCategorizationTestSupport.freshEmail("authz2-m"), "MEMBER");

        // MEMBER POST /api/tags → 403.
        given()
            .header("Authorization", "Bearer " + member).contentType(ContentType.JSON)
            .body("{\"name\":\"authz2 should fail\"}")
        .when().post("/api/tags")
        .then().statusCode(403);

        // ADMIN POST → 201.
        UUID tagId = createTag(admin, "authz2 ok " + UUID.randomUUID(), null);

        // MEMBER PUT → 403.
        given()
            .header("Authorization", "Bearer " + member).contentType(ContentType.JSON)
            .body("{\"name\":\"x\"}")
        .when().put("/api/tags/" + tagId)
        .then().statusCode(403);

        // MEMBER DELETE → 403.
        given()
            .header("Authorization", "Bearer " + member)
        .when().delete("/api/tags/" + tagId)
        .then().statusCode(403);

        // MEMBER attach → 201 (attach is not admin-gated).
        given()
            .header("Authorization", "Bearer " + member).contentType(ContentType.JSON)
            .body("{\"entityType\":\"product\",\"entityId\":\"authz2-allowed\"}")
        .when().post("/api/tags/" + tagId + "/attach")
        .then().statusCode(201);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID createTag(String adminToken, String name, UUID parentId) {
        String body = (parentId == null)
            ? "{\"name\":\"" + name + "\"}"
            : "{\"name\":\"" + name + "\",\"parentTagId\":\"" + parentId + "\"}";
        return UUID.fromString(given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body(body)
        .when().post("/api/tags")
        .then().statusCode(201)
            .extract().path("id"));
    }

    private void attach(String token, UUID tagId, String entityType, String entityId) {
        given()
            .header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"" + entityType + "\",\"entityId\":\"" + entityId + "\"}")
        .when().post("/api/tags/" + tagId + "/attach")
        .then().statusCode(Matchers.anyOf(Matchers.equalTo(201), Matchers.equalTo(200)));
    }

}
