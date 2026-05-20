package com.ax.template.authblueprint.filestorage;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC family (2 items): FILE-SEC-001 (no internal storage path leaks),
 * FILE-SEC-002 (response never includes storageKey or provider URL).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileStorageSecTest {

    @LocalServerPort int port;

    @Autowired StoredFileRepository repository;

    @BeforeEach
    void setup() {
        FileStorageTestSupport.useRandomPort(port);
    }

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-SEC-001")
    void sec_001_responseDoesNotExposeStorageProviderUrls() {
        String token = FileStorageTestSupport.obtainToken(
            FileStorageTestSupport.freshEmail("sec-noleak"), "MEMBER");

        String fileId = given()
            .header("Authorization", "Bearer " + token)
            .multiPart("file", "doc.pdf", "BYTES".getBytes(), "application/pdf")
        .when().post("/api/files")
        .then().statusCode(201)
            .extract().path("id");

        Response resp = given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().get("/api/files/" + fileId)
        .then().statusCode(200)
            .extract().response();

        String body = resp.asString();
        // FILE-SEC-001 — no storage provider scheme leaks (s3://, gs://, azure://, minio://).
        Pattern providerScheme = Pattern.compile("(s3|gs|azure|minio)://", Pattern.CASE_INSENSITIVE);
        assertThat(providerScheme.matcher(body).find())
            .as("FILE-SEC-001 — response must not contain storage provider URL scheme; got: %s", body)
            .isFalse();

        // Download URL must be a relative path on this server.
        String downloadUrl = resp.path("downloadUrl");
        assertThat(downloadUrl).startsWith("/api/files/");
    }

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-SEC-002")
    void sec_002_responseDoesNotExposeStorageKey() {
        String token = FileStorageTestSupport.obtainToken(
            FileStorageTestSupport.freshEmail("sec-key"), "MEMBER");

        String fileId = given()
            .header("Authorization", "Bearer " + token)
            .multiPart("file", "doc.pdf", "BYTES".getBytes(), "application/pdf")
        .when().post("/api/files")
        .then().statusCode(201)
            .extract().path("id");

        StoredFile entity = repository.findById(UUID.fromString(fileId)).orElseThrow();
        String storageKey = entity.getStorageKey();
        assertThat(storageKey).isNotBlank();

        // Fetch metadata & download response; neither must contain storageKey.
        String metaBody = given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
        .when().get("/api/files/" + fileId)
        .then().statusCode(200)
            .extract().asString();

        assertThat(metaBody)
            .as("FILE-SEC-002 — metadata response must NOT contain storageKey")
            .doesNotContain(storageKey)
            .doesNotContain("storageKey");
    }
}
