package com.ax.template.authblueprint.netmetering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * signed-dual-register-l0 net meter (bidirectional revenue meter). It holds TWO independently
 * value-monotone direction registers — {@code cumulativeImport} (+) and {@code cumulativeExport} (−) —
 * and a DERIVED signed {@code net = cumulativeImport − cumulativeExport}. The net is NOT an independent
 * settable field: it is re-derived from the two cumulatives on every {@link #advance}. {@code closedThroughAt}
 * is the latest closed billing-period boundary (a reading effective at/before it is a 409 backdate);
 * {@code netAtPeriodStart} is the net at the start of the currently-open period (the period close uses
 * {@code net − netAtPeriodStart} as the period net delta). {@code meterKey}/{@code createdAt} are immutable;
 * the cumulatives and net move ONLY via the package-private {@link #advance}, and the period boundary ONLY
 * via {@link #closePeriod} (no public setter — {@link NetMeterService} is the sole mutator, always under a
 * PESSIMISTIC_WRITE row lock). {@code @Version} backstops.
 *
 * <p>NETM-RATE-001 extension: {@code rateImport}/{@code rateExport} are the per-unit billing rates
 * (immutable once set at creation — injected via policy, never hardcoded); {@code
 * importCumulativeAtPeriodStart}/{@code exportCumulativeAtPeriodStart} are the per-direction cumulatives
 * snapshotted at the start of the currently-open period (mirrors {@code netAtPeriodStart}) so a period
 * close can derive its per-direction delta from the SAME registers that already satisfy NETM-DIRECTION-001.
 */
@AggregateRoot
@Entity
@Table(name = "net_meters")
// NETM-DIRECTION-001 / NETM-RATE-001 — direction cumulatives non-negative; rates strictly positive. LIVE under ddl-auto.
@Check(constraints = "cumulative_import >= 0 AND cumulative_export >= 0 AND rate_import > 0 AND rate_export > 0")
public class NetMeter {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "meter_key", nullable = false, updatable = false, length = 200, unique = true)
    private String meterKey;

    /** IMPORT register — energy drawn from the grid (+). Monotone; advances only on an IMPORT read. */
    @Column(name = "cumulative_import", nullable = false, precision = 19, scale = 4)
    private BigDecimal cumulativeImport;

    /** EXPORT register — energy fed back to the grid (−). Monotone; advances only on an EXPORT read. */
    @Column(name = "cumulative_export", nullable = false, precision = 19, scale = 4)
    private BigDecimal cumulativeExport;

    /** DERIVED net = cumulativeImport − cumulativeExport. Re-derived on every advance; never set directly. */
    @Column(name = "net_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal net;

    /** IMMUTABLE net at creation (initialImport − initialExport). The independent net recompute is
     *  baselineNet + Σimport-deltas − Σexport-deltas — readings only capture deltas FROM this baseline,
     *  so the cross-check must add the baseline (a meter may open at a non-zero cumulative). */
    @Column(name = "baseline_net", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal baselineNet;

    /** Net at the start of the currently-open period — the period close delta is net − netAtPeriodStart. */
    @Column(name = "net_at_period_start", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAtPeriodStart;

    /** NETM-RATE-001 — per-unit billing rate for IMPORT. Immutable once set (injected via policy at creation). */
    @Column(name = "rate_import", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal rateImport;

    /** NETM-RATE-001 — per-unit billing rate for EXPORT. Immutable once set (injected via policy at creation). */
    @Column(name = "rate_export", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal rateExport;

    /** NETM-RATE-001 — import cumulative at the start of the currently-open period (period-delta baseline). */
    @Column(name = "import_cumulative_at_period_start", nullable = false, precision = 19, scale = 4)
    private BigDecimal importCumulativeAtPeriodStart;

    /** NETM-RATE-001 — export cumulative at the start of the currently-open period (period-delta baseline). */
    @Column(name = "export_cumulative_at_period_start", nullable = false, precision = 19, scale = 4)
    private BigDecimal exportCumulativeAtPeriodStart;

    /** Latest closed period boundary; a reading effective at/before it is a 409 backdate. */
    @Column(name = "closed_through_at", nullable = false)
    private Instant closedThroughAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NetMeter() {}

    public NetMeter(UUID id, String meterKey, BigDecimal cumulativeImport, BigDecimal cumulativeExport,
                    BigDecimal rateImport, BigDecimal rateExport, Instant closedThroughAt, Instant createdAt) {
        this.id = id;
        this.meterKey = meterKey;
        this.cumulativeImport = cumulativeImport;
        this.cumulativeExport = cumulativeExport;
        this.net = cumulativeImport.subtract(cumulativeExport);   // DERIVED at construction
        this.baselineNet = this.net;                              // immutable recompute baseline
        this.netAtPeriodStart = this.net;
        this.rateImport = rateImport;
        this.rateExport = rateExport;
        this.importCumulativeAtPeriodStart = cumulativeImport;
        this.exportCumulativeAtPeriodStart = cumulativeExport;
        this.closedThroughAt = closedThroughAt;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — advance the given direction's cumulative to a newly-read value and RE-DERIVE the net. */
    void advance(MeterDirection direction, BigDecimal read) {
        if (direction == MeterDirection.IMPORT) {
            this.cumulativeImport = read;
        } else {
            this.cumulativeExport = read;
        }
        this.net = this.cumulativeImport.subtract(this.cumulativeExport);   // net is ALWAYS derived
    }

    /** Sole-mutator hook — snapshot the period and move the closed boundary + period-start baselines forward. */
    void closePeriod(Instant boundaryAt) {
        this.closedThroughAt = boundaryAt;
        this.netAtPeriodStart = this.net;
        this.importCumulativeAtPeriodStart = this.cumulativeImport;
        this.exportCumulativeAtPeriodStart = this.cumulativeExport;
    }

    /** The cumulative for one direction — import reads compare to this, export reads to the other. */
    public BigDecimal cumulativeFor(MeterDirection direction) {
        return direction == MeterDirection.IMPORT ? cumulativeImport : cumulativeExport;
    }

    public UUID getId() { return id; }
    public String getMeterKey() { return meterKey; }
    public BigDecimal getCumulativeImport() { return cumulativeImport; }
    public BigDecimal getCumulativeExport() { return cumulativeExport; }
    public BigDecimal getNet() { return net; }
    public BigDecimal getBaselineNet() { return baselineNet; }
    public BigDecimal getNetAtPeriodStart() { return netAtPeriodStart; }
    public BigDecimal getRateImport() { return rateImport; }
    public BigDecimal getRateExport() { return rateExport; }
    public BigDecimal getImportCumulativeAtPeriodStart() { return importCumulativeAtPeriodStart; }
    public BigDecimal getExportCumulativeAtPeriodStart() { return exportCumulativeAtPeriodStart; }
    public Instant getClosedThroughAt() { return closedThroughAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
