package com.ax.template.authblueprint.commercecatalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * An option dimension of a {@link CatalogProduct} (e.g. "Color", "Size").
 *
 * <p>{@code attribute_name} is immutable after creation (updatable=false) — changing it would
 * orphan existing {@link ProductOptionValue} rows and invalidate stored option_signatures.
 * UNIQUE {@code (product_id, attribute_name)} prevents duplicate dimension definitions.
 *
 * <p>{@code useInSkuGeneration=true} means option values of this dimension are included in the
 * sorted-join that forms the SKU option_signature (INV-1).
 */
@AggregateMember(root = CatalogProduct.class)
@Entity
@Table(name = "catalog_product_options", uniqueConstraints = {
    @UniqueConstraint(name = "uq_product_option_attr", columnNames = {"product_id", "attribute_name"})
})
public class ProductOption {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(name = "attribute_name", updatable = false, nullable = false, length = 200)
    private String attributeName;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "use_in_sku_generation", nullable = false)
    private boolean useInSkuGeneration;

    protected ProductOption() {}

    public ProductOption(UUID id, UUID productId, String attributeName,
                         boolean required, boolean useInSkuGeneration) {
        this.id = id;
        this.productId = productId;
        this.attributeName = attributeName;
        this.required = required;
        this.useInSkuGeneration = useInSkuGeneration;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getAttributeName() { return attributeName; }
    public boolean isRequired() { return required; }
    public boolean isUseInSkuGeneration() { return useInSkuGeneration; }
}
