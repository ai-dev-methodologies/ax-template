package com.ax.template.authblueprint.orderquantization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * order-multiple-quantization repository. A quantization is an append-only fact: NO delete method
 * is declared anywhere in this domain. The only collection-returning method takes a {@link Pageable}
 * (an unbounded raw-List return would fail ArchitectureUnboundedRepositoryListTest).
 */
public interface OrderQuantizationRepository extends JpaRepository<OrderQuantization, UUID> {

    Page<OrderQuantization> findByItemRefOrderByCreatedAtDesc(String itemRef, Pageable pageable);
}
