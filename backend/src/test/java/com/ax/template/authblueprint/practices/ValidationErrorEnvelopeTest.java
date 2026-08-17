package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-VAL-004")
class ValidationErrorEnvelopeTest {

    @LocalServerPort
    private int port;

    @Test
    void practices_VAL_004_envelopeHasRfc7807FieldsPlusErrorsArray() {
        given()
                .contentType(JSON)
                .body(Map.of("name", "", "email", "bad", "username", "BAD"))
                .when().post("/practices/demo/users")
                .then()
                .statusCode(400)
                .header("Content-Type", containsString("application/problem+json"))
                .body("type", equalTo("https://errors.example.com/validation"))
                .body("title", equalTo("Validation Error"))
                .body("status", equalTo(400))
                .body("detail", notNullValue())
                .body("errors", notNullValue())
                .body("errors.size()", greaterThan(0))
                .body("errors.field", hasItem("name"))
                .body("errors.field", hasItem("email"))
                .body("errors.field", hasItem("username"));
    }
}
