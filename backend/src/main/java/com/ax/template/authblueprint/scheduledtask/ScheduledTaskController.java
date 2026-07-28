package com.ax.template.authblueprint.scheduledtask;

import com.ax.template.authblueprint.auditlog.Audited;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Admin REST surface for the scheduled-task domain.
 * <p>
 * Trace:
 * <ul>
 *   <li>blueprints/scheduled-task-manifest.yaml#admin_api — endpoints + ROLE_ADMIN gate</li>
 *   <li>blueprints/scheduled-task-manifest.yaml#authz — {@code /api/admin/**}
 *       is locked to ROLE_ADMIN by the global SecurityConfig; this controller
 *       ALSO declares a class-level
 *       {@code @PreAuthorize("hasAuthority('ROLE_ADMIN')")} as defense-in-depth
 *       (method security is the primary, locally-verifiable gate; the path
 *       matcher stays as a complementary layer).</li>
 *   <li>SCHED-IDEMPOTENT-001 — POST /{id}/trigger uses the same lock as the
 *       in-process scheduler, so simultaneous admin triggers are idempotent.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/scheduled-tasks")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ScheduledTaskController {

    public static final String NOT_FOUND_TYPE = "https://ax-template.dev/problems/scheduled-task-not-found";

    private final ScheduledTaskService service;

    public ScheduledTaskController(ScheduledTaskService service) {
        this.service = service;
    }

    /**
     * P2-35(c) — the contract has always declared a {@code status} query parameter on
     * this operation (scheduled-task-openapi.yaml, operationId listScheduledTasks), but
     * the handler took NO arguments, so Spring silently discarded it: a client filtering
     * by status got the unfiltered list back and had no way to tell. Now bound, with the
     * family's ALL no-filter sentinel (notification-openapi is the reference) and the
     * shipped ScheduledTaskStatus vocabulary — the contract previously advertised
     * ACTIVE/PAUSED, which this domain has never had (REGISTERED/ENABLED/DISABLED).
     */
    @GetMapping
    public List<ScheduledTaskDto.TaskResponse> list(
            @RequestParam(required = false) String status) {
        ScheduledTaskStatus filter = parseStatusFilter(status);
        return service.listAll().stream()
            .filter(t -> filter == null || t.getStatus() == filter)
            .map(ScheduledTaskDto.TaskResponse::from)
            .toList();
    }

    /**
     * Accepts REGISTERED | ENABLED | DISABLED | ALL (case-insensitive).
     * ALL / blank / absent → no filter. Unknown → {@link IllegalArgumentException}
     * → 400 via {@link #handleBadRequest}.
     */
    private ScheduledTaskStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return null;
        }
        return ScheduledTaskStatus.valueOf(status.toUpperCase(Locale.ROOT));
    }

    /**
     * Owns the 400 for an unknown {@code status} token; the shared advice deliberately
     * does not map {@link IllegalArgumentException}. The raw value is not echoed.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Parameter 'status' must be one of REGISTERED, ENABLED, DISABLED, ALL.");
        pd.setProperty("code", "SCHEDULED_TASK_BAD_REQUEST");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @GetMapping("/{id}")
    public ScheduledTaskDto.TaskResponse detail(@PathVariable UUID id) {
        ScheduledTask task = service.findById(id)
            .orElseThrow(() -> new ScheduledTaskNotFoundException(id));
        return ScheduledTaskDto.TaskResponse.from(task);
    }

    @PostMapping("/{id}/enable")
    @Audited(action = "ENABLE", resourceType = "scheduled_task")
    public ScheduledTaskDto.TaskResponse enable(@PathVariable UUID id) {
        return ScheduledTaskDto.TaskResponse.from(service.enable(id));
    }

    @PostMapping("/{id}/disable")
    @Audited(action = "DISABLE", resourceType = "scheduled_task")
    public ScheduledTaskDto.TaskResponse disable(@PathVariable UUID id) {
        return ScheduledTaskDto.TaskResponse.from(service.disable(id));
    }

    @PostMapping("/{id}/trigger")
    @Audited(action = "TRIGGER", resourceType = "scheduled_task")
    public ScheduledTaskDto.TriggerResponse trigger(@PathVariable UUID id) {
        return service.triggerManual(id)
            .map(ScheduledTaskDto.TriggerResponse::executed)
            .orElseGet(() -> ScheduledTaskDto.TriggerResponse.skipped("lock-held"));
    }

    @GetMapping("/{id}/history")
    public List<ScheduledTaskDto.HistoryResponse> history(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.history(id, page, size).stream()
            .map(ScheduledTaskDto.HistoryResponse::from)
            .toList();
    }

    @ExceptionHandler(ScheduledTaskNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ScheduledTaskNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(NOT_FOUND_TYPE));
        pd.setTitle("Scheduled task not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }
}
