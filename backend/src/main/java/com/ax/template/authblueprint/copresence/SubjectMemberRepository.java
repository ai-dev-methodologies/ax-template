package com.ax.template.authblueprint.copresence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectMemberRepository extends JpaRepository<SubjectMember, UUID> {

    /** The active member SET the gate intersects the candidate against (read under the subject lock). */
    List<SubjectMember> findBySubjectIdAndStatus(UUID subjectId, MemberStatus status);

    /** Paginated view for the API. */
    Page<SubjectMember> findBySubjectIdOrderByCreatedAtAsc(UUID subjectId, Pageable pageable);
}
