package com.ax.template.authblueprint.checkout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * CLEAN — every list-shaped query is bounded (api-pagination-pageable.md):
 * findAll(Pageable) returns a Page<Order>, never a raw List<Order>/Iterable<Order>
 * of the whole table. Admin/list surfaces must page through this, not slurp it.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findAllByBuyerId(Long buyerId, Pageable pageable);
}
