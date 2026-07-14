package com.ax.template.authblueprint.facetcount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * FACET-ALLOWLIST-002 — one fixed, parameterized aggregation query PER allowlisted field.
 * {@link FacetCountService} selects between them via a switch on the resolved internal
 * property name; no query is ever built by concatenating the client-supplied field name.
 * Both queries share the SAME scope predicate (ownerId) the list endpoint would use
 * (FACET-COUNT-001) — never a wider, table-wide rollup.
 */
public interface FacetableItemRepository extends JpaRepository<FacetableItem, UUID> {

    long countByOwnerId(String ownerId);

    @Query("SELECT f.category, COUNT(f) FROM FacetableItem f WHERE f.ownerId = :ownerId GROUP BY f.category")
    List<Object[]> countsByCategoryForOwner(@Param("ownerId") String ownerId);

    @Query("SELECT f.status, COUNT(f) FROM FacetableItem f WHERE f.ownerId = :ownerId GROUP BY f.status")
    List<Object[]> countsByStatusForOwner(@Param("ownerId") String ownerId);
}
