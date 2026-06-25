package com.ax.template.authblueprint.commerceorder;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Sole mutator of {@link CommerceOrder#getStatus()} and {@link CommerceOrder#freezeTotals}.
 *
 * <p>Trace: ORDER-LIFECYCLE-001.
 * ALLOWED transitions:
 * <ul>
 *   <li>IN_PROCESS → SUBMITTED</li>
 *   <li>IN_PROCESS → CANCELLED</li>
 *   <li>SUBMITTED  → CANCELLED</li>
 *   <li>CANCELLED  → (empty — terminal)</li>
 * </ul>
 * No re-open edge (SUBMITTED → IN_PROCESS is forbidden).
 */
@Component
public class CommerceOrderStateMachine {

    private static final Map<CommerceOrderStatus, Set<CommerceOrderStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(CommerceOrderStatus.class);
        ALLOWED.put(CommerceOrderStatus.IN_PROCESS,
            EnumSet.of(CommerceOrderStatus.SUBMITTED, CommerceOrderStatus.CANCELLED));
        ALLOWED.put(CommerceOrderStatus.SUBMITTED,
            EnumSet.of(CommerceOrderStatus.CANCELLED));
        ALLOWED.put(CommerceOrderStatus.CANCELLED,
            EnumSet.noneOf(CommerceOrderStatus.class));
    }

    /**
     * Transition the order to SUBMITTED and freeze totals atomically.
     *
     * @param order    the order to submit
     * @param total    caller-supplied priced total (subTotal + tax)
     * @param subTotal merchandise sub-total
     * @param tax      tax amount
     */
    public void submit(CommerceOrder order, long total, long subTotal, long tax) {
        assertTransition(order.getStatus(), CommerceOrderStatus.SUBMITTED);
        order.setStatus(CommerceOrderStatus.SUBMITTED);
        order.freezeTotals(total, subTotal, tax);
    }

    /** Transition the order to CANCELLED. */
    public void cancel(CommerceOrder order) {
        assertTransition(order.getStatus(), CommerceOrderStatus.CANCELLED);
        order.setStatus(CommerceOrderStatus.CANCELLED);
    }

    /** Returns the ALLOWED map (read-only). Used by ViolationProofTest. */
    static Map<CommerceOrderStatus, Set<CommerceOrderStatus>> getAllowedMap() {
        return ALLOWED;
    }

    private static void assertTransition(CommerceOrderStatus from, CommerceOrderStatus to) {
        Set<CommerceOrderStatus> allowed = ALLOWED.getOrDefault(
            from, EnumSet.noneOf(CommerceOrderStatus.class));
        if (!allowed.contains(to)) {
            throw new CommerceOrderException(
                "ORDER_INVALID_TRANSITION", 409,
                "Illegal order transition: " + from + " → " + to + "; allowed=" + allowed);
        }
    }
}
