package com.ax.template.authblueprint.ecommerce;

/**
 * Order lifecycle states.
 * Legal transitions (mirror {@link OrderStateMachine}):
 * <pre>
 *   PENDING   → PAID
 *   PENDING   → CANCELLED
 *   PAID      → SHIPPED
 *   PAID      → CANCELLED
 *   SHIPPED   → DELIVERED
 *   SHIPPED   → CANCELLED
 * </pre>
 * {@code DELIVERED} and {@code CANCELLED} are terminal.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
