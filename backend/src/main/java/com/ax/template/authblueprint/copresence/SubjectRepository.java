package com.ax.template.authblueprint.copresence;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    Optional<Subject> findBySubjectKey(String subjectKey);

    boolean existsBySubjectKey(String subjectKey);

    /** GATE-CONCURRENT-001 — lock the subject so the active-set evaluation is in-transaction and two
     *  concurrent introducing writes serialize (the second sees the first's just-admitted member). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subject s WHERE s.subjectKey = :subjectKey")
    Optional<Subject> findBySubjectKeyForUpdate(@Param("subjectKey") String subjectKey);
}
