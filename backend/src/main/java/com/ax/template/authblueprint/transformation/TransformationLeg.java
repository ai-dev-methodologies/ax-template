package com.ax.template.authblueprint.transformation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One leg of a transformation (input consumed, good output produced, or classified residual).
 * Fully IMMUTABLE — every column is {@code @Column(updatable=false)}, there is no setter, and the
 * row is never updated (a correction is a reversal transformation). {@code disposition} is non-null
 * only for a RESIDUAL leg (XFORM-RESIDUAL-CLASSIFIED-001).
 */
@AggregateMember(root = TransformationRun.class)
@Entity
@Table(name = "transformation_legs")
// XFORM-RESIDUAL-CLASSIFIED-001 — DB backstop (LIVE under ddl-auto): a RESIDUAL leg MUST carry a
// disposition (no unexplained shrinkage); quantities are non-negative.
@Check(constraints = "qty >= 0 AND (role <> 'RESIDUAL' OR disposition IS NOT NULL)")
public class TransformationLeg {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, updatable = false, length = 16)
    private LegRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", updatable = false, length = 16)
    private TransformationDisposition disposition;

    @Column(name = "material_code", nullable = false, updatable = false, length = 120)
    private String materialCode;

    @Column(name = "qty", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal qty;

    @Column(name = "unit", nullable = false, updatable = false, length = 32)
    private String unit;

    protected TransformationLeg() {}

    public TransformationLeg(UUID id, UUID runId, LegRole role, TransformationDisposition disposition,
                            String materialCode, BigDecimal qty, String unit) {
        this.id = id;
        this.runId = runId;
        this.role = role;
        this.disposition = disposition;
        this.materialCode = materialCode;
        this.qty = qty;
        this.unit = unit;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public LegRole getRole() { return role; }
    public TransformationDisposition getDisposition() { return disposition; }
    public String getMaterialCode() { return materialCode; }
    public BigDecimal getQty() { return qty; }
    public String getUnit() { return unit; }
}
