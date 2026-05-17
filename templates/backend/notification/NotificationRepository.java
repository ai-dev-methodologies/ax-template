/**
 * @ax-template-meta
 * template_id: backend/notification/NotificationRepository
 * layer: backend-domain
 * domain: notification
 * anchors_rule: testing-archunit-repository-shape.md (PRACTICES-TEST-004)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Defining Repository Interfaces"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html"
 *   - source_type: external
 *     citation: "Spring Data Commons Reference — CrudRepository / JpaRepository hierarchy"
 *     url: "https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   NotificationRepository extends BaseRepository for soft-delete support.
 *   All list queries must filter by recipientUserId to prevent cross-user data leaks.
 */
package com.example.app.notification;

import com.example.app.repositories.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Notification entities.
 *
 * <p>Extends {@link BaseRepository} to inherit:
 * <ul>
 *   <li>{@code findAllActive(Pageable)} — excludes soft-deleted rows
 *   <li>{@code findActiveById(ID)} — excludes soft-deleted by ID
 *   <li>{@code softDelete(ID)} — marks {@code deleted=true} without removing the row
 * </ul>
 *
 * <p>All query methods automatically scope by {@code recipientUserId} to enforce
 * per-user isolation (NOTIF-AUTHZ-002).
 */
public interface NotificationRepository extends BaseRepository<Notification, UUID> {

    /**
     * Returns paginated notifications for a specific recipient, excluding soft-deleted rows.
     * Ordered by createdAt DESC (newest first).
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.recipientUserId = :userId
          AND n.deleted = false
        ORDER BY n.createdAt DESC
        """)
    Page<Notification> findByRecipientUserId(
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Returns paginated notifications filtered by status.
     * Used by GET /api/notifications?status=UNREAD|READ.
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.recipientUserId = :userId
          AND n.deleted = false
          AND n.status = :status
        ORDER BY n.createdAt DESC
        """)
    Page<Notification> findByRecipientUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") Notification.NotificationStatus status,
            Pageable pageable);

    /**
     * Counts UNREAD notifications for the given recipient.
     * Used to populate the X-Unread-Count response header (NOTIF-LIST-002).
     */
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.recipientUserId = :userId
          AND n.deleted = false
          AND n.status = 'UNREAD'
        """)
    long countUnreadByRecipientUserId(@Param("userId") UUID userId);

    /**
     * Finds a specific notification by ID, scoped to the recipient.
     * Returns empty if the notification belongs to a different user (NOTIF-AUTHZ-002).
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.id = :id
          AND n.recipientUserId = :userId
          AND n.deleted = false
        """)
    Optional<Notification> findByIdAndRecipientUserId(
            @Param("id") UUID id,
            @Param("userId") UUID userId);
}
