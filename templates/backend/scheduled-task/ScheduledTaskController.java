/**
 * @ax-template-meta
 * template_id: backend/scheduled-task/ScheduledTaskController
 * layer: backend-domain
 * domain: scheduled-task
 * anchors_rule: bfla-privileged-endpoint-authz-presence.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring MVC Reference — @RestController and @RequestMapping"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html"
 *   - source_type: external
 *     citation: "OWASP ASVS V4.1 — Verify that access control policies are enforced"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   ScheduledTaskController is admin-only; all methods require ROLE_ADMIN.
 *   All operations delegate to ScheduledTaskService — no business logic here.
 *   Extends BaseController (SP13).
 */
package com.example.app.scheduledtask;

import com.example.app.common.BaseController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Admin REST controller for the scheduled-task domain.
 *
 * <p>All endpoints require {@code ROLE_ADMIN}.
 * No end-user facing endpoints — scheduling is fully server-internal.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code listScheduledTasks}  — GET /api/admin/scheduled-tasks
 *   <li>{@code getScheduledTask}    — GET /api/admin/scheduled-tasks/{id}
 *   <li>{@code triggerScheduledTask} — POST /api/admin/scheduled-tasks/{id}/trigger
 *   <li>{@code getTaskHistory}      — GET /api/admin/scheduled-tasks/{id}/history
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/scheduled-tasks")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ScheduledTaskController extends BaseController {

    private final ScheduledTaskService taskService;

    public ScheduledTaskController(ScheduledTaskService taskService) {
        this.taskService = taskService;
    }

    // ─── list ─────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/scheduled-tasks
     *
     * <p>Paginated list of all registered tasks. Optional ?status=REGISTERED|ACTIVE|PAUSED|ALL.
     */
    @GetMapping
    public Page<ScheduledTaskDto.Summary> list(
            @RequestParam(required = false) ScheduledTask.ScheduledTaskStatus status,
            @PageableDefault(size = 20, sort = "name",
                    direction = org.springframework.data.domain.Sort.Direction.ASC)
            Pageable pageable) {
        return taskService.listForAdmin(status, pageable)
                .map(ScheduledTaskDto.Summary::from);
    }

    // ─── get ──────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/scheduled-tasks/{id}
     *
     * <p>Single task detail including lock state.
     */
    @GetMapping("/{id}")
    public ScheduledTaskDto.Detail get(@PathVariable UUID id) {
        return ScheduledTaskDto.Detail.from(taskService.getForAdmin(id));
    }

    // ─── trigger ──────────────────────────────────────────────────────────

    /**
     * POST /api/admin/scheduled-tasks/{id}/trigger
     *
     * <p>Manually triggers task execution. Subject to distributed lock.
     * Returns triggered=true if lock acquired; triggered=false if already running.
     */
    @PostMapping("/{id}/trigger")
    public Map<String, Object> trigger(@PathVariable UUID id) {
        boolean triggered = taskService.triggerManual(id);
        return Map.of(
                "taskId", id,
                "triggered", triggered,
                "message", triggered
                        ? "Task triggered successfully"
                        : "Task is currently locked — already running on another node");
    }

    // ─── history ──────────────────────────────────────────────────────────

    /**
     * GET /api/admin/scheduled-tasks/{id}/history
     *
     * <p>Paginated execution history for a task, newest first.
     */
    @GetMapping("/{id}/history")
    public Page<ScheduledTaskDto.JobHistorySummary> history(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "startedAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return taskService.getHistory(id, pageable)
                .map(ScheduledTaskDto.JobHistorySummary::from);
    }
}
