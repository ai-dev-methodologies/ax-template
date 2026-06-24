package com.ax.template.authblueprint.commercecatalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * commercecatalog aggregate root — one sellable product definition.
 *
 * <p>INV-3: a product is created with a default SKU in the same transaction
 * (service enforces 422 CATALOG_DEFAULT_SKU_REQUIRED if absent). Variant SKUs
 * that omit their own price/dates inherit from the default SKU at READ time.
 *
 * <p>{@code canSellWithoutOptions=true} means the product can be purchased
 * using its default SKU directly (no option selection required). When false,
 * callers MUST call {@code resolveSku} with a complete option map before any
 * purchasable assertion.
 */
@AggregateRoot
@Entity
@Table(name = "catalog_products")
@Check(constraints = "active_end_date IS NULL OR active_start_date IS NULL OR active_end_date > active_start_date")
public class CatalogProduct {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 400)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    /** Points to the default SKU — set after the default SKU is persisted in the create transaction. */
    @Column(name = "default_sku_id")
    private UUID defaultSkuId;

    @Column(name = "can_sell_without_options", nullable = false)
    private boolean canSellWithoutOptions;

    @Column(name = "active_start_date")
    private Instant activeStartDate;

    @Column(name = "active_end_date")
    private Instant activeEndDate;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    protected CatalogProduct() {}

    public CatalogProduct(UUID id, String name, String description,
                          boolean canSellWithoutOptions,
                          Instant activeStartDate, Instant activeEndDate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.canSellWithoutOptions = canSellWithoutOptions;
        this.activeStartDate = activeStartDate;
        this.activeEndDate = activeEndDate;
        this.archived = false;
    }

    /** Called by {@link CatalogProductService} after the default SKU row is persisted. */
    void assignDefaultSku(UUID skuId) {
        this.defaultSkuId = skuId;
    }

    void archive() {
        this.archived = true;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public UUID getDefaultSkuId() { return defaultSkuId; }
    public boolean isCanSellWithoutOptions() { return canSellWithoutOptions; }
    public Instant getActiveStartDate() { return activeStartDate; }
    public Instant getActiveEndDate() { return activeEndDate; }
    public boolean isArchived() { return archived; }
}
