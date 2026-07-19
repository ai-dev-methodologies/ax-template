package com.ax.template.authblueprint.checkout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * OrderAdminController — CLEAN on both axes this scenario proves:
 *   1. layering — delegates to OrderService, never touches OrderRepository
 *      directly (controller_repository_shell_guard.sh).
 *   2. boundedness — the list surface takes a Pageable and returns a Page<Order>,
 *      never an unbounded List<Order> findAll() of the whole order table
 *      (api-pagination-pageable.md; scenario-guards/unbounded_repository_read_guard.sh).
 */
@RestController
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class OrderAdminController {

    private final OrderService orderService;

    public OrderAdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/api/admin/orders")
    public Page<Order> listOrders(@PathVariable Long buyerId, Pageable pageable) {
        return orderService.listForBuyer(buyerId, pageable);
    }
}
