package com.ax.template.authblueprint.taxapplication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
import java.util.UUID;

/**
 * Thin tax-application controller — delegates ALL tax logic to {@link TaxApplicationService}.
 * Defining the taxable order, declaring exemption, and re-pricing (recompute) are mutating ADMIN
 * catalog operations ({@code /api/admin/tax-orders/**}, gated by the upstream {@code /api/admin/**}
 * ROLE_ADMIN rule + the {@code @PreAuthorize} backstop). Reading the combined tax requires a valid
 * JWT.
 */
@RestController
public class TaxApplicationController {

    // ── Request / Response DTOs ──────────────────────────────────────────────────

    public record LineDto(long taxableBaseMinor, boolean exempt) {}

    public record CreateOrderReq(boolean customerExempt, List<LineDto> lines) {}

    public record ExemptReq(boolean customerExempt) {}

    public record RecomputeReq(@NotNull Long rateBasisPoints) {}

    public record OrderDto(UUID id, boolean customerExempt, List<LineDto> lines, Instant createdAt) {
        static OrderDto of(TaxableOrder o) {
            List<LineDto> lines = o.getLines().stream()
                .map(l -> new LineDto(l.getTaxableBaseMinor(), l.isExempt()))
                .toList();
            return new OrderDto(o.getId(), o.isCustomerExempt(), lines, o.getCreatedAt());
        }
    }

    public record TaxDto(UUID orderId, boolean present, long taxAmountMinor, UUID assessmentId) {
        static TaxDto of(TaxApplicationService.TaxResult r) {
            return new TaxDto(r.orderId(), r.present(), r.taxAmountMinor(), r.assessmentId());
        }
    }

    private final TaxApplicationService service;

    public TaxApplicationController(TaxApplicationService service) {
        this.service = service;
    }

    // ── Taxable-order definition (ADMIN) ─────────────────────────────────────────

    @PostMapping("/api/admin/tax-orders")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderReq req) {
        List<TaxLine> lines = req.lines() == null ? List.of()
            : req.lines().stream().map(l -> new TaxLine(l.taxableBaseMinor(), l.exempt())).toList();
        TaxableOrder order = service.createOrder(req.customerExempt(), lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderDto.of(order));
    }

    @PostMapping("/api/admin/tax-orders/{id}/exempt-customer")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public OrderDto declareExempt(@PathVariable UUID id, @Valid @RequestBody ExemptReq req) {
        return OrderDto.of(service.declareCustomerExempt(id, req.customerExempt()));
    }

    @PostMapping("/api/admin/tax-orders/{id}/recompute")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TaxDto recompute(@PathVariable UUID id, @Valid @RequestBody RecomputeReq req) {
        return TaxDto.of(service.recompute(id, req.rateBasisPoints()));
    }

    // ── Read (authenticated) ─────────────────────────────────────────────────────

    @GetMapping("/api/tax-orders/{id}")
    public OrderDto getOrder(@PathVariable UUID id) {
        return OrderDto.of(service.getOrder(id));
    }

    @GetMapping("/api/tax-orders/{id}/tax")
    public TaxDto getTax(@PathVariable UUID id) {
        return TaxDto.of(service.currentTax(id));
    }

    // ── Exception handler ────────────────────────────────────────────────────────

    @ExceptionHandler(TaxApplicationException.class)
    public ResponseEntity<ProblemDetail> handle(TaxApplicationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
