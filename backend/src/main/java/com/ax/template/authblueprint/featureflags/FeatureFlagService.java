package com.ax.template.authblueprint.featureflags;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feature-flag domain service.
 * <p>
 * Trace:
 * <ul>
 *   <li>FF-CRUD-001 — create with duplicate-name check (409).</li>
 *   <li>FF-CRUD-002 — paginated listing.</li>
 *   <li>FF-CRUD-003 — patch enabled / description.</li>
 *   <li>FF-CRUD-004 — hard delete; subsequent eval is fail-closed.</li>
 *   <li>FF-EVAL-001 — known flag returns current enabled state.</li>
 *   <li>FF-EVAL-002 — unknown flag returns false (fail-closed).</li>
 *   <li>FF-EVAL-003 — cache invalidated on every mutation.</li>
 * </ul>
 */
@Service
public class FeatureFlagService {

    private final FeatureFlagRepository repository;
    private final FeatureFlagCache cache;

    public FeatureFlagService(FeatureFlagRepository repository, FeatureFlagCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    /** FF-EVAL-001/002/003 — cache-load-through; loader is fail-closed. */
    public boolean isActive(String name) {
        return cache.getActive(name,
            n -> repository.findById(n).map(FeatureFlag::isEnabled).orElse(false));
    }

    /** FF-CRUD-001 — create. Duplicate names raise {@link DuplicateFeatureFlagException}. */
    @Transactional
    public FeatureFlag create(String name, boolean enabled, String description) {
        if (repository.existsById(name)) {
            throw new DuplicateFeatureFlagException(name);
        }
        FeatureFlag flag = repository.save(FeatureFlag.create(name, enabled, description));
        cache.invalidate(name);
        return flag;
    }

    /** FF-CRUD-002. */
    public Page<FeatureFlag> list(Pageable pageable) {
        return repository.findAllByOrderByNameAsc(pageable);
    }

    /** FF-CRUD-003. */
    @Transactional
    public FeatureFlag update(String name, Boolean enabled, String description) {
        FeatureFlag flag = repository.findById(name)
            .orElseThrow(() -> new FeatureFlagNotFoundException(name));
        flag.update(enabled, description);
        FeatureFlag saved = repository.save(flag);
        cache.invalidate(name);
        return saved;
    }

    /** FF-CRUD-004 — hard delete. */
    @Transactional
    public void delete(String name) {
        if (!repository.existsById(name)) {
            throw new FeatureFlagNotFoundException(name);
        }
        repository.deleteById(name);
        cache.invalidate(name);
    }
}
