/**
 * FIXTURE: mdc_traceid_required/pass
 *
 * Demonstrates CORRECT pattern: MdcCorrelationIdInterceptor is registered via
 * WebMvcConfigurer so every request to this controller has a traceId in MDC.
 *
 * Guard exits 0: HandlerInterceptor registered for all /** paths, populates
 * MDC "traceId" from X-Correlation-Id request header or generated UUID.
 *
 * Complies with: mdc-traceid-required-on-controller rule (PRACTICES-OBS-003).
 */
package com.example.fixture.mdc_traceid_required.pass;

import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * CORRECT: MdcCorrelationIdInterceptor registered for all request paths.
 */
@Configuration
class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // CORRECT: interceptor registered globally — all controllers get MDC traceId
        registry.addInterceptor(new MdcCorrelationIdInterceptor()).addPathPatterns("/**");
    }
}

/**
 * Interceptor that reads or generates X-Correlation-Id and puts it in MDC.
 */
class MdcCorrelationIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("traceId", correlationId);
        response.setHeader("X-Correlation-Id", correlationId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        MDC.remove("traceId");
    }
}

@RestController
@RequestMapping("/api/auth")
class AuthController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        // CORRECT: MDC.get("traceId") is populated by the interceptor above
        return ResponseEntity.ok("ok");
    }
}
