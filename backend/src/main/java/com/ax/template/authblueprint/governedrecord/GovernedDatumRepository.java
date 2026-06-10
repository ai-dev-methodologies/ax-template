package com.ax.template.authblueprint.governedrecord;

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

public interface GovernedDatumRepository extends JpaRepository<GovernedDatum, UUID> {

    boolean existsByName(String name);

    /** ACR-APPEND-ONLY-001 — lock the datum so the per-field change sequence is allocated serially
     *  and the oldValue read is exactly the pre-edit value (ACR-PREIMAGE-001). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM GovernedDatum d WHERE d.id = :id")
    Optional<GovernedDatum> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — ChangeRecord owns no repository) ──

    /** Append-only history of one datum's field, causal order; paginated so completeness is
     *  signalled (totalElements/hasNext) — never silently capped. */
    @Query("SELECT c FROM ChangeRecord c WHERE c.datumId = :datumId ORDER BY c.sequenceNo ASC")
    Page<ChangeRecord> findChangesPage(
        @Param("datumId") UUID datumId, Pageable pageable);

    /** Current max sequence for ONE (datum, field) (0 if none) — next is +1, under the datum's lock;
     *  field-scoped to match uq_governed_change_seq so each field's chain is independently monotonic. */
    @Query("SELECT COALESCE(MAX(c.sequenceNo), 0) FROM ChangeRecord c "
        + "WHERE c.datumId = :datumId AND c.fieldName = :fieldName")
    long maxSequence(@Param("datumId") UUID datumId, @Param("fieldName") String fieldName);
}