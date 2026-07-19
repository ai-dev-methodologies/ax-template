package com.ax.template.authblueprint.billing;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * R21 admin billing endpoints.
 * <p>SecurityConfig pins {@code /api/admin/**} to ROLE_ADMIN (BILLING-AUTHZ-003).
 * This controller ALSO declares a class-level
 * {@code @PreAuthorize("hasAuthority('ROLE_ADMIN')")} as defense-in-depth: method
 * security is the primary, locally-verifiable gate (see
 * {@code admin_preauthorize_guard.sh}), and the path matcher stays as a
 * complementary layer.
 */
@RestController
@RequestMapping("/api/admin/billing")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class BillingAdminController {

    public static final String VALIDATION_TYPE = "https://ax-template.dev/problems/billing-validation";
    public static final String INVALID_AMOUNT_TYPE = "urn:ax:billing:invalid-amount";

    private final BillingService service;

    public BillingAdminController(BillingService service) {
        this.service = service;
    }

    @PostMapping("/plans")
    public ResponseEntity<BillingDto.PlanResponse> createPlan(
        @RequestBody @Valid BillingDto.CreatePlanRequest req) {

        Plan plan = service.createPlan(req.name(), req.amount(), req.currency(), req.billingCycle());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BillingDto.PlanResponse.from(plan));
    }

    @GetMapping("/plans")
    public List<BillingDto.PlanResponse> listPlans(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Page<Plan> p = service.listPlans(PageRequest.of(page, safeSize));
        return p.getContent().stream().map(BillingDto.PlanResponse::from).toList();
    }

    // ─── error mapping ─────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(URI.create(VALIDATION_TYPE));
        pd.setTitle("Validation failed");
        return ResponseEntity.badRequest().body(pd);
    }

    /**
     * BILLING-CUR-001 — Jackson rejects float JSON into a {@code long}/{@code Long}
     * field with {@link HttpMessageNotReadableException}. Translate to a
     * ProblemDetail with the {@code urn:ax:billing:invalid-amount} type so the
     * client can distinguish from generic validation errors.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
        String msg = ex.getMostSpecificCause() == null
            ? "invalid request body"
            : ex.getMostSpecificCause().getMessage();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            msg != null && msg.contains("amount")
                ? "amount must be an integer in minor units (e.g. won for KRW, cents for USD); float not accepted"
                : "invalid request body");
        pd.setType(URI.create(INVALID_AMOUNT_TYPE));
        pd.setTitle("Invalid amount encoding");
        return ResponseEntity.badRequest().body(pd);
    }
}
