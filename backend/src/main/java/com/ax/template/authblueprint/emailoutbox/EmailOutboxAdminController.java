package com.ax.template.authblueprint.emailoutbox;

import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.ax.template.authblueprint.emailoutbox.EmailOutboxDto.OutboxPage;
import com.ax.template.authblueprint.emailoutbox.EmailOutboxDto.OutboxResponse;

/**
 * EMAIL-ADMIN-001 — admin surface for the email outbox. All endpoints
 * require ROLE_ADMIN; the {@link PreAuthorize} annotation is the source
 * of truth + the frontend useCallerRole hook mirrors the gate.
 *
 * R52 catalog rule applied: Cache-Control: no-store on every response —
 * shared-workstation enterprise scenarios (hot-desk, kiosk, screen-share
 * replay, back-button-after-logout) MUST NOT cache the prior admin's
 * outbox view.
 */
@RestController
@RequestMapping("/api/admin/email-outbox")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class EmailOutboxAdminController {

    private final EmailOutboxService service;

    public EmailOutboxAdminController(EmailOutboxService service) {
        this.service = service;
    }

    /**
     * P2-35(b) — {@code status} is bound as a raw String, not as the enum. The contract
     * declares this parameter with {@code default: ALL}, so a client that follows the
     * contract sends {@code ?status=ALL}; bound directly to {@link EmailOutboxStatus}
     * that raised MethodArgumentTypeMismatchException and the endpoint answered its own
     * documented default with a 400. ALL / blank / absent → no filter, matching the
     * family convention set by {@code NotificationController.parseStatusFilter}. An
     * unknown token still 400s — via the same shared advice, since
     * {@link IllegalArgumentException} is deliberately not globally mapped, so the
     * controller-local handler below owns it.
     */
    @GetMapping
    public ResponseEntity<OutboxPage> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        // R60 dogfood F3 closure — return Page metadata so the operator UI
        // sees totalElements / totalPages and paginates with accurate counts
        // instead of inferring queue size from a List length.
        Page<EmailOutbox> rows = service.adminList(parseStatusFilter(status), page, size);
        List<OutboxResponse> items = rows.getContent().stream().map(OutboxResponse::from).toList();
        OutboxPage body = new OutboxPage(items, rows.getNumber(), rows.getSize(),
            rows.getTotalElements(), rows.getTotalPages());
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header("Pragma", "no-cache")
            .body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutboxResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header("Pragma", "no-cache")
            .body(OutboxResponse.from(service.adminGet(id)));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<OutboxResponse> retry(@PathVariable UUID id) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(OutboxResponse.from(service.adminRetry(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.adminDelete(id);
        // RFC 9110 §9.3.5 idempotent DELETE — 204 even on absent target
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    /**
     * P2-35(b) — accepts PENDING | RETRY | SENT | DLQ | ALL (case-insensitive).
     * ALL / blank / absent → no filter. Unknown → {@link IllegalArgumentException}
     * → 400 via {@link #handleBadRequest}. Mirrors
     * {@code NotificationController.parseStatusFilter}, the family reference.
     */
    private EmailOutboxStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return null;
        }
        return EmailOutboxStatus.valueOf(status.toUpperCase(Locale.ROOT));
    }

    /**
     * Owns the 400 for an unknown {@code status} token. The shared
     * {@code common.GlobalProblemDetailAdvice} deliberately does NOT map
     * {@link IllegalArgumentException} (it would mask genuine programming bugs), so
     * this controller-local handler does — same posture as NotificationController.
     * The offending raw value is NOT echoed (response-amplification defense).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Parameter 'status' must be one of PENDING, RETRY, SENT, DLQ, ALL.");
        pd.setProperty("code", "EMAIL_OUTBOX_BAD_REQUEST");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(EmailOutboxNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EmailOutboxNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setProperty("code", "EMAIL_OUTBOX_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setProperty("code", "EMAIL_OUTBOX_INVALID_TRANSITION");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }
}
