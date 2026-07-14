package com.ax.template.authblueprint.tieredeligibility;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.ax.template.authblueprint.common.MemberWriter;

/**
 * tiered-eligibility-l0 sole orchestrator. Every accrual, use, AND restore acquires the ladder row under
 * PESSIMISTIC_WRITE (TIER-DERIVE-001) so all three write-paths serialize on one lock — a use racing a
 * crossing accrual always observes the post-crossing tier. {@link #accrue} can cross MULTIPLE tier
 * boundaries in one call (TIER-LADDER-001); {@link #restore} is the ONLY path that may decrease count, and
 * is always recorded with a reason in a ledger separate from accruals (TIER-MONOTONE-001). Tier is ALWAYS
 * {@link TierLadder#deriveTierIndex} of count — never an independently-set field.
 */
@Service
public class TierLadderService {

    private final TierLadderRepository ladders;
    private final TierLadderStateMachine stateMachine;
    private final MemberWriter members;
    private final TierMetrics metrics;
    private final Clock clock;

    public TierLadderService(TierLadderRepository ladders, TierLadderStateMachine stateMachine,
                             MemberWriter members, TierMetrics metrics, Clock clock) {
        this.ladders = ladders;
        this.stateMachine = stateMachine;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public TierLadder createLadder(String ladderKey, List<String> tierNames, List<Integer> thresholds,
                                   int initialCount) {
        if (tierNames == null || tierNames.size() < 2
                || thresholds == null || thresholds.size() != tierNames.size() - 1
                || initialCount < 0) {
            metrics.record("create", "invalid");
            throw TierException.invalidValue();
        }
        int prev = 0;
        for (int i = 0; i < thresholds.size(); i++) {
            Integer t = thresholds.get(i);
            if (t == null || t <= 0 || t <= prev) {
                metrics.record("create", "invalid");
                throw TierException.invalidValue();
            }
            prev = t;
        }
        List<TierDefinition> tiers = new ArrayList<>();
        tiers.add(new TierDefinition(tierNames.get(0), 0));
        for (int i = 1; i < tierNames.size(); i++) {
            tiers.add(new TierDefinition(tierNames.get(i), thresholds.get(i - 1)));
        }
        if (ladders.existsByLadderKey(ladderKey)) {
            metrics.record("create", "rejected");
            throw TierException.duplicateLadder();
        }
        try {
            TierLadder ladder = ladders.saveAndFlush(
                new TierLadder(UUID.randomUUID(), ladderKey, tiers, initialCount, Instant.now(clock)));
            metrics.record("create", "ok");
            return ladder;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw TierException.duplicateLadder();
        }
    }

    /** TIER-LADDER-001 / TIER-MONOTONE-001 — count strictly increases; MAY cross multiple boundaries. */
    @Transactional
    public TierLadder accrue(String ladderKey, int delta) {
        TierLadder ladder = ladders.findByLadderKeyForUpdate(ladderKey).orElseThrow(TierException::notFound);
        if (delta <= 0) {
            metrics.record("accrue", "invalid");
            throw TierException.invalidValue();
        }
        int newCount = ladder.getCount() + delta;
        int newTierIndex = ladder.deriveTierIndex(newCount);
        boolean crossed = newTierIndex > ladder.getCurrentTierIndex();
        stateMachine.degrade(ladder, newCount, newTierIndex);
        long seq = ladders.maxAccrualSequence(ladder.getId()) + 1;
        members.persist(new TierAccrual(UUID.randomUUID(), ladder.getId(), delta, newCount, newTierIndex,
            seq, Instant.now(clock)));
        metrics.record("accrue", crossed ? "crossed" : "ok");
        return ladder;
    }

    /** TIER-DERIVE-001 — fail-closed once at the worst tier; using is not accruing. */
    @Transactional
    public TierLadder use(String ladderKey) {
        TierLadder ladder = ladders.findByLadderKeyForUpdate(ladderKey).orElseThrow(TierException::notFound);
        if (ladder.isAtWorstTier()) {
            metrics.record("use", "suspended");
            throw TierException.suspended();
        }
        metrics.record("use", "ok");
        return ladder;
    }

    /** TIER-MONOTONE-001 — the ONLY path that may decrease count; always audited with a reason, in a
     *  ledger separate from accruals. */
    @Transactional
    public TierLadder restore(String ladderKey, int newCount, String reason) {
        TierLadder ladder = ladders.findByLadderKeyForUpdate(ladderKey).orElseThrow(TierException::notFound);
        if (reason == null || reason.isBlank() || newCount < 0 || newCount >= ladder.getCount()) {
            metrics.record("restore", "invalid");
            throw TierException.invalidValue();
        }
        int newTierIndex = ladder.deriveTierIndex(newCount);
        stateMachine.restore(ladder, newCount, newTierIndex);
        long seq = ladders.maxRestoreSequence(ladder.getId()) + 1;
        members.persist(new TierRestoreEvent(UUID.randomUUID(), ladder.getId(), newCount, newTierIndex,
            reason, seq, Instant.now(clock)));
        metrics.record("restore", "ok");
        return ladder;
    }

    @Transactional(readOnly = true)
    public TierLadder getLadder(String ladderKey) {
        return ladders.findByLadderKey(ladderKey).orElseThrow(TierException::notFound);
    }

    @Transactional(readOnly = true)
    public Page<TierAccrual> listAccruals(String ladderKey, int page, int size) {
        TierLadder ladder = getLadder(ladderKey);
        return ladders.findAccrualsPage(ladder.getId(), PageRequest.of(safePage(page), safeSize(size)));
    }

    @Transactional(readOnly = true)
    public Page<TierRestoreEvent> listRestores(String ladderKey, int page, int size) {
        TierLadder ladder = getLadder(ladderKey);
        return ladders.findRestoresPage(ladder.getId(), PageRequest.of(safePage(page), safeSize(size)));
    }

    private int safePage(int page) { return Math.max(page, 0); }

    private int safeSize(int size) { return Math.min(Math.max(size, 1), 200); }
}
