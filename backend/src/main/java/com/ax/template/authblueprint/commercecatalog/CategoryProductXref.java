package com.ax.template.authblueprint.commercecatalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * Links a {@link CatalogProduct} to a {@link Category} by category id reference only
 * (cross-aggregate by ID — no Category object pointer per DDD decomposition rules).
 *
 * <p>UNIQUE {@code (product_id, category_id)} prevents duplicate membership.
 */
@AggregateMember(root = CatalogProduct.class)
@Entity
@Table(name = "catalog_category_products", uniqueConstraints = {
    @UniqueConstraint(name = "uq_cat_product", columnNames = {"product_id", "category_id"})
})
@IdClass(CategoryProductXref.Pk.class)
public class CategoryProductXref {

    public static class Pk implements Serializable {
        private UUID productId;
        private UUID categoryId;

        public Pk() {}

        public Pk(UUID productId, UUID categoryId) {
            this.productId = productId;
            this.categoryId = categoryId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(productId, pk.productId) && Objects.equals(categoryId, pk.categoryId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, categoryId);
        }
    }

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Id
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected CategoryProductXref() {}

    public CategoryProductXref(UUID productId, UUID categoryId, int displayOrder) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.displayOrder = displayOrder;
    }

    public UUID getProductId() { return productId; }
    public UUID getCategoryId() { return categoryId; }
    public int getDisplayOrder() { return displayOrder; }
}
