package com.ax.template.authblueprint.scheduledtask;

/**
 * Dispatch-by-name SPI invoked by {@link ScheduledTaskService}. Fork-receivers
 * register one bean and route taskName → handler logic.
 * <p>
 * Trace: blueprints/scheduled-task-manifest.yaml#execute (default_impl).
 */
public interface TaskExecutor {

    /**
     * Run the named task. Implementations throw on failure — the service
     * records the failure as a {@link JobHistory} row with outcome=FAILED.
     */
    void execute(String taskName);
}
