package com.ax.template.authblueprint.queryguard;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * query-field-allowlist-l0 repository for {@link CatalogItem}. The list path uses
 * {@link JpaSpecificationExecutor#findAll(org.springframework.data.jpa.domain.Specification,
 * org.springframework.data.domain.Pageable)} — which returns a bounded {@code Page}
 * (QUERY-ALLOWLIST-PAGE-001, never an unbounded raw {@code List}). The Specification and the
 * Sort are built by {@link QueryGuardService} ONLY from allowlisted internal properties + a
 * parameter-bound value, so a client-supplied field name / SQL fragment can never reach the query.
 */
public interface CatalogItemRepository
        extends JpaRepository<CatalogItem, UUID>, JpaSpecificationExecutor<CatalogItem> {
}
