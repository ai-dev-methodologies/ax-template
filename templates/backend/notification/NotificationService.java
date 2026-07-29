/**
 * @ax-template-meta
 * template_id: backend/notification/NotificationService
 * layer: backend-domain
 * domain: notification
 * anchors_rule: testing-archunit-layer-boundary.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Service and transaction management"
 *     url: "https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html"
 *   - source_type: external
 *     citation: "OWASP ASVS V4.2.1 — Verify that the application has business logic limits"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   NotificationService owns all business logic for the notification domain.
 *   Controller delegates entirely to this service; no business logic in the controller.
 *   All operations are scoped to the callerUserId derived from the JWT principal.
 */
package com.example.app.notification;

import com.example.app.common.BaseService;
import com.example.app.notification.Notification.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for the notification domain.
 *
 * <p>Operations:
 * <ul>
 *   <li>{@link #list(UUID, NotificationStatus, Pageable)} — paginated list with optional status filter
 *   <li>{@link #countUnread(UUID)} — unread count for X-Unread-Count header
 *   <li>{@link #get(UUID, UUID)} — single notification (IDOR-safe: scoped by recipient)
 *   <li>{@link #send(NotificationDto.SendRequest)} — create + dispatch (admin use)
 *   <li>{@link #markRead(UUID, UUID)} — idempotent mark-read
 *   <li>{@link #dismiss(UUID, UUID)} — soft-delete
 *   <li>{@link #getPreferences(UUID)} — get or return defaults
 *   <li>{@link #updatePreferences(UUID, NotificationDto.UpdatePreferencesRequest)} — upsert preferences
 * </ul>
 *
 * <p>Extends {@link BaseService} (from SP13) for shared exception handling utilities.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final NotificationDispatcher dispatcher;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferencesRepository preferencesRepository,
            NotificationDispatcher dispatcher) {
        this.notificationRepository = notificationRepository;
        this.preferencesRepository = preferencesRepository;
        this.dispatcher = dispatcher;
    }

    // ─── list ─────────────────────────────────────────────────────────────

    /**
     * Returns paginated notifications for the caller.
     *
     * @param callerUserId  JWT principal's user ID
     * @param statusFilter  null = ALL, UNREAD, READ
     * @param pageable      page / size / sort from request params
     */
    public Page<Notification> list(UUID callerUserId, NotificationStatus statusFilter, Pageable pageable) {
        if (statusFilter != null) {
            return notificationRepository.findByRecipientUserIdAndStatus(callerUserId, statusFilter, pageable);
        }
        return notificationRepository.findByRecipientUserId(callerUserId, pageable);
    }

    /**
     * Counts UNREAD notifications for the caller (used for X-Unread-Count header).
     */
    public long countUnread(UUID callerUserId) {
        return notificationRepository.countUnreadByRecipientUserId(callerUserId);
    }

    // ─── get ──────────────────────────────────────────────────────────────

    /**
     * Returns a single notification by ID, scoped to the caller.
     *
     * <p>Throws {@code EntityNotFoundException} (→ 404) if the notification
     * does not exist or belongs to a different user (NOTIF-AUTHZ-002).
     */
    public Notification get(UUID id, UUID callerUserId) {
        return notificationRepository
                .findByIdAndRecipientUserId(id, callerUserId)
                .orElseThrow(() -> entityNotFound("Notification", id));
    }

    // ─── send ─────────────────────────────────────────────────────────────

    /**
     * Creates and dispatches a notification (admin-only path).
     *
     * <p>Dispatch is non-blocking: channel delivery failures are logged at WARN
     * level but do not roll back the transaction (NOTIF-SEND-002).
     */
    @Transactional
    public Notification send(NotificationDto.SendRequest req) {
        var notification = Notification.create(
                req.recipientUserId(),
                req.type(),
                req.title(),
                req.body(),
                req.actionUrl());
        var saved = notificationRepository.save(notification);

        // Non-blocking dispatch — failure must not fail the HTTP request
        try {
            dispatcher.dispatch(saved);
        } catch (Exception ex) {
            log.warn("Notification dispatch failed for id={}: {}", saved.getId(), ex.getMessage());
        }

        return saved;
    }

    // ─── mark-read ────────────────────────────────────────────────────────

    /**
     * Marks a notification as READ (idempotent).
     *
     * <p>Calling on an already-READ notification returns the entity unchanged (NOTIF-READ-001).
     */
    @Transactional
    public Notification markRead(UUID id, UUID callerUserId) {
        var notification = get(id, callerUserId);
        notification.markRead();
        return notificationRepository.save(notification);
    }

    // ─── dismiss ──────────────────────────────────────────────────────────

    /**
     * Soft-deletes (dismisses) a notification.
     *
     * <p>Uses {@code BaseRepository.softDelete} which sets {@code deleted=true}
     * without removing the row (NOTIF-DISMISS-001).
     */
    @Transactional
    public void dismiss(UUID id, UUID callerUserId) {
        // Verify ownership first (throws 404 if not found or wrong user)
        get(id, callerUserId);
        notificationRepository.softDelete(id);
    }

    // ─── preferences ──────────────────────────────────────────────────────

    /**
     * Returns the caller's preferences, or defaults if no row exists (lazy-init).
     * No row is inserted by this call (NOTIF-PREF-001).
     */
    public NotificationDto.PreferencesResponse getPreferences(UUID callerUserId) {
        return preferencesRepository
                .findByUserId(callerUserId)
                .map(NotificationDto.PreferencesResponse::from)
                .orElse(NotificationDto.PreferencesResponse.defaults(callerUserId));
    }

    /**
     * Partially updates (or creates) the caller's preferences row (NOTIF-PREF-002).
     *
     * <p>Only non-null fields in the request are applied (PATCH semantics).
     */
    @Transactional
    public NotificationDto.PreferencesResponse updatePreferences(
            UUID callerUserId,
            NotificationDto.UpdatePreferencesRequest req) {

        var prefs = preferencesRepository
                .findByUserId(callerUserId)
                .orElseGet(() -> NotificationPreferences.create(callerUserId, true, true));

        prefs.applyPartialUpdate(req);
        var saved = preferencesRepository.save(prefs);
        return NotificationDto.PreferencesResponse.from(saved);
    }
}
