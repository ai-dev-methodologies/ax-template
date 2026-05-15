package com.ax.template.authblueprint.practices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Anti-pattern fixture for PRACTICES-CORE-001: field injection.
 * The dependency is mutable (no `final`), cannot be set in tests without reflection,
 * and circular dependencies surface only at runtime.
 */
@Service
public class FieldInjectedService {

    @Autowired
    private ParentRepository parents;

    public long countParents() {
        return parents.count();
    }
}
