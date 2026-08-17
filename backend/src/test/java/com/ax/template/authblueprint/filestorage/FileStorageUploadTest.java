package com.ax.template.authblueprint.filestorage;

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
 * UPLOAD family (3 items): FILE-UPLOAD-001 (MIME allowlist), FILE-UPLOAD-002
 * (size limit), FILE-UPLOAD-003 (filename sanitization / CWE-22).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileStorageUploadTest {

    @LocalServerPort int port;

    @Autowired StoredFileRepository repository;
    @Autowired FileStorageProperties properties;

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-UPLOAD-001")
    void upload_001_disallowedMimeReturns415() {
        String token = FileStorageTestSupport.obtainToken(
            FileStorageTestSupport.freshEmail("upload-mime"), "MEMBER");

        given()
            .header("Authorization", "Bearer " + token)
            .multiPart("file", "malware.bin", new byte[]{1, 2, 3}, "application/x-executable")
        .when().post("/api/files")
        .then().statusCode(415);
    }

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-UPLOAD-002")
    void upload_002_oversizedReturns413() {
        // Override the in-memory FileStorageProperties to make the limit small
        // for this test — direct mutation is safe because the bean is request-
        // scoped only at read time.
        long originalMb = properties.getMaxFileSizeMb();
        properties.setMaxFileSizeMb(1); // 1 MB
        try {
            String token = FileStorageTestSupport.obtainToken(
                FileStorageTestSupport.freshEmail("upload-size"), "MEMBER");

            // 2 MB payload — service-layer guard rejects (the Spring multipart
            // layer is set to 100MB in application.yml, so the service-layer
            // re-check is what fires; both layers map to 413 in production).
            byte[] big = new byte[2 * 1024 * 1024];
            given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "big.pdf", big, "application/pdf")
            .when().post("/api/files")
            .then().statusCode(413);
        } finally {
            properties.setMaxFileSizeMb(originalMb);
        }
    }

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-UPLOAD-003")
    void upload_003_pathTraversalIsStripped() {
        String token = FileStorageTestSupport.obtainToken(
            FileStorageTestSupport.freshEmail("upload-traversal"), "MEMBER");

        // Malicious filename containing path traversal sequences.
        String maliciousName = "../../etc/passwd";

        String fileId = given()
            .header("Authorization", "Bearer " + token)
            .multiPart("file", maliciousName, "PWNED".getBytes(), "text/plain")
        .when().post("/api/files")
        .then().statusCode(201)
            .extract().path("id");

        StoredFile saved = repository.findById(UUID.fromString(fileId)).orElseThrow();

        // FILE-UPLOAD-003 — the stored filename must NOT contain any traversal sequence.
        assertThat(saved.getFileName()).doesNotContain("..");
        assertThat(saved.getFileName()).doesNotContain("/");
        assertThat(saved.getFileName()).doesNotContain("\\");

        // FILE-SEC-001 / FILE-UPLOAD-003 — internal storage key is server-
        // generated UUID, never derived from user input.
        assertThat(saved.getStorageKey()).doesNotContain("..");
        assertThat(saved.getStorageKey()).doesNotContain("/");
        // Storage key matches UUID pattern.
        UUID.fromString(saved.getStorageKey());
    }
}
