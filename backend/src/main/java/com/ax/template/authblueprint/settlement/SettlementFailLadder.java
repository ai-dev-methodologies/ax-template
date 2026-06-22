package com.ax.template.authblueprint.settlement;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * settlement-finality-l0 fail-ladder state machine (SETTLE-LADDER-001). The SOLE mutator of an
 * instruction's status along the FAIL path — an illegal edge throws → 409 (HG-STATE-SOLE-MUTATOR).
 *
 * <p>The ladder is {@code PENDING → FAILED → RETRY → BUYIN}, each edge taken AT MOST ONCE
 * (exactly-once transitions): a failed settlement that has already reached RETRY cannot be
 * "failed" back to FAILED, and BUYIN is terminal-failed (zero outgoing edges). Settlement to
 * finality (→ SETTLED) is NOT a ladder edge — it is the DvP commit in {@link SettlementService},
 * legal from any non-final, non-terminal state (PENDING/FAILED/RETRY). After finality the
 * instruction is irrevocable, so this machine refuses every edge out of SETTLED.
 */
@Component
public class SettlementFailLadder {

    private static final Map<SettlementStatus, Set<SettlementStatus>> ALLOWED =
        new EnumMap<>(SettlementStatus.class);

    static {
        ALLOWED.put(SettlementStatus.PENDING, Set.of(SettlementStatus.FAILED));
        ALLOWED.put(SettlementStatus.FAILED, Set.of(SettlementStatus.RETRY));
        ALLOWED.put(SettlementStatus.RETRY, Set.of(SettlementStatus.BUYIN));
        ALLOWED.put(SettlementStatus.BUYIN, Set.of());      // terminal-failed
        ALLOWED.put(SettlementStatus.SETTLED, Set.of());    // irrevocable — never leaves finality
    }

    /**
     * Walk the instruction one rung down the fail ladder. The state machine is the only caller
     * permitted to move {@code status} along the fail path (it invokes the package-private
     * {@link SettlementInstruction#moveStatus}). An edge not in the graph is a 409.
     */
    public void advance(SettlementInstruction instruction, SettlementStatus next) {
        SettlementStatus from = instruction.getStatus();
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(next)) {
            throw SettlementException.illegalLadderEdge(from, next);
        }
        instruction.moveStatus(next);
    }
}
