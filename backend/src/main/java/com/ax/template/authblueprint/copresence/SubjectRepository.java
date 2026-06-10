package com.ax.template.authblueprint.copresence;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    Optional<Subject> findBySubjectKey(String subjectKey);

    boolean existsBySubjectKey(String subjectKey);

    /** GATE-CONCURRENT-001 — lock the subject so the active-set evaluation is in-transaction and two
     *  concurrent introducing writes serialize (the second sees the first's just-admitted member). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subject s WHERE s.subjectKey = :subjectKey")
    Optional<Subject> findBySubjectKeyForUpdate(@Param("subjectKey") String subjectKey);

    /** The active member SET the gate intersects against — through-root member read (HG-AGG-REPO),
     *  executed under the subject lock. */
    @Query("SELECT m FROM SubjectMember m WHERE m.subjectId = :subjectId AND m.status = :status")
    List<SubjectMember> findMembers(@Param("subjectId") UUID subjectId, @Param("status") MemberStatus status);

    /** Paginated member view for the API — through-root. */
    @Query("SELECT m FROM SubjectMember m WHERE m.subjectId = :subjectId ORDER BY m.createdAt ASC")
    Page<SubjectMember> findMembersPage(
        @Param("subjectId") UUID subjectId, Pageable pageable);
}
