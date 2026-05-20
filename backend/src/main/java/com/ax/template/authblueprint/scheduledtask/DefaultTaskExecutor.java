package com.ax.template.authblueprint.scheduledtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default {@link TaskExecutor} — dispatch table of {@link TaskHandler} beans
 * keyed by Spring bean name.
 * <p>
 * Fork-receivers register their tasks as {@code @Component("my-task")
 * class MyTask implements TaskHandler} and the executor resolves them by name.
 * <p>
 * Trace: blueprints/scheduled-task-manifest.yaml#execute (default_impl).
 */
@Component
public class DefaultTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultTaskExecutor.class);

    private final Map<String, TaskHandler> handlers;

    /**
     * Spring auto-collects all {@link TaskHandler} beans keyed by Spring bean
     * name. Fork-receivers register handlers via
     * {@code @Component("cleanup-expired-tokens") class CleanupTask implements TaskHandler}.
     */
    public DefaultTaskExecutor(Map<String, TaskHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void execute(String taskName) {
        TaskHandler handler = handlers.get(taskName);
        if (handler == null) {
            log.warn("scheduled-task: no handler registered for taskName={}", taskName);
            throw new IllegalStateException("No handler registered for task: " + taskName);
        }
        handler.run(new TaskExecutionContext(taskName));
    }
}
