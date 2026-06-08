package com.ax.template.authblueprint.ecommerce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Product entity — recipes/e-commerce/RECIPE.md (crud L4).
 * <p>
 * Price is stored as integer minor units (BILLING-CUR-001 convention reused).
 */
@AggregateRoot
@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "id", length = 36, updatable = false)
    private String id;

    @Column(name = "owner_user_id", nullable = false, length = 36, updatable = false)
    private String ownerUserId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    /** Minor units (e.g. KRW won, USD cents). */
    @Column(name = "price", nullable = false)
    private long price;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "image_file_id", length = 36)
    private String imageFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Product() {}

    private Product(String id, String ownerUserId, String name, String description,
                    long price, String currency, int stock, String imageFileId,
                    ProductStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.ownerUserId = Objects.requireNonNull(ownerUserId);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.price = price;
        this.currency = Objects.requireNonNull(currency);
        this.stock = stock;
        this.imageFileId = imageFileId;
        this.status = Objects.requireNonNull(status);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(String ownerUserId, String name, String description,
                                 long price, String currency, int stock, String imageFileId) {
        Instant now = Instant.now();
        return new Product(UUID.randomUUID().toString(), ownerUserId, name, description,
            price, currency, stock, imageFileId, ProductStatus.ACTIVE, now, now);
    }

    public void update(String name, String description, Long price, Integer stock, String imageFileId) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (stock != null) this.stock = stock;
        if (imageFileId != null) this.imageFileId = imageFileId;
        this.updatedAt = Instant.now();
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void decrementStock(int by) {
        if (by < 0) throw new IllegalArgumentException("decrement must be non-negative");
        if (this.stock < by) {
            throw new EcommerceException.InsufficientStock(id, this.stock, by);
        }
        this.stock -= by;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getOwnerUserId() { return ownerUserId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getPrice() { return price; }
    public String getCurrency() { return currency; }
    public int getStock() { return stock; }
    public String getImageFileId() { return imageFileId; }
    public ProductStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
