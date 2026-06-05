package com.ax.template.authblueprint.optlock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link OptlockResource}. {@code findByIdAndOwnerId} is the owner-scoped lookup
 * that makes a cross-owner reference return an IDOR-safe 404 rather than leaking existence.
 */
public interface OptlockResourceRepository extends JpaRepository<OptlockResource, UUID> {
    Optional<OptlockResource> findByIdAndOwnerId(UUID id, String ownerId);
}
