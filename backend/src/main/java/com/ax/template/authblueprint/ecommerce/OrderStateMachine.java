package com.ax.template.authblueprint.ecommerce;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Sole legal mutator of {@link Order#setStatus(OrderStatus)} — mirrors the
 * pattern used by {@code SubscriptionStateMachine} (billing) and
 * {@code PaymentStateMachine}.
 *
 * <pre>
 *   PENDING   → PAID
 *   PENDING   → CANCELLED
 *   PAID      → SHIPPED
 *   PAID      → CANCELLED
 *   SHIPPED   → DELIVERED
 *   SHIPPED   → CANCELLED
 *   DELIVERED → (terminal)
 *   CANCELLED → (terminal)
 * </pre>
 */
@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> LEGAL = new EnumMap<>(OrderStatus.class);

    static {
        LEGAL.put(OrderStatus.PENDING,   Set.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        LEGAL.put(OrderStatus.PAID,      Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        LEGAL.put(OrderStatus.SHIPPED,   Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        LEGAL.put(OrderStatus.DELIVERED, Set.of());
        LEGAL.put(OrderStatus.CANCELLED, Set.of());
    }

    public void transition(Order order, OrderStatus next) {
        OrderStatus current = order.getStatus();
        Set<OrderStatus> allowed = LEGAL.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new IllegalStateException(
                "illegal order transition: " + current + " -> " + next);
        }
        order.setStatus(next);
    }

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        return LEGAL.getOrDefault(from, Set.of()).contains(to);
    }
}
