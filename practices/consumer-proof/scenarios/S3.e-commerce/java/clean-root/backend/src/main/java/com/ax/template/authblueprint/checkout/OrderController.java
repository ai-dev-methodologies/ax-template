package com.ax.template.authblueprint.checkout;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * OrderController — thin route, delegates to OrderService. Never touches
 * OrderRepository directly (controller_repository_shell_guard.sh).
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/api/orders/{id}")
    public Order get(@PathVariable Long id) {
        return null; // thin fixture — full impl composes OrderService.get(id)
    }
}
