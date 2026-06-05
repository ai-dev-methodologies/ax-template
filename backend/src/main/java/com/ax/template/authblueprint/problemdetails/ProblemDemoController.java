package com.ax.template.authblueprint.problemdetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * problem-details-l0 reference workload — a self-contained surface that deliberately
 * triggers the three problem shapes the contract must cover, each routed through
 * {@link ProblemDemoAdvice} so every error body is uniformly RFC 9457
 * {@code application/problem+json}:
 *
 * <ul>
 *   <li>{@code POST /api/problem-demo/insufficient-funds} → a custom 402 carrying a stable
 *       {@code type} URI (TYPE-001), top-level {@code balance}/{@code accounts} extension
 *       members (EXTENSION-001), an {@code Accept-Language}-localized {@code detail}
 *       (I18N-001), and a {@code trace_id} (TRACE-001) — the all-members happy path for
 *       FORMAT-001;</li>
 *   <li>{@code POST /api/problem-demo/validate} → a 400 whose body reports EVERY field
 *       error in one {@code errors[]} array with RFC 6901 pointers (VALIDATION-001);</li>
 *   <li>{@code POST /api/problem-demo/boom} → a deliberate 500 proving the {@code detail}
 *       leaks no stack trace and still carries a {@code trace_id} (TRACE-001).</li>
 * </ul>
 *
 * <p>The controller stays thin: it only raises typed signals. All problem+json shaping,
 * metrics, trace correlation, and localization live in {@link ProblemDemoAdvice}.
 * Spec: specs/problem-details-l0.yaml.
 */
@RestController
@RequestMapping("/api/problem-demo")
public class ProblemDemoController {

    /** Validation target — two independently-validated fields so VALIDATION-001 can assert "all errors, not fail-fast". */
    public record TransferRequest(
            @NotBlank String fromAccount,
            @Positive BigDecimal amount) {}

    @PostMapping("/insufficient-funds")
    public ResponseEntity<Void> insufficientFunds() {
        // Structured context lives on the exception, never in a detail string (EXTENSION-001).
        throw new InsufficientFundsException(
                new BigDecimal("12.50"), List.of("acct-1001", "acct-2002"));
    }

    @PostMapping("/validate")
    public ResponseEntity<Void> validate(@Valid @RequestBody TransferRequest request) {
        // Unreachable on invalid input: MethodArgumentNotValidException is raised during
        // binding and mapped by ProblemDemoAdvice into the errors[] array.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/boom")
    public ResponseEntity<Void> boom() {
        // A genuine server fault carrying internals that MUST NOT reach the client detail.
        throw new IllegalStateException(
                "ledger row 4471 failed at com.ax.template.ledger.PostingEngine.flush() — SQLSTATE 40001");
    }
}
