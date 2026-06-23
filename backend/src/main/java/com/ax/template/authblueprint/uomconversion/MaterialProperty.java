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
 * One immutable VERSIONED bridging property of a {@link Material} (UOMCONV-MATERIAL/VERSION-001):
 * the factor that bridges a (fromDimension → toDimension) pair, expressed in the to-dimension base
 * unit per from-dimension base unit (e.g. VOLUME→MASS factor = density in kg/L; COUNT→MASS factor =
 * unit-weight in kg/each). Append-only — a corrected density is a NEW version (a higher
 * {@code version}), the prior versions preserved; uq(material_id, version) makes a duplicate
 * version a deterministic conflict. A recorded {@link Conversion} keeps citing the version it used.
 */
@AggregateMember(root = Material.class)
@Entity
@Table(name = "uom_material_properties", uniqueConstraints = {
    @UniqueConstraint(name = "uq_uom_material_version", columnNames = {"material_id", "version"})
})
public class MaterialProperty {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "material_id", nullable = false, updatable = false)
    private UUID materialId;

    @Column(name = "version", nullable = false, updatable = false)
    private long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_dimension", nullable = false, updatable = false, length = 20)
    private Dimension fromDimension;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_dimension", nullable = false, updatable = false, length = 20)
    private Dimension toDimension;

    /** The bridging factor (to-base per from-base): VOLUME→MASS = density kg/L; COUNT→MASS = kg/each. */
    @Column(name = "factor", nullable = false, updatable = false, precision = 38, scale = 12)
    private BigDecimal factor;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected MaterialProperty() {}

    public MaterialProperty(UUID id, UUID materialId, long version, Dimension fromDimension,
                            Dimension toDimension, BigDecimal factor, Instant recordedAt) {
        this.id = id;
        this.materialId = materialId;
        this.version = version;
        this.fromDimension = fromDimension;
        this.toDimension = toDimension;
        this.factor = factor;
        this.recordedAt = recordedAt;
    }

    public boolean bridges(Dimension from, Dimension to) {
        return this.fromDimension == from && this.toDimension == to;
    }

    public UUID getId() { return id; }
    public UUID getMaterialId() { return materialId; }
    public long getVersion() { return version; }
    public Dimension getFromDimension() { return fromDimension; }
    public Dimension getToDimension() { return toDimension; }
    public BigDecimal getFactor() { return factor; }
    public Instant getRecordedAt() { return recordedAt; }
}
