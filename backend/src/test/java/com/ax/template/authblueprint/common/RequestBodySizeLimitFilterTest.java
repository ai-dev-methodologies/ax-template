package com.ax.template.authblueprint.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioral proof of {@link RequestBodySizeLimitFilter} — the transport body cap that seals the
 * raw-buffer amplification surface (A1–A10). Asserts BOTH bound paths NON-VACUOUSLY (each has an
 * under-cap counterpart that is allowed through), so the filter provably blocks rather than always
 * or never firing:
 * <ul>
 *   <li>declared {@code Content-Length} &gt; cap → 413 fast, the downstream chain is NEVER invoked
 *       (the body is not read), and the response echoes no request content;</li>
 *   <li>chunked / absent {@code Content-Length} with actual bytes &gt; cap → the bounded stream
 *       throws while the chain reads, mapped to 413 (never a 500).</li>
 * </ul>
 */
@Tag("COMMON_BODY_SIZE")
class RequestBodySizeLimitFilterTest {

    private static final long CAP = RequestBodySizeLimitFilter.MAX_BODY_BYTES;

    /** A chain that records whether it ran and (optionally) fully drains the request body. */
    private static final class RecordingChain implements FilterChain {
        final boolean drainBody;
        boolean invoked;
        RecordingChain(boolean drainBody) {
            this.drainBody = drainBody;
        }
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            invoked = true;
            if (drainBody) {
                ((HttpServletRequest) request).getInputStream().readAllBytes(); // forces the bounded read
            }
        }
    }

    @Test
    void declaredContentLengthOverCap_rejects413FastWithoutReadingBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override public long getContentLengthLong() { return CAP + 1; }
            @Override public int getContentLength() { return -1; }
        };
        request.setMethod("POST");
        request.setRequestURI("/api/idempotency-demo/resources");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(false);

        new RequestBodySizeLimitFilter().doFilter(request, response, chain);

        assertThat(response.getStatus()).as("413 on declared oversized body").isEqualTo(413);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(chain.invoked).as("downstream chain must NOT run — body never read").isFalse();
        assertThat(response.getContentAsString())
            .as("413 body must not echo request content")
            .contains("REQUEST_BODY_TOO_LARGE")
            .doesNotContain("Z");
    }

    @Test
    void declaredContentLengthUnderCap_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContent("{\"v\":1}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(true);

        new RequestBodySizeLimitFilter().doFilter(request, response, chain);

        assertThat(chain.invoked).as("under-cap request flows to the chain").isTrue();
        assertThat(response.getStatus()).as("no 413 for an under-cap body").isEqualTo(200);
    }

    @Test
    void chunkedBodyOverCap_boundedStreamRejects413() throws Exception {
        // No Content-Length (chunked/absent) but the ACTUAL bytes exceed the cap.
        byte[] oversized = new byte[(int) CAP + 16];
        java.util.Arrays.fill(oversized, (byte) 'Z');
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override public long getContentLengthLong() { return -1; }
            @Override public int getContentLength() { return -1; }
        };
        request.setMethod("POST");
        request.setContent(oversized);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(true);

        new RequestBodySizeLimitFilter().doFilter(request, response, chain);

        assertThat(response.getStatus())
            .as("chunked oversized body mapped to 413 (not 500) via bounded stream")
            .isEqualTo(413);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).doesNotContain("Z");
    }

    @Test
    void chunkedBodyUnderCap_boundedStreamPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override public long getContentLengthLong() { return -1; }
            @Override public int getContentLength() { return -1; }
        };
        request.setMethod("POST");
        request.setContent("{\"v\":1}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain(true);

        new RequestBodySizeLimitFilter().doFilter(request, response, chain);

        assertThat(chain.invoked).as("under-cap chunked body flows to the chain").isTrue();
        assertThat(response.getStatus()).as("no 413 for an under-cap chunked body").isEqualTo(200);
    }
}
