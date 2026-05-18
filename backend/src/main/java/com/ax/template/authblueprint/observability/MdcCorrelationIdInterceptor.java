package com.ax.template.authblueprint.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that wires a per-request correlation id into MDC and echoes
 * it in the {@code X-Correlation-Id} response header.
 *
 * <p>Runs as a servlet filter (not a Spring MVC interceptor) so it intercepts
 * <em>all</em> requests, including Spring Boot Actuator endpoints that bypass
 * the DispatcherServlet interceptor chain.
 *
 * <p>On entry to {@link #doFilterInternal}:
 * <ul>
 *   <li>Reads {@code X-Correlation-Id} from the inbound request header.</li>
 *   <li>Generates a UUID when the header is absent or blank.</li>
 *   <li>Puts the effective id in MDC under {@code "traceId"} and
 *       {@code "correlationId"}.</li>
 *   <li>Sets the {@code X-Correlation-Id} response header to the same value.</li>
 * </ul>
 *
 * <p>In the {@code finally} block:
 * <ul>
 *   <li>Removes both MDC entries so the next request on a reused servlet
 *       thread does not inherit the previous request's id.</li>
 * </ul>
 *
 * <p>Registered automatically via {@code @Component} — no {@code WebMvcConfigurer}
 * wiring required.
 *
 * @see <a href="https://www.slf4j.org/manual.html#mdc">SLF4J MDC</a>
 * @see <a href="https://www.w3.org/TR/trace-context/#trace-id">W3C Trace Context</a>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MdcCorrelationIdInterceptor extends OncePerRequestFilter {

    /** MDC key read by GlobalExceptionHandler for ProblemDetail traceId. */
    public static final String MDC_TRACE_ID = "traceId";

    /** MDC key for Logback JSON encoder. */
    public static final String MDC_CORRELATION_ID = "correlationId";

    /** HTTP header name. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);

        MDC.put(MDC_TRACE_ID, correlationId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_CORRELATION_ID);
        }
    }

    private static String resolveCorrelationId(HttpServletRequest request) {
        String inbound = request.getHeader(CORRELATION_ID_HEADER);
        if (inbound != null && !inbound.isBlank()) {
            return inbound;
        }
        return UUID.randomUUID().toString();
    }
}
