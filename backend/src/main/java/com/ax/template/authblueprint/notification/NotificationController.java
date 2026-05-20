package com.ax.template.authblueprint.notification;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Notification REST surface — owner-only access enforced in the service layer.
 * <p>
 * Trace:
 * <ul>
 *   <li>NOTIF-AUTHZ-001 — every endpoint requires JWT (SecurityConfig maps {@code /api/notifications/**} to authenticated())</li>
 *   <li>NOTIF-AUTHZ-002 — service uses caller's userId from {@link Authentication#getName()}, never a path arg</li>
 *   <li>NOTIF-AUTHZ-003 — preferences endpoints never accept a userId in the URL</li>
 *   <li>NOTIF-SEND-001 — POST persists + dispatches</li>
 *   <li>NOTIF-LIST-001/002 — GET supports pagination + status filter + X-Unread-Count header</li>
 *   <li>NOTIF-READ-001 — PATCH /{id}/read</li>
 *   <li>NOTIF-DISMISS-001 — DELETE /{id} (soft delete)</li>
 *   <li>NOTIF-PREF-001/002 — GET + PATCH /preferences</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationSendRequest req) {
        Notification saved = service.send(
            req.recipientUserId(),
            req.type(),
            req.title(),
            req.body(),
            req.link()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<NotificationPage> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status,
        Authentication auth,
        HttpServletResponse response
    ) {
        String userId = auth.getName();
        NotificationStatus filter = parseStatusFilter(status);
        Page<Notification> result = service.list(userId, filter, page, size);
        long unread = service.unreadCount(userId);
        response.setHeader("X-Unread-Count", Long.toString(unread));

        NotificationPage body = new NotificationPage(
            result.map(NotificationResponse::from).getContent(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.getNumber(),
            result.getSize()
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public NotificationResponse getOne(@PathVariable UUID id, Authentication auth) {
        return NotificationResponse.from(service.getOwned(id, auth.getName()));
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id, Authentication auth) {
        return NotificationResponse.from(service.markRead(id, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> dismiss(@PathVariable UUID id, Authentication auth) {
        service.dismiss(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public NotificationPreferencesResponse getPreferences(Authentication auth) {
        return NotificationPreferencesResponse.from(service.getPreferences(auth.getName()));
    }

    @PatchMapping("/preferences")
    public NotificationPreferencesResponse updatePreferences(
        @RequestBody NotificationPreferencesUpdateRequest req,
        Authentication auth
    ) {
        return NotificationPreferencesResponse.from(
            service.updatePreferences(auth.getName(), req.inAppEnabled(), req.emailEnabled()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /**
     * NOTIF-AUTHZ-002 — cross-user IDOR returns 404 to avoid leaking the
     * existence of another user's row. Explicit handler is used instead of
     * relying solely on {@code @ResponseStatus} so the mapping cannot be
     * shadowed by other resolvers in the application context.
     */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "not_found"));
    }

    /**
     * NOTIF-LIST-002 — accepts UNREAD | READ | ALL (case-insensitive).
     * "ALL" / null → no filter. Unknown → 400 via IllegalArgumentException.
     */
    private NotificationStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return null;
        }
        return NotificationStatus.valueOf(status.toUpperCase());
    }
}
