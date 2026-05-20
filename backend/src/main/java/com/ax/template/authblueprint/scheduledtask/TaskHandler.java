package com.ax.template.authblueprint.scheduledtask;

/**
 * Application-supplied work unit. Fork-receivers implement this for each task
 * and register the bean by name; {@link DefaultTaskExecutor} dispatches by name.
 * <p>
 * Trace: blueprints/scheduled-task-manifest.yaml#execute (executor_interface).
 */
public interface TaskHandler {
    void run(TaskExecutionContext ctx);
}
