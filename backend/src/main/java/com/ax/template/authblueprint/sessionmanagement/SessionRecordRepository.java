package com.ax.template.authblueprint.sessionmanagement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRecordRepository extends JpaRepository<SessionRecord, UUID> {

    Optional<SessionRecord> findByUserIdAndJti(String userId, String jti);

    Optional<SessionRecord> findByJti(String jti);

    Optional<SessionRecord> findByIdAndUserId(UUID id, String userId);

    List<SessionRecord> findByUserIdOrderByCreatedAtDesc(String userId);

    @Modifying
    @Query("UPDATE SessionRecord s " +
           "SET s.status = com.ax.template.authblueprint.sessionmanagement.SessionStatus.REVOKED, " +
           "    s.revokedAt = :now, " +
           "    s.revokedByUserId = :actor " +
           "WHERE s.userId = :userId " +
           "AND s.id <> :keepId " +
           "AND s.status = com.ax.template.authblueprint.sessionmanagement.SessionStatus.ACTIVE")
    int revokeOthers(@Param("userId") String userId,
                     @Param("keepId") UUID keepId,
                     @Param("now") Instant now,
                     @Param("actor") String actorUserId);
}
