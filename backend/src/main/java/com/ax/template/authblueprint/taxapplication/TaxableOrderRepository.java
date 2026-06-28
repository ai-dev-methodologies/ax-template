package com.ax.template.authblueprint.taxapplication;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Root repository for the {@link TaxableOrder} aggregate. The lines are an
 * {@code @ElementCollection} loaded through the root, so they own no repository of their own
 * (AX-DDD-MEMBER-REPO). All access is by order id; there is no unbounded collection finder.
 */
public interface TaxableOrderRepository extends JpaRepository<TaxableOrder, UUID> {
}
