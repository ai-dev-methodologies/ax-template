package com.ax.template.authblueprint.commercecatalog;

/**
 * CAT-INVENTORY-GATE-001: tri-state inventory policy flag on a SKU.
 *
 * <p>Consulted by {@link CatalogProductService#assertPurchasable} BEFORE any quantity arithmetic:
 * <ul>
 *   <li>{@link #UNAVAILABLE} — never purchasable regardless of stock (catalog rejects immediately).
 *   <li>{@link #ALWAYS_AVAILABLE} — purchasable WITHOUT consulting any quantity.
 *   <li>{@link #CHECK_QUANTITY} — catalog gate does NOT block; actual quantity check is deferred
 *       to the inventory-reservation vertical.
 * </ul>
 *
 * <p>The catalog deliberately carries NO quantity/stock field — only this policy flag.
 * Quantity arithmetic belongs to the inventory-reservation vertical.
 */
public enum InventoryType {
    UNAVAILABLE,
    ALWAYS_AVAILABLE,
    CHECK_QUANTITY
}
