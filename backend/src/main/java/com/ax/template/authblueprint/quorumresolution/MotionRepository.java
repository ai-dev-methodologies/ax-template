package com.ax.template.authblueprint.quorumresolution;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MotionRepository extends JpaRepository<Motion, UUID> {

    /** QR-CONCURRENT-001 — both castBallot and resolve serialize on this PESSIMISTIC_WRITE row lock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Motion m WHERE m.id = :id")
    Optional<Motion> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (no member repositories) ──

    @Query("SELECT v FROM EligibleVoter v WHERE v.motionId = :motionId AND v.voterId = :voterId")
    Optional<EligibleVoter> findEligibleVoter(@Param("motionId") UUID motionId,
                                               @Param("voterId") String voterId);

    @Query("SELECT b FROM Ballot b WHERE b.motionId = :motionId ORDER BY b.castAt ASC")
    List<Ballot> findBallots(@Param("motionId") UUID motionId);

    @Query("SELECT b FROM Ballot b WHERE b.motionId = :motionId ORDER BY b.castAt ASC")
    Page<Ballot> findBallotsPage(@Param("motionId") UUID motionId, Pageable pageable);

    @Query("SELECT b FROM Ballot b WHERE b.motionId = :motionId AND b.voterId = :voterId")
    Optional<Ballot> findBallot(@Param("motionId") UUID motionId, @Param("voterId") String voterId);

    @Query("SELECT r FROM Resolution r WHERE r.motionId = :motionId")
    Optional<Resolution> findResolution(@Param("motionId") UUID motionId);
}
