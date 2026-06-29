package com.ax.template.authblueprint.bundlepricing;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * CompositeItem — a bundle / kit whose price is a CONSERVING roll-up of its children
 * (a composite/bundle order-item's conserving roll-up).
 *
 * <p><b>The invariant, made unrepresentable-to-violate.</b> A composite has NO stored
 * rolled-up total column. Its price is a PURE derivation ({@link #priceRollUp()}) recomputed
 * on every read from its IMMUTABLE children + fees (ITEM_SUM) or its IMMUTABLE fixed base
 * (BUNDLE). Because there is no total column and no public price setter, a composite total
 * that contradicts its children is impossible to persist.
 *
 * <p>Invariant traces:
 * <ul>
 *   <li>BUNDLE-ITEMSUM-001 — ITEM_SUM mode: retail/sale/taxable = Σ child×qty + Σ fees;
 *       sale falls back to a child's retail when the child has no sale price.</li>
 *   <li>BUNDLE-FIXED-001 — BUNDLE mode: the fixed {@code baseRetailPrice} (and
 *       {@code baseSalePrice} else base retail) is used, NOT summed from children.
 *       The {@code @Check} makes the mode/base-price shape mutually exclusive.</li>
 *   <li>BUNDLE-DERIVED-001 — taxability is DERIVED (taxable iff any child or fee is
 *       taxable); there is no stored taxable boolean and no stored total to hand-edit.</li>
 * </ul>
 */
@AggregateRoot
@Entity
@Table(name = "composite_items")
@Check(constraints =
    "((pricing_model = 'BUNDLE' AND base_retail_price IS NOT NULL) "
  + "OR (pricing_model = 'ITEM_SUM' AND base_retail_price IS NULL)) "
  + "AND (base_sale_price IS NULL OR base_sale_price <= base_retail_price) "
  + "AND (pricing_model = 'BUNDLE' OR base_sale_price IS NULL)")
public class CompositeItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false, updatable = false, length = 16)
    private BundlePricingModel pricingModel;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    /** BUNDLE-mode fixed retail price (minor units) — NULL in ITEM_SUM mode (enforced by @Check). */
    @Column(name = "base_retail_price", updatable = false)
    private Long baseRetailPrice;

    /** BUNDLE-mode fixed sale price (optional override; minor units). Null ⇒ base retail. */
    @Column(name = "base_sale_price", updatable = false)
    private Long baseSalePrice;

    @OneToMany(mappedBy = "compositeItem", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CompositeComponent> components = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "composite_item_fees",
                     joinColumns = @JoinColumn(name = "composite_item_id"))
    private List<BundleFee> fees = new ArrayList<>();

    protected CompositeItem() {}

    private CompositeItem(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.pricingModel = b.pricingModel;
        this.currency = b.currency;
        this.baseRetailPrice = b.baseRetailPrice;
        this.baseSalePrice = b.baseSalePrice;
    }

    public UUID getId() { return id; }
    public long getVersion() { return version; }
    public BundlePricingModel getPricingModel() { return pricingModel; }
    public String getCurrency() { return currency; }
    public Long getBaseRetailPrice() { return baseRetailPrice; }
    public Long getBaseSalePrice() { return baseSalePrice; }

    public List<CompositeComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public List<BundleFee> getFees() {
        return Collections.unmodifiableList(fees);
    }

    /** Package-private — children/fees are attached only at creation, by the service. */
    void addComponent(CompositeComponent component) {
        component.setCompositeItem(this);
        this.components.add(component);
    }

    void addFee(BundleFee fee) {
        this.fees.add(fee);
    }

    /**
     * The DERIVED taxability of the bundle (BUNDLE-DERIVED-001): the bundle is taxable iff
     * at least one child OR at least one fee is taxable. Computed from the current children —
     * never stored, never hand-set.
     */
    boolean derivedTaxable() {
        for (CompositeComponent c : components) {
            if (c.isTaxable()) {
                return true;
            }
        }
        for (BundleFee f : fees) {
            if (f.isTaxable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The conserving roll-up — recomputed from the immutable children + fees (ITEM_SUM) or
     * the immutable fixed base (BUNDLE) on every read. There is no stored total: this pure
     * function IS the composite price, so it can never drift from the rows it summarizes.
     *
     * <p>ITEM_SUM: retail = Σ child.unitRetailPrice×qty + Σ fee.amount; sale = Σ
     * (child.unitSalePrice ?? child.unitRetailPrice)×qty + Σ fee.amount; taxablePrice = Σ
     * over the taxable children of unitRetailPrice×qty + Σ over the taxable fees of amount.
     *
     * <p>BUNDLE: retail = baseRetailPrice (NOT summed); sale = baseSalePrice ?? baseRetailPrice;
     * taxablePrice = derivedTaxable ? baseRetailPrice : 0.
     */
    Pricing priceRollUp() {
        boolean taxable = derivedTaxable();
        if (pricingModel == BundlePricingModel.BUNDLE) {
            long retail = baseRetailPrice;
            long sale = (baseSalePrice != null) ? baseSalePrice : baseRetailPrice;
            long taxablePrice = taxable ? retail : 0L;
            return new Pricing(retail, sale, taxablePrice, taxable);
        }
        long retail = 0L;
        long sale = 0L;
        long taxablePrice = 0L;
        for (CompositeComponent c : components) {
            retail = Math.addExact(retail, c.retailSubtotal());
            sale = Math.addExact(sale, c.saleSubtotal());
            if (c.isTaxable()) {
                taxablePrice = Math.addExact(taxablePrice, c.retailSubtotal());
            }
        }
        for (BundleFee f : fees) {
            retail = Math.addExact(retail, f.getAmount());
            sale = Math.addExact(sale, f.getAmount());
            if (f.isTaxable()) {
                taxablePrice = Math.addExact(taxablePrice, f.getAmount());
            }
        }
        return new Pricing(retail, sale, taxablePrice, taxable);
    }

    /** The derived composite price (no stored total). All amounts in integer minor units. */
    public record Pricing(long retailPrice, long salePrice, long taxablePrice, boolean taxable) {}

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private BundlePricingModel pricingModel;
        private String currency;
        private Long baseRetailPrice;
        private Long baseSalePrice;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder pricingModel(BundlePricingModel v) { this.pricingModel = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder baseRetailPrice(Long v) { this.baseRetailPrice = v; return this; }
        public Builder baseSalePrice(Long v) { this.baseSalePrice = v; return this; }

        public CompositeItem build() { return new CompositeItem(this); }
    }
}
