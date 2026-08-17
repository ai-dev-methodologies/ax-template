package com.ax.template.authblueprint.sample;

import io.restassured.RestAssured;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;

/**
 * FAIL fixture — every forbidden shape, alongside the legal reads that must NOT be counted.
 * Expected: exactly 5 violations — the three writes here, the single-static-import in
 * SampleTestSupport, and the wildcard static import in SampleWildcardSupport.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleComplianceTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;                       // (A) the dominant shape
    }

    @Test
    void fullyQualifiedWriteIsAlsoAWrite() {
        io.restassured.RestAssured.port = port + 1;    // (A) qualified
    }

    @Test
    void compoundAssignmentIsAlsoAWrite() {
        RestAssured.port += 1;                         // (A) compound
    }

    @Test
    void theseAreReadsAndMustNotBeCounted() {
        int observed = RestAssured.port;
        boolean same = RestAssured.port == port;
        boolean atLeast = RestAssured.port >= 1024;
        boolean differs = RestAssured.port != 8080;
        String report = "RestAssured.port=" + observed + " same=" + same
            + " atLeast=" + atLeast + " differs=" + differs;
        given().port(port).when().get("/api/health").then().statusCode(200);
        System.out.println(report);
    }
}
