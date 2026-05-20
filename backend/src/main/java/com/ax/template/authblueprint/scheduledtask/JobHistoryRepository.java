package com.ax.template.authblueprint.scheduledtask;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Trace:
 * <ul>
 *   <li>SCHED-EXECUTE-001 — append-only persistence of execution history</li>
 *   <li>blueprints/scheduled-task-manifest.yaml#admin_api — GET /history pagination</li>
 * </ul>
 */
@Repository
public interface JobHistoryRepository extends JpaRepository<JobHistory, UUID> {
    Page<JobHistory> findByTaskNameOrderByStartedAtDesc(String taskName, Pageable pageable);
}
