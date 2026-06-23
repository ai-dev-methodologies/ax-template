package com.ax.template.authblueprint.timedoffer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** NO delete method — once a subject is assigned it stays assigned (TIMEDOFFER-EXCLUSIVE-001). */
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    /** TIMEDOFFER-EXCLUSIVE-001 — the (at most one) assignment for a subject, if any. */
    Optional<Assignment> findBySubjectId(String subjectId);
}
