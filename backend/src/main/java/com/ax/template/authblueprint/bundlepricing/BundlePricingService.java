package com.ax.template.authblueprint.bundlepricing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.ComponentBreakdown;
import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.ComponentRequest;
import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.ComponentView;
import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.CompositeItemResponse;
import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.CreateCompositeItemRequest;
import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.FeeRequest;
import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.FeeView;
import com.ax.template.authblueprint.bundlepricing.BundlePricingDtos.PricedBundleResponse;
import com.ax.template.authblueprint.bundlepricing.BundlePricingExceptions.CompositeItemNotFoundException;
import com.ax.template.authblueprint.bundlepricing.BundlePricingExceptions.InvalidCompositeItemException;

/**
 * Sole orchestrator + sole mutator for the bundle-pricing domain.
 *
 * <p>All repository access, the create-time validation of the mode/base-price invariant, and
 * the conserving roll-up derivation live here. The roll-up arithmetic itself is owned by the
 * {@link CompositeItem} aggregate ({@link CompositeItem#priceRollUp()}) — this service reads it
 * and maps to a DTO inside the transaction (lazy collections stay session-bound).
 *
 * <p>Invariant traces: BUNDLE-ITEMSUM-001, BUNDLE-FIXED-001, BUNDLE-DERIVED-001.
 */
@Service
public class BundlePricingService {

    private final CompositeItemRepository repository;

    public BundlePricingService(CompositeItemRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a composite item, validating the mode/base-price invariant BEFORE persist
     * (the @Check is the DB backstop; this gives a clean 400 + message). Writes ONE aggregate
     * root (the composite + its cascaded children/fees).
     */
    @Transactional
    public CompositeItemResponse create(CreateCompositeItemRequest req) {
        validate(req);

        CompositeItem composite = CompositeItem.builder()
            .pricingModel(req.pricingModel())
            .currency(req.currency())
            .baseRetailPrice(req.baseRetailPrice())
            .baseSalePrice(req.baseSalePrice())
            .build();

        if (req.components() != null) {
            for (ComponentRequest c : req.components()) {
                composite.addComponent(new CompositeComponent(
                    UUID.randomUUID(), c.name(), c.quantity(),
                    c.unitRetailPrice(), c.unitSalePrice(), c.taxable()));
            }
        }
        if (req.fees() != null) {
            for (FeeRequest f : req.fees()) {
                composite.addFee(new BundleFee(f.label(), f.amount(), f.taxable()));
            }
        }

        return toDefinition(repository.save(composite));
    }

    @Transactional(readOnly = true)
    public CompositeItemResponse getDefinition(UUID id) {
        return toDefinition(require(id));
    }

    /** Returns the conserving roll-up (BUNDLE-ITEMSUM-001 / BUNDLE-FIXED-001 / BUNDLE-DERIVED-001). */
    @Transactional(readOnly = true)
    public PricedBundleResponse price(UUID id) {
        CompositeItem composite = require(id);
        CompositeItem.Pricing pricing = composite.priceRollUp();

        List<ComponentBreakdown> breakdown = new ArrayList<>();
        for (CompositeComponent c : composite.getComponents()) {
            breakdown.add(new ComponentBreakdown(
                c.getId(), c.getName(), c.getQuantity(),
                c.getUnitRetailPrice(), c.getUnitSalePrice(), c.isTaxable(),
                c.retailSubtotal(), c.saleSubtotal()));
        }
        return new PricedBundleResponse(
            composite.getId(), composite.getPricingModel(), composite.getCurrency(),
            pricing.retailPrice(), pricing.salePrice(), pricing.taxablePrice(), pricing.taxable(),
            breakdown, feeViews(composite));
    }

    // ─── validation (mode/base-price invariant) ───────────────────────────────

    private void validate(CreateCompositeItemRequest req) {
        boolean hasComponents = req.components() != null && !req.components().isEmpty();

        if (req.pricingModel() == BundlePricingModel.ITEM_SUM) {
            if (req.baseRetailPrice() != null || req.baseSalePrice() != null) {
                throw new InvalidCompositeItemException(
                    "ITEM_SUM composite MUST NOT carry a base price — its price is summed from its children");
            }
            if (!hasComponents) {
                throw new InvalidCompositeItemException(
                    "ITEM_SUM composite MUST have at least one child component to sum");
            }
        } else { // BUNDLE
            if (req.baseRetailPrice() == null) {
                throw new InvalidCompositeItemException(
                    "BUNDLE composite MUST carry a fixed base retail price");
            }
            if (req.baseRetailPrice() < 0) {
                throw new InvalidCompositeItemException("base retail price must be non-negative");
            }
            if (req.baseSalePrice() != null
                    && (req.baseSalePrice() < 0 || req.baseSalePrice() > req.baseRetailPrice())) {
                throw new InvalidCompositeItemException(
                    "base sale price must be between 0 and the base retail price");
            }
        }

        if (req.components() != null) {
            for (ComponentRequest c : req.components()) {
                if (c.quantity() <= 0) {
                    throw new InvalidCompositeItemException("component quantity must be positive");
                }
                if (c.unitRetailPrice() < 0) {
                    throw new InvalidCompositeItemException("component unit retail price must be non-negative");
                }
                if (c.unitSalePrice() != null
                        && (c.unitSalePrice() < 0 || c.unitSalePrice() > c.unitRetailPrice())) {
                    throw new InvalidCompositeItemException(
                        "component unit sale price must be between 0 and its unit retail price");
                }
            }
        }
        if (req.fees() != null) {
            for (FeeRequest f : req.fees()) {
                if (f.amount() < 0) {
                    throw new InvalidCompositeItemException("fee amount must be non-negative");
                }
            }
        }
    }

    // ─── mapping ──────────────────────────────────────────────────────────────

    private CompositeItem require(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new CompositeItemNotFoundException(id));
    }

    private CompositeItemResponse toDefinition(CompositeItem composite) {
        List<ComponentView> components = new ArrayList<>();
        for (CompositeComponent c : composite.getComponents()) {
            components.add(new ComponentView(
                c.getId(), c.getName(), c.getQuantity(),
                c.getUnitRetailPrice(), c.getUnitSalePrice(), c.isTaxable()));
        }
        return new CompositeItemResponse(
            composite.getId(), composite.getPricingModel(), composite.getCurrency(),
            composite.getVersion(), composite.getBaseRetailPrice(), composite.getBaseSalePrice(),
            components, feeViews(composite));
    }

    private List<FeeView> feeViews(CompositeItem composite) {
        List<FeeView> fees = new ArrayList<>();
        for (BundleFee f : composite.getFees()) {
            fees.add(new FeeView(f.getLabel(), f.getAmount(), f.isTaxable()));
        }
        return fees;
    }
}
