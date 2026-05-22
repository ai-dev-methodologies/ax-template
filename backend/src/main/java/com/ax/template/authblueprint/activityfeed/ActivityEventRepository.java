package com.ax.template.authblueprint.activityfeed;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    Optional<ActivityEvent> findByActorUserIdAndIdempotencyKey(String actorUserId, String idempotencyKey);

    /**
     * Feed query — events visible to the caller (actor or audience). Sorted by createdAt DESC
     * via the Pageable.
     */
    @Query(
        "SELECT DISTINCT e FROM ActivityEvent e " +
        "LEFT JOIN e.audienceUserIds a " +
        "WHERE e.actorUserId = :userId OR a = :userId"
    )
    Page<ActivityEvent> findVisibleTo(@Param("userId") String userId, Pageable pageable);

    /**
     * Unread-only feed — events visible to the caller AND NOT in the ActivityRead table for that user.
     */
    @Query(
        "SELECT DISTINCT e FROM ActivityEvent e " +
        "LEFT JOIN e.audienceUserIds a " +
        "WHERE (e.actorUserId = :userId OR a = :userId) " +
        "AND NOT EXISTS (" +
        "  SELECT 1 FROM ActivityRead r " +
        "  WHERE r.eventId = e.id AND r.userId = :userId" +
        ")"
    )
    Page<ActivityEvent> findUnreadVisibleTo(@Param("userId") String userId, Pageable pageable);

    /** Single event with visibility check. */
    @Query(
        "SELECT DISTINCT e FROM ActivityEvent e " +
        "LEFT JOIN e.audienceUserIds a " +
        "WHERE e.id = :id AND (e.actorUserId = :userId OR a = :userId)"
    )
    Optional<ActivityEvent> findVisibleSingle(@Param("id") UUID id, @Param("userId") String userId);
}
