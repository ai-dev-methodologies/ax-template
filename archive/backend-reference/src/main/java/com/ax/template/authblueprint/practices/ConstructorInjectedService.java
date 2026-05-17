package com.ax.template.authblueprint.practices;

import org.springframework.stereotype.Service;

/**
 * Correct fixture for PRACTICES-CORE-001: constructor injection.
 * The dependency is final (immutable), trivially mockable in plain JUnit, and circular
 * dependencies are caught at construction time.
 */
@Service
public class ConstructorInjectedService {

    private final ParentRepository parents;

    public ConstructorInjectedService(ParentRepository parents) {
        this.parents = parents;
    }

    public long countParents() {
        return parents.count();
    }
}
