package com.ax.template.authblueprint.checkout;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * VIOLATING — money-boundary seam bug: a raw single-arg BigDecimal.valueOf(order
 * .getTotalAmount()) leaves the value in MINOR units while the payment PG edge
 * interprets a BigDecimal as MAJOR units — a silent 100x over-charge for every
 * 2-decimal currency (money_boundary_seam_guard.sh). Should be
 * Money.toMajorUnits(order.getTotalAmount(), order.getCurrency()).
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> listForBuyer(Long buyerId) {
        return orderRepository.findAllByBuyerId(buyerId);
    }

    public BigDecimal chargeAmount(Order order) {
        return BigDecimal.valueOf(order.getTotalAmount());
    }
}
