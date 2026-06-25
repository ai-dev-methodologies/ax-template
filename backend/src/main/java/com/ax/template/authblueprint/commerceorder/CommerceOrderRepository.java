package com.ax.template.authblueprint.commerceorder;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the {@link CommerceOrder} aggregate root.
 * No member repositories — mutations to CommerceOrderItems and CommerceFulfillmentGroups
 * flow through the root.
 */
public interface CommerceOrderRepository extends JpaRepository<CommerceOrder, UUID> {

    /**
     * Load order with a PESSIMISTIC_WRITE lock for concurrent cart mutations
     * (ORDER-MERGE-001, ORDER-IMMUTABLE-001 concurrent add/update/remove).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CommerceOrder o WHERE o.id = :id")
    Optional<CommerceOrder> findByIdForUpdate(@Param("id") UUID id);

    /**
     * User-scoped lookup — ORDER-AUTHZ-001.
     * Returns empty when the order exists but belongs to a different user (IDOR-safe 404).
     */
    @Query("SELECT o FROM CommerceOrder o WHERE o.id = :id AND o.userId = :userId")
    Optional<CommerceOrder> findByIdAndUserId(@Param("id") UUID id, @Param("userId") String userId);

    /**
     * User-scoped locking load for mutations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CommerceOrder o WHERE o.id = :id AND o.userId = :userId")
    Optional<CommerceOrder> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") String userId);
}
