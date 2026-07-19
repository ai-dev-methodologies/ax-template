package com.ax.template.authblueprint.checkout;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

/**
 * VIOLATING — an explicit unbounded list query: findAllByBuyerId(buyerId) returns
 * List<Order> for the WHOLE result set, no Pageable, no LIMIT. A buyer (or an
 * admin sweep) with a large order history loads every row into memory on a single
 * request (api-pagination-pageable.md; scenario-guards/unbounded_repository_read_guard.sh).
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select o from Order o where o.buyerId = :buyerId")
    List<Order> findAllByBuyerId(@Param("buyerId") Long buyerId);
}
