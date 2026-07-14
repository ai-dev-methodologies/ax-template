package com.ax.template.authblueprint.netmetering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * signed-dual-register-l0 IMMUTABLE billing-period snapshot (NETM-PERIOD-001). A period close records both
 * direction cumulatives at the boundary AND the period net delta ({@code periodNetDelta = net_end −
 * net_start}). EVERY column is {@code @Column(updatable=false)} and there is no setter — a closed period is
 * frozen; a later reading backdated at/before {@code boundaryAt} is a 409, never an UPDATE of this row.
 * {@code sequenceNo} is strictly monotonic per meter (boundaries move strictly forward).
 *
 * <p>NETM-RATE-001 extension: {@code importDelta}/{@code exportDelta} are the per-direction quantities
 * consumed THIS period (derived from the same cumulatives that satisfy NETM-DIRECTION-001 conservation);
 * {@code rateImport}/{@code rateExport} are the rates in effect AT CLOSE (immutable evidence — a later rate
 * change never reshapes an already-closed period); {@code billedAmount = importDelta*rateImport −
 * exportDelta*rateExport}, cross-checked against an independent chain recompute before being persisted here.
 */
@AggregateMember(root = NetMeter.class)
@Entity
@Table(name = "net_meter_periods",
    uniqueConstraints = @UniqueConstraint(name = "uq_net_meter_period_seq",
        columnNames = {"meter_id", "sequence_no"}))
public class NetMeterPeriod {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "meter_id", nullable = false, updatable = false)
    private UUID meterId;

    @Column(name = "boundary_at", nullable = false, updatable = false)
    private Instant boundaryAt;

    @Column(name = "import_cumulative", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal importCumulative;

    @Column(name = "export_cumulative", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal exportCumulative;

    /** net at the start of the period (the prior boundary's net). */
    @Column(name = "net_start", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal netStart;

    /** net at the close boundary (= importCumulative − exportCumulative). */
    @Column(name = "net_end", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal netEnd;

    /** period net delta = net_end − net_start — the SIGNED quantity settled for this period (PURPA offset). */
    @Column(name = "period_net_delta", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal periodNetDelta;

    /** NETM-RATE-001 — import quantity consumed THIS period (= importCumulative − importCumulativeStart). */
    @Column(name = "import_delta", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal importDelta;

    /** NETM-RATE-001 — export quantity consumed THIS period (= exportCumulative − exportCumulativeStart). */
    @Column(name = "export_delta", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal exportDelta;

    /** NETM-RATE-001 — rate in effect at close (immutable evidence). */
    @Column(name = "rate_import", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal rateImport;

    /** NETM-RATE-001 — rate in effect at close (immutable evidence). */
    @Column(name = "rate_export", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal rateExport;

    /** NETM-RATE-001 — billed = importDelta*rateImport − exportDelta*rateExport, cross-checked before persist. */
    @Column(name = "billed_amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal billedAmount;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    @Column(name = "closed_at", nullable = false, updatable = false)
    private Instant closedAt;

    protected NetMeterPeriod() {}

    public NetMeterPeriod(UUID id, UUID meterId, Instant boundaryAt, BigDecimal importCumulative,
                          BigDecimal exportCumulative, BigDecimal netStart, BigDecimal netEnd,
                          BigDecimal periodNetDelta, BigDecimal importDelta, BigDecimal exportDelta,
                          BigDecimal rateImport, BigDecimal rateExport, BigDecimal billedAmount,
                          long sequenceNo, Instant closedAt) {
        this.id = id;
        this.meterId = meterId;
        this.boundaryAt = boundaryAt;
        this.importCumulative = importCumulative;
        this.exportCumulative = exportCumulative;
        this.netStart = netStart;
        this.netEnd = netEnd;
        this.periodNetDelta = periodNetDelta;
        this.importDelta = importDelta;
        this.exportDelta = exportDelta;
        this.rateImport = rateImport;
        this.rateExport = rateExport;
        this.billedAmount = billedAmount;
        this.sequenceNo = sequenceNo;
        this.closedAt = closedAt;
    }

    public UUID getId() { return id; }
    public UUID getMeterId() { return meterId; }
    public Instant getBoundaryAt() { return boundaryAt; }
    public BigDecimal getImportCumulative() { return importCumulative; }
    public BigDecimal getExportCumulative() { return exportCumulative; }
    public BigDecimal getNetStart() { return netStart; }
    public BigDecimal getNetEnd() { return netEnd; }
    public BigDecimal getPeriodNetDelta() { return periodNetDelta; }
    public BigDecimal getImportDelta() { return importDelta; }
    public BigDecimal getExportDelta() { return exportDelta; }
    public BigDecimal getRateImport() { return rateImport; }
    public BigDecimal getRateExport() { return rateExport; }
    public BigDecimal getBilledAmount() { return billedAmount; }
    public long getSequenceNo() { return sequenceNo; }
    public Instant getClosedAt() { return closedAt; }
}
