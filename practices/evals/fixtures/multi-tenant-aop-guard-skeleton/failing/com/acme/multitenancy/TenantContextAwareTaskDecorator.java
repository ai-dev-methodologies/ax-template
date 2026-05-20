package com.acme.multitenancy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.core.task.TaskDecorator;

/**
 * Captures the calling thread's TenantContext at submission time
 * and restores it on the worker thread before the task runs.
 * Guarantees:
 *   (a) absent context at submission -> wrapped Runnable raises
 *       TenantContextMissingException on the worker (NOT a silent
 *       default-tenant fallback). This matches #async-propagation
 *       failure_mode contract.
 *   (b) ThreadLocal is cleared in finally — no leakage to the next
 *       task scheduled on the same worker thread.
 *
 * Generated from blueprints/multi-tenant-manifest.yaml#async-propagation.task_decorator_skeleton
 * with <root> = acme.
 */
public class TenantContextAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Captured at SUBMISSION (calling thread, still has request context).
        Optional<UUID> captured = TenantContext.current();
        return () -> {
            if (captured.isEmpty()) {
                // Crash loud — see #async-propagation failure_mode contract.
                throw new TenantContextMissingException(
                    "TenantContext was empty at @Async submission; refusing to run async task");
            }
            try {
                TenantContext.set(captured.get());
                runnable.run();
            } finally {
                // MUST clear — worker threads are pooled and reused.
                TenantContext.clear();
            }
        };
    }
}
