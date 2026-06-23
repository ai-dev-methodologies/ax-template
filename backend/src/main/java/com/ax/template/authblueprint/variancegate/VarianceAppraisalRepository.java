package com.ax.template.authblueprint.variancegate;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — an appraisal is a record, never removed. */
public interface VarianceAppraisalRepository extends JpaRepository<VarianceAppraisal, UUID> {

    /** VG-CONCURRENT-001 — the appraisal row serializes the check-not-disposed / write-disposition sequence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM VarianceAppraisal a WHERE a.id = :id")
    Optional<VarianceAppraisal> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member read (HG-AGG-REPO — VarianceDisposition owns no repository) ──

    @Query("SELECT d FROM VarianceDisposition d WHERE d.appraisalId = :appraisalId")
    Optional<VarianceDisposition> findDisposition(@Param("appraisalId") UUID appraisalId);
}
