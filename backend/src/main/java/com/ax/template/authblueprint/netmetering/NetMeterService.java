package com.ax.template.authblueprint.netmetering;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.MemberWriter;

/**
 * signed-dual-register-l0 sole orchestrator. Every append acquires the METER row under PESSIMISTIC_WRITE
 * (NETM-CONCURRENT-001) so both directions serialize and no direction delta is computed against a stale
 * cumulative. A reading is compared ONLY to its own direction cumulative (NETM-DIRECTION-001, delta =
 * read − cumulative, ≥ 0); the signed net is DERIVED = cumulativeImport − cumulativeExport (re-derived on
 * advance, never stored as a settable field) and CROSS-CHECKED against an independent Σimport − Σexport
 * recompute (NETM-NET-001); a closed billing period is frozen — a backdated reading or a non-forward
 * re-close is a 409 (NETM-PERIOD-001). The cumulatives/net/boundary move ONLY here (no public setter).
 */
@Service
public class NetMeterService {

    static final int MEASURE_SCALE = 4;

    private final NetMeterRepository meters;
    private final MemberWriter members;
    private final NetMeterMetrics metrics;
    private final Clock clock;

    public NetMeterService(NetMeterRepository meters, MemberWriter members,
                           NetMeterMetrics metrics, Clock clock) {
        this.meters = meters;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public NetMeter createMeter(String meterKey, BigDecimal initialImport, BigDecimal initialExport) {
        return createMeter(meterKey, initialImport, initialExport, null, null);
    }

    /** NETM-RATE-001 — rates are injected via policy at creation, never hardcoded; omitted rates default
     *  to 1 (the symmetric-rate case degenerates to the plain net delta). */
    @Transactional
    public NetMeter createMeter(String meterKey, BigDecimal initialImport, BigDecimal initialExport,
                                BigDecimal rateImport, BigDecimal rateExport) {
        BigDecimal imp = requireNonNegative(initialImport, "create");
        BigDecimal exp = requireNonNegative(initialExport, "create");
        BigDecimal rImp = requirePositiveRate(rateImport);
        BigDecimal rExp = requirePositiveRate(rateExport);
        if (meters.existsByMeterKey(meterKey)) {
            metrics.record("create", "rejected");
            throw NetMeterException.duplicateMeter();
        }
        try {
            // closedThroughAt = Instant.MIN so the first reading at any real instant is forward of it.
            NetMeter m = meters.saveAndFlush(new NetMeter(UUID.randomUUID(), meterKey, imp, exp, rImp, rExp,
                Instant.MIN, Instant.now(clock)));
            metrics.record("create", "ok");
            return m;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw NetMeterException.duplicateMeter();
        }
    }

    /** NETM-DIRECTION/NET/CONCURRENT-001 — append one direction reading under the meter row lock. */
    @Transactional
    public NetMeterReading append(String meterKey, MeterDirection direction, BigDecimal readingValue,
                                  Instant effectiveAt) {
        NetMeter m = meters.findByMeterKeyForUpdate(meterKey).orElseThrow(NetMeterException::notFound);
        Instant effective = effectiveAt == null ? Instant.now(clock) : effectiveAt;
        if (effective.compareTo(m.getClosedThroughAt()) <= 0) {        // NETM-PERIOD-001 — backdate into closed period
            metrics.record(direction.name(), "period_closed");
            throw NetMeterException.periodClosed();
        }
        BigDecimal read = requireNonNegative(readingValue, direction.name());
        BigDecimal cumulative = m.cumulativeFor(direction);
        if (read.compareTo(cumulative) < 0) {                          // NETM-DIRECTION-001 — value-monotone per direction
            metrics.record(direction.name(), "not_monotone");
            throw NetMeterException.notMonotone();
        }
        BigDecimal delta = read.subtract(cumulative);                  // delta ≥ 0
        long seq = meters.maxSequence(m.getId(), direction) + 1;
        m.advance(direction, read);                                    // direction cumulative := read; net re-derived

        // NETM-NET-001 — cross-check the derived net against an INDEPENDENT recompute (never by-construction):
        // baselineNet + Σ(prior committed import deltas) − Σ(prior committed export deltas) + (this reading's
        // signed contribution). Readings capture deltas FROM the immutable creation baseline, so the recompute
        // must add baselineNet (a meter may open at a non-zero cumulative).
        BigDecimal signed = direction == MeterDirection.IMPORT ? delta : delta.negate();
        BigDecimal recomputed = m.getBaselineNet().add(meters.sumImportDelta(m.getId()))
            .subtract(meters.sumExportDelta(m.getId())).add(signed).setScale(MEASURE_SCALE);
        if (m.getNet().compareTo(recomputed) != 0) {                   // a divergence is a defect — never bill it
            metrics.record(direction.name(), "net_mismatch");
            throw NetMeterException.netMismatch();
        }

        NetMeterReading row = members.persist(new NetMeterReading(UUID.randomUUID(), m.getId(), direction,
            read, cumulative, delta.setScale(MEASURE_SCALE), m.getNet(), m.getCumulativeImport(),
            m.getCumulativeExport(), seq, effective, Instant.now(clock)));
        metrics.record(direction.name(), "ok");
        return row;
    }

    /** NETM-PERIOD-001 / NETM-RATE-001 — close a billing period: snapshot both cumulatives + the net delta +
     *  the rate-asymmetric billed amount (cross-checked against an independent chain recompute); freeze
     *  the period. */
    @Transactional
    public NetMeterPeriod closePeriod(String meterKey, Instant boundaryAt) {
        NetMeter m = meters.findByMeterKeyForUpdate(meterKey).orElseThrow(NetMeterException::notFound);
        Instant priorBoundary = m.getClosedThroughAt();
        if (boundaryAt == null || boundaryAt.compareTo(priorBoundary) <= 0) {  // strictly-forward boundary
            metrics.record("close", "period_closed");
            throw NetMeterException.periodClosed();
        }
        BigDecimal netStart = m.getNetAtPeriodStart();
        BigDecimal netEnd = m.getNet();
        BigDecimal periodNetDelta = netEnd.subtract(netStart).setScale(MEASURE_SCALE);

        // NETM-RATE-001 — the SAME per-direction cumulatives that already satisfy NETM-DIRECTION-001 conservation.
        BigDecimal importDelta = m.getCumulativeImport().subtract(m.getImportCumulativeAtPeriodStart())
            .setScale(MEASURE_SCALE);
        BigDecimal exportDelta = m.getCumulativeExport().subtract(m.getExportCumulativeAtPeriodStart())
            .setScale(MEASURE_SCALE);
        // independent recompute from the immutable reading chain — never trusted by-construction.
        BigDecimal importDeltaRecomputed = meters.sumImportDeltaInRange(m.getId(), priorBoundary, boundaryAt)
            .setScale(MEASURE_SCALE);
        BigDecimal exportDeltaRecomputed = meters.sumExportDeltaInRange(m.getId(), priorBoundary, boundaryAt)
            .setScale(MEASURE_SCALE);
        if (importDelta.compareTo(importDeltaRecomputed) != 0 || exportDelta.compareTo(exportDeltaRecomputed) != 0) {
            metrics.record("close", "rate_mismatch");
            throw NetMeterException.rateMismatch();
        }
        BigDecimal billedAmount = importDelta.multiply(m.getRateImport())
            .subtract(exportDelta.multiply(m.getRateExport()))
            .setScale(MEASURE_SCALE, RoundingMode.HALF_UP);

        long seq = meters.maxPeriodSequence(m.getId()) + 1;
        NetMeterPeriod period = members.persist(new NetMeterPeriod(UUID.randomUUID(), m.getId(), boundaryAt,
            m.getCumulativeImport(), m.getCumulativeExport(), netStart, netEnd, periodNetDelta,
            importDelta, exportDelta, m.getRateImport(), m.getRateExport(), billedAmount, seq,
            Instant.now(clock)));
        m.closePeriod(boundaryAt);                                     // move the boundary + period-start baselines forward
        metrics.record("close", "ok");
        return period;
    }

    @Transactional(readOnly = true)
    public NetMeter getMeter(String meterKey) {
        return meters.findByMeterKey(meterKey).orElseThrow(NetMeterException::notFound);
    }

    @Transactional(readOnly = true)
    public Page<NetMeterReading> listReadings(String meterKey, int page, int size) {
        NetMeter m = meters.findByMeterKey(meterKey).orElseThrow(NetMeterException::notFound);
        return meters.findReadingsPage(m.getId(), PageRequest.of(safePage(page), safeSize(size)));
    }

    @Transactional(readOnly = true)
    public Page<NetMeterPeriod> listPeriods(String meterKey, int page, int size) {
        NetMeter m = meters.findByMeterKey(meterKey).orElseThrow(NetMeterException::notFound);
        return meters.findPeriodsPage(m.getId(), PageRequest.of(safePage(page), safeSize(size)));
    }

    /** NETM-RATE-001 — a null rate defaults to 1 (symmetric); a provided rate must be strictly positive. */
    private BigDecimal requirePositiveRate(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ONE.setScale(MEASURE_SCALE);
        }
        if (rate.signum() <= 0) {
            metrics.record("create", "invalid");
            throw NetMeterException.invalidRate();
        }
        return rate.setScale(MEASURE_SCALE);
    }

    private BigDecimal requireNonNegative(BigDecimal v, String op) {
        if (v == null || v.signum() < 0) {
            metrics.record(op, "invalid");
            throw NetMeterException.invalidReading();
        }
        return v.setScale(MEASURE_SCALE);
    }

    private int safePage(int page) { return Math.max(page, 0); }

    private int safeSize(int size) { return Math.min(Math.max(size, 1), 200); }
}
