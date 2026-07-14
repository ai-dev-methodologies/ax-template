package com.ax.template.authblueprint.correctionrefire;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a published version is append-only. */
public interface CorrectedRecordRepository extends JpaRepository<CorrectedRecord, UUID> {

    /** CRF-CHAIN-004 — the CURRENT version is ALWAYS derived on read (MAX version), never stored. */
    Optional<CorrectedRecord> findTopBySubjectRefOrderByVersionDesc(String subjectRef);

    Optional<CorrectedRecord> findBySubjectRefAndVersion(String subjectRef, int version);

    List<CorrectedRecord> findBySubjectRefOrderByVersionAsc(String subjectRef);

    // ── through-root member reads (HG-AGG-REPO — AckRecord owns no repository) ──

    @Query("SELECT a FROM AckRecord a WHERE a.recordId = :recordId")
    Optional<AckRecord> findAckByRecordId(@Param("recordId") UUID recordId);
}
