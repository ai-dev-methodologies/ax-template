package com.ax.template.authblueprint.commentthread;

import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// BEFORE_CLASS (not AFTER_CLASS): forces a fresh context boot before this class so it
// cannot inherit an evicted (dead) Tomcat instance under the heavy per-domain aggregate
// (R22 ContextCache eviction flake — same mitigation as RateLimit/Billing/FeatureFlag).
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("COMMENT")
class CommentComplianceTest {

    @LocalServerPort int port;


    // ─── CRUD family ────────────────────────────────────────────────────────

    @Test
    @Tag("COMMENT-CRUD-001")
    void crud_001_postCreatesRootComment() {
        String token = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("c1"), "MEMBER");

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"post\",\"entityId\":\"p-c1\",\"body\":\"first\"}")
        .when().post("/api/comments")
        .then().statusCode(201)
            .body("body", Matchers.equalTo("first"))
            .body("status", Matchers.equalTo("ACTIVE"))
            .body("parentCommentId", Matchers.nullValue());
    }

    @Test
    @Tag("COMMENT-CRUD-002")
    void crud_002_editCapturesHistory() {
        String token = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("c2"), "MEMBER");
        String id = createComment(token, "post", "p-c2", "original", null);

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"body\":\"edited\"}")
        .when().put("/api/comments/" + id)
        .then().statusCode(200)
            .body("body", Matchers.equalTo("edited"))
            .body("updatedAt", Matchers.notNullValue());

        // Author can read history; pre-image preserved.
        given().header("Authorization", "Bearer " + token)
        .when().get("/api/comments/" + id + "/history")
        .then().statusCode(200)
            .body("edits.size()", Matchers.equalTo(1))
            .body("edits[0].previousBody", Matchers.equalTo("original"));
    }

    @Test
    @Tag("COMMENT-CRUD-003")
    void crud_003_softDeleteMasksBody() {
        String token = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("c3"), "MEMBER");
        String rootId = createComment(token, "post", "p-c3", "to be deleted", null);
        createComment(token, "post", "p-c3", "reply preserved", UUID.fromString(rootId));

        given().header("Authorization", "Bearer " + token)
        .when().delete("/api/comments/" + rootId)
        .then().statusCode(204);

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/comments/" + rootId)
        .then().statusCode(200)
            .body("status", Matchers.equalTo("DELETED"))
            .body("body", Matchers.equalTo("[deleted]"))
            .body("deletedAt", Matchers.notNullValue());

        // Reply preserved.
        given().header("Authorization", "Bearer " + token)
        .when().get("/api/comments/by-entity/post/p-c3")
        .then().statusCode(200)
            .body("totalElements", Matchers.equalTo(2));
    }

    // ─── THREAD family ──────────────────────────────────────────────────────

    @Test
    @Tag("COMMENT-THREAD-001")
    void thread_001_missingParentReturns400() {
        String token = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("t1"), "MEMBER");

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"post\",\"entityId\":\"p-t1\",\"body\":\"orphan\","
                  + "\"parentCommentId\":\"" + UUID.randomUUID() + "\"}")
        .when().post("/api/comments")
        .then().statusCode(400)
            .body("code", Matchers.equalTo("PARENT_COMMENT_NOT_FOUND"));
    }

    @Test
    @Tag("COMMENT-THREAD-002")
    void thread_002_byEntityFlatOrderedAscIncludesDeleted() {
        String token = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("t2"), "MEMBER");

        String root = createComment(token, "post", "p-t2", "root", null);
        sleepMs(15);
        createComment(token, "post", "p-t2", "reply-1", UUID.fromString(root));
        sleepMs(15);
        createComment(token, "post", "p-t2", "reply-2", UUID.fromString(root));

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/comments/by-entity/post/p-t2")
        .then().statusCode(200)
            .body("totalElements", Matchers.equalTo(3))
            .body("items[0].body", Matchers.equalTo("root"))
            .body("items[1].body", Matchers.equalTo("reply-1"))
            .body("items[2].body", Matchers.equalTo("reply-2"));
    }

    @Test
    @Tag("COMMENT-THREAD-003")
    void thread_003_crossEntityReplyReturns400() {
        String token = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("t3"), "MEMBER");
        String rootOnPost = createComment(token, "post", "p-t3", "root on post", null);

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"entityType\":\"article\",\"entityId\":\"a-t3\",\"body\":\"reply\","
                  + "\"parentCommentId\":\"" + rootOnPost + "\"}")
        .when().post("/api/comments")
        .then().statusCode(400)
            .body("code", Matchers.equalTo("CROSS_ENTITY_REPLY"));
    }

    // ─── AUTHZ family ───────────────────────────────────────────────────────

    @Test
    @Tag("COMMENT-AUTHZ-001")
    void authz_001_unauthenticatedReturns401() {
        given().contentType(ContentType.JSON).body("{}")
        .when().post("/api/comments").then().statusCode(401);
        given().when().get("/api/comments/" + UUID.randomUUID()).then().statusCode(401);
        given().when().put("/api/comments/" + UUID.randomUUID()).then().statusCode(401);
        given().when().delete("/api/comments/" + UUID.randomUUID()).then().statusCode(401);
        given().when().get("/api/comments/by-entity/post/x").then().statusCode(401);
        given().when().get("/api/comments/" + UUID.randomUUID() + "/history").then().statusCode(401);
    }

    @Test
    @Tag("COMMENT-AUTHZ-002")
    void authz_002_nonAuthorCannotEditEvenIfAdmin() {
        String authorToken = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("az2-author"), "MEMBER");
        String otherToken = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("az2-other"), "MEMBER");
        String adminToken = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("az2-admin"), "ADMIN");

        String id = createComment(authorToken, "post", "p-az2", "original", null);

        given().header("Authorization", "Bearer " + otherToken).contentType(ContentType.JSON)
            .body("{\"body\":\"hijacked\"}")
        .when().put("/api/comments/" + id)
        .then().statusCode(403)
            .body("code", Matchers.equalTo("EDIT_FORBIDDEN"));

        // ADMIN cannot rewrite either.
        given().header("Authorization", "Bearer " + adminToken).contentType(ContentType.JSON)
            .body("{\"body\":\"admin override\"}")
        .when().put("/api/comments/" + id)
        .then().statusCode(403)
            .body("code", Matchers.equalTo("EDIT_FORBIDDEN"));
    }

    @Test
    @Tag("COMMENT-AUTHZ-003")
    void authz_003_deleteAllowedToAuthorAndAdmin() {
        String authorToken = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("az3-author"), "MEMBER");
        String otherToken = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("az3-other"), "MEMBER");
        String adminToken = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("az3-admin"), "ADMIN");
        String adminUserId = given().header("Authorization", "Bearer " + adminToken)
            .when().get("/api/auth/me").then().extract().path("userId");

        String id1 = createComment(authorToken, "post", "p-az3", "to be admin-deleted", null);
        String id2 = createComment(authorToken, "post", "p-az3", "to be self-deleted", null);

        // Non-author non-admin → 403.
        given().header("Authorization", "Bearer " + otherToken)
        .when().delete("/api/comments/" + id1).then().statusCode(403);

        // Admin → 204 + deletedByUserId = admin.
        given().header("Authorization", "Bearer " + adminToken)
        .when().delete("/api/comments/" + id1).then().statusCode(204);
        given().header("Authorization", "Bearer " + authorToken)
        .when().get("/api/comments/" + id1)
        .then().body("deletedByUserId", Matchers.equalTo(adminUserId));

        // Self-delete → 204.
        given().header("Authorization", "Bearer " + authorToken)
        .when().delete("/api/comments/" + id2).then().statusCode(204);
    }

    // ─── HISTORY family ─────────────────────────────────────────────────────

    @Test
    @Tag("COMMENT-HISTORY-001")
    void history_001_editCapturesPreviousBody() {
        String token = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("h1"), "MEMBER");
        String id = createComment(token, "post", "p-h1", "v1", null);

        edit(token, id, "v2");
        edit(token, id, "v3");

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/comments/" + id + "/history")
        .then().statusCode(200)
            .body("edits.size()", Matchers.equalTo(2))
            .body("edits[0].previousBody", Matchers.equalTo("v1"))
            .body("edits[1].previousBody", Matchers.equalTo("v2"));
    }

    @Test
    @Tag("COMMENT-HISTORY-002")
    void history_002_nonAuthorReceives404() {
        String authorToken = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("h2-a"), "MEMBER");
        String otherToken = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("h2-o"), "MEMBER");
        String adminToken = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("h2-adm"), "ADMIN");

        String id = createComment(authorToken, "post", "p-h2", "v1", null);
        edit(authorToken, id, "v2");

        // Other → 404 (IDOR-safe).
        given().header("Authorization", "Bearer " + otherToken)
        .when().get("/api/comments/" + id + "/history")
        .then().statusCode(404);

        // Admin → 200.
        given().header("Authorization", "Bearer " + adminToken)
        .when().get("/api/comments/" + id + "/history")
        .then().statusCode(200);
    }

    @Test
    @Tag("COMMENT-HISTORY-003")
    void history_003_preservedAcrossDelete() {
        String token = CommentThreadTestSupport.obtainToken(
            CommentThreadTestSupport.freshEmail("h3"), "MEMBER");
        String id = createComment(token, "post", "p-h3", "v1", null);
        edit(token, id, "v2");

        given().header("Authorization", "Bearer " + token)
        .when().delete("/api/comments/" + id).then().statusCode(204);

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/comments/" + id + "/history")
        .then().statusCode(200)
            .body("edits.size()", Matchers.equalTo(1))
            .body("edits[0].previousBody", Matchers.equalTo("v1"));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String createComment(String token, String entityType, String entityId,
                                  String body, UUID parentId) {
        StringBuilder b = new StringBuilder("{\"entityType\":\"").append(entityType)
            .append("\",\"entityId\":\"").append(entityId)
            .append("\",\"body\":\"").append(body).append('"');
        if (parentId != null) {
            b.append(",\"parentCommentId\":\"").append(parentId).append('"');
        }
        b.append('}');
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(b.toString())
        .when().post("/api/comments")
        .then().statusCode(201)
            .extract().path("id");
    }

    private void edit(String token, String id, String body) {
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"body\":\"" + body + "\"}")
        .when().put("/api/comments/" + id).then().statusCode(200);
    }

    private static void sleepMs(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }
}
