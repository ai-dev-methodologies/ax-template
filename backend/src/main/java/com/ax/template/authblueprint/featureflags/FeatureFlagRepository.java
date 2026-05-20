package com.ax.template.authblueprint.featureflags;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Trace: blueprints/feature-flags-manifest.yaml#backend.repository.
 */
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {

    /** FF-CRUD-002 — paginated listing for admin UI. */
    Page<FeatureFlag> findAllByOrderByNameAsc(Pageable pageable);
}
