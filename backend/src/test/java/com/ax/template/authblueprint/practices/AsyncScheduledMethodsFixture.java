package com.ax.template.authblueprint.practices;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Test-scope fixture for PRACTICES-ASYNC-003. Lives under src/test/java so neither
 * @Scheduled body actually runs in production — the rule's verification is by reflection
 * on the annotation attributes (fixedDelay vs fixedRate). No @Component / no @EnableScheduling.
 */
public class AsyncScheduledMethodsFixture {

    /**
     * fixedDelay = N ms — waits N ms AFTER the previous invocation FINISHES before
     * starting the next. Long-running tasks self-throttle: cleanup that occasionally
     * takes 5 minutes will not stack up.
     */
    @Scheduled(fixedDelay = 60_000L)
    public void cleanupTask() {
        // no-op fixture
    }

    /**
     * fixedRate = N ms — invokes every N ms from the previous invocation's START. If a
     * heartbeat takes longer than the interval, the next one starts immediately and
     * invocations can stack up. Use fixedRate ONLY for cheap, near-instant work.
     */
    @Scheduled(fixedRate = 60_000L)
    public void heartbeatTask() {
        // no-op fixture
    }
}
