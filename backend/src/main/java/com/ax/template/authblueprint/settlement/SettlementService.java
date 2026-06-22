package com.ax.template.authblueprint.settlement;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * settlement-finality-l0 sole orchestrator. All repository access, the DvP atomic commit, the
 * finality irrevocability gate, the obligation-conserving novation, and the exactly-once fail
 * ladder live here. Every write-path takes the instruction's PESSIMISTIC_WRITE row lock
 * (SETTLE-CONCURRENT-001 / CWE-362) so concurrent settles serialize and exactly one reaches
 * finality. NovationRecord rows are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class SettlementService {

    private final SettlementInstructionRepository instructions;
    private final MemberWriter members;
    private final SettlementFailLadder ladder;
    private final SettlementMetrics metrics;
    private final Clock clock;

    public SettlementService(SettlementInstructionRepository instructions, MemberWriter members,
                             SettlementFailLadder ladder, SettlementMetrics metrics, Clock clock) {
        this.instructions = instructions;
        this.members = members;
        this.ladder = ladder;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public SettlementInstruction createInstruction(String tradeRef, String deliveryParty,
                                                   String paymentParty, BigDecimal netObligation) {
        return instructions.save(new SettlementInstruction(UUID.randomUUID(), tradeRef,
            deliveryParty, paymentParty, netObligation, Instant.now(clock)));
    }

    /**
     * SETTLE-DVP-001 + SETTLE-FINAL-001 — commit BOTH legs ATOMICALLY and reach irrevocable
     * finality. Refused if already final (idempotent-safe 409) or terminal-failed (BUYIN). The
     * row lock makes this the settle-once keystone: of N concurrent settles, exactly one finds
     * the instruction non-final and commits the DvP; the rest see SETTLED → 409.
     */
    @Transactional
    public SettlementInstruction settle(UUID id) {
        SettlementInstruction s = instructions.findByIdForUpdate(id).orElseThrow(SettlementException::notFound);
        if (s.isFinal()) {
            metrics.record("settle", "final_blocked");
            throw SettlementException.alreadyFinal();
        }
        if (s.getStatus() == SettlementStatus.BUYIN) {
            metrics.record("settle", "rejected");
            throw SettlementException.notSettleable(s.getStatus());
        }
        s.settleBothLegs(Instant.now(clock));          // DvP: both legs or neither — atomic
        metrics.record("settle", "ok");
        return s;
    }

    /**
     * SETTLE-NOVATE-001 — replace ONE leg's counterparty before finality, CONSERVING the net
     * obligation (the released party is discharged from, and the assuming party takes on, the
     * identical amount). After finality this is refused (409). The novation is recorded
     * append-only; the original instruction is retained.
     */
    @Transactional
    public SettlementInstruction novate(UUID id, SettlementLeg leg, String assumingParty, String novatedBy) {
        SettlementInstruction s = instructions.findByIdForUpdate(id).orElseThrow(SettlementException::notFound);
        if (s.isFinal()) {
            metrics.record("novate", "final_blocked");
            throw SettlementException.alreadyFinal();   // irrevocable — counterparty is locked in
        }
        String released = leg == SettlementLeg.DELIVERY ? s.getDeliveryParty() : s.getPaymentParty();
        if (Objects.equals(released, assumingParty)) {
            metrics.record("novate", "invalid");
            throw SettlementException.novationNoChange();
        }
        // CONSERVATION: the new party assumes the SAME net obligation the old party was released
        // from. netObligation is @Column(updatable=false) on the instruction, so it cannot drift.
        BigDecimal conserved = s.getNetObligation();
        members.persist(new NovationRecord(UUID.randomUUID(), s.getId(), leg, released, assumingParty,
            conserved, novatedBy, Instant.now(clock)));
        if (leg == SettlementLeg.DELIVERY) {
            s.replaceDeliveryParty(assumingParty);
        } else {
            s.replacePaymentParty(assumingParty);
        }
        metrics.record("novate", "ok");
        return s;
    }

    /** SETTLE-LADDER-001 — PENDING → FAILED (exactly once). Refused after finality. */
    @Transactional
    public SettlementInstruction fail(UUID id) {
        return ladderStep(id, SettlementStatus.FAILED, "fail");
    }

    /** SETTLE-LADDER-001 — FAILED → RETRY (exactly once). */
    @Transactional
    public SettlementInstruction retry(UUID id) {
        return ladderStep(id, SettlementStatus.RETRY, "retry");
    }

    /** SETTLE-LADDER-001 — RETRY → BUYIN (terminal-failed; exactly once). */
    @Transactional
    public SettlementInstruction buyin(UUID id) {
        return ladderStep(id, SettlementStatus.BUYIN, "buyin");
    }

    private SettlementInstruction ladderStep(UUID id, SettlementStatus next, String op) {
        SettlementInstruction s = instructions.findByIdForUpdate(id).orElseThrow(SettlementException::notFound);
        if (s.isFinal()) {
            metrics.record(op, "final_blocked");
            throw SettlementException.alreadyFinal();   // a final settlement never fails
        }
        ladder.advance(s, next);                        // sole status mutator on the fail path
        metrics.record(op, "ok");
        return s;
    }

    @Transactional(readOnly = true)
    public SettlementInstruction get(UUID id) {
        return instructions.findById(id).orElseThrow(SettlementException::notFound);
    }

    @Transactional(readOnly = true)
    public List<NovationRecord> novations(UUID id) {
        get(id);                                        // 404 before an empty list
        return instructions.findNovations(id);
    }
}
