package com.ax.template.authblueprint.commercecatalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * Stockkeeping Unit — an @AggregateMember of {@link CatalogProduct}.
 *
 * <p>INV-1 backstop: {@code (product_id, option_signature)} UNIQUE (where option_signature
 * IS NOT NULL) prevents two SKUs of the same product from matching the same option-value set.
 * {@code option_signature} is the sorted-join of the sku-generating option-value ids computed
 * at write time by the service; a null signature means "default/no-option SKU".
 *
 * <p>INV: sale_price <= retail_price enforced by @Check + DB UNIQUE.
 *
 * <p>All columns are {@code updatable=false} on identity fields; the append-only
 * {@code is_default} is fixed at creation and never changed.
 */
@AggregateMember(root = CatalogProduct.class)
@Entity
@Table(name = "catalog_skus", uniqueConstraints = {
    @UniqueConstraint(name = "uq_sku_product_option_sig", columnNames = {"product_id", "option_signature"})
})
@Check(constraints = "sale_price IS NULL OR sale_price <= retail_price")
public class Sku {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** True for the product's default SKU — fixed at creation, never changed. */
    @Column(name = "is_default", nullable = false, updatable = false)
    private boolean isDefault;

    /** Retail price in minor currency units (e.g. cents). Nullable for variant SKUs that inherit. */
    @Column(name = "retail_price")
    private Long retailPrice;

    /** Sale price (optional override). Must be <= retail_price (INV). */
    @Column(name = "sale_price")
    private Long salePrice;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "active_start_date")
    private Instant activeStartDate;

    @Column(name = "active_end_date")
    private Instant activeEndDate;

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(name = "upc", length = 50)
    private String upc;

    /**
     * Sorted-join of the sku-generating option-value ids (INV-1).
     * Null for default/no-option SKUs; unique per (product_id, option_signature) when non-null.
     */
    @Column(name = "option_signature", length = 4000)
    private String optionSignature;

    protected Sku() {}

    public Sku(UUID id, UUID productId, boolean isDefault,
               Long retailPrice, Long salePrice, String currency,
               Instant activeStartDate, Instant activeEndDate,
               String externalId, String upc, String optionSignature) {
        this.id = id;
        this.productId = productId;
        this.isDefault = isDefault;
        this.retailPrice = retailPrice;
        this.salePrice = salePrice;
        this.currency = currency;
        this.activeStartDate = activeStartDate;
        this.activeEndDate = activeEndDate;
        this.externalId = externalId;
        this.upc = upc;
        this.optionSignature = optionSignature;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public Long getVersion() { return version; }
    public boolean isDefault() { return isDefault; }
    public Long getRetailPrice() { return retailPrice; }
    public Long getSalePrice() { return salePrice; }
    public String getCurrency() { return currency; }
    public Instant getActiveStartDate() { return activeStartDate; }
    public Instant getActiveEndDate() { return activeEndDate; }
    public String getExternalId() { return externalId; }
    public String getUpc() { return upc; }
    public String getOptionSignature() { return optionSignature; }
}
