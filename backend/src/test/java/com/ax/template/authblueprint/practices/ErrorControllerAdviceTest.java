package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-ERR-001")
class ErrorControllerAdviceTest {

    @LocalServerPort
    private int port;

    @Test
    void practices_ERR_001_controllerAdviceMapsIllegalArgumentTo400() {
        given()
                .when().get("/practices/demo/bad")
                .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    @Test
    void practices_ERR_001_controllerAdviceMapsNoSuchElementTo404() {
        given()
                .when().get("/practices/demo/missing")
                .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }
}
