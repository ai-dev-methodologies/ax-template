package com.ax.template.authblueprint.practices;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised exception → RFC-7807 ProblemDetail mapper for the practices demo package.
 * Scoped to `basePackages` so the existing AuthExceptionHandler in other packages is not
 * affected. Every emitted body is `application/problem+json` and carries a stable
 * `type` URI plus a human title and detail — never the underlying exception's stack
 * trace or class name.
 */
@RestControllerAdvice(basePackages = "com.ax.template.authblueprint.practices")
public class PracticesProblemDetailAdvice {

    private static final URI BAD_ARGUMENT_TYPE = URI.create("https://errors.example.com/bad-argument");
    private static final URI NOT_FOUND_TYPE = URI.create("https://errors.example.com/not-found");
    private static final URI VALIDATION_TYPE = URI.create("https://errors.example.com/validation");

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(BAD_ARGUMENT_TYPE);
        pd.setTitle("Bad Argument");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoSuchElementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(NOT_FOUND_TYPE);
        pd.setTitle("Resource Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setType(VALIDATION_TYPE);
        pd.setTitle("Validation Error");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage() == null ? "invalid" : err.getDefaultMessage()
                ))
                .toList();
        pd.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
