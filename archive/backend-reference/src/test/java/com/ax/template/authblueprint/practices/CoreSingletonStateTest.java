package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("PRACTICES")
@Tag("PRACTICES-CORE-003")
class CoreSingletonStateTest {

    @Autowired
    private AtomicSingletonCounter atomic;

    @Autowired
    private MutableSingletonCounter mutable;

    private static final int THREADS = 32;
    private static final int PER_THREAD = 1_000;
    private static final long EXPECTED_TOTAL = (long) THREADS * PER_THREAD;

    @BeforeEach
    void reset() {
        atomic.reset();
        mutable.reset();
    }

    @Test
    void practices_CORE_003_atomicSingletonStateIsThreadSafe() throws Exception {
        runConcurrent(atomic::increment);
        assertThat(atomic.get())
                .as("AtomicLong-backed singleton state is exact under concurrent load")
                .isEqualTo(EXPECTED_TOTAL);
    }

    @Test
    void practices_CORE_003_mutableSingletonStateIsLessThanOrEqual() throws Exception {
        runConcurrent(mutable::increment);
        // The race is not deterministic, but unsafe mutable state cannot exceed the total
        // and almost always loses updates on real hardware.
        assertThat((long) mutable.get())
                .as("unsynchronized int++ in a singleton must not exceed expected — and typically loses updates")
                .isLessThanOrEqualTo(EXPECTED_TOTAL);
    }

    private void runConcurrent(Runnable work) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (int t = 0; t < THREADS; t++) {
                pool.submit(() -> {
                    for (int i = 0; i < PER_THREAD; i++) {
                        work.run();
                    }
                });
            }
        } finally {
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
