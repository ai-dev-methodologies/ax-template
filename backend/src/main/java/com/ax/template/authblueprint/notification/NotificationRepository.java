package com.ax.template.authblueprint.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Notification}.
 * <p>
 * All read paths must filter {@code recipientUserId} so callers cannot leak
 * cross-user rows (NOTIF-AUTHZ-002).
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** NOTIF-AUTHZ-002 — strict owner lookup. */
    Optional<Notification> findByIdAndRecipientUserIdAndDeletedFalse(UUID id, String recipientUserId);

    /** NOTIF-LIST-001 — all (non-deleted) for caller. */
    Page<Notification> findByRecipientUserIdAndDeletedFalse(String recipientUserId, Pageable pageable);

    /** NOTIF-LIST-002 — filtered by status. */
    Page<Notification> findByRecipientUserIdAndStatusAndDeletedFalse(
        String recipientUserId, NotificationStatus status, Pageable pageable);

    /** NOTIF-LIST-002 — X-Unread-Count header. */
    @Query("select count(n) from Notification n where n.recipientUserId = :userId and n.status = com.ax.template.authblueprint.notification.NotificationStatus.UNREAD and n.deleted = false")
    long countUnreadByRecipientUserId(@Param("userId") String userId);
}
