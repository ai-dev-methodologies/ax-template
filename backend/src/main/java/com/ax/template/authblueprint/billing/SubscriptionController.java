package com.ax.template.authblueprint.billing;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * R21 user-facing subscription endpoints.
 * <p>Trace:
 * <ul>
 *   <li>BILLING-AUTHZ-001 — SecurityConfig pins {@code /api/subscriptions/**} to authenticated.</li>
 *   <li>BILLING-AUTHZ-002 — cross-user lookup → 404 (never 403).</li>
 *   <li>BILLING-CUR-001 — float amounts rejected by Jackson on the admin
 *       create-plan path; this controller does not accept amount inputs.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    public static final String SUB_NOT_FOUND_TYPE = "https://ax-template.dev/problems/subscription-not-found";
    public static final String PLAN_NOT_FOUND_TYPE = "https://ax-template.dev/problems/plan-not-found";

    private final BillingService service;

    public SubscriptionController(BillingService service) {
        this.service = service;
    }

    @GetMapping
    public BillingDto.SubscriptionList list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        int safeSize = Math.min(size, 100);
        Page<Subscription> p = service.listOwn(userId,
            PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<BillingDto.SubscriptionResponse> items = p.getContent().stream()
            .map(BillingDto.SubscriptionResponse::from)
            .toList();
        return new BillingDto.SubscriptionList(items, p.getTotalElements());
    }

    @PostMapping
    public ResponseEntity<BillingDto.SubscriptionResponse> create(
        @RequestBody @Valid BillingDto.CreateSubscriptionRequest req,
        @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        Subscription sub = service.createSubscription(userId, req.planId(), req.provider());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(BillingDto.SubscriptionResponse.from(sub));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillingDto.SubscriptionResponse> get(
        @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {

        return service.findOwn(id, jwt.getSubject())
            .map(s -> ResponseEntity.ok(BillingDto.SubscriptionResponse.from(s)))
            .orElseThrow(() -> new BillingException.SubscriptionNotFound(id));
    }

    @PostMapping("/{id}/cancel")
    public BillingDto.SubscriptionResponse cancel(
        @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return BillingDto.SubscriptionResponse.from(service.cancelOwn(id, jwt.getSubject()));
    }

    // ─── error mapping ─────────────────────────────────────────────────────────

    @ExceptionHandler(BillingException.SubscriptionNotFound.class)
    public ResponseEntity<ProblemDetail> handleSubNotFound(BillingException.SubscriptionNotFound ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(SUB_NOT_FOUND_TYPE));
        pd.setTitle("Subscription not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(BillingException.PlanNotFound.class)
    public ResponseEntity<ProblemDetail> handlePlanNotFound(BillingException.PlanNotFound ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(PLAN_NOT_FOUND_TYPE));
        pd.setTitle("Plan not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "invalid request body");
        return ResponseEntity.badRequest().body(pd);
    }
}
