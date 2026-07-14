package com.ax.template.authblueprint.duplicatesubmission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a submission is withdrawn/rejected, never removed. */
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /** DUPKEY-NATURAL-001 — the exact-match pre-check (the UNIQUE constraint is the race backstop). */
    @Query("SELECT s FROM Submission s WHERE s.channelId = :channelId AND s.activeKey = :naturalKey")
    Optional<Submission> findActiveByNaturalKey(@Param("channelId") UUID channelId, @Param("naturalKey") String naturalKey);

    /** DUPKEY-FUZZY-002 — other ACTIVE submissions on the same channel/subject/type within the fuzzy window. */
    @Query("SELECT s FROM Submission s WHERE s.channelId = :channelId AND s.status = 'ACTIVE'"
        + " AND s.subjectRef = :subjectRef AND s.lossType = :lossType"
        + " AND s.lossDate BETWEEN :from AND :to")
    List<Submission> findFuzzyCandidates(@Param("channelId") UUID channelId, @Param("subjectRef") String subjectRef,
                                         @Param("lossType") String lossType, @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);
}
