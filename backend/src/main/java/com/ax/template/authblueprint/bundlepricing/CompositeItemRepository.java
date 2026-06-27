package com.ax.template.authblueprint.bundlepricing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for the {@link CompositeItem} aggregate root. Only the root has a repository —
 * children ({@link CompositeComponent}) and fees ({@link BundleFee}) are loaded/saved through
 * it (HG-AGG-REPO). No collection-returning finder is declared (an unbounded raw-{@code List}
 * return would fail {@code ArchitectureUnboundedRepositoryListTest}).
 */
@Repository
public interface CompositeItemRepository extends JpaRepository<CompositeItem, UUID> {
}
