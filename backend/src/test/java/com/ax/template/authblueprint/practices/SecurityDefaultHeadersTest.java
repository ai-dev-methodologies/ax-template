package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-SECURITY-003")
class SecurityDefaultHeadersTest {

    @LocalServerPort
    private int port;

    @Test
    void practices_SECURITY_003_responseCarriesContentTypeOptionsNosniff() {
        // Spring Security's default header chain MUST stay on. X-Content-Type-Options: nosniff
        // stops MIME-sniffing attacks. Switching .headers(headers -> headers.disable())
        // anywhere in the chain silently drops this header.
        given()
                .when().get("/actuator/health")
                .then()
                .header("X-Content-Type-Options", "nosniff");
    }

    @Test
    void practices_SECURITY_003_responseCarriesFrameOptionsHeader() {
        // X-Frame-Options (SAMEORIGIN or DENY) stops clickjacking. The project's
        // SecurityConfig uses sameOrigin() — we just assert the header exists.
        given()
                .when().get("/actuator/health")
                .then()
                .header("X-Frame-Options", anyOf(
                        containsString("SAMEORIGIN"),
                        containsString("DENY")));
    }
}
