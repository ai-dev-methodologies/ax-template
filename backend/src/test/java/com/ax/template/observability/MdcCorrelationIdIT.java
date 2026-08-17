package com.ax.template.observability;

import com.ax.template.authblueprint.AuthBlueprintBackendApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration test: MdcCorrelationIdInterceptor wires X-Correlation-Id into MDC.
 *
 * <p>Uses RestAssured (black-box HTTP) per PRACTICES-TEST-001.
 *
 * <p>GREEN: passes when MdcCorrelationIdInterceptor (OncePerRequestFilter + @Component)
 * is registered in the application context — no WebMvcConfigurer wiring needed.
 *
 * <p>Rule protected: mdc-traceid-required-on-controller (PRACTICES-OBS-003).
 *
 * @see <a href="https://www.slf4j.org/manual.html#mdc">SLF4J MDC</a>
 * @see <a href="https://www.w3.org/TR/trace-context/#trace-id">W3C Trace Context</a>
 */
@Tag("OBSERVABILITY")
@SpringBootTest(
        classes = AuthBlueprintBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MdcCorrelationIdIT {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("GET /actuator/health without X-Correlation-Id returns a generated UUID in response header")
    void health_withoutCorrelationIdHeader_generatesCorrelationId() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .header("X-Correlation-Id",
                        matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("GET /actuator/health with X-Correlation-Id: abc-123 echoes the same value")
    void health_withCorrelationIdHeader_echoesCorrelationId() {
        given()
                .header("X-Correlation-Id", "abc-123")
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .header("X-Correlation-Id", equalTo("abc-123"));
    }

    @Test
    @DisplayName("Error response contains X-Correlation-Id equal to the inbound header")
    void errorEndpoint_withCorrelationIdHeader_echoesCorrelationId() {
        // Verifies the filter runs on non-existent paths too (servlet filter level,
        // not MVC interceptor level), so X-Correlation-Id is always echoed.
        given()
                .header("X-Correlation-Id", "test-trace-001")
                .when()
                .get("/api/nonexistent-endpoint-that-returns-404")
                .then()
                .header("X-Correlation-Id", equalTo("test-trace-001"));
    }
}
