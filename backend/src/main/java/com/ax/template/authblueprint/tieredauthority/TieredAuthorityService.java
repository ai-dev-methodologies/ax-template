package com.ax.template.authblueprint.tieredauthority;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * amount-tiered-authority-l0 sole orchestrator (전결 규정). Config changes are append-only
 * NEW table versions (ATA-BOUNDARY-001 validates tiling at config time); decisions are an
 * append-only log whose rows snapshot the table version + band + decider level they were
 * evaluated against (ATA-SNAPSHOT-001), so a later reconfiguration never rewrites a past
 * decision's record. Members ({@link AuthorityTierBand}, {@link TieredDecisionRecord}) are
 * written via {@link MemberWriter}; reads live as JPQL on {@link AuthorityTierTableRepository}.
 */
@Service
public class TieredAuthorityService {

    public record BandInput(BigDecimal minAmount, BigDecimal maxAmount, int minDeciderLevel) {}

    private final AuthorityTierTableRepository tables;
    private final MemberWriter members;
    private final Clock clock;

    public TieredAuthorityService(AuthorityTierTableRepository tables, MemberWriter members, Clock clock) {
        this.tables = tables;
        this.members = members;
        this.clock = clock;
    }

    /** ATA-BOUNDARY-001 — validates tiling, then appends a NEW table version (never edits one). */
    @Transactional
    public AuthorityTierTable createTable(List<BandInput> bandInputs, String createdBy) {
        List<BandInput> sorted = validateTiling(bandInputs);

        int nextVersion = tables.findTopByOrderByTableVersionDesc()
            .map(t -> t.getTableVersion() + 1)
            .orElse(1);
        AuthorityTierTable table = tables.saveAndFlush(
            new AuthorityTierTable(UUID.randomUUID(), nextVersion, createdBy, Instant.now(clock)));

        int order = 0;
        for (BandInput b : sorted) {
            members.persist(new AuthorityTierBand(UUID.randomUUID(), table.getId(), order++,
                b.minAmount(), b.maxAmount(), b.minDeciderLevel()));
        }
        return table;
    }

    @Transactional(readOnly = true)
    public AuthorityTierTable currentTable() {
        return tables.findTopByOrderByTableVersionDesc().orElseThrow(TieredAuthorityException::tableNotFound);
    }

    @Transactional(readOnly = true)
    public List<AuthorityTierBand> currentBands() {
        return bandsFor(currentTable().getId());
    }

    @Transactional(readOnly = true)
    public List<AuthorityTierBand> bandsFor(UUID tableId) {
        return tables.findBands(tableId);
    }

    /**
     * ATA-TIER-001 — reject BEFORE persisting anything if the decider's level is below the
     * covering band's minimum (403, fail-closed — never auto-escalated). ATA-SNAPSHOT-001 —
     * the resulting record is an immutable snapshot of the table version + band + decider level
     * used, regardless of what the table looks like later.
     */
    @Transactional
    public TieredDecisionRecord decide(BigDecimal amount, int deciderLevel, String outcome, String actor) {
        AuthorityTierTable table = currentTable();
        List<AuthorityTierBand> bands = tables.findBands(table.getId());
        AuthorityTierBand band = bands.stream()
            .filter(b -> b.covers(amount))
            .findFirst()
            .orElseThrow(() -> TieredAuthorityException.noTierMatch(
                "no configured band covers amount=" + amount));

        if (deciderLevel < band.getMinDeciderLevel()) {
            throw TieredAuthorityException.insufficientAuthority(
                "decider level " + deciderLevel + " is below the required minimum "
                + band.getMinDeciderLevel() + " for amount=" + amount);
        }

        return members.persist(new TieredDecisionRecord(UUID.randomUUID(), table.getId(),
            table.getTableVersion(), amount, band.getMinAmount(), band.getMaxAmount(),
            band.getMinDeciderLevel(), deciderLevel, outcome, actor, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public TieredDecisionRecord decision(UUID id) {
        return tables.findDecision(id).orElseThrow(TieredAuthorityException::decisionNotFound);
    }

    /**
     * ATA-BOUNDARY-001 — sorts by minAmount and asserts the chain tiles exactly: each band's
     * minAmount equals the previous band's maxAmount, and only the LAST band may be open-ended
     * ({@code maxAmount == null}). Any gap, overlap, or a non-last open-ended band is 422.
     */
    private List<BandInput> validateTiling(List<BandInput> bandInputs) {
        if (bandInputs == null || bandInputs.isEmpty()) {
            throw TieredAuthorityException.boundaryInvalid("at least one band is required");
        }
        List<BandInput> sorted = new ArrayList<>(bandInputs);
        sorted.sort(Comparator.comparing(BandInput::minAmount));

        for (int i = 0; i < sorted.size(); i++) {
            BandInput b = sorted.get(i);
            if (b.maxAmount() != null && b.maxAmount().compareTo(b.minAmount()) <= 0) {
                throw TieredAuthorityException.boundaryInvalid(
                    "band maxAmount must be greater than minAmount: " + b);
            }
            if (i < sorted.size() - 1) {
                if (b.maxAmount() == null) {
                    throw TieredAuthorityException.boundaryInvalid(
                        "only the LAST band may be open-ended (null maxAmount): " + b);
                }
                BigDecimal nextMin = sorted.get(i + 1).minAmount();
                if (b.maxAmount().compareTo(nextMin) != 0) {
                    throw TieredAuthorityException.boundaryInvalid(
                        "bands must tile without gap or overlap: band ending at " + b.maxAmount()
                        + " must be followed by a band starting at the same amount, found " + nextMin);
                }
            }
        }
        return sorted;
    }
}
