package com.ax.template.authblueprint.orders;

import org.springframework.stereotype.Service;

// PASS fixture: the *Service is the correct home for repository access. A
// Service→Repository field is proper layering and MUST NOT be flagged — the
// guard scans *Controller.java only, so this file is never even read.
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderView findById(Long id) {
        return orderRepository.findById(id);
    }
}
