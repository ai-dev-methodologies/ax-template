package com.ax.template.authblueprint.commerceorder;

/**
 * Order lifecycle status (Broadleaf cart/order unification pattern).
 *
 * <p>ORDER-LIFECYCLE-001: only IN_PROCESS has outgoing edges; CANCELLED is terminal.
 * <ul>
 *   <li>IN_PROCESS → SUBMITTED (submit)</li>
 *   <li>IN_PROCESS → CANCELLED (cancel)</li>
 *   <li>SUBMITTED → CANCELLED (cancel)</li>
 * </ul>
 * Re-opening a SUBMITTED order is forbidden (no SUBMITTED → IN_PROCESS edge).
 */
public enum CommerceOrderStatus {
    IN_PROCESS,
    SUBMITTED,
    CANCELLED;

    /** True only for IN_PROCESS — the only status that allows cart mutations. */
    public boolean editable() {
        return this == CommerceOrderStatus.IN_PROCESS;
    }
}
