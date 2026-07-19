package com.ax.template.authblueprint.consumerproof;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// VIOLATING — controller_repository_shell_guard
// The controller injects and calls a *Repository DIRECTLY (field + ctor-param +
// method-call) instead of routing through a *Service — a layer-boundary break.
@RestController
public class OrderAdminController {

    private final OrderRepository orderRepository;

    OrderAdminController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/api/orders/{id}")
    public Order get(@PathVariable Long id) {
        return orderRepository.findById(id).orElseThrow();
    }
}
