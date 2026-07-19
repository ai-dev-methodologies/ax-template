package com.ax.template.authblueprint.consumerproof;

import java.util.Optional;

// Spring-Data-style repository. Controllers MUST NOT touch this directly.
public interface OrderRepository {
    Optional<Order> findById(Long id);
}
