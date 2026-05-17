package com.ax.template.authblueprint.practices;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Correct fixture for PRACTICES-CORE-003: a singleton-scoped @Component whose mutable
 * state is guarded by an atomic primitive. Concurrent invocations are safe; no shared
 * lock is required for this access pattern.
 */
@Component
public class AtomicSingletonCounter {

    private final AtomicLong count = new AtomicLong();

    public void increment() {
        count.incrementAndGet();
    }

    public long get() {
        return count.get();
    }

    public void reset() {
        count.set(0);
    }
}
