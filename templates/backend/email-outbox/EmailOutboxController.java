/**
 * @ax-template-meta
 * template_id: backend/email-outbox/EmailOutboxController
 * layer: backend-domain
 * domain: email-outbox
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
 *   EmailOutboxController is admin-only; all methods require ROLE_ADMIN.
 *   All operations delegate to EmailOutboxService — no business logic here.
 *   Extends BaseController (SP13).
 */
package com.example.app.emailoutbox;

import com.example.app.common.BaseController;
import com.example.app.emailoutbox.EmailOutbox.EmailOutboxStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Admin REST controller for the email-outbox domain.
 *
 * <p>All endpoints require {@code ROLE_ADMIN} (EMAIL-ADMIN-001).
 * No end-user facing endpoints exist in this domain — delivery is fully async.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code listEmailOutbox}      — GET /api/admin/email-outbox
 *   <li>{@code getEmailOutboxEntry}  — GET /api/admin/email-outbox/{id}
 *   <li>{@code cancelEmailOutboxEntry} — DELETE /api/admin/email-outbox/{id}
 *   <li>{@code retryEmailOutboxEntry}  — POST /api/admin/email-outbox/{id}/retry
 *   <li>{@code previewEmailTemplate}   — POST /api/admin/email-outbox/preview
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/email-outbox")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class EmailOutboxController extends BaseController {

    private final EmailOutboxService outboxService;

    public EmailOutboxController(EmailOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    // ─── list ─────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/email-outbox
     *
     * <p>Paginated list of outbox entries. Optional ?status=PENDING|SENT|RETRY|DLQ|ALL filter.
     */
    @GetMapping
    public Page<EmailOutboxDto.Response> list(
            @RequestParam(required = false) EmailOutboxStatus status,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return outboxService.listForAdmin(status, pageable)
                .map(EmailOutboxDto.Response::from);
    }

    // ─── get ──────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/email-outbox/{id}
     *
     * <p>Single outbox entry detail.
     */
    @GetMapping("/{id}")
    public EmailOutboxDto.Response get(@PathVariable UUID id) {
        return EmailOutboxDto.Response.from(outboxService.getForAdmin(id));
    }

    // ─── cancel ───────────────────────────────────────────────────────────

    /**
     * DELETE /api/admin/email-outbox/{id}
     *
     * <p>Cancels a PENDING or RETRY entry (moves to DLQ with "admin-cancelled" reason).
     * Returns 409 if entry is already SENT.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        outboxService.cancel(id);
    }

    // ─── retry ────────────────────────────────────────────────────────────

    /**
     * POST /api/admin/email-outbox/{id}/retry
     *
     * <p>Resets a DLQ entry to PENDING for the next processQueue() cycle.
     * Returns 409 if entry is not in DLQ status.
     */
    @PostMapping("/{id}/retry")
    public EmailOutboxDto.Response retry(@PathVariable UUID id) {
        return EmailOutboxDto.Response.from(outboxService.retryFromDlq(id));
    }

    // ─── preview ──────────────────────────────────────────────────────────

    /**
     * POST /api/admin/email-outbox/preview
     *
     * <p>Renders a template without persisting or sending (EMAIL-QUEUE-002).
     */
    @PostMapping("/preview")
    public EmailOutboxDto.PreviewResponse preview(
            @Valid @RequestBody EmailOutboxDto.PreviewRequest req) {
        var rendered = outboxService.previewTemplate(req.templateCode(), req.templateVars());
        return new EmailOutboxDto.PreviewResponse(rendered.subject(), rendered.body());
    }
}
