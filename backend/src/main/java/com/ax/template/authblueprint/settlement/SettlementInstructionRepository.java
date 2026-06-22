package com.ax.template.authblueprint.settlement;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NO delete method is declared anywhere in this domain — a settled instruction is final and a
 * failed one walks the ladder; neither is ever removed.
 */
public interface SettlementInstructionRepository extends JpaRepository<SettlementInstruction, UUID> {

    /**
     * SETTLE-CONCURRENT-001 — the row lock makes the settle-once finality deterministic: two
     * concurrent settle attempts serialize, exactly one commits the DvP (CWE-362).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SettlementInstruction s WHERE s.id = :id")
    Optional<SettlementInstruction> findByIdForUpdate(@Param("id") UUID id);

    // ── through-root member reads (HG-AGG-REPO — NovationRecord owns no repository) ──

    @Query("SELECT n FROM NovationRecord n WHERE n.instructionId = :instructionId ORDER BY n.novatedAt ASC")
    List<NovationRecord> findNovations(@Param("instructionId") UUID instructionId);
}
