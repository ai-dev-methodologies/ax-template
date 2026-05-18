/**
 * @ax-template-meta
 * template_id: backend/observability/MdcCorrelationIdInterceptor
 * layer: backend-cross-cutting
 * anchors_rule: observability-mdc-trace-propagation.md (PRACTICES-OBS-002)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "SLF4J Manual §Mapped Diagnostic Context — MDC cleared in finally"
 *     url: "https://www.slf4j.org/manual.html#mdc"
 *   - source_type: external
 *     citation: "W3C Trace Context — trace-id format (32 lowercase hex digits)"
 *     url: "https://www.w3.org/TR/trace-context/#trace-id"
 * usage: |
 *   1. Replace 'com.example.app' with your base package.
 *   2. Register this interceptor via WebMvcConfig.addInterceptors().
 *   3. The interceptor sets MDC keys "traceId" and "correlationId" on preHandle
 *      and removes them on afterCompletion (always — even on exception).
 *   4. The X-Correlation-Id response header echoes the effective correlation id.
 *
 *   MDC keys set:
 *     traceId       — same value as correlationId (used by GlobalExceptionHandler)
 *     correlationId — inbound X-Correlation-Id or generated UUID when absent
 */
package com.example.app.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * MVC interceptor that wires a per-request correlation id into MDC and echoes
 * it in the response {@code X-Correlation-Id} header.
 *
 * <p>Usage pattern:
 * <ul>
 *   <li>Inbound {@code X-Correlation-Id} header — echoed as-is.</li>
 *   <li>No inbound header — a fresh UUID is generated.</li>
 * </ul>
 *
 * <p>The correlation id is placed in two MDC slots:
 * <ul>
 *   <li>{@code traceId} — read by GlobalExceptionHandler to populate
 *       {@code ProblemDetail.setProperty("traceId", ...)}.</li>
 *   <li>{@code correlationId} — for Logback JSON encoder ({@code %X{correlationId}}).</li>
 * </ul>
 *
 * <p>Both MDC entries are cleared in {@code afterCompletion} (finally-equivalent) so
 * the next request on a reused servlet thread does not inherit the previous id.
 *
 * @see <a href="https://www.slf4j.org/manual.html#mdc">SLF4J MDC Manual</a>
 * @see <a href="https://www.w3.org/TR/trace-context/#trace-id">W3C Trace Context</a>
 */
public class MdcCorrelationIdInterceptor implements HandlerInterceptor {

    /** MDC key read by GlobalExceptionHandler for ProblemDetail traceId. */
    public static final String MDC_TRACE_ID = "traceId";

    /** MDC key used in Logback JSON encoder pattern. */
    public static final String MDC_CORRELATION_ID = "correlationId";

    /** HTTP request/response header name. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        String correlationId = resolveCorrelationId(request);

        MDC.put(MDC_TRACE_ID, correlationId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {

        // Always clear MDC — even when an exception occurred — so the next
        // request on this reused servlet thread does not inherit this id.
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_CORRELATION_ID);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String resolveCorrelationId(HttpServletRequest request) {
        String inbound = request.getHeader(CORRELATION_ID_HEADER);
        if (inbound != null && !inbound.isBlank()) {
            return inbound;
        }
        return UUID.randomUUID().toString();
    }
}
