package com.ax.template.authblueprint.filestorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;

/**
 * QUOTA family (1 item): FILE-QUOTA-001 — per-user storage cap.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileStorageQuotaTest {

    @LocalServerPort int port;

    @Autowired FileStorageProperties properties;

    @BeforeEach
    void setup() {
        FileStorageTestSupport.useRandomPort(port);
    }

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-QUOTA-001")
    void quota_001_overQuotaReturns413WithProblemDetail() {
        long originalMb = properties.getMaxQuotaMb();
        long originalFileMb = properties.getMaxFileSizeMb();
        properties.setMaxQuotaMb(1);          // 1 MB total per user
        properties.setMaxFileSizeMb(2);       // single file can be 2MB (so the
                                              // per-file limit is NOT what fires)
        try {
            String token = FileStorageTestSupport.obtainToken(
                FileStorageTestSupport.freshEmail("quota"), "MEMBER");

            byte[] payload = new byte[600 * 1024]; // 600 KB

            // First upload: 600 KB under 1MB quota — succeeds.
            given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "first.pdf", payload, "application/pdf")
            .when().post("/api/files")
            .then().statusCode(201);

            // Second upload would push total to ~1.17 MB → exceeds quota → 413.
            given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "second.pdf", payload, "application/pdf")
            .when().post("/api/files")
            .then().statusCode(413)
                .body("type", org.hamcrest.Matchers.equalTo(
                    "https://ax-template.example/problems/quota-exceeded"));
        } finally {
            properties.setMaxQuotaMb(originalMb);
            properties.setMaxFileSizeMb(originalFileMb);
        }
    }
}
