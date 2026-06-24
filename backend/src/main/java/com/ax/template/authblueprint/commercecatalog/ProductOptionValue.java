package com.ax.template.authblueprint.commercecatalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One concrete value for a {@link ProductOption} (e.g. "Red", "XL").
 *
 * <p>{@code attribute_value} is immutable (updatable=false) — changing it would invalidate
 * stored option_signatures. UNIQUE {@code (option_id, attribute_value)} prevents duplicates.
 *
 * <p>{@code priceAdjustment} is an optional delta in minor currency units applied on top of the
 * SKU's own retail_price (or the product's default SKU price) when this value is selected.
 * Nullable means "no price adjustment" (the SKU price stands as-is).
 */
@AggregateMember(root = CatalogProduct.class)
@Entity
@Table(name = "catalog_option_values", uniqueConstraints = {
    @UniqueConstraint(name = "uq_option_value_attr", columnNames = {"option_id", "attribute_value"})
})
public class ProductOptionValue {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "option_id", updatable = false, nullable = false)
    private UUID optionId;

    @Column(name = "attribute_value", updatable = false, nullable = false, length = 200)
    private String attributeValue;

    /** Optional price delta in minor currency units. Null = no adjustment. */
    @Column(name = "price_adjustment")
    private Long priceAdjustment;

    protected ProductOptionValue() {}

    public ProductOptionValue(UUID id, UUID optionId, String attributeValue, Long priceAdjustment) {
        this.id = id;
        this.optionId = optionId;
        this.attributeValue = attributeValue;
        this.priceAdjustment = priceAdjustment;
    }

    public UUID getId() { return id; }
    public UUID getOptionId() { return optionId; }
    public String getAttributeValue() { return attributeValue; }
    public Long getPriceAdjustment() { return priceAdjustment; }
}
