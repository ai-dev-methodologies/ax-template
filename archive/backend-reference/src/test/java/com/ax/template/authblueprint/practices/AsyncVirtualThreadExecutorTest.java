package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-ASYNC-001")
class AsyncVirtualThreadExecutorTest {

    @Test
    void practices_ASYNC_001_executorRunsTaskOnVirtualThread() throws Exception {
        VirtualThreadDispatcher dispatcher = new VirtualThreadDispatcher();
        try (ExecutorService exec = dispatcher.virtualThreadExecutor()) {
            AtomicReference<Boolean> wasVirtual = new AtomicReference<>(null);
            AtomicReference<String> threadName = new AtomicReference<>(null);
            exec.submit(() -> {
                wasVirtual.set(Thread.currentThread().isVirtual());
                threadName.set(Thread.currentThread().getName());
            }).get();
            assertThat(wasVirtual.get())
                    .as("Executors.newVirtualThreadPerTaskExecutor() must produce virtual threads")
                    .isTrue();
            assertThat(threadName.get()).isNotNull();
        }
    }
}
