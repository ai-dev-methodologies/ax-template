package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-ERR-002")
class ErrorRfc7807ProblemDetailTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void practices_ERR_002_responseContentTypeIsApplicationProblemJson() {
        given()
                .when().get("/practices/demo/bad")
                .then()
                .header("Content-Type", containsString("application/problem+json"));
    }

    @Test
    void practices_ERR_002_responseHasAllRfc7807Fields() {
        // RFC 7807 §3.1 — a problem detail object MAY have type, title, status, detail, instance.
        // Spring's ProblemDetail emits type/title/status/detail by default; instance is optional.
        given()
                .when().get("/practices/demo/missing")
                .then()
                .body("type", equalTo("https://errors.example.com/not-found"))
                .body("title", equalTo("Resource Not Found"))
                .body("status", equalTo(404))
                .body("detail", notNullValue());
    }
}
