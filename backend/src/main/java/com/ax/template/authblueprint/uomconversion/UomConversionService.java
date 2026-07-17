package com.ax.template.authblueprint.uomconversion;

import com.ax.template.authblueprint.common.IdempotentInsert;
import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * dimensional-uom-conversion-l0 sole orchestrator (UOMCONV-COMPAT/MATERIAL/BASIS/DETERMINISM/VERSION-001).
 * A conversion first classifies the from/to units' {@link Dimension}: a SAME_DIMENSION conversion
 * applies the pure in-dimension ratio (no material); a CROSS_DIMENSION conversion is admissible ONLY
 * with a recorded bridging {@link MaterialProperty} for the (from-dim → to-dim) pair, else a
 * deterministic 422 INCOMPATIBLE_DIMENSIONS naming both dimensions (a bare ratio across dimensions
 * would be a wrong number — CWE-682). The bridge is VERSIONED append-only; a recorded {@link Conversion}
 * keeps citing the version it used. All arithmetic is BigDecimal at {@link #RESULT_SCALE} with HALF_UP
 * so it is deterministic; an identical re-request returns the recorded conversion verbatim.
 * Property/conversion rows are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class UomConversionService {

    /** The recorded result scale — deterministic BigDecimal rounding (UOMCONV-DETERMINISM-001). */
    static final int RESULT_SCALE = 6;

    private final MaterialRepository materials;
    private final MemberWriter members;
    private final IdempotentInsert idempotentInsert;
    private final UomConversionMetrics metrics;
    private final Clock clock;

    public UomConversionService(MaterialRepository materials, MemberWriter members,
                                IdempotentInsert idempotentInsert, UomConversionMetrics metrics, Clock clock) {
        this.materials = materials;
        this.members = members;
        this.idempotentInsert = idempotentInsert;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public Material registerMaterial(String materialRef) {
        Material m = new Material(UUID.randomUUID(), materialRef, Instant.now(clock));
        Material saved = materials.save(m);
        metrics.record("register_material", "ok");
        return saved;
    }

    /** UOMCONV-MATERIAL/VERSION-001 — append a NEW immutable bridging-property version under the
     *  material's PESSIMISTIC_WRITE lock; the prior versions are preserved (never overwritten). */
    @Transactional
    public MaterialProperty recordProperty(UUID materialId, Dimension fromDimension,
                                           Dimension toDimension, BigDecimal factor) {
        Material material = materials.findByIdForUpdate(materialId).orElseThrow(UomConversionException::notFound);
        long nextVersion = material.advanceVersion();
        MaterialProperty p = new MaterialProperty(UUID.randomUUID(), material.getId(), nextVersion,
            fromDimension, toDimension, factor, Instant.now(clock));
        members.persistAndFlush(p);
        metrics.record("record_property", "ok");
        return p;
    }

    /** UOMCONV-COMPAT-001 — classify the from/to dimensions, then dispatch: same-dimension is a pure
     *  ratio (no material); cross-dimension requires a recorded bridge at the material's current
     *  version, else 422 INCOMPATIBLE_DIMENSIONS naming both dimensions. An unknown unit → 422. */
    @Transactional
    public Conversion convert(UUID materialId, BigDecimal fromQuantity, String fromUnitCode,
                              String toUnitCode, String actor) {
        Unit from = Unit.byCode(fromUnitCode).orElseThrow(() -> UomConversionException.unknownUnit(fromUnitCode));
        Unit to = Unit.byCode(toUnitCode).orElseThrow(() -> UomConversionException.unknownUnit(toUnitCode));
        Dimension fromDim = from.dimension();
        Dimension toDim = to.dimension();

        if (fromDim == toDim) {                                   // same dimension ⇒ pure in-dimension ratio
            return sameDimension(fromQuantity, from, to, actor);
        }
        // cross dimension ⇒ a recorded bridging material property is REQUIRED (else 422)
        Material material = materials.findByIdForUpdate(materialId)
            .orElseThrow(() -> UomConversionException.incompatibleDimensions(fromDim, toDim));
        MaterialProperty bridge = currentBridge(material, fromDim, toDim)
            .orElseThrow(() -> UomConversionException.incompatibleDimensions(fromDim, toDim));
        return crossDimension(material, bridge, fromQuantity, from, to, actor);   // mass = volume × density
    }

    /** UOMCONV-VERSION-001 — re-derive a conversion against a NAMED material version (the prior bridge
     *  is preserved append-only, so an old conversion keeps using the factor it recorded). */
    @Transactional
    public Conversion convertAtVersion(UUID materialId, BigDecimal fromQuantity, String fromUnitCode,
                                       String toUnitCode, long materialVersion, String actor) {
        Unit from = Unit.byCode(fromUnitCode).orElseThrow(() -> UomConversionException.unknownUnit(fromUnitCode));
        Unit to = Unit.byCode(toUnitCode).orElseThrow(() -> UomConversionException.unknownUnit(toUnitCode));
        Dimension fromDim = from.dimension();
        Dimension toDim = to.dimension();
        if (fromDim == toDim) {
            return sameDimension(fromQuantity, from, to, actor);
        }
        materials.findByIdForUpdate(materialId)
            .orElseThrow(() -> UomConversionException.incompatibleDimensions(fromDim, toDim));
        MaterialProperty bridge = materials.findProperty(materialId, materialVersion)
            .orElseThrow(() -> UomConversionException.unknownMaterialVersion(materialVersion));
        if (!bridge.bridges(fromDim, toDim)) {
            metrics.record("convert", "incompatible");
            throw UomConversionException.incompatibleDimensions(fromDim, toDim);
        }
        Material material = materials.findById(materialId).orElseThrow(UomConversionException::notFound);
        return crossDimension(material, bridge, fromQuantity, from, to, actor);
    }

    private Conversion sameDimension(BigDecimal fromQuantity, Unit from, Unit to, String actor) {
        BigDecimal ratio = from.toBaseFactor().divide(to.toBaseFactor(), RESULT_SCALE + 6, RoundingMode.HALF_UP);
        BigDecimal result = fromQuantity.multiply(ratio).setScale(RESULT_SCALE, RoundingMode.HALF_UP);
        Conversion c = new Conversion(UUID.randomUUID(), null, fromQuantity, from, to,
            Conversion.Mode.SAME_DIMENSION, ratio, 0L, RESULT_SCALE, result,
            idempotencyBasis(null, fromQuantity, from, to, 0L), Instant.now(clock), actor);
        metrics.record("convert", "same_dimension");
        return idempotentRecord(c);
    }

    private Conversion crossDimension(Material material, MaterialProperty bridge, BigDecimal fromQuantity,
                                      Unit from, Unit to, String actor) {
        BigDecimal fromInBase = fromQuantity.multiply(from.toBaseFactor());      // to the from-dimension base unit
        BigDecimal toBase = fromInBase.multiply(bridge.getFactor());             // bridge: e.g. volume×density = mass
        BigDecimal result = toBase.divide(to.toBaseFactor(), RESULT_SCALE, RoundingMode.HALF_UP);
        Conversion c = new Conversion(UUID.randomUUID(), material.getId(), fromQuantity, from, to,
            Conversion.Mode.CROSS_DIMENSION, bridge.getFactor(), bridge.getVersion(), RESULT_SCALE, result,
            idempotencyBasis(material.getId(), fromQuantity, from, to, bridge.getVersion()),
            Instant.now(clock), actor);
        metrics.record("convert", "cross_dimension");
        return idempotentRecord(c);
    }

    /** UOMCONV-DETERMINISM-001 — an identical re-request returns the recorded conversion verbatim. */
    private Conversion idempotentRecord(Conversion c) {
        Optional<Conversion> existing = materials.findConversionByBasis(c.getIdempotencyBasis());
        if (existing.isPresent()) {
            metrics.record("convert", "idempotent");
            return existing.get();
        }
        try {
            // P1-64 — isolate the racy insert in a REQUIRES_NEW inner tx so a uq(idempotency_basis)
            // violation aborts only that inner tx; the catch-block requery runs in this (unpoisoned)
            // outer tx even on PostgreSQL (25P02).
            idempotentInsert.insert(() -> members.persistAndFlush(c));
        } catch (DataIntegrityViolationException dup) {
            metrics.record("convert", "idempotent");
            return materials.findConversionByBasis(c.getIdempotencyBasis()).orElseThrow(UomConversionException::notFound);
        }
        return c;
    }

    /** The current bridge for the (from → to) dimension pair at the material's current version. */
    private Optional<MaterialProperty> currentBridge(Material material, Dimension from, Dimension to) {
        return materials.findProperty(material.getId(), material.getCurrentVersion())
            .filter(p -> p.bridges(from, to));
    }

    private static String idempotencyBasis(UUID materialId, BigDecimal fromQuantity, Unit from, Unit to,
                                           long materialVersion) {
        return (materialId == null ? "-" : materialId)
            + "|" + fromQuantity.stripTrailingZeros().toPlainString()
            + "|" + from.name() + "|" + to.name() + "|" + materialVersion;
    }

    @Transactional(readOnly = true)
    public Material getMaterial(UUID materialId) {
        return materials.findById(materialId).orElseThrow(UomConversionException::notFound);
    }

    @Transactional(readOnly = true)
    public List<MaterialProperty> properties(UUID materialId) {
        getMaterial(materialId);                                  // 404 before an empty list
        return materials.findProperties(materialId);
    }

    @Transactional(readOnly = true)
    public Conversion getConversion(UUID conversionId) {
        return materials.findConversion(conversionId).orElseThrow(UomConversionException::notFound);
    }
}
