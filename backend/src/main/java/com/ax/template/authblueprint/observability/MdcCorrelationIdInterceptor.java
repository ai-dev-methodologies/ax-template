package com.ax.template.authblueprint.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * MVC interceptor that wires a per-request correlation id into MDC and echoes
 * it in the {@code X-Correlation-Id} response header.
 *
 * <p>On {@code preHandle}:
 * <ul>
 *   <li>Reads {@code X-Correlation-Id} from the inbound request header.</li>
 *   <li>Generates a UUID when the header is absent or blank.</li>
 *   <li>Puts the effective id in MDC under {@code "traceId"} and
 *       {@code "correlationId"}.</li>
 *   <li>Sets the {@code X-Correlation-Id} response header to the same value.</li>
 * </ul>
 *
 * <p>On {@code afterCompletion} (equivalent to {@code finally}):
 * <ul>
 *   <li>Removes both MDC entries so the next request on a reused servlet
 *       thread does not inherit the previous request's id.</li>
 * </ul>
 *
 * <p>Register via {@link WebMvcConfig#addInterceptors}.
 *
 * @see <a href="https://www.slf4j.org/manual.html#mdc">SLF4J MDC</a>
 * @see <a href="https://www.w3.org/TR/trace-context/#trace-id">W3C Trace Context</a>
 */
@Component
public class MdcCorrelationIdInterceptor implements HandlerInterceptor {

    /** MDC key read by GlobalExceptionHandler for ProblemDetail traceId. */
    public static final String MDC_TRACE_ID = "traceId";

    /** MDC key for Logback JSON encoder. */
    public static final String MDC_CORRELATION_ID = "correlationId";

    /** HTTP header name. */
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

        // Always clear MDC — even on exception — so the next request on
        // this reused servlet thread does not inherit the previous id.
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_CORRELATION_ID);
    }

    private static String resolveCorrelationId(HttpServletRequest request) {
        String inbound = request.getHeader(CORRELATION_ID_HEADER);
        if (inbound != null && !inbound.isBlank()) {
            return inbound;
        }
        return UUID.randomUUID().toString();
    }
}
