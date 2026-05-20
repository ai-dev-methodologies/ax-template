package com.ax.template.authblueprint.notification;

import com.ax.template.authblueprint.auditlog.Audited;
import com.ax.template.authblueprint.auditlog.ResourceId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service for the notification domain.
 * <p>
 * Trace:
 * <ul>
 *   <li>NOTIF-SEND-001 / NOTIF-SEND-002 — {@link #send(String, String, String, String, String)}</li>
 *   <li>NOTIF-LIST-001 / NOTIF-LIST-002 — {@link #list(String, NotificationStatus, int, int)}</li>
 *   <li>NOTIF-READ-001 — {@link #markRead(UUID, String)}</li>
 *   <li>NOTIF-DISMISS-001 — {@link #dismiss(UUID, String)}</li>
 *   <li>NOTIF-AUTHZ-002 — every read/write filters on {@code recipientUserId = caller}</li>
 * </ul>
 */
@Service
public class NotificationService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository repository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final NotificationDispatcher dispatcher;

    public NotificationService(NotificationRepository repository,
                               NotificationPreferencesRepository preferencesRepository,
                               NotificationDispatcher dispatcher) {
        this.repository = repository;
        this.preferencesRepository = preferencesRepository;
        this.dispatcher = dispatcher;
    }

    /**
     * NOTIF-SEND-001 + NOTIF-SEND-002 — persist then dispatch.
     * The {@code @Audited} annotation makes the {@link com.ax.template.authblueprint.auditlog.AuditLoggingAspect}
     * record an audit row.
     */
    @Audited(action = "NOTIFICATION_SEND", resourceType = "notification")
    @Transactional
    public Notification send(@ResourceId String recipientUserId,
                             String type,
                             String title,
                             String body,
                             String link) {
        Instant now = Instant.now();
        Notification entity = Notification.builder()
            .recipientUserId(recipientUserId)
            .type(type)
            .title(title)
            .body(body)
            .link(link)
            .status(NotificationStatus.UNREAD)
            .createdAt(now)
            .updatedAt(now)
            .build();
        Notification saved = repository.save(entity);

        NotificationPreferences prefs = preferencesRepository.findById(recipientUserId)
            .orElseGet(() -> NotificationPreferences.defaultsFor(recipientUserId));
        dispatcher.dispatch(saved, prefs);
        return saved;
    }

    /** NOTIF-LIST-001 + NOTIF-LIST-002 — paginated list, optionally filtered. */
    @Transactional(readOnly = true)
    public Page<Notification> list(String userId, NotificationStatus status, int page, int size) {
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                "page size " + size + " exceeds maximum " + MAX_PAGE_SIZE);
        }
        int boundedSize = Math.max(size, 1);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize,
            Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return repository.findByRecipientUserIdAndStatusAndDeletedFalse(userId, status, pageable);
        }
        return repository.findByRecipientUserIdAndDeletedFalse(userId, pageable);
    }

    /** NOTIF-LIST-002 — count of unread notifications for caller. */
    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return repository.countUnreadByRecipientUserId(userId);
    }

    /** NOTIF-AUTHZ-002 — strict owner lookup, returns 404 for missing or cross-user. */
    @Transactional(readOnly = true)
    public Notification getOwned(UUID id, String userId) {
        return repository.findByIdAndRecipientUserIdAndDeletedFalse(id, userId)
            .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    /** NOTIF-READ-001 — idempotent mark as read. */
    @Audited(action = "NOTIFICATION_READ", resourceType = "notification")
    @Transactional
    public Notification markRead(@ResourceId UUID id, String userId) {
        Notification n = getOwned(id, userId);
        n.markRead(Instant.now());
        return n;
    }

    /** NOTIF-DISMISS-001 — soft delete; cross-user returns 404. */
    @Audited(action = "NOTIFICATION_DISMISS", resourceType = "notification")
    @Transactional
    public void dismiss(@ResourceId UUID id, String userId) {
        Notification n = getOwned(id, userId);
        n.softDelete(Instant.now());
    }

    /** NOTIF-PREF-001 — returns defaults when no row exists. */
    @Transactional(readOnly = true)
    public NotificationPreferences getPreferences(String userId) {
        return preferencesRepository.findById(userId)
            .orElseGet(() -> NotificationPreferences.defaultsFor(userId));
    }

    /** NOTIF-PREF-002 — partial update; row is upserted. */
    @Transactional
    public NotificationPreferences updatePreferences(String userId,
                                                      Boolean inAppEnabled,
                                                      Boolean emailEnabled) {
        NotificationPreferences prefs = preferencesRepository.findById(userId)
            .orElseGet(() -> NotificationPreferences.defaultsFor(userId));
        prefs.apply(inAppEnabled, emailEnabled);
        return preferencesRepository.save(prefs);
    }
}
