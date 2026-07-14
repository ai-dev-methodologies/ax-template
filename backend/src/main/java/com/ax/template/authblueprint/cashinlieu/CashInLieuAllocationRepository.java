package com.ax.template.authblueprint.cashinlieu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** NO delete method — an allocation, once made, is frozen for its (subject, event) pair (CIL-IDEMPOTENT-003). */
public interface CashInLieuAllocationRepository extends JpaRepository<CashInLieuAllocation, UUID> {

    Optional<CashInLieuAllocation> findBySubjectRefAndEventRef(String subjectRef, String eventRef);
}
