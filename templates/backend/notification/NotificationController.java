/**
 * @ax-template-meta
 * template_id: backend/notification/NotificationController
 * layer: backend-domain
 * domain: notification
 * anchors_rule: api-controller-service-separation.md (PRACTICES-API-003)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring MVC Reference — @RestController and @RequestMapping"
 *     url: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html"
 *   - source_type: external
 *     citation: "Spring Data Web Support — Pageable resolution from request parameters"
 *     url: "https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.basic"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   NotificationController extends BaseController (from SP13).
 *   All operations delegate to NotificationService — no business logic here.
 *   callerUserId is resolved from the JWT principal via BaseController.currentUserId().
 */
package com.example.app.notification;

import com.example.app.common.BaseController;
import com.example.app.notification.Notification.NotificationStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * REST controller for the notification domain.
 *
 * <p>Endpoints (operationId → HTTP):
 * <ul>
 *   <li>{@code listNotifications}             — GET /api/notifications
 *   <li>{@code sendNotification}              — POST /api/notifications (ADMIN only)
 *   <li>{@code getNotification}               — GET /api/notifications/{id}
 *   <li>{@code dismissNotification}           — DELETE /api/notifications/{id}
 *   <li>{@code markNotificationRead}          — PATCH /api/notifications/{id}/read
 *   <li>{@code getNotificationPreferences}    — GET /api/notifications/preferences
 *   <li>{@code updateNotificationPreferences} — PATCH /api/notifications/preferences
 * </ul>
 *
 * <p>Rule reference: PRACTICES-API-003 (controller delegates to service; no business logic here).
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ─── list ─────────────────────────────────────────────────────────────

    /**
     * GET /api/notifications
     *
     * <p>Returns paginated notifications for the caller.
     * Appends X-Unread-Count response header (NOTIF-LIST-002).
     * Supports ?status=UNREAD|READ|ALL filter.
     *
     * @param status   optional status filter (null = ALL)
     * @param pageable Spring resolves page/size/sort from request params;
     *                 default: size=20, sort=createdAt DESC
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDto.Response>> list(
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable,
            HttpServletResponse httpResponse) {

        var callerUserId = currentUserId();
        var page = notificationService.list(callerUserId, status, pageable);
        var unreadCount = notificationService.countUnread(callerUserId);

        httpResponse.setHeader("X-Unread-Count", String.valueOf(unreadCount));
        httpResponse.setHeader("Access-Control-Expose-Headers", "X-Unread-Count");

        return ResponseEntity.ok(page.map(NotificationDto.Response::from));
    }

    // ─── send (admin only) ────────────────────────────────────────────────

    /**
     * POST /api/notifications
     *
     * <p>Admin-only: creates and dispatches a notification to the target user.
     * Non-admin callers receive 403 (enforced by @PreAuthorize).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public NotificationDto.Response send(
            @Valid @RequestBody NotificationDto.SendRequest req) {
        var notification = notificationService.send(req);
        return NotificationDto.Response.from(notification);
    }

    // ─── get ──────────────────────────────────────────────────────────────

    /**
     * GET /api/notifications/{id}
     *
     * <p>Returns a single notification scoped to the caller.
     * Returns 404 if not found or belongs to a different user (NOTIF-AUTHZ-002).
     */
    @GetMapping("/{id}")
    public NotificationDto.Response get(@PathVariable UUID id) {
        var notification = notificationService.get(id, currentUserId());
        return NotificationDto.Response.from(notification);
    }

    // ─── dismiss ──────────────────────────────────────────────────────────

    /**
     * DELETE /api/notifications/{id}
     *
     * <p>Soft-deletes a notification for the caller.
     * Returns 404 if not found or belongs to a different user (NOTIF-DISMISS-001).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismiss(@PathVariable UUID id) {
        notificationService.dismiss(id, currentUserId());
    }

    // ─── mark-read ────────────────────────────────────────────────────────

    /**
     * PATCH /api/notifications/{id}/read
     *
     * <p>Marks a notification as READ (idempotent — safe to call multiple times).
     * Returns 404 if not found or belongs to a different user (NOTIF-READ-001).
     */
    @PatchMapping("/{id}/read")
    public NotificationDto.Response markRead(@PathVariable UUID id) {
        var notification = notificationService.markRead(id, currentUserId());
        return NotificationDto.Response.from(notification);
    }

    // ─── preferences ──────────────────────────────────────────────────────

    /**
     * GET /api/notifications/preferences
     *
     * <p>Returns caller's preferences, or defaults (all channels enabled) if never set.
     * No row is created by this call (lazy-init policy, NOTIF-PREF-001).
     */
    @GetMapping("/preferences")
    public NotificationDto.PreferencesResponse getPreferences() {
        return notificationService.getPreferences(currentUserId());
    }

    /**
     * PATCH /api/notifications/preferences
     *
     * <p>Partially updates caller's preferences. Null fields are ignored (NOTIF-PREF-002).
     */
    @PatchMapping("/preferences")
    public NotificationDto.PreferencesResponse updatePreferences(
            @Valid @RequestBody NotificationDto.UpdatePreferencesRequest req) {
        return notificationService.updatePreferences(currentUserId(), req);
    }
}
