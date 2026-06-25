package com.ax.template.authblueprint.commercepricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Commerce pricing thin controller. Delegates all invariant logic to {@link PricingPipeline}. */
@RestController
public class PricingController {

    // ── Request DTO ───────────────────────────────────────────────────────────────

    public record LineReq(@NotBlank String sku, long amount) {}

    public record QuoteReq(
        @NotNull List<LineReq> lines,
        long orderDiscount,
        long shipping,
        int taxBasisPoints) {}

    // ── Fields ────────────────────────────────────────────────────────────────────

    private final PricingPipeline pipeline;
    private final PricingMetrics metrics;

    public PricingController(PricingPipeline pipeline, PricingMetrics metrics) {
        this.pipeline = pipeline;
        this.metrics = metrics;
    }

    // ── Endpoint ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/pricing/quote — compute a stateless pricing quote.
     * Caller identity is Authentication.getName() (read-only; no ownership state written).
     */
    @PostMapping("/api/pricing/quote")
    public ResponseEntity<PricingPipeline.PricedOrder> quote(
            @Valid @RequestBody QuoteReq req,
            Authentication auth) {
        List<PricingPipeline.Line> lines = req.lines().stream()
            .map(l -> new PricingPipeline.Line(l.sku(), l.amount()))
            .toList();

        PricingPipeline.PricedOrder result;
        try {
            result = pipeline.priceOrder(lines, req.orderDiscount(), req.shipping(), req.taxBasisPoints());
        } catch (PricingException ex) {
            metrics.recordQuote("error");
            throw ex;
        }
        metrics.recordQuote("ok");
        return ResponseEntity.ok(result);
    }

    // ── Exception handler ─────────────────────────────────────────────────────────

    @ExceptionHandler(PricingException.class)
    public ResponseEntity<ProblemDetail> handle(PricingException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
