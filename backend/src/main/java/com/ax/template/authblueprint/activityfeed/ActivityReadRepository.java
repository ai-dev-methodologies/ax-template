package com.ax.template.authblueprint.activityfeed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivityReadRepository extends JpaRepository<ActivityRead, UUID> {

    Optional<ActivityRead> findByEventIdAndUserId(UUID eventId, String userId);

    /**
     * Returns the count of ActivityRead rows that would be created if mark-all-read ran
     * for the caller — i.e., events visible to the caller minus events already read.
     */
    @Query(
        "SELECT e.id FROM ActivityEvent e " +
        "LEFT JOIN e.audienceUserIds a " +
        "WHERE (e.actorUserId = :userId OR a = :userId) " +
        "AND NOT EXISTS (" +
        "  SELECT 1 FROM ActivityRead r WHERE r.eventId = e.id AND r.userId = :userId" +
        ")"
    )
    java.util.List<UUID> findUnreadEventIdsForUser(@Param("userId") String userId);
}
