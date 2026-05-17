package com.ax.template.authblueprint.practices;

import org.springframework.stereotype.Component;

/**
 * Anti-pattern fixture for PRACTICES-CORE-003: a singleton-scoped @Component holding
 * mutable, non-synchronized instance state. Concurrent invocations race; lost updates
 * are silently invisible without an explicit thread-safety contract.
 */
@Component
public class MutableSingletonCounter {

    private int count;

    public void increment() {
        count++;
    }

    public int get() {
        return count;
    }

    public void reset() {
        count = 0;
    }
}
