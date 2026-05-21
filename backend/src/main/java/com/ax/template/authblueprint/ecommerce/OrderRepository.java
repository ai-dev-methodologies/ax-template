package com.ax.template.authblueprint.ecommerce;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    Page<Order> findAllByUserId(String userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(String id, String userId);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
