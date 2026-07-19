package com.ax.template.authblueprint.checkout;

import com.ax.template.authblueprint.common.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * OrderService — sole mutator for the checkout slice (cart -> order -> payment ->
 * receipt). Composes catalog L4 domains rather than reinventing them:
 *   - payment L4    : PaymentGateway.charge(...) at the PG edge (BigDecimal MAJOR units)
 *   - notification L4: NotificationSender.sendReceipt(...) on PAID transition
 *   - audit-log L4  : AuditLogService.record(...) on every state transition
 * (Interfaces only in this thin slice — full implementations live in the real
 * templates/L4/{payment,notification,audit-log} catalog assets this scenario composes.)
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Page<Order> listForBuyer(Long buyerId, Pageable pageable) {
        return orderRepository.findAllByBuyerId(buyerId, pageable);
    }

    /**
     * CLEAN money-boundary seam: the PG-edge charge amount is derived through
     * Money.toMajorUnits(minor, currency) — never a raw single-arg BigDecimal
     * conversion of the minor-unit getter, which would silently leave the
     * value in minor units while the PG interprets it as MAJOR units (a 100x
     * over-charge for 2-decimal currencies).
     */
    public BigDecimal chargeAmount(Order order) {
        return Money.toMajorUnits(order.getTotalAmount(), order.getCurrency());
    }
}
