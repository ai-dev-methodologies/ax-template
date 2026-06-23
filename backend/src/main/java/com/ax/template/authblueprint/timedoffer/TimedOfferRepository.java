package com.ax.template.authblueprint.timedoffer;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — the re-offer ladder is append-only. */
public interface TimedOfferRepository extends JpaRepository<TimedOffer, UUID> {

    /** TIMEDOFFER-CONCURRENT-001 — lock EVERY offer row for the subject so concurrent accepts across
     *  COMPETING offers for the same subject serialize on the subject (not just on one offer row).
     *  The first locker assigns; the rest, behind the lock, re-read the now-assigned subject and lose. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM TimedOffer o WHERE o.subjectId = :subjectId")
    List<TimedOffer> findBySubjectIdForUpdate(@Param("subjectId") String subjectId);

    /** Lock one offer row (decline / re-offer take the offer's own lock). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM TimedOffer o WHERE o.id = :id")
    Optional<TimedOffer> findByIdForUpdate(@Param("id") UUID id);

    /** TIMEDOFFER-LADDER-001 — the next strictly-monotonic attempt position for the subject. */
    long countBySubjectId(String subjectId);

    /** TIMEDOFFER-LIFECYCLE-001 — due OPEN offers for the timeout sweep. Bounded batch (Pageable). */
    @Query("SELECT o.id FROM TimedOffer o WHERE o.status = :open AND o.deadline <= :now ORDER BY o.deadline ASC")
    List<UUID> findDueOfferIds(@Param("open") OfferStatus open,
                               @Param("now") Instant now,
                               Pageable pageable);

    /** The append-only attempt ladder for a subject, in attempt order (read-side). Bounded (Pageable). */
    @Query("SELECT o FROM TimedOffer o WHERE o.subjectId = :subjectId ORDER BY o.attemptSeq ASC")
    List<TimedOffer> findLadderBySubjectId(@Param("subjectId") String subjectId, Pageable pageable);
}
