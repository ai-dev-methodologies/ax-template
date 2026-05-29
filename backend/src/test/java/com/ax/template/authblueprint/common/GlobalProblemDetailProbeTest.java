package com.ax.template.authblueprint.common;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;

/**
 * Black-box (RestAssured) proof of the two IDW3 COMMON handlers added to
 * {@link GlobalProblemDetailAdvice}:
 * <ol>
 *   <li>{@link ResourceNotFoundException} → {@code 404 application/problem+json}
 *       (code {@code NOT_FOUND}) — NOT the {@code 403} the {@code @ResponseStatus} +
 *       {@code /error} re-dispatch trap produces under the reference
 *       {@code SecurityConfig};</li>
 *   <li>an out-of-range {@link OffsetPageSupport#clamp(int, int, int)} →
 *       {@code 400 application/problem+json} (code {@code PAGE_SIZE_INVALID}).</li>
 * </ol>
 *
 * <p>Both endpoints live on a TEST-ONLY {@link ProblemDetailProbeController} mounted
 * under {@code /api/items/probe/**} (already {@code authenticated()}), so the requests
 * traverse the REAL filter chain. The setup mirrors {@link GlobalProblemDetailAdviceTest}'s
 * signup/login to obtain a valid JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(ProblemDetailProbeController.class)
@Tag("COMMON_ADVICE")
class GlobalProblemDetailProbeTest {

    @LocalServerPort int port;

    private String token;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        String email = "common-advice-probe-" + UUID.randomUUID() + "@example.com";
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"MEMBER\"}")
        .when().post("/api/auth/email/signup");
        token = given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }

    @Test
    @Tag("COMMON-ADVICE-NOT-FOUND")
    void resourceNotFoundReturnsProblemJson404NotForbidden() {
        given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/items/probe/not-found")
        .then()
            .statusCode(404)
            .contentType("application/problem+json")
            .body("status", Matchers.equalTo(404))
            .body("code", Matchers.equalTo("NOT_FOUND"))
            .body("detail", Matchers.equalTo("probe resource not found"));
    }

    @Test
    @Tag("COMMON-ADVICE-PAGE-SIZE")
    void outOfRangePageSizeReturnsProblemJson400() {
        given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/items/probe/page?size=999")
        .then()
            .statusCode(400)
            .contentType("application/problem+json")
            .body("status", Matchers.equalTo(400))
            .body("code", Matchers.equalTo("PAGE_SIZE_INVALID"));
    }
}
