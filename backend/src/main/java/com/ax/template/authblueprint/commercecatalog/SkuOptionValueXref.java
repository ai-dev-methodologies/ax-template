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
 * Junction between a {@link Sku} and the {@link ProductOptionValue}s that define it.
 *
 * <p>Both columns are {@code updatable=false} — this is an immutable association row.
 * UNIQUE {@code (sku_id, option_value_id)} prevents duplicate associations.
 */
@AggregateMember(root = CatalogProduct.class)
@Entity
@Table(name = "catalog_sku_option_values", uniqueConstraints = {
    @UniqueConstraint(name = "uq_sku_option_value", columnNames = {"sku_id", "option_value_id"})
})
@IdClass(SkuOptionValueXref.Pk.class)
public class SkuOptionValueXref {

    public static class Pk implements Serializable {
        private UUID skuId;
        private UUID optionValueId;

        public Pk() {}

        public Pk(UUID skuId, UUID optionValueId) {
            this.skuId = skuId;
            this.optionValueId = optionValueId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(skuId, pk.skuId) && Objects.equals(optionValueId, pk.optionValueId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(skuId, optionValueId);
        }
    }

    @Id
    @Column(name = "sku_id", updatable = false, nullable = false)
    private UUID skuId;

    @Id
    @Column(name = "option_value_id", updatable = false, nullable = false)
    private UUID optionValueId;

    protected SkuOptionValueXref() {}

    public SkuOptionValueXref(UUID skuId, UUID optionValueId) {
        this.skuId = skuId;
        this.optionValueId = optionValueId;
    }

    public UUID getSkuId() { return skuId; }
    public UUID getOptionValueId() { return optionValueId; }
}
