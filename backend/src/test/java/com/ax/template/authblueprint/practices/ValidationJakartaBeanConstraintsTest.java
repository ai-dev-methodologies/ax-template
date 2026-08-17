package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

import io.restassured.RestAssured;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-VAL-002")
class ValidationJakartaBeanConstraintsTest {

    @LocalServerPort
    private int port;

    @Test
    void practices_VAL_002_validPayloadAccepted() {
        given()
                .contentType(JSON)
                .body(Map.of(
                        "name", "Alice Anderson",
                        "email", "alice@example.com",
                        "username", "alice_99"
                ))
                .when().post("/practices/demo/users")
                .then().statusCode(200);
    }

    @Test
    void practices_VAL_002_blankNameRejected() {
        given()
                .contentType(JSON)
                .body(Map.of("name", "", "email", "a@a.com", "username", "alice_99"))
                .when().post("/practices/demo/users")
                .then().statusCode(400);
    }

    @Test
    void practices_VAL_002_invalidEmailRejected() {
        given()
                .contentType(JSON)
                .body(Map.of("name", "Alice", "email", "not-an-email", "username", "alice_99"))
                .when().post("/practices/demo/users")
                .then().statusCode(400);
    }

    @Test
    void practices_VAL_002_oversizedNameRejected() {
        // @Size(min=3, max=50) — supply 51-char string
        String tooLong = "a".repeat(51);
        given()
                .contentType(JSON)
                .body(Map.of("name", tooLong, "email", "a@a.com", "username", "alice_99"))
                .when().post("/practices/demo/users")
                .then().statusCode(400);
    }
}
