package com.ax.template.authblueprint.offereligibility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Thin offer-eligibility controller — delegates ALL applicability logic to
 * {@link OfferEligibilityService}. Offer DEFINITION is an ADMIN catalog mutation
 * ({@code POST /api/admin/offers}, {@code @PreAuthorize} backstop); reading and evaluating
 * eligibility require a valid JWT.
 */
@RestController
public class OfferEligibilityController {

    // ── Request / Response DTOs ──────────────────────────────────────────────────

    public record CreateOfferReq(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String qualifierSku,
        @Size(max = 100) String qualifierTag,
        @Min(1) int minQualifierQty,
        @Size(max = 100) String targetSku,
        @Size(max = 100) String targetTag,
        long discountBasisPoints,
        @Size(max = 100) String eligibleSegment,
        Set<UUID> eligibleCustomerIds) {}

    public record LineDto(String sku, String tag, int quantity) {}

    public record EvaluateReq(
        @NotNull UUID customerId,
        Set<String> customerSegments,
        List<LineDto> lines) {}

    public record OfferDto(UUID id, String name, String qualifierSku, String qualifierTag,
                           int minQualifierQty, String targetSku, String targetTag,
                           long discountBasisPoints, String eligibleSegment,
                           Set<UUID> eligibleCustomerIds, Instant createdAt) {
        static OfferDto of(EligibilityOffer o) {
            return new OfferDto(o.getId(), o.getName(), o.getQualifierSku(), o.getQualifierTag(),
                o.getMinQualifierQty(), o.getTargetSku(), o.getTargetTag(), o.getDiscountBasisPoints(),
                o.getEligibleSegment(), o.getEligibleCustomerIds(), o.getCreatedAt());
        }
    }

    public record DecisionDto(UUID offerId, boolean applied, String reason) {
        static DecisionDto of(OfferEligibilityService.EligibilityDecision d) {
            return new DecisionDto(d.offerId(), d.applied(), d.reason().name());
        }
    }

    private final OfferEligibilityService service;

    public OfferEligibilityController(OfferEligibilityService service) {
        this.service = service;
    }

    // ── Offer definition (ADMIN) ─────────────────────────────────────────────────

    @PostMapping("/api/admin/offers")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OfferDto> createOffer(@Valid @RequestBody CreateOfferReq req) {
        EligibilityOffer offer = service.createOffer(req.name(), req.qualifierSku(), req.qualifierTag(),
            req.minQualifierQty(), req.targetSku(), req.targetTag(), req.discountBasisPoints(),
            req.eligibleSegment(), req.eligibleCustomerIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(OfferDto.of(offer));
    }

    @GetMapping("/api/offers/{id}")
    public OfferDto getOffer(@PathVariable UUID id) {
        return OfferDto.of(service.getOffer(id));
    }

    // ── Evaluate eligibility (authenticated) ─────────────────────────────────────

    /**
     * Evaluate an offer against a SUPPLIED order/customer context. This is a pure
     * <em>evaluate-supplied-context</em> primitive: it computes the deterministic, fail-closed
     * applicability decision for the {@code customerId} and {@code customerSegments} passed in the
     * request body. It does NOT bind the evaluated identity to the authenticated principal and does
     * NOT itself enforce that the caller owns the supplied customer context — it assumes a TRUSTED
     * UPSTREAM has already resolved and authorized that context. The CWE-285 (Improper Authorization)
     * concern this domain anchors to is the gating of the discount as a protected resource downstream,
     * not an ownership check at this evaluation seam; a deployment that exposes this endpoint to an
     * untrusted caller MUST resolve the customer context from the authenticated principal upstream.
     */
    @PostMapping("/api/offers/{id}/evaluate")
    public DecisionDto evaluate(@PathVariable UUID id, @Valid @RequestBody EvaluateReq req) {
        List<OfferEligibilityService.Line> lines = req.lines() == null ? List.of()
            : req.lines().stream()
                .map(l -> new OfferEligibilityService.Line(l.sku(), l.tag(), l.quantity()))
                .toList();
        var ctx = new OfferEligibilityService.EvaluationContext(
            req.customerId(), req.customerSegments(), lines);
        return DecisionDto.of(service.evaluate(id, ctx));
    }

    // ── Exception handler ────────────────────────────────────────────────────────

    @ExceptionHandler(OfferEligibilityException.class)
    public ResponseEntity<ProblemDetail> handle(OfferEligibilityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
