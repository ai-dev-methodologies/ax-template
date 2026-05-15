package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Async;

@SpringBootTest
@Tag("PRACTICES")
@Tag("PRACTICES-ASYNC-002")
class AsyncSpringAsyncReturnsCompletableFutureTest {

    @Autowired
    private AsyncReporterService reporter;

    @Test
    void practices_ASYNC_002_asyncMethodReturnsCompletableFuture() throws Exception {
        Method method = AsyncReporterService.class.getDeclaredMethod("generateReportAsync");
        assertThat(method.isAnnotationPresent(Async.class))
                .as("the method must carry @Async to be eligible for off-thread execution")
                .isTrue();
        assertThat(method.getReturnType())
                .as("@Async methods must return CompletableFuture (or one of the supported "
                        + "reactive types). void hides exceptions; raw values defeat the proxy.")
                .isEqualTo(CompletableFuture.class);
    }

    @Test
    void practices_ASYNC_002_callReturnsCompletedFutureWithThreadInformation() throws Exception {
        CompletableFuture<String> future = reporter.generateReportAsync();
        String result = future.get();
        assertThat(result)
                .as("the future must resolve to a non-empty value carrying the worker thread name")
                .startsWith("report-from-");
    }
}
