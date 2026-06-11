package com.ax.template.authblueprint.trueup;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * remeasurement-trueup-l0 sole orchestrator. Every write path takes the period row's
 * PESSIMISTIC_WRITE lock first (TUP-CONCURRENT-001); when a recompute also needs the true-up
 * target period, both rows are locked in ascending-id order (the deadlock guard the dispatch
 * and recordlinkage domains already taught the catalog). Supersession appends, runs version,
 * corrections post forward — nothing in this domain rewrites or deletes. TrueUpPosting rows
 * are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class TrueUpService {

    /** Reference estimator names, recorded verbatim on every estimated row (TUP-GRID-001). */
    static final String ESTIMATION_CARRY_FORWARD = "CARRY_FORWARD";
    static final String ESTIMATION_ZERO_FILL = "ZERO_FILL";

    private final SettlementPeriodRepository periods;
    private final MeterReadingRepository readings;
    private final SettlementRunRepository runs;
    private final MemberWriter members;
    private final TrueUpMetrics metrics;
    private final Clock clock;

    public TrueUpService(SettlementPeriodRepository periods, MeterReadingRepository readings,
                         SettlementRunRepository runs, MemberWriter members, TrueUpMetrics metrics,
                         Clock clock) {
        this.periods = periods;
        this.readings = readings;
        this.runs = runs;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public SettlementPeriod createPeriod(String subject, String label, int gridSlots) {
        return periods.save(new SettlementPeriod(UUID.randomUUID(), subject, label, gridSlots,
            Instant.now(clock)));
    }

    /** TUP-SUPERSEDE-001 — first reading lands at slot_version 1; a better one appends version+1. */
    @Transactional
    public MeterReading recordReading(UUID periodId, int slotIndex, BigDecimal value,
                                      ReadingSource source, String estimationMethod) {
        SettlementPeriod period = periods.findByIdForUpdate(periodId).orElseThrow(TrueUpException::notFound);
        requireNotSealed(period);
        if (slotIndex < 0 || slotIndex >= period.getGridSlots()) {
            metrics.record("reading", "rejected");
            throw TrueUpException.slotRange(period.getGridSlots());
        }
        boolean estimated = source == ReadingSource.ESTIMATED;
        if (estimated == (estimationMethod == null || estimationMethod.isBlank())) {
            metrics.record("reading", "rejected");
            throw TrueUpException.invalidMethod();
        }
        MeterReading prior = readings.findActive(periodId, slotIndex).orElse(null);
        if (prior != null && prior.getSource() == ReadingSource.ACTUAL && estimated) {
            metrics.record("reading", "rejected");
            throw TrueUpException.downgrade();                    // facts never degrade to estimates
        }
        MeterReading next = readings.save(new MeterReading(UUID.randomUUID(), periodId, slotIndex,
            prior == null ? 1 : prior.getSlotVersion() + 1, value, source,
            estimated ? estimationMethod : null, Instant.now(clock)));
        if (prior != null) {
            prior.supersededBy(next.getId());                     // pointer; value retained verbatim
        }
        metrics.record("reading", "ok");
        return next;
    }

    /** TUP-GRID-001 — gap-fill is EXPLICIT: ESTIMATED rows appended with the method recorded. */
    @Transactional
    public List<MeterReading> estimateMissing(UUID periodId) {
        SettlementPeriod period = periods.findByIdForUpdate(periodId).orElseThrow(TrueUpException::notFound);
        requireNotSealed(period);
        Map<Integer, MeterReading> active = activeBySlot(periodId);
        List<MeterReading> created = new ArrayList<>();
        BigDecimal carried = null;
        for (int slot = 0; slot < period.getGridSlots(); slot++) {
            MeterReading existing = active.get(slot);
            if (existing != null) {
                carried = existing.getReadingValue();
                continue;
            }
            String method = carried != null ? ESTIMATION_CARRY_FORWARD : ESTIMATION_ZERO_FILL;
            BigDecimal value = carried != null ? carried : BigDecimal.ZERO;
            created.add(readings.save(new MeterReading(UUID.randomUUID(), periodId, slot, 1,
                value, ReadingSource.ESTIMATED, method, Instant.now(clock))));
        }
        metrics.record("estimate", "ok");
        return created;
    }

    /**
     * TUP-RUNVERSION-001 + TUP-DELTA-001 — unchanged basis converges idempotently onto the
     * latest run; a changed basis appends version+1, and for a CLOSED period the NET delta
     * posts forward into the OPEN target in the same transaction.
     */
    @Transactional
    public SettlementRun recompute(UUID periodId, UUID targetPeriodId) {
        LockedPair locked = lockPeriods(periodId, targetPeriodId);
        SettlementPeriod period = locked.source();
        requireNotSealed(period);

        List<MeterReading> grid = new ArrayList<>(readings.findActiveByPeriod(periodId));
        List<Integer> missing = missingSlots(period, grid);
        if (!missing.isEmpty()) {
            metrics.record("recompute", "rejected");
            throw TrueUpException.gridIncomplete(missing);
        }
        String basisJson = basisJson(grid);
        String basisHash = sha256(basisJson);
        SettlementRun latest = runs.findTopByPeriodIdOrderByRunVersionDesc(periodId).orElse(null);
        if (latest != null && latest.getBasisHash().equals(basisHash)) {
            metrics.record("recompute", "idempotent");
            return latest;                                        // unchanged basis — same run
        }
        BigDecimal total = grid.stream().map(MeterReading::getReadingValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        SettlementRun next = runs.saveAndFlush(new SettlementRun(UUID.randomUUID(), periodId,
            latest == null ? 1 : latest.getRunVersion() + 1, basisJson, basisHash, total,
            Instant.now(clock)));

        if (period.getStatus() == PeriodStatus.CLOSED) {
            postTrueUp(period, locked.target(), latest, next);
            metrics.record("recompute", "trued_up");
        } else {
            metrics.record("recompute", "ok");
        }
        return next;
    }

    /** TUP-SEALED-001 — closing fixes the run-of-record. */
    @Transactional
    public SettlementPeriod close(UUID periodId) {
        SettlementPeriod period = periods.findByIdForUpdate(periodId).orElseThrow(TrueUpException::notFound);
        if (period.getStatus() != PeriodStatus.OPEN) {
            metrics.record("close", "rejected");
            throw TrueUpException.invalidState();
        }
        // unlocked run read is safe ONLY because every run append holds this period's row
        // lock (taken above) — a concurrent recompute is fully committed or fully rolled
        // back before this close() acquires the lock
        SettlementRun latest = runs.findTopByPeriodIdOrderByRunVersionDesc(periodId)
            .orElseThrow(TrueUpException::noRun);
        period.close(latest.getId());
        metrics.record("close", "ok");
        return period;
    }

    /** TUP-SEALED-001 — one-way; only a CLOSED period seals. */
    @Transactional
    public SettlementPeriod seal(UUID periodId) {
        SettlementPeriod period = periods.findByIdForUpdate(periodId).orElseThrow(TrueUpException::notFound);
        if (period.getStatus() != PeriodStatus.CLOSED) {
            metrics.record("seal", "rejected");
            throw TrueUpException.invalidState();
        }
        period.seal();
        metrics.record("seal", "ok");
        return period;
    }

    @Transactional(readOnly = true)
    public SettlementPeriod getPeriod(UUID id) {
        return periods.findById(id).orElseThrow(TrueUpException::notFound);
    }

    @Transactional(readOnly = true)
    public List<MeterReading> readingTrail(UUID periodId) {
        getPeriod(periodId);                                      // 404 before an empty list
        return readings.findByPeriodIdOrderBySlotIndexAscSlotVersionAsc(periodId);
    }

    @Transactional(readOnly = true)
    public List<SettlementRun> runsOf(UUID periodId) {
        getPeriod(periodId);
        return runs.findByPeriodIdOrderByRunVersionAsc(periodId);
    }

    @Transactional(readOnly = true)
    public List<TrueUpPosting> postingsFor(UUID sourcePeriodId) {
        getPeriod(sourcePeriodId);
        return runs.findPostingsBySource(sourcePeriodId);
    }

    // ── internals ───────────────────────────────────────────────────────────────────

    private record LockedPair(SettlementPeriod source, SettlementPeriod target) {}

    /**
     * Ascending-id lock order across the source and target period rows (deadlock guard).
     * Both rows are locked UP FRONT even though only the CLOSED branch writes the target:
     * the source's status is unknowable before its lock is held, and acquiring the target
     * lazily afterwards would break the ordering whenever target < source (circular wait).
     * Deliberate trade-off — a momentarily over-wide lock beats a deadlock.
     */
    private LockedPair lockPeriods(UUID periodId, UUID targetPeriodId) {
        if (targetPeriodId == null || targetPeriodId.equals(periodId)) {
            SettlementPeriod source = periods.findByIdForUpdate(periodId)
                .orElseThrow(TrueUpException::notFound);
            return new LockedPair(source, targetPeriodId == null ? null : source);
        }
        UUID firstId = periodId.compareTo(targetPeriodId) < 0 ? periodId : targetPeriodId;
        UUID secondId = periodId.compareTo(targetPeriodId) < 0 ? targetPeriodId : periodId;
        SettlementPeriod first = periods.findByIdForUpdate(firstId).orElseThrow(TrueUpException::notFound);
        SettlementPeriod second = periods.findByIdForUpdate(secondId).orElseThrow(TrueUpException::notFound);
        SettlementPeriod source = first.getId().equals(periodId) ? first : second;
        return new LockedPair(source, source == first ? second : first);
    }

    /** TUP-DELTA-001 — NET of the recalculated total vs everything previously settled. */
    private void postTrueUp(SettlementPeriod source, SettlementPeriod target,
                            SettlementRun previousLatest, SettlementRun next) {
        if (target == null) {
            throw TrueUpException.targetRequired();
        }
        if (target.getStatus() != PeriodStatus.OPEN) {
            throw TrueUpException.targetNotOpen();
        }
        SettlementRun runOfRecord = runs.findById(source.getRunOfRecordId())
            .orElseThrow(TrueUpException::notFound);
        BigDecimal settled = runOfRecord.getTotalValue()
            .add(runs.sumPostingsForSource(source.getId()));      // independent repo-SUM derivation
        BigDecimal delta = next.getTotalValue().subtract(settled);
        if (delta.signum() != 0) {
            members.persist(new TrueUpPosting(UUID.randomUUID(), next.getId(), source.getId(),
                target.getId(), previousLatest.getRunVersion(), next.getRunVersion(), delta,
                Instant.now(clock)));
        }
    }

    private Map<Integer, MeterReading> activeBySlot(UUID periodId) {
        Map<Integer, MeterReading> bySlot = new HashMap<>();
        for (MeterReading r : readings.findActiveByPeriod(periodId)) {
            bySlot.put(r.getSlotIndex(), r);
        }
        return bySlot;
    }

    private static List<Integer> missingSlots(SettlementPeriod period, List<MeterReading> grid) {
        Map<Integer, MeterReading> bySlot = new HashMap<>();
        grid.forEach(r -> bySlot.put(r.getSlotIndex(), r));
        List<Integer> missing = new ArrayList<>();
        for (int slot = 0; slot < period.getGridSlots(); slot++) {
            if (!bySlot.containsKey(slot)) {
                missing.add(slot);
            }
        }
        return missing;
    }

    /** The reproducibility trail: exact reading rows at slot versions, slot-ordered. */
    private static String basisJson(List<MeterReading> grid) {
        grid.sort(java.util.Comparator.comparingInt(MeterReading::getSlotIndex));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < grid.size(); i++) {
            MeterReading r = grid.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"slot\":").append(r.getSlotIndex())
              .append(",\"reading\":\"").append(r.getId())
              .append("\",\"slotVersion\":").append(r.getSlotVersion()).append('}');
        }
        return sb.append(']').toString();
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static void requireNotSealed(SettlementPeriod period) {
        if (period.getStatus() == PeriodStatus.SEALED) {
            throw TrueUpException.sealed();
        }
    }
}
