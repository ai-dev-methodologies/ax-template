package com.ax.template.authblueprint.approvalworkflow;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * REST surface for the approval-workflow domain.
 *
 * <p>Trace:
 * <ul>
 *   <li>WF-AUTHZ-001 — SecurityConfig matcher {@code /api/approvals/**} authenticated()</li>
 *   <li>WF-AUTHZ-002 — service uses owner-scoped + visibility-scoped lookups, returning 404
 *       (mapped from {@link ApprovalRequestNotFoundException}) for cross-user access</li>
 *   <li>WF-AUTHZ-003 — step actions check {@code approverUserId} == caller; mismatch
 *       throws {@link NotApproverException} → 403</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApprovalRequestResponse> create(Authentication auth,
                                                          @Valid @RequestBody CreateApprovalRequest body) {
        ApprovalRequestResponse response = service.create(auth.getName(), body);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .location(URI.create("/api/approvals/" + response.id()))
            .body(response);
    }

    @GetMapping
    public ApprovalListResponse list(Authentication auth) {
        return service.listOwn(auth.getName());
    }

    @GetMapping("/inbox")
    public ApprovalInboxResponse inbox(Authentication auth) {
        return service.inbox(auth.getName());
    }

    @GetMapping("/{id}")
    public ApprovalRequestResponse get(Authentication auth, @PathVariable UUID id) {
        return service.getVisible(auth.getName(), id);
    }

    @PostMapping("/{id}/submit")
    public ApprovalRequestResponse submit(Authentication auth, @PathVariable UUID id) {
        return service.submit(auth.getName(), id);
    }

    @PostMapping("/{id}/cancel")
    public ApprovalRequestResponse cancel(Authentication auth, @PathVariable UUID id) {
        return service.cancel(auth.getName(), id);
    }

    @PostMapping("/{id}/steps/{stepId}/approve")
    public ApprovalRequestResponse approveStep(Authentication auth,
                                               @PathVariable UUID id,
                                               @PathVariable UUID stepId,
                                               @Valid @RequestBody(required = false) StepActionRequest body) {
        String comment = body == null ? null : body.comment();
        return service.approveStep(auth.getName(), id, stepId, comment);
    }

    @PostMapping("/{id}/steps/{stepId}/reject")
    public ApprovalRequestResponse rejectStep(Authentication auth,
                                              @PathVariable UUID id,
                                              @PathVariable UUID stepId,
                                              @Valid @RequestBody(required = false) StepActionRequest body) {
        String comment = body == null ? null : body.comment();
        return service.rejectStep(auth.getName(), id, stepId, comment);
    }

    // ── Exception → HTTP mapping ─────────────────────────────────────────────

    @ExceptionHandler(ApprovalRequestNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ApprovalRequestNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(NotApproverException.class)
    public ResponseEntity<ProblemDetail> handleNotApprover(NotApproverException ex) {
        return problem(HttpStatus.FORBIDDEN, "NOT_APPROVER", ex.getMessage());
    }

    @ExceptionHandler(RequestTerminalException.class)
    public ResponseEntity<ProblemDetail> handleTerminal(RequestTerminalException ex) {
        return problem(HttpStatus.CONFLICT, "REQUEST_TERMINAL", ex.getMessage());
    }

    @ExceptionHandler(StepOutOfOrderException.class)
    public ResponseEntity<ProblemDetail> handleOutOfOrder(StepOutOfOrderException ex) {
        return problem(HttpStatus.CONFLICT, "STEP_OUT_OF_ORDER", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        return problem(HttpStatus.CONFLICT, "ILLEGAL_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(DuplicateApproverException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateApproverException ex) {
        return problem(HttpStatus.BAD_REQUEST, "DUPLICATE_APPROVER", ex.getMessage());
    }

    @ExceptionHandler(SelfApproveForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleSelfApprove(SelfApproveForbiddenException ex) {
        return problem(HttpStatus.BAD_REQUEST, "SELF_APPROVE_FORBIDDEN", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
