package com.ax.template.authblueprint.orders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// PASS fixture: a thin controller that routes through a *Service ONLY. It never
// declares a *Repository field, never takes one as a constructor parameter, and
// never calls one. The javadoc below deliberately MENTIONS the word Repository
// in prose ("does NOT touch any OrderRepository") to prove comment stripping is
// load-bearing — this clean fixture MUST still exit 0.
/**
 * Order read API. Delegates to {@link OrderService}; does NOT touch any
 * OrderRepository directly (layer-boundary discipline mirrors ArchUnit).
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/api/orders/{id}")
    public OrderView get(@PathVariable Long id) {
        return orderService.findById(id);
    }
}
