package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-API-001")
class ApiPaginationPageableTest {

    @LocalServerPort
    private int port;

    @Test
    void practices_API_001_listEndpointAcceptsPageAndSizeParams() {
        given()
                .when().get("/practices/demo/v1/parents?page=0&size=5")
                .then().statusCode(200)
                .body("size", equalTo(5))
                .body("number", equalTo(0));
    }

    @Test
    void practices_API_001_defaultPageSizeIsBoundedToSafeWindow() {
        given()
                .when().get("/practices/demo/v1/parents")
                .then().statusCode(200)
                .body("size", lessThanOrEqualTo(100));
    }

    @Test
    void practices_API_001_oversizedSizeRequestClampedToMaximum() {
        given()
                .when().get("/practices/demo/v1/parents?page=0&size=10000")
                .then().statusCode(200)
                .body("size", lessThanOrEqualTo(100));
    }
}
