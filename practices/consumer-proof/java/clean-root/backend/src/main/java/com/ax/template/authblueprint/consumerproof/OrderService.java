package com.ax.template.authblueprint.consumerproof;

import org.springframework.stereotype.Service;

// A *Service MAY touch a *Repository — correct layering. Out of scope for
// controller_repository_shell_guard (which only scans *Controller.java).
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order get(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }
}
