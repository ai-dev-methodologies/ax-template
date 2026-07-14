package com.ax.template.authblueprint.eventingest;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IngestStateRepository extends JpaRepository<IngestState, UUID> {

    Optional<IngestState> findByStreamAndSubjectId(String stream, String subjectId);

    /** The row is the serialization point for every apply — a webhook retry storm converges here. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM IngestState s WHERE s.stream = :stream AND s.subjectId = :subjectId")
    Optional<IngestState> findByStreamAndSubjectIdForUpdate(@Param("stream") String stream,
                                                            @Param("subjectId") String subjectId);

    // ── through-root member reads (HG-AGG-REPO — ProcessedEvent owns no repository) ──

    @Query("SELECT COUNT(p) > 0 FROM ProcessedEvent p WHERE p.stream = :stream AND p.eventId = :eventId")
    boolean existsProcessedEvent(@Param("stream") String stream, @Param("eventId") String eventId);

    @Query("SELECT COUNT(p) FROM ProcessedEvent p WHERE p.stream = :stream AND p.eventId = :eventId")
    long countProcessedEvent(@Param("stream") String stream, @Param("eventId") String eventId);
}
