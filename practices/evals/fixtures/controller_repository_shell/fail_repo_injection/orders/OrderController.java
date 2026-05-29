package com.ax.template.authblueprint.orders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// FAIL fixture: an adversarial controller that bypasses the service layer and
// injects + calls a *Repository directly. This is the exact Controller→Repository
// leak that ArchitectureLayerBoundaryTest bans inside the JVM, and that the 54
// shell guards previously MISSED (IDW4 coverage asymmetry). The guard MUST exit 1
// here, naming all three banned shapes:
//   (1) repository-typed FIELD
//   (2) repository-typed CONSTRUCTOR PARAM
//   (3) METHOD CALL on a repository receiver
@RestController
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/api/orders/{id}")
    public OrderView get(@PathVariable Long id) {
        return orderRepository.findById(id);
    }
}
