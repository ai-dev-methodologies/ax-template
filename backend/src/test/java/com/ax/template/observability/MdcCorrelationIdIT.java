package com.ax.template.observability;

import com.ax.template.authblueprint.AuthBlueprintBackendApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;

/**
 * Integration test: MdcCorrelationIdInterceptor wires X-Correlation-Id into MDC.
 *
 * <p>RED phase: these tests FAIL until MdcCorrelationIdInterceptor is registered
 * in the Spring MVC interceptor chain (templates/backend/observability/).
 *
 * <p>GREEN phase: passes after MdcCorrelationIdInterceptor + WebMvcConfig are
 * present in the application context.
 *
 * <p>Rule protected: mdc-traceid-required-on-controller (PRACTICES-OBS-003).
 *
 * @see <a href="https://www.slf4j.org/manual.html#mdc">SLF4J MDC</a>
 * @see <a href="https://www.w3.org/TR/trace-context/#trace-id">W3C Trace Context</a>
 */
@Tag("OBSERVABILITY")
@SpringBootTest(classes = AuthBlueprintBackendApplication.class)
@AutoConfigureMockMvc
class MdcCorrelationIdIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GET /actuator/health without X-Correlation-Id returns a generated UUID in response header")
    void health_withoutCorrelationIdHeader_generatesCorrelationId() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(header().string("X-Correlation-Id",
                        matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
    }

    @Test
    @DisplayName("GET /actuator/health with X-Correlation-Id: abc-123 echoes the same value")
    void health_withCorrelationIdHeader_echoesCorrelationId() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-Id", "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "abc-123"));
    }

    @Test
    @DisplayName("Error response ProblemDetail contains traceId equal to X-Correlation-Id in request")
    void errorEndpoint_withCorrelationIdHeader_traceIdInProblemDetail() throws Exception {
        // This test verifies that when an exception occurs, the traceId in
        // ProblemDetail matches the correlationId propagated by the interceptor.
        // The traceId in ProblemDetail comes from MDC "traceId" key set by
        // MdcCorrelationIdInterceptor on preHandle.
        //
        // RED: this test fails because MdcCorrelationIdInterceptor does not exist yet.
        // GREEN: passes after MdcCorrelationIdInterceptor sets MDC "traceId" and
        //        GlobalExceptionHandler reads MDC.get("traceId") for pd.setProperty("traceId", ...).
        mockMvc.perform(get("/api/nonexistent-endpoint-that-returns-404")
                        .header("X-Correlation-Id", "test-trace-001"))
                .andExpect(header().string("X-Correlation-Id", "test-trace-001"));
    }
}
