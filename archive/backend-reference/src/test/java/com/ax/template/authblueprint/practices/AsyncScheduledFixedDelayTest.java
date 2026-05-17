package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

@Tag("PRACTICES")
@Tag("PRACTICES-ASYNC-003")
class AsyncScheduledFixedDelayTest {

    @Test
    void practices_ASYNC_003_cleanupUsesFixedDelay_notFixedRate() throws Exception {
        Method method = AsyncScheduledMethodsFixture.class.getDeclaredMethod("cleanupTask");
        Scheduled ann = method.getAnnotation(Scheduled.class);
        assertThat(ann).isNotNull();
        assertThat(ann.fixedDelay())
                .as("cleanup tasks must use fixedDelay so a slow run does not pile up the next one")
                .isPositive();
        assertThat(ann.fixedRate())
                .as("fixedRate MUST be unset on cleanup tasks — they can self-pile and exhaust the pool")
                .isLessThanOrEqualTo(0L);
    }

    @Test
    void practices_ASYNC_003_heartbeatUsesFixedRate_notFixedDelay() throws Exception {
        Method method = AsyncScheduledMethodsFixture.class.getDeclaredMethod("heartbeatTask");
        Scheduled ann = method.getAnnotation(Scheduled.class);
        assertThat(ann).isNotNull();
        assertThat(ann.fixedRate())
                .as("near-instant heartbeats can use fixedRate to keep the cadence regardless of jitter")
                .isPositive();
        assertThat(ann.fixedDelay())
                .as("fixedDelay MUST be unset on a fixedRate heartbeat — only one of the two applies")
                .isLessThanOrEqualTo(0L);
    }
}
