package com.ax.template.authblueprint.checkout;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * VIOLATING on two axes:
 *   1. layering — the controller injects OrderRepository directly and calls it,
 *      instead of routing through OrderService (controller_repository_shell_guard.sh).
 *   2. boundedness — it also happens to call the unbounded finder, returning the
 *      buyer's ENTIRE order history as a raw List<Order> with no Pageable
 *      (api-pagination-pageable.md).
 */
@RestController
public class OrderAdminController {

    private final OrderRepository orderRepository;

    public OrderAdminController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/api/admin/orders")
    public List<Order> listOrders(@PathVariable Long buyerId) {
        return orderRepository.findAllByBuyerId(buyerId);
    }
}
