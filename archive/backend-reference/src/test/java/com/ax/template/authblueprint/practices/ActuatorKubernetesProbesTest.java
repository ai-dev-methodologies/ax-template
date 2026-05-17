package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-ACTUATOR-001")
class ActuatorKubernetesProbesTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void practices_ACTUATOR_001_livenessProbeIsReachableAndReports200() {
        given()
                .when().get("/actuator/health/liveness")
                .then().statusCode(200)
                .body("status", anyOf(equalTo("UP"), equalTo("DOWN")));
    }

    @Test
    void practices_ACTUATOR_001_readinessProbeIsReachableAndReports200() {
        given()
                .when().get("/actuator/health/readiness")
                .then().statusCode(200)
                .body("status", anyOf(equalTo("UP"), equalTo("DOWN")));
    }
}
