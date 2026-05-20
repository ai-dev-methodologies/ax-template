package com.ax.template.authblueprint.scheduledtask;

import com.ax.template.authblueprint.auditlog.Audited;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Admin REST surface for the scheduled-task domain.
 * <p>
 * Trace:
 * <ul>
 *   <li>blueprints/scheduled-task-manifest.yaml#admin_api — endpoints + ROLE_ADMIN gate</li>
 *   <li>blueprints/scheduled-task-manifest.yaml#authz — {@code /api/admin/**}
 *       is locked to ROLE_ADMIN by the global SecurityConfig.</li>
 *   <li>SCHED-IDEMPOTENT-001 — POST /{id}/trigger uses the same lock as the
 *       in-process scheduler, so simultaneous admin triggers are idempotent.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/scheduled-tasks")
public class ScheduledTaskController {

    public static final String NOT_FOUND_TYPE = "https://ax-template.dev/problems/scheduled-task-not-found";

    private final ScheduledTaskService service;
    private final JobHistoryRepository historyRepository;

    public ScheduledTaskController(ScheduledTaskService service,
                                   JobHistoryRepository historyRepository) {
        this.service = service;
        this.historyRepository = historyRepository;
    }

    @GetMapping
    public List<ScheduledTaskDto.TaskResponse> list() {
        return service.listAll().stream().map(ScheduledTaskDto.TaskResponse::from).toList();
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
        ScheduledTask task = service.findById(id)
            .orElseThrow(() -> new ScheduledTaskNotFoundException(id));
        Page<JobHistory> rows = historyRepository
            .findByTaskNameOrderByStartedAtDesc(task.getName(), PageRequest.of(page, size));
        return rows.stream().map(ScheduledTaskDto.HistoryResponse::from).toList();
    }

    @ExceptionHandler(ScheduledTaskNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ScheduledTaskNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(NOT_FOUND_TYPE));
        pd.setTitle("Scheduled task not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }
}
