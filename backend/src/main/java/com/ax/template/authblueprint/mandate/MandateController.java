package com.ax.template.authblueprint.mandate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * mandate-fanout-l0 thin controller. The acting principal is ALWAYS the authenticated caller
 * (caller-authentication-only-no-userid-param). Delegates to {@link MandateService}.
 */
@RestController
public class MandateController {

    public record IssueReq(@NotBlank @Size(max = 500) String directive,
                           @NotNull Integer taskCount,
                           List<@NotBlank @Size(max = 100) String> checkKeys) {}
    public record CompleteTaskReq(@NotNull MandateTaskState target) {}
    public record RecordCheckReq(@NotBlank @Size(max = 100) String checkKey,
                                 @NotNull MandateCheckVerdict verdict) {}

    public record MandateDto(UUID id, String directive, int issuedCount, MandateStatus status,
                             long terminalCount, boolean complete, String satisfiedBy,
                             Instant satisfiedAt, Instant createdAt) {
        static MandateDto of(MandateService.MandateView v) {
            Mandate m = v.mandate();
            return new MandateDto(m.getId(), m.getDirective(), m.getIssuedCount(), m.getStatus(),
                v.terminalCount(), v.complete(), m.getSatisfiedBy(), m.getSatisfiedAt(), m.getCreatedAt());
        }
        static MandateDto of(Mandate m) {
            // issue/satisfy responses: a freshly-mutated root; the recall is re-read on GET
            boolean complete = false;
            return new MandateDto(m.getId(), m.getDirective(), m.getIssuedCount(), m.getStatus(),
                0L, complete, m.getSatisfiedBy(), m.getSatisfiedAt(), m.getCreatedAt());
        }
    }
    public record TaskDto(UUID id, int taskSeq, MandateTaskState state, Instant deemedDeadline,
                          String resolvedBy, String resolveReason, Instant resolvedAt) {
        static TaskDto of(MandateTask t) {
            return new TaskDto(t.getId(), t.getTaskSeq(), t.getState(), t.getDeemedDeadline(),
                t.getResolvedBy(), t.getResolveReason(), t.getResolvedAt());
        }
    }
    public record CheckDto(String checkKey, MandateCheckVerdict verdict, String recordedBy,
                           Instant recordedAt) {
        static CheckDto of(MandateCheck c) {
            return new CheckDto(c.getCheckKey(), c.getVerdict(), c.getRecordedBy(), c.getRecordedAt());
        }
    }

    private final MandateService service;

    public MandateController(MandateService service) {
        this.service = service;
    }

    /** MANDATE-FANOUT-001 — issue atomically fans out to exactly N tasks + declares the battery. */
    @PostMapping("/api/mandate/mandates")
    public ResponseEntity<MandateDto> issue(@Valid @RequestBody IssueReq req, Authentication auth) {
        Mandate m = service.issue(req.directive(), req.taskCount(),
            req.checkKeys() == null ? List.of() : req.checkKeys(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(MandateDto.of(m));
    }

    /** MANDATE-FANOUT/CONCURRENT-001 — an explicit response (DONE/DECLINED) on one child. */
    @PostMapping("/api/mandate/tasks/{taskId}/complete")
    public TaskDto completeTask(@PathVariable UUID taskId, @Valid @RequestBody CompleteTaskReq req,
                                Authentication auth) {
        return TaskDto.of(service.completeTask(taskId, req.target(), auth.getName()));
    }

    /** MANDATE-BATTERY-001 — record/supersede a single check's verdict. */
    @PostMapping("/api/mandate/mandates/{id}/checks")
    public CheckDto recordCheck(@PathVariable UUID id, @Valid @RequestBody RecordCheckReq req,
                                Authentication auth) {
        return CheckDto.of(service.recordCheck(id, req.checkKey(), req.verdict(), auth.getName()));
    }

    /** MANDATE-BATTERY-001 — clear the mandate only when every declared check is PASSED. */
    @PostMapping("/api/mandate/mandates/{id}/satisfy")
    public MandateDto satisfy(@PathVariable UUID id, Authentication auth) {
        return MandateDto.of(service.satisfy(id, auth.getName()));
    }

    @GetMapping("/api/mandate/mandates/{id}")
    public MandateDto get(@PathVariable UUID id) {
        return MandateDto.of(service.get(id));
    }

    @GetMapping("/api/mandate/mandates/{id}/tasks")
    public List<TaskDto> tasks(@PathVariable UUID id) {
        return service.tasks(id).stream().map(TaskDto::of).toList();
    }

    @GetMapping("/api/mandate/mandates/{id}/checks")
    public List<CheckDto> checks(@PathVariable UUID id) {
        return service.checks(id).stream().map(CheckDto::of).toList();
    }

    @ExceptionHandler(MandateException.class)
    public ResponseEntity<ProblemDetail> handle(MandateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
