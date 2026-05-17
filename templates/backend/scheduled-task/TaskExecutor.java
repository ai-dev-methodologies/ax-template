/**
 * @ax-template-meta
 * template_id: backend/scheduled-task/TaskExecutor
 * layer: backend-domain
 * domain: scheduled-task
 * anchors_rule: api-controller-service-separation.md (PRACTICES-API-003)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Scheduled annotation and task execution"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
 *   - source_type: external
 *     citation: "Command Pattern — Gang of Four Design Patterns"
 *     url: "https://refactoring.guru/design-patterns/command"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   TaskExecutor is the interface for executing a ScheduledTask.
 *   DefaultTaskExecutor implements a dispatch table: task name → Runnable.
 *   Register task handlers in Spring configuration or via a @Bean registry.
 *   ScheduledTaskService calls this interface — no business logic in the executor itself.
 */
package com.example.app.scheduledtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface for executing a registered ScheduledTask.
 *
 * <p>Implementations receive a {@link ScheduledTask} and are responsible for
 * running the task's business logic. Exceptions must be thrown (not swallowed)
 * so that {@link ScheduledTaskService} can record FAILED in {@link JobHistory}.
 */
public interface TaskExecutor {

    /**
     * Executes the given scheduled task.
     *
     * @param task the task to execute
     * @throws RuntimeException on execution failure; will be caught by ScheduledTaskService
     *                         and recorded in JobHistory as FAILED
     */
    void execute(ScheduledTask task);

    // ─── DefaultTaskExecutor ───────────────────────────────────────────────

    /**
     * Default executor that dispatches to registered {@link Runnable} handlers by task name.
     *
     * <p>Register handlers at startup:
     * <pre>
     *   executor.register("cleanup-expired-tokens", () -> tokenService.cleanupExpired());
     *   executor.register("send-digest-emails", () -> digestService.sendDaily());
     * </pre>
     *
     * <p>If no handler is registered for a task name, throws {@link IllegalStateException}.
     */
    @Component
    class DefaultTaskExecutor implements TaskExecutor {

        private static final Logger log = LoggerFactory.getLogger(DefaultTaskExecutor.class);

        private final Map<String, Runnable> handlers = new ConcurrentHashMap<>();

        /**
         * Registers a handler for the given task name.
         * Thread-safe; safe to call at application startup or from @PostConstruct.
         *
         * @param taskName task name (matches ScheduledTask.name)
         * @param handler  runnable that performs the task's work
         */
        public void register(String taskName, Runnable handler) {
            handlers.put(taskName, handler);
            log.info("TaskExecutor: registered handler for '{}'", taskName);
        }

        @Override
        public void execute(ScheduledTask task) {
            var handler = handlers.get(task.getName());
            if (handler == null) {
                throw new IllegalStateException(
                        "No handler registered for task: " + task.getName());
            }
            log.info("TaskExecutor: executing '{}' (id={})", task.getName(), task.getId());
            handler.run();
        }
    }
}
