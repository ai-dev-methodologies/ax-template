package com.ax.template.authblueprint.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Transport-level request-body size cap — the ONLY correct fix for the raw-buffer
 * amplification / DoS surface (A1–A10). A handler-level {@code body.length()} check is
 * structurally too late: by the time a controller runs, {@code StringHttpMessageConverter} /
 * the {@code byte[]} converter has already buffered the ENTIRE body onto the heap. This filter
 * runs BEFORE the {@code DispatcherServlet} (and before the Spring Security chain — registered
 * at {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE}), so an oversized body is
 * rejected before any converter can allocate it.
 *
 * <p>Two independent bounds, so the {@code Transfer-Encoding: chunked} bypass is closed too:
 * <ul>
 *   <li><b>Declared length</b> — if the {@code Content-Length} header exceeds
 *       {@link #MAX_BODY_BYTES}, respond {@code 413} immediately, BEFORE reading a single byte;</li>
 *   <li><b>Actual bytes</b> — the request's {@code getInputStream()}/{@code getReader()} are
 *       wrapped by a {@link BoundedServletInputStream} that throws once cumulative bytes read
 *       exceed the cap. This covers chunked / absent-Content-Length requests where the declared
 *       length is unknown or a lie. The thrown signal is mapped to {@code 413} (never a 500).</li>
 * </ul>
 *
 * <p>Multipart uploads are deliberately EXCLUDED — those are streamed by the multipart resolver
 * (never buffered as a String) and have their own, larger size limits
 * ({@code spring.servlet.multipart.max-request-size}); the amplification vector this filter closes
 * is JSON / text bodies buffered by message converters.
 *
 * <p>The {@code 413} body is a small RFC 9457-style {@code application/problem+json} document that
 * NEVER echoes any request content.
 */
public class RequestBodySizeLimitFilter extends OncePerRequestFilter {

    /**
     * Maximum request body accepted for any non-multipart request. 20&nbsp;MiB — the transport
     * outer bound that matches the Jackson {@code max-string-length} cap
     * ({@code spring.jackson.factory.constraints.read.max-string-length=20000000} in application.yml
     * and the standalone {@code RequestFingerprint} mapper). Slightly more generous than the exact
     * 20&nbsp;000&nbsp;000 Jackson cap so that Jackson's own string constraint (or a controller
     * belt-and-suspenders check) is the tight inner bound while this filter stops truly huge bodies
     * (100&nbsp;MB, 1&nbsp;GB) from ever being buffered onto the heap.
     */
    public static final long MAX_BODY_BYTES = 20L * 1024 * 1024;

    /** RFC 9457 problem+json 413 body. Constant — it never contains any request-derived value. */
    private static final String TOO_LARGE_BODY =
            "{\"type\":\"https://errors.example.com/request-body-too-large\","
            + "\"title\":\"Request body too large\",\"status\":413,"
            + "\"code\":\"REQUEST_BODY_TOO_LARGE\","
            + "\"detail\":\"The request body exceeds the maximum allowed size.\"}";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Multipart uploads are streamed by the multipart resolver with their own limits — never
        // buffered as a String — so they are not part of this amplification surface.
        if (isMultipart(request)) {
            chain.doFilter(request, response);
            return;
        }

        // Fast path: reject a declared-oversized body BEFORE reading the stream.
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            writeTooLarge(response);
            return;
        }

        // Slow path: enforce the same bound on the ACTUAL bytes (chunked / lying Content-Length).
        BoundedRequestWrapper wrapped = new BoundedRequestWrapper(request);
        try {
            chain.doFilter(wrapped, response);
        } catch (BodySizeLimitExceededException ex) {
            handleOverflow(response);
        } catch (ServletException | IOException | RuntimeException ex) {
            if (hasOverflowCause(ex)) {
                handleOverflow(response);
            } else {
                throw ex;
            }
        }
    }

    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        // Locale.ROOT: a default-locale toLowerCase() mis-maps ASCII under some locales (Turkish
        // 'I' → 'ı'), so "MULTIPART/FORM-DATA" would fail the prefix test and a legit large upload
        // would be wrongly capped at 20 MiB. Locale-independent lowering keeps detection correct.
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }

    private void handleOverflow(HttpServletResponse response) throws IOException {
        if (response.isCommitted()) {
            return; // a partially-written response cannot be rewritten; nothing safe to do
        }
        response.reset();
        writeTooLarge(response);
    }

    private void writeTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE); // 413
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(TOO_LARGE_BODY);
    }

    private static boolean hasOverflowCause(Throwable t) {
        for (Throwable c = t; c != null; c = (c.getCause() == c) ? null : c.getCause()) {
            if (c instanceof BodySizeLimitExceededException) {
                return true;
            }
        }
        return false;
    }

    /** Unchecked so a wrapped {@code getInputStream().read(...)} can raise it without the
     *  message-converter {@code catch (IOException)} re-labeling it as a 400 malformed-body. */
    static final class BodySizeLimitExceededException extends RuntimeException {
        BodySizeLimitExceededException() {
            super("request body exceeds the maximum allowed size");
        }
    }

    /** Wraps {@code getInputStream()}/{@code getReader()} with the byte-counting bounded stream. */
    private static final class BoundedRequestWrapper extends HttpServletRequestWrapper {
        private BoundedServletInputStream stream;
        private BufferedReader reader;

        BoundedRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                stream = new BoundedServletInputStream(((HttpServletRequest) getRequest()).getInputStream());
            }
            return stream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            if (reader == null) {
                String enc = getCharacterEncoding();
                reader = new BufferedReader(new InputStreamReader(getInputStream(),
                        enc != null ? enc : StandardCharsets.UTF_8.name()));
            }
            return reader;
        }
    }

    /** Delegating {@link ServletInputStream} that throws once more than {@link #MAX_BODY_BYTES} are read. */
    private static final class BoundedServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private long count;

        BoundedServletInputStream(ServletInputStream delegate) {
            this.delegate = delegate;
        }

        private void tally(int read) {
            if (read > 0) {
                count += read;
                if (count > MAX_BODY_BYTES) {
                    throw new BodySizeLimitExceededException();
                }
            }
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            tally(b >= 0 ? 1 : 0);
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            tally(n);
            return n;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }
}
