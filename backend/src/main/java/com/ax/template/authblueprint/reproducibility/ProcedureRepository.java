package com.ax.template.authblueprint.reproducibility;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** NO delete method is declared anywhere in this domain — a procedure is recorded, never removed. */
public interface ProcedureRepository extends JpaRepository<Procedure, UUID> {

    /**
     * PROC-CLASS-001 — the (input_hash, classifier_version, kind) row, taken under PESSIMISTIC_WRITE
     * so two concurrent classify calls for the same input+version serialize: the first records, the
     * rest find the existing row (idempotent, byte-identical) rather than writing a divergent duplicate.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Procedure p WHERE p.inputHash = :inputHash"
        + " AND p.classifierVersion = :version AND p.kind = com.ax.template.authblueprint.reproducibility.ProcedureKind.CLASSIFICATION")
    Optional<Procedure> findClassificationForUpdate(@Param("inputHash") String inputHash,
                                                    @Param("version") String version);
}
