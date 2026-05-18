/**
 * @ax-template-meta
 * template_id: backend/feature-flags/FeatureFlagService
 * layer: backend-domain
 * domain: feature-flags
 * anchors_rule: service-layer-owns-business-logic.md (PRACTICES-ARCH-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Service stereotype and transaction boundaries"
 *     url: "https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html#beans-meta-annotations"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   FeatureFlagService owns all business logic: duplicate detection, cache eviction.
 *   FeatureFlagCache.evict() must be called after every write to maintain consistency.
 */
package com.example.app.featureflags;

import com.example.app.featureflags.FeatureFlagDto.CreateRequest;
import com.example.app.featureflags.FeatureFlagDto.FlagResponse;
import com.example.app.featureflags.FeatureFlagDto.UpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for feature flag CRUD and evaluation.
 *
 * <p>All mutations evict the flag from FeatureFlagCache to ensure
 * the next evaluation reflects the updated state (FF-EVAL-003).
 *
 * <p>spec_ref: specs/feature-flags-l0.yaml
 */
@Service
@Transactional(readOnly = true)
public class FeatureFlagService {

    private final FeatureFlagRepository repository;
    private final FeatureFlagCache cache;

    public FeatureFlagService(FeatureFlagRepository repository, FeatureFlagCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    // ─── evaluation ───────────────────────────────────────────────────────────

    /**
     * Returns whether the named flag is active.
     * Fail-closed: unknown flags return false (FF-EVAL-002).
     */
    public boolean isActive(String name) {
        return cache.isActive(name);
    }

    // ─── admin CRUD ───────────────────────────────────────────────────────────

    public Page<FlagResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(FlagResponse::from);
    }

    @Transactional
    public FlagResponse create(CreateRequest req) {
        if (repository.existsById(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Feature flag already exists: " + req.name());
        }
        FeatureFlag flag = new FeatureFlag(req.name(), req.enabled(), req.description());
        FeatureFlag saved = repository.save(flag);
        cache.evict(saved.getName());
        return FlagResponse.from(saved);
    }

    @Transactional
    public FlagResponse update(String name, UpdateRequest req) {
        FeatureFlag flag = repository.findById(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Feature flag not found: " + name));
        if (req.enabled() != null) flag.setEnabled(req.enabled());
        if (req.description() != null) flag.setDescription(req.description());
        FeatureFlag saved = repository.save(flag);
        cache.evict(name);
        return FlagResponse.from(saved);
    }

    @Transactional
    public void delete(String name) {
        if (!repository.existsById(name)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Feature flag not found: " + name);
        }
        repository.deleteById(name);
        cache.evict(name);
    }
}
