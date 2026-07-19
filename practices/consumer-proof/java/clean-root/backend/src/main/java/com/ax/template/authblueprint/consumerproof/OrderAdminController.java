package com.ax.template.authblueprint.consumerproof;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// CLEAN — the thin controller delegates to a *Service, never to a *Repository.
@RestController
public class OrderAdminController {

    private final OrderService orderService;

    OrderAdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/api/orders/{id}")
    public Order get(@PathVariable Long id) {
        return orderService.get(id);
    }
}
