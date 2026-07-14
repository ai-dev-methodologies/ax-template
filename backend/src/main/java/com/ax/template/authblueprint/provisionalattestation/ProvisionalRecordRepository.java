package com.ax.template.authblueprint.provisionalattestation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** NO delete method is declared — a provisional/attested record is never removed. */
public interface ProvisionalRecordRepository extends JpaRepository<ProvisionalRecord, UUID> {

    /** PATT-DOWNSTREAM-004 — attested-only vs include-provisional query filter. */
    Page<ProvisionalRecord> findByStatus(ProvisionalRecordStatus status, Pageable pageable);
}
