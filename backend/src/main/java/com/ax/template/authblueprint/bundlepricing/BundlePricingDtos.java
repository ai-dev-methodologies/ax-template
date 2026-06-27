package com.ax.template.authblueprint.bundlepricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class BundlePricingDtos {

    private BundlePricingDtos() {}

    // ─── requests ────────────────────────────────────────────────────────────

    public record CreateCompositeItemRequest(
        @NotNull BundlePricingModel pricingModel,
        @NotNull @Size(min = 3, max = 3) String currency,
        Long baseRetailPrice,
        Long baseSalePrice,
        @Valid List<ComponentRequest> components,
        @Valid List<FeeRequest> fees
    ) {}

    public record ComponentRequest(
        @Size(max = 200) String name,
        int quantity,
        long unitRetailPrice,
        Long unitSalePrice,
        boolean taxable
    ) {}

    public record FeeRequest(
        @Size(max = 200) String label,
        long amount,
        boolean taxable
    ) {}

    // ─── definition echo ─────────────────────────────────────────────────────

    public record CompositeItemResponse(
        UUID id,
        BundlePricingModel pricingModel,
        String currency,
        long version,
        Long baseRetailPrice,
        Long baseSalePrice,
        List<ComponentView> components,
        List<FeeView> fees
    ) {}

    public record ComponentView(
        UUID id,
        String name,
        int quantity,
        long unitRetailPrice,
        Long unitSalePrice,
        boolean taxable
    ) {}

    public record FeeView(
        String label,
        long amount,
        boolean taxable
    ) {}

    // ─── priced roll-up ──────────────────────────────────────────────────────

    /**
     * The conserving roll-up. {@code retailPrice}/{@code salePrice}/{@code taxablePrice} are the
     * authoritative composite totals; the per-component {@code retailSubtotal}/{@code saleSubtotal}
     * breakdown is disclosed so a consumer can INDEPENDENTLY reconstruct the total (Σ subtotals +
     * Σ fees == total) — the conservation cross-check (BUNDLE-ITEMSUM-001).
     */
    public record PricedBundleResponse(
        UUID id,
        BundlePricingModel pricingModel,
        String currency,
        long retailPrice,
        long salePrice,
        long taxablePrice,
        boolean taxable,
        List<ComponentBreakdown> components,
        List<FeeView> fees
    ) {}

    public record ComponentBreakdown(
        UUID id,
        String name,
        int quantity,
        long unitRetailPrice,
        Long unitSalePrice,
        boolean taxable,
        long retailSubtotal,
        long saleSubtotal
    ) {}
}
