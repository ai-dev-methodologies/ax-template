package com.ax.template.authblueprint.uomconversion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable CONVERSION record (UOMCONV-BASIS/DETERMINISM-001): the full reconstructible basis
 * of a single unit conversion — the from-quantity, from-unit, to-unit, both dimensions, whether it
 * was a SAME_DIMENSION pure ratio or a CROSS_DIMENSION material bridge, the numeric factor applied,
 * the material property version cited (null for a same-dimension conversion), the recorded result
 * scale, and the result quantity. Append-only; the from/to quantity columns are {@code from_quantity}
 * / {@code to_quantity} (NEVER 'value'). The {@code idempotency_basis} + uq make an identical
 * re-request return the recorded conversion verbatim rather than compute a second drifting result.
 */
@AggregateMember(root = Material.class)
@Entity
@Table(name = "uom_conversions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_uom_conversion_basis", columnNames = {"idempotency_basis"})
})
public class Conversion {

    /** SAME_DIMENSION = a pure in-dimension unit ratio; CROSS_DIMENSION = a material-bridged conversion. */
    public enum Mode { SAME_DIMENSION, CROSS_DIMENSION }

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The bridging material; null for a SAME_DIMENSION conversion (no material needed). */
    @Column(name = "material_id", updatable = false)
    private UUID materialId;

    @Column(name = "from_quantity", nullable = false, updatable = false, precision = 38, scale = 12)
    private BigDecimal fromQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_unit", nullable = false, updatable = false, length = 20)
    private Unit fromUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_unit", nullable = false, updatable = false, length = 20)
    private Unit toUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_dimension", nullable = false, updatable = false, length = 20)
    private Dimension fromDimension;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_dimension", nullable = false, updatable = false, length = 20)
    private Dimension toDimension;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, updatable = false, length = 20)
    private Mode mode;

    /** The factor applied: the in-dimension ratio (SAME_DIMENSION) or the material density/unit-weight (CROSS). */
    @Column(name = "factor", nullable = false, updatable = false, precision = 38, scale = 12)
    private BigDecimal factor;

    /** The material property version cited; 0 for a SAME_DIMENSION conversion (no material). */
    @Column(name = "material_version", nullable = false, updatable = false)
    private long materialVersion;

    @Column(name = "result_scale", nullable = false, updatable = false)
    private int resultScale;

    @Column(name = "to_quantity", nullable = false, updatable = false, precision = 38, scale = 12)
    private BigDecimal toQuantity;

    /** Deterministic idempotency key: materialId|fromQuantity|fromUnit|toUnit|materialVersion. */
    @Column(name = "idempotency_basis", nullable = false, updatable = false, length = 300)
    private String idempotencyBasis;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    protected Conversion() {}

    public Conversion(UUID id, UUID materialId, BigDecimal fromQuantity, Unit fromUnit, Unit toUnit,
                      Mode mode, BigDecimal factor, long materialVersion, int resultScale,
                      BigDecimal toQuantity, String idempotencyBasis, Instant occurredAt, String actor) {
        this.id = id;
        this.materialId = materialId;
        this.fromQuantity = fromQuantity;
        this.fromUnit = fromUnit;
        this.toUnit = toUnit;
        this.fromDimension = fromUnit.dimension();
        this.toDimension = toUnit.dimension();
        this.mode = mode;
        this.factor = factor;
        this.materialVersion = materialVersion;
        this.resultScale = resultScale;
        this.toQuantity = toQuantity;
        this.idempotencyBasis = idempotencyBasis;
        this.occurredAt = occurredAt;
        this.actor = actor;
    }

    public UUID getId() { return id; }
    public UUID getMaterialId() { return materialId; }
    public BigDecimal getFromQuantity() { return fromQuantity; }
    public Unit getFromUnit() { return fromUnit; }
    public Unit getToUnit() { return toUnit; }
    public Dimension getFromDimension() { return fromDimension; }
    public Dimension getToDimension() { return toDimension; }
    public Mode getMode() { return mode; }
    public BigDecimal getFactor() { return factor; }
    public long getMaterialVersion() { return materialVersion; }
    public int getResultScale() { return resultScale; }
    public BigDecimal getToQuantity() { return toQuantity; }
    public String getIdempotencyBasis() { return idempotencyBasis; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActor() { return actor; }
}
