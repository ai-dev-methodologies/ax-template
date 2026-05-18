/**
 * FIXTURE: mdc_traceid_required/fail_no_interceptor
 *
 * Demonstrates WRONG pattern: a controller is registered without the
 * MdcCorrelationIdInterceptor in the Spring MVC interceptor chain.
 *
 * Guard must catch: MDC_INTERCEPTOR_MISSING — no HandlerInterceptor implementation
 * that populates "traceId" in MDC is registered for this controller's request path.
 *
 * Violates: mdc-traceid-required-on-controller rule (PRACTICES-OBS-003).
 */
package com.example.fixture.mdc_traceid_required.fail_no_interceptor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth controller without MDC interceptor wired.
 *
 * <p>The application context has no WebMvcConfigurer that registers a
 * HandlerInterceptor putting "traceId" into MDC. Every request to this
 * controller runs without a trace ID in structured log output.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        // VIOLATION: no MDC.get("traceId") available — MdcCorrelationIdInterceptor
        // is not registered in the interceptor chain for this controller.
        return ResponseEntity.ok("ok");
    }
}
