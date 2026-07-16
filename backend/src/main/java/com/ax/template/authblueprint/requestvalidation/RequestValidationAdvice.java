package com.ax.template.authblueprint.requestvalidation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * request-validation-l0 contract enforcer for the {@code requestvalidation} reference
 * surface. {@code basePackages}-scoped + {@code @Order(HIGHEST_PRECEDENCE)} so it claims ONLY
 * this package's binding/validation failures (never masks another domain) and wins over the
 * {@code LOWEST_PRECEDENCE} {@code common.GlobalProblemDetailAdvice} for this package.
 *
 * <p>It SPECIALIZES problem-details-l0 PROBLEM-VALIDATION-001 — same RFC 9457 {@code errors}
 * extension array (each entry has {@code pointer}/{@code name} + {@code detail}) and ADDS a
 * stable {@code code} per entry — rather than defining a competing envelope (VALIDATION-ERROR-001).
 * Every violation from one request appears in the single array (no fail-fast). Each failure
 * also increments the bounded {@link RequestValidationMetrics} (VALIDATION-OBSERVABILITY-001).
 *
 * <p>Spec: specs/request-validation-l0.yaml.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.requestvalidation")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestValidationAdvice {

    private static final URI VALIDATION_TYPE = URI.create("https://errors.example.com/validation");

    private final RequestValidationMetrics metrics;

    public RequestValidationAdvice(RequestValidationMetrics metrics) {
        this.metrics = metrics;
    }

    /** @Valid body-binding failures — every field + cross-field + object error, no fail-fast. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.add(entry(pointerFromSpring(fe.getField()), code(fe.getCode()), message(fe.getDefaultMessage())));
        }
        for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
            errors.add(entry("", code(oe.getCode()), message(oe.getDefaultMessage())));
        }
        return problem(errors);
    }

    /** @Validated method-parameter / path / query constraint failures. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleParamValidation(ConstraintViolationException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        if (ex.getConstraintViolations() != null) {
            for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
                errors.add(entry(pointerFromSpring(leaf(cv.getPropertyPath())),
                        code(annotationCode(cv)), message(cv.getMessage())));
            }
        }
        return problem(errors);
    }

    /**
     * Strict-type / unknown-field / malformed-body binding failures (VALIDATION-TYPE-001).
     * No {@code BindingResult} exists for these, so the offending field + a stable code are
     * recovered from the Jackson cause: a {@link UnrecognizedPropertyException} → {@code UnknownField},
     * any other {@link DatabindException} (wrong type, strict numeric, unlisted enum) →
     * {@code TypeMismatch}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
        String pointer = "";
        String code = "Malformed";
        String detail = "Request body is missing or could not be parsed.";
        Throwable cause = ex.getCause();
        if (cause instanceof UnrecognizedPropertyException upe) {
            pointer = pointerFromJackson(upe.getPath());
            code = "UnknownField";
            detail = "Unknown field is not permitted by the schema.";
        } else if (cause instanceof DatabindException jme) {
            pointer = pointerFromJackson(jme.getPath());
            code = "TypeMismatch";
            detail = "Field value does not match the declared type; no coercion is applied.";
        }
        List<Map<String, String>> errors = new ArrayList<>();
        errors.add(entry(pointer, code, detail));
        return problem(errors);
    }

    // ── shared problem+json builder ─────────────────────────────────────────────

    private ResponseEntity<ProblemDetail> problem(List<Map<String, String>> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields failed validation.");
        pd.setType(VALIDATION_TYPE);
        pd.setTitle("Validation Failed");
        pd.setProperty("code", "VALIDATION_FAILED");
        pd.setProperty("errors", errors);

        for (Map<String, String> e : errors) {
            metrics.failure(e.get("pointer"), e.get("code")); // OBSERVABILITY-001: bounded {field,code}
        }
        metrics.rejected(HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * One errors[] entry in the EXACT shape {@code common.GlobalProblemDetailAdvice.errorEntry()}
     * emits — {@code {field, name, pointer, code, message, detail}} — so the two handlers of the
     * shared problem+json validation pattern produce the SAME entry structure (VALIDATION-ERROR-001
     * "reuses the SAME errors extension array"), adding only the {@code code} the spec specializes.
     */
    private static Map<String, String> entry(String pointer, String code, String message) {
        String p = pointer == null ? "" : pointer;
        // mirror Global: field == name == the dotted binding path; pointer == the RFC 6901 form
        String field = p.isEmpty() ? "" : p.substring(1).replace('/', '.');
        return Map.of(
                "field", field,
                "name", field,
                "pointer", p,
                "code", code,
                "message", message,
                "detail", message);
    }

    /** Spring binding path → RFC 6901 pointer: {@code items[2].quantity} → {@code /items/2/quantity}. */
    static String pointerFromSpring(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String dotted = path.replaceAll("\\[(\\d+)\\]", ".$1"); // items[2] → items.2
        StringBuilder sb = new StringBuilder();
        for (String seg : dotted.split("\\.")) {
            if (!seg.isEmpty()) {
                sb.append('/').append(seg);
            }
        }
        return sb.toString();
    }

    /** Jackson reference chain → RFC 6901 pointer (field names + collection indices). */
    static String pointerFromJackson(List<JacksonException.Reference> refs) {
        if (refs == null || refs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JacksonException.Reference ref : refs) {
            if (ref.getPropertyName() != null) {
                sb.append('/').append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                sb.append('/').append(ref.getIndex());
            }
        }
        return sb.toString();
    }

    private static String leaf(Path path) {
        String leaf = "";
        for (Path.Node node : path) {
            if (node.getName() != null) {
                leaf = node.getName();
            }
        }
        return leaf;
    }

    private static String code(String c) {
        return (c == null || c.isBlank()) ? "Invalid" : c;
    }

    private static String message(String m) {
        return (m == null || m.isBlank()) ? "invalid" : m;
    }

    private static String annotationCode(ConstraintViolation<?> cv) {
        if (cv.getConstraintDescriptor() == null || cv.getConstraintDescriptor().getAnnotation() == null) {
            return "Invalid";
        }
        return cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
    }
}
