package com.ax.template.authblueprint.governedrecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ChangeRecordRepository extends JpaRepository<ChangeRecord, UUID> {

    /** Append-only history of one datum's field, in causal (sequence) order. Paginated so the full
     *  trail is retrievable AND completeness is signalled (totalElements/hasNext) — never silently
     *  capped (a truncated audit trail would itself obscure recorded changes, the inverse of the spec). */
    Page<ChangeRecord> findByDatumIdOrderBySequenceNoAsc(UUID datumId, Pageable pageable);

    /** Current max sequence for ONE (datum, field) (0 if none) — next sequence is this + 1, allocated
     *  under the datum's row lock. Field-scoped to match the uq_governed_change_seq (datum_id, field_name,
     *  sequence_no) uniqueness so each field's chain is independently monotonic from 1. */
    @Query("SELECT COALESCE(MAX(c.sequenceNo), 0) FROM ChangeRecord c "
        + "WHERE c.datumId = :datumId AND c.fieldName = :fieldName")
    long maxSequence(@Param("datumId") UUID datumId, @Param("fieldName") String fieldName);
}
