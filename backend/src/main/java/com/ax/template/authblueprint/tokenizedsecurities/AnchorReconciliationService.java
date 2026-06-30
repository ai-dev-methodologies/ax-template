package com.ax.template.authblueprint.tokenizedsecurities;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compares the off-chain register against the OnChainAnchor view (ANCHOR-002).
 * The pure static reconcile() method is independently testable without Spring context.
 */
@Service
public class AnchorReconciliationService {

    private final SecurityTokenRegisterRepository registers;
    private final OnChainAnchor onChainAnchor;

    public AnchorReconciliationService(SecurityTokenRegisterRepository registers,
                                       OnChainAnchor onChainAnchor) {
        this.registers = registers;
        this.onChainAnchor = onChainAnchor;
    }

    @Transactional(readOnly = true)
    public ReconcileResult reconcile(String tokenCode) {
        SecurityTokenRegister register = registers.findByTokenCode(tokenCode)
                .orElseThrow(TokenizedSecuritiesException::notFound);
        List<AnchorRecord> anchorRecords = onChainAnchor.recordsFor(tokenCode);
        return reconcile(register.getEntries(), anchorRecords);
    }

    /**
     * Pure comparator — no I/O. Matches by transferId.
     * A break = entry without matching anchor record, anchor record without matching entry,
     * or units mismatch. converged = breaks.isEmpty().
     */
    public static ReconcileResult reconcile(List<TransferEntry> entries,
                                            List<AnchorRecord> anchorRecords) {
        Map<String, AnchorRecord> anchorByTransferId = anchorRecords.stream()
                .collect(Collectors.toMap(AnchorRecord::transferId, r -> r));
        Map<String, TransferEntry> entryByTransferId = entries.stream()
                .collect(Collectors.toMap(TransferEntry::getTransferId, e -> e));

        List<String> breaks = new ArrayList<>();

        // Entry-side: no matching anchor record, or units mismatch
        for (TransferEntry entry : entries) {
            AnchorRecord anchor = anchorByTransferId.get(entry.getTransferId());
            if (anchor == null || anchor.units() != entry.getUnits()) {
                breaks.add(entry.getTransferId());
            }
        }

        // Anchor-side: anchor record has no matching entry
        for (AnchorRecord anchor : anchorRecords) {
            if (!entryByTransferId.containsKey(anchor.transferId())) {
                breaks.add(anchor.transferId());
            }
        }

        return new ReconcileResult(breaks.isEmpty(), List.copyOf(breaks));
    }
}
