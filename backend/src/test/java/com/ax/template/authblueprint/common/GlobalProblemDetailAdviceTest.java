package com.ax.template.authblueprint.common;

import io.restassured.http.ContentType;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;

/**
 * Black-box (RestAssured) proof that the shared {@link GlobalProblemDetailAdvice}
 * fallback turns the COMMON framework exceptions into RFC 9457
 * {@code application/problem+json} responses for a domain that ships NO local
 * validation handler ({@code crud.ItemController} has a {@code @Valid @RequestBody}
 * but no {@code @ExceptionHandler}).
 *
 * <p>IMW1-B regression anchor: before this advice, an authenticated {@code @Valid}
 * failure on a handler-less domain fell through to a misleading Spring Security
 * {@code 403} at {@code /error}. These tests assert {@code 400 problem+json} (NOT
 * 403) with the shared {@code errors[]} array.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// R22 ContextCache lever: BEFORE_CLASS, not AFTER_CLASS. AFTER_CLASS only evicts on
// exit, so it cannot stop this class from *inheriting* a context that the cap-32 LRU
// already evicted -- the dead-Tomcat symptom is a uniform IllegalStateException on the
// first @LocalServerPort request. BEFORE_CLASS forces a fresh boot on entry. Same lever
// already applied to BillingFlowIT / FeatureFlagFlowIT / PageEnvelopeCatalogSweepTest.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("COMMON_ADVICE")
class GlobalProblemDetailAdviceTest {

    @LocalServerPort int port;

    private String token;

    @BeforeEach
    void setup() {
        String email = "common-advice-" + UUID.randomUUID() + "@example.com";
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"MEMBER\"}")
        .when().post("/api/auth/email/signup");
        token = given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().statusCode(200).extract().path("accessToken");
    }

    @Test
    @Tag("COMMON-ADVICE-VALIDATION")
    void validValidationFailureReturnsProblemJson400NotForbidden() {
        // /api/items POST has @Valid @RequestBody CreateItemRequest (title @NotBlank)
        // but the crud package ships NO @ExceptionHandler — pre-advice this 403'd at /error.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"description\":\"no title\"}")
        .when().post("/api/items")
        .then()
            .statusCode(400)
            .contentType("application/problem+json")
            .body("status", Matchers.equalTo(400))
            .body("code", Matchers.equalTo("VALIDATION_FAILED"))
            .body("errors", Matchers.notNullValue())
            .body("errors.field", Matchers.hasItem("title"))
            .body("errors.code", Matchers.hasItem("NotBlank"))
            .body("errors[0].pointer", Matchers.equalTo("/title"))
            .body("errors[0].message", Matchers.notNullValue());
    }

    @Test
    @Tag("COMMON-ADVICE-MALFORMED")
    void malformedBodyReturnsProblemJson400() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ not json ")
        .when().post("/api/items")
        .then()
            .statusCode(400)
            .contentType("application/problem+json")
            .body("code", Matchers.equalTo("MALFORMED_REQUEST_BODY"));
    }

    @Test
    @Tag("COMMON-ADVICE-MEDIATYPE")
    void unsupportedMediaTypeReturnsProblemJson415() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.TEXT)
            .body("plain text body")
        .when().post("/api/items")
        .then()
            .statusCode(415)
            .contentType("application/problem+json")
            .body("code", Matchers.equalTo("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @Tag("COMMON-ADVICE-METHOD")
    void unsupportedMethodReturnsProblemJson405() {
        // /api/items has no PATCH mapping → HttpRequestMethodNotSupportedException.
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"title\":\"x\"}")
        .when().patch("/api/items")
        .then()
            .statusCode(405)
            .contentType("application/problem+json")
            .body("code", Matchers.equalTo("METHOD_NOT_ALLOWED"));
    }
}
