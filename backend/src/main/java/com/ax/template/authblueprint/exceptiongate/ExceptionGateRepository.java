package com.ax.template.authblueprint.exceptiongate;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExceptionGateRepository extends JpaRepository<ExceptionGate, UUID> {

    Optional<ExceptionGate> findBySubjectTypeAndSubjectId(String subjectType, String subjectId);

    /** The row is the serialization point for raise/lift/advance (EXC-DIM-INDEPENDENT-001). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM ExceptionGate g WHERE g.subjectType = :subjectType AND g.subjectId = :subjectId")
    Optional<ExceptionGate> findBySubjectTypeAndSubjectIdForUpdate(@Param("subjectType") String subjectType,
                                                                   @Param("subjectId") String subjectId);

    // ── through-root member reads (HG-AGG-REPO — ExceptionAuditEntry owns no repository) ──

    @Query("SELECT a FROM ExceptionAuditEntry a WHERE a.gateId = :gateId ORDER BY a.occurredAt ASC")
    List<ExceptionAuditEntry> findAuditByGateId(@Param("gateId") UUID gateId);
}
