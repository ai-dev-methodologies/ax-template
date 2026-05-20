package com.ax.template.authblueprint.filestorage;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUTHZ family (3 items): FILE-AUTHZ-001, FILE-AUTHZ-002, FILE-AUTHZ-003.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileStorageAuthZTest {

    @LocalServerPort int port;

    @Autowired StoredFileRepository repository;

    @BeforeEach
    void setup() {
        FileStorageTestSupport.useRandomPort(port);
    }

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-AUTHZ-001")
    void authz_001_unauthenticatedRequestsAre401() {
        // POST upload — no JWT
        given().accept(ContentType.JSON).multiPart("file", "a.txt", new byte[]{1, 2, 3})
            .when().post("/api/files")
            .then().statusCode(401);

        // GET metadata — no JWT
        given().accept(ContentType.JSON)
            .when().get("/api/files/" + UUID.randomUUID())
            .then().statusCode(401);

        // GET download — no JWT
        given().accept(ContentType.JSON)
            .when().get("/api/files/" + UUID.randomUUID() + "/download")
            .then().statusCode(401);

        // DELETE — no JWT
        given().accept(ContentType.JSON)
            .when().delete("/api/files/" + UUID.randomUUID())
            .then().statusCode(401);
    }

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-AUTHZ-002")
    void authz_002_crossUserAccessReturns404() {
        String tokenA = FileStorageTestSupport.obtainToken(
            FileStorageTestSupport.freshEmail("file-authz-a"), "MEMBER");
        String tokenB = FileStorageTestSupport.obtainToken(
            FileStorageTestSupport.freshEmail("file-authz-b"), "MEMBER");

        // UserA uploads a file.
        String fileId = given()
            .header("Authorization", "Bearer " + tokenA)
            .multiPart("file", "doc.pdf", "PDF-CONTENT".getBytes(), "application/pdf")
        .when().post("/api/files")
        .then().statusCode(201)
            .extract().path("id");

        UUID id = UUID.fromString(fileId);

        // UserB attempts to read userA's file metadata → 404 (not 403).
        given()
            .header("Authorization", "Bearer " + tokenB)
            .accept(ContentType.JSON)
        .when().get("/api/files/" + id)
        .then().statusCode(404);

        // UserB attempts to download → 404.
        given()
            .header("Authorization", "Bearer " + tokenB)
        .when().get("/api/files/" + id + "/download")
        .then().statusCode(404);

        // UserB attempts to delete → 404.
        given()
            .header("Authorization", "Bearer " + tokenB)
        .when().delete("/api/files/" + id)
        .then().statusCode(404);

        // Owner still has the file intact.
        StoredFile stillThere = repository.findById(id).orElseThrow();
        assertThat(stillThere.isDeleted()).isFalse();
    }

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-AUTHZ-003")
    void authz_003_deleteOwnerOnly() {
        String tokenA = FileStorageTestSupport.obtainToken(
            FileStorageTestSupport.freshEmail("file-del-a"), "MEMBER");
        String tokenB = FileStorageTestSupport.obtainToken(
            FileStorageTestSupport.freshEmail("file-del-b"), "MEMBER");

        String fileId = given()
            .header("Authorization", "Bearer " + tokenA)
            .multiPart("file", "doc.pdf", "BYTES".getBytes(), "application/pdf")
        .when().post("/api/files")
        .then().statusCode(201)
            .extract().path("id");

        UUID id = UUID.fromString(fileId);

        // FILE-AUTHZ-003 — userB's DELETE must NOT succeed. Spec accepts 403
        // OR 404 (idiomatic when row hydration is filtered by owner first —
        // see notification authz_002 pattern); both prevent the unauthorized
        // deletion. We assert non-2xx + verify the row is still there.
        given()
            .header("Authorization", "Bearer " + tokenB)
        .when().delete("/api/files/" + id)
        .then().statusCode(org.hamcrest.Matchers.anyOf(
            org.hamcrest.Matchers.equalTo(403),
            org.hamcrest.Matchers.equalTo(404)));

        StoredFile stillThere = repository.findById(id).orElseThrow();
        assertThat(stillThere.isDeleted())
            .as("FILE-AUTHZ-003 — non-owner DELETE must NOT soft-delete the row")
            .isFalse();

        // Owner can delete their own.
        given()
            .header("Authorization", "Bearer " + tokenA)
        .when().delete("/api/files/" + id)
        .then().statusCode(204);

        StoredFile afterOwnerDelete = repository.findById(id).orElseThrow();
        assertThat(afterOwnerDelete.isDeleted()).isTrue();
    }
}
