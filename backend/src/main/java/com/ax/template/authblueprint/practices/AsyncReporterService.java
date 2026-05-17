package com.ax.template.authblueprint.practices;

import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Fixture for PRACTICES-ASYNC-002: @Async + CompletableFuture return type.
 * The Spring @Async proxy returns immediately; the body runs on the task executor.
 * Returning CompletableFuture lets the caller compose / await; returning void hides
 * exceptions in the executor and is an anti-pattern for anything that can fail.
 */
@Service
public class AsyncReporterService {

    @Async
    public CompletableFuture<String> generateReportAsync() {
        // The thread name carries enough information to assert that the work ran
        // off-thread relative to the caller in the integration test.
        return CompletableFuture.completedFuture("report-from-" + Thread.currentThread().getName());
    }
}
