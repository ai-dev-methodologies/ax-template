package com.ax.template.authblueprint.filestorage;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * OBS family (1 item): FILE-OBS-001 — Micrometer counters + MDC tags.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileStorageObsTest {

    @LocalServerPort int port;

    @Autowired MeterRegistry meterRegistry;

    @BeforeEach
    void setup() {
        FileStorageTestSupport.useRandomPort(port);
    }

    @Test
    @Tag("FILE_STORAGE")
    @Tag("FILE-OBS-001")
    void obs_001_upload_increments_micrometer_counters() {
        String token = FileStorageTestSupport.obtainToken(
            FileStorageTestSupport.freshEmail("obs-counter"), "MEMBER");

        double beforeUploads = meterRegistry.counter("files.uploaded.total").count();
        double beforeBytes = meterRegistry.counter("upload.bytes.total").count();

        byte[] payload = "hello-observability".getBytes();
        given()
            .header("Authorization", "Bearer " + token)
            .multiPart("file", "obs.txt", payload, "text/plain")
        .when().post("/api/files")
        .then().statusCode(201);

        double afterUploads = meterRegistry.counter("files.uploaded.total").count();
        double afterBytes = meterRegistry.counter("upload.bytes.total").count();

        assertThat(afterUploads).isEqualTo(beforeUploads + 1.0);
        assertThat(afterBytes).isEqualTo(beforeBytes + payload.length);
    }
}
