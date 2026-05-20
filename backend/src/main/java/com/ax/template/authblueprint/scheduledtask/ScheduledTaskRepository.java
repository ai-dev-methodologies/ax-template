package com.ax.template.authblueprint.scheduledtask;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Trace:
 * <ul>
 *   <li>SCHED-REGISTER-001 — {@code save} persists with status=REGISTERED + UUID</li>
 *   <li>SCHED-EXECUTE-001 — {@code findById} resolves the task for the executor</li>
 * </ul>
 */
@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, UUID> {
    Optional<ScheduledTask> findByName(String name);
}
