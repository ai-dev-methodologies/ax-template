package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.restassured.RestAssured;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-VAL-003")
class ValidationCustomConstraintTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void practices_VAL_003_invalidUsernameRejectedByCustomConstraint() {
        // valid name + email, but username breaks the @ValidUsername regex
        // (uppercase letters, hyphens, and the dash all forbidden)
        given()
                .contentType(JSON)
                .body(Map.of(
                        "name", "Alice Anderson",
                        "email", "alice@example.com",
                        "username", "BAD-USERNAME"
                ))
                .when().post("/practices/demo/users")
                .then()
                .statusCode(400)
                .body("errors.field", hasItem("username"));
    }

    @Test
    void practices_VAL_003_tooShortUsernameRejected() {
        given()
                .contentType(JSON)
                .body(Map.of(
                        "name", "Alice",
                        "email", "a@a.com",
                        "username", "ab"
                ))
                .when().post("/practices/demo/users")
                .then()
                .statusCode(400)
                .body("errors.field", hasItem("username"));
    }

    @Test
    void practices_VAL_003_validUsernameAccepted() {
        given()
                .contentType(JSON)
                .body(Map.of(
                        "name", "Bob",
                        "email", "bob@example.com",
                        "username", "bob_1"
                ))
                .when().post("/practices/demo/users")
                .then()
                .statusCode(200)
                .body("username", equalTo("bob_1"));
    }
}
