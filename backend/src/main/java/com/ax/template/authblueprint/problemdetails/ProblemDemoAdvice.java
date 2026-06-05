package com.ax.template.authblueprint.problemdetails;

import com.ax.template.authblueprint.observability.MdcCorrelationIdInterceptor;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * problem-details-l0 contract enforcer for the {@code problemdetails} reference surface.
 *
 * <p>{@code basePackages}-scoped so it ONLY claims this package's controllers (never masks
 * another domain's exceptions), and {@code @Order(HIGHEST_PRECEDENCE)} so for those
 * controllers it wins over the {@code LOWEST_PRECEDENCE}
 * {@code common.GlobalProblemDetailAdvice} fallback — giving every demo error body the
 * uniform RFC 9457 shape: stable {@code type} URI, top-level extension members, a
 * {@code trace_id}, an {@code Accept-Language}-localized {@code detail}, and bounded-label
 * metrics.
 *
 * <p>Every member of the {@code emit(...)} path is anchored to a spec item:
 * <ul>
 *   <li>FORMAT-001 — five RFC 9457 members ({@code type}/{@code title}/{@code status}/
 *       {@code detail}/{@code instance}) + {@code application/problem+json};</li>
 *   <li>TYPE-001 — {@code type} from the closed {@link ProblemTypeRegistry};</li>
 *   <li>EXTENSION-001 — domain context as TOP-LEVEL members (e.g. {@code balance});</li>
 *   <li>VALIDATION-001 — every field error in one {@code errors[]} array w/ RFC 6901 pointer;</li>
 *   <li>TRACE-001 — {@code trace_id} (W3C {@code traceparent} or correlation id) + {@code Trace-Id}
 *       header, with NO stack/SQL/secret leaked into {@code detail};</li>
 *   <li>I18N-001 — localized {@code detail} via {@link MessageSource} + {@code Content-Language},
 *       {@code type} URI byte-identical across locales;</li>
 *   <li>OBSERVABILITY-001 — {@link ProblemMetrics} bounded-label meters.</li>
 * </ul>
 * Spec: specs/problem-details-l0.yaml.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.problemdetails")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProblemDemoAdvice {

    private static final Logger log = LoggerFactory.getLogger(ProblemDemoAdvice.class);

    private final MessageSource messageSource;
    private final ProblemMetrics metrics;

    public ProblemDemoAdvice(MessageSource messageSource, ProblemMetrics metrics) {
        this.messageSource = messageSource;
        this.metrics = metrics;
    }

    /** EXTENSION-001 + I18N-001 + the FORMAT-001 happy path: a localized 402 with structured context. */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientFunds(
            InsufficientFundsException ex, HttpServletRequest request) {
        // Resolve via the i18n-policy AcceptHeaderLocaleResolver (supported locales + explicit
        // default), not the raw servlet locale — honors I18N-LOCALE-NEG-001.
        Locale locale = RequestContextUtils.getLocale(request);
        String detail = localize("problem.insufficient-funds.detail", locale,
                "The account has insufficient funds to complete this transfer.");
        return emit(HttpStatus.PAYMENT_REQUIRED, InsufficientFundsException.SLUG,
                "Insufficient Funds", "INSUFFICIENT_FUNDS", detail, request, locale, pd -> {
                    // Top-level extension members — siblings of type/title/status/detail (NOT nested,
                    // NOT folded into the detail prose).
                    pd.setProperty("balance", ex.balance());
                    pd.setProperty("accounts", ex.accounts());
                });
    }

    /** VALIDATION-001 — ALL field + object errors in one {@code errors[]} array, each with an RFC 6901 pointer. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.add(errorEntry(fe.getField(), code(fe.getCode()), message(fe.getDefaultMessage())));
        }
        for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
            errors.add(errorEntry(oe.getObjectName(), code(oe.getCode()), message(oe.getDefaultMessage())));
        }
        return emit(HttpStatus.BAD_REQUEST, ProblemTypeRegistry.VALIDATION,
                "Validation Failed", "VALIDATION_FAILED", "One or more fields failed validation.",
                request, null, pd -> pd.setProperty("errors", errors));
    }

    /**
     * TRACE-001 — a genuine 5xx. Maps ONLY the dedicated {@link DemoServerFaultException} (NOT a
     * broad {@code Exception}) so this HIGHEST_PRECEDENCE package advice never masks the framework
     * handlers in {@code common.GlobalProblemDetailAdvice}. The {@code detail} is a fixed neutral
     * message; the exception's own message (a stack frame + SQLSTATE) is logged server-side against
     * the same {@code trace_id} but NEVER returned to the client.
     */
    @ExceptionHandler(DemoServerFaultException.class)
    public ResponseEntity<ProblemDetail> handleServerFault(DemoServerFaultException ex, HttpServletRequest request) {
        String traceId = traceId(request);
        // Detailed context goes to the LOG (correlated by trace_id), not the response body.
        log.error("problem-demo server fault trace_id={}", traceId, ex);
        return emit(HttpStatus.INTERNAL_SERVER_ERROR, ProblemTypeRegistry.SERVER_ERROR,
                "Internal Server Error", "INTERNAL_ERROR",
                "An unexpected error occurred. Reference the trace id when reporting this.",
                request, null, pd -> {});
    }

    // ── shared emit path ───────────────────────────────────────────────────────

    private interface Customizer {
        void apply(ProblemDetail pd);
    }

    private ResponseEntity<ProblemDetail> emit(
            HttpStatus status, String slug, String title, String code, String detail,
            HttpServletRequest request, Locale localizedLocale, Customizer customizer) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail); // status member == HTTP status (FORMAT-001)
        pd.setType(ProblemTypeRegistry.uri(slug));                            // stable machine key (TYPE-001)
        pd.setTitle(title);                                                   // stable per-type label (I18N-001)
        pd.setInstance(URI.create(request.getRequestURI()));                 // fifth member (FORMAT-001)
        pd.setProperty("code", code);                                         // client branches on code, not prose

        String traceId = traceId(request);
        pd.setProperty("trace_id", traceId);                                 // TRACE-001
        customizer.apply(pd);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.set("Trace-Id", traceId);                                    // echoed correlation (TRACE-001)
        if (localizedLocale != null) {
            headers.set(HttpHeaders.CONTENT_LANGUAGE, localizedLocale.toLanguageTag()); // I18N-001
        }

        metrics.record(slug, status.value(), elapsed(request));             // OBSERVABILITY-001
        return ResponseEntity.status(status).headers(headers).body(pd);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * TRACE-001 trace id: the W3C {@code traceparent} trace-id when a valid trace context is
     * present, else the per-request correlation id the {@link MdcCorrelationIdInterceptor}
     * already put in MDC (a server-generated UUID when no inbound id), never blank.
     */
    private static String traceId(HttpServletRequest request) {
        String fromTraceparent = traceparentTraceId(request.getHeader("traceparent"));
        if (fromTraceparent != null) {
            return fromTraceparent;
        }
        String mdc = MDC.get(MdcCorrelationIdInterceptor.MDC_TRACE_ID);
        return (mdc == null || mdc.isBlank()) ? "unknown" : mdc;
    }

    /**
     * Extract the 32-hex trace-id from a W3C {@code traceparent} header
     * ({@code version-trace_id-parent_id-flags}). Returns null when absent or malformed,
     * or when the trace-id is the invalid all-zeroes value.
     * W3C Trace Context §3.2.2.3.
     */
    static String traceparentTraceId(String traceparent) {
        if (traceparent == null) {
            return null;
        }
        String[] parts = traceparent.trim().split("-");
        if (parts.length < 3) {
            return null;
        }
        String traceId = parts[1];
        if (traceId.length() != 32 || !traceId.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            return null;
        }
        if (traceId.chars().allMatch(c -> c == '0')) {
            return null; // all-zeroes trace-id is invalid per the spec
        }
        return traceId;
    }

    private long startNanos(HttpServletRequest request) {
        Object attr = request.getAttribute(ProblemRequestTimer.START_NANOS_ATTR);
        return attr instanceof Long l ? l : System.nanoTime();
    }

    private Duration elapsed(HttpServletRequest request) {
        return Duration.ofNanos(System.nanoTime() - startNanos(request));
    }

    private String localize(String key, Locale locale, String fallback) {
        try {
            return messageSource.getMessage(key, null, locale);
        } catch (NoSuchMessageException e) {
            return fallback;
        }
    }

    private static Map<String, String> errorEntry(String field, String code, String message) {
        String safe = field == null ? "" : field;
        return Map.of(
                "field", safe,
                "name", safe,
                "pointer", "/" + safe.replace('.', '/'), // RFC 6901 JSON Pointer
                "code", code,
                "message", message,
                "detail", message);
    }

    private static String code(String c) {
        return (c == null || c.isBlank()) ? "Invalid" : c;
    }

    private static String message(String m) {
        return (m == null || m.isBlank()) ? "invalid" : m;
    }
}
