package com.ax.template.authblueprint.practices;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propagates a stable trace identifier through every log statement produced during a
 * request. Reads X-Request-Id from the inbound headers when present; otherwise mints a
 * UUID. Sets MDC.trace_id for the duration of the filter chain and clears it on exit.
 *
 * <p>Not annotated @Component on purpose — production wiring decides whether to register
 * this filter globally. The integration test below instantiates it directly to verify the
 * MDC contract without depending on ApplicationContext-level filter ordering.
 */
public class MdcRequestIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "trace_id";
    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String id = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
        MDC.put(MDC_KEY, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
