package com.ax.template.authblueprint.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    /** BILLING-AUTHZ-002 — owner-scoped lookup (cross-user returns empty → 404). */
    Optional<Subscription> findByIdAndUserId(String id, String userId);

    Page<Subscription> findAllByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);
}
