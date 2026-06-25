package com.ax.template.authblueprint.commercepromotion;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Commerce promotion thin controller. Delegates all invariant logic to {@link PromotionService}. */
@RestController
public class PromotionController {

    // ── Request / Response DTOs ────────────────────────────────────────────────────

    public record CreateOfferReq(
        @NotBlank @Size(max = 200) String name,
        @NotNull DiscountType discountType,
        long discountValue,
        @NotNull OfferScope scope,
        int priority,
        boolean combinable,
        boolean stackable,
        Boolean applyToSalePrice,
        long maxUses,
        long maxUsesPerCustomer,
        Instant activeStart,
        Instant activeEnd) {}

    public record LineItemDto(
        @NotBlank String skuId,
        int quantity,
        long unitPrice) {}

    public record ApplyReq(
        @NotNull List<LineItemDto> lineItems,
        List<String> offerCodes,
        @NotBlank @Size(max = 200) String orderRef) {}

    public record AdjustmentDto(UUID offerId, String offerName, String skuId, long amount) {
        static AdjustmentDto of(PromotionService.Adjustment a) {
            return new AdjustmentDto(a.offerId(), a.offerName(), a.skuId(), a.amount());
        }
    }

    public record OfferDto(UUID id, String name, DiscountType discountType, long discountValue,
                           OfferScope scope, int priority, boolean combinable, boolean stackable,
                           Boolean applyToSalePrice, long maxUses, long maxUsesPerCustomer,
                           Instant activeStart, Instant activeEnd) {
        static OfferDto of(PromoOffer o) {
            return new OfferDto(o.getId(), o.getName(), o.getDiscountType(), o.getDiscountValue(),
                o.getScope(), o.getPriority(), o.isCombinable(), o.isStackable(),
                o.getApplyToSalePrice(), o.getMaxUses(), o.getMaxUsesPerCustomer(),
                o.getActiveStart(), o.getActiveEnd());
        }
    }

    public record RedemptionDto(UUID id, UUID offerId, String customerId, String orderRef, Instant redeemedAt) {
        static RedemptionDto of(PromoOfferRedemption r) {
            return new RedemptionDto(r.getId(), r.getOfferId(), r.getCustomerId(), r.getOrderRef(), r.getRedeemedAt());
        }
    }

    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    // ── Offer CRUD ────────────────────────────────────────────────────────────────

    @PostMapping("/api/promotion/offers")
    public ResponseEntity<OfferDto> createOffer(@Valid @RequestBody CreateOfferReq req,
                                                 Authentication auth) {
        PromoOffer offer = service.createOffer(req.name(), req.discountType(), req.discountValue(),
            req.scope(), req.priority(), req.combinable(), req.stackable(),
            req.applyToSalePrice(), req.maxUses(), req.maxUsesPerCustomer(),
            req.activeStart(), req.activeEnd());
        return ResponseEntity.status(HttpStatus.CREATED).body(OfferDto.of(offer));
    }

    @GetMapping("/api/promotion/offers/{id}")
    public OfferDto getOffer(@PathVariable UUID id) {
        return OfferDto.of(service.getOffer(id));
    }

    // ── Apply ─────────────────────────────────────────────────────────────────────

    @PostMapping("/api/promotion/apply")
    public ResponseEntity<List<AdjustmentDto>> applyOffers(@Valid @RequestBody ApplyReq req,
                                                            Authentication auth) {
        List<PromotionService.LineItem> lineItems = req.lineItems().stream()
            .map(dto -> new PromotionService.LineItem(dto.skuId(), dto.quantity(), dto.unitPrice()))
            .toList();
        List<PromotionService.Adjustment> adjustments =
            service.applyOffers(lineItems, req.offerCodes(), auth.getName(), req.orderRef());
        return ResponseEntity.ok(adjustments.stream().map(AdjustmentDto::of).toList());
    }

    // ── Exception handler ─────────────────────────────────────────────────────────

    @ExceptionHandler(PromotionException.class)
    public ResponseEntity<ProblemDetail> handle(PromotionException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
