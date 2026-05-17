package com.ax.template.authblueprint.practices;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;

/**
 * Fixture for PRACTICES-ASYNC-001: JDK 21 virtual threads.
 * Exposes a virtual-thread-per-task ExecutorService for blocking-IO workloads.
 * Platform threads remain the default for CPU-bound work — they are scarce; virtual
 * threads are cheap, so use them when each task spends most of its life parked on IO.
 */
@Service
public class VirtualThreadDispatcher {

    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
