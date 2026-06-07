package com.ax.template.authblueprint.transformation;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pure conservation logic for transformation-conservation-l0 (no Spring, no DB — testable in
 * isolation). Validates, for one transformation, that Σ(input) == Σ(good output) + Σ(residual)
 * exactly per base unit (XFORM-ACCOUNTED-LOSS-001 / XFORM-DIMENSION-001), with every residual leg
 * carrying a governed disposition (XFORM-RESIDUAL-CLASSIFIED-001). Throws {@link TransformationException}
 * on any violation; returns the conserved totals on success.
 */
public final class ConservationCheck {

    private ConservationCheck() {}

    /** A unified transformation leg (disposition is non-null only for RESIDUAL). */
    public record Leg(LegRole role, String materialCode, BigDecimal qty, String unit,
                      TransformationDisposition disposition) {}

    public record Result(String baseUnit, BigDecimal totalInput, BigDecimal totalGood, BigDecimal totalResidual) {}

    /** Persisted quantity scale (NUMERIC(19,4)) — a finer-scale qty would lose precision on store. */
    public static final int MAX_SCALE = 4;
    /** A transformation is bounded so a run's legs always fit one page (response is always complete). */
    public static final int MAX_LEGS = 200;

    public static Result check(List<Leg> legs) {
        if (legs == null || legs.isEmpty() || legs.size() > MAX_LEGS) {
            throw TransformationException.invalidAmount();
        }
        String baseUnit = null;
        boolean hasInput = false;
        boolean hasOutput = false;
        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalGood = BigDecimal.ZERO;
        BigDecimal totalResidual = BigDecimal.ZERO;

        for (Leg leg : legs) {
            if (leg.role() == null || leg.qty() == null || leg.qty().signum() < 0 || leg.unit() == null) {
                throw TransformationException.invalidAmount();
            }
            // a finer-than-storable scale would throw on setScale(4) deep in the service -> reject here (clean 422)
            if (leg.qty().scale() > MAX_SCALE) {
                throw TransformationException.invalidAmount();
            }
            String unit = leg.unit().trim();   // normalize so " kg" and "kg" are the same physical unit
            if (unit.isEmpty()) {
                throw TransformationException.invalidAmount();
            }
            // XFORM-DIMENSION-001 — one base unit across all legs (no naive cross-dimension sum)
            if (baseUnit == null) {
                baseUnit = unit;
            } else if (!baseUnit.equals(unit)) {
                throw TransformationException.mixedUnit();
            }
            switch (leg.role()) {
                case INPUT -> {
                    if (leg.disposition() != null) throw TransformationException.dispositionNotAllowed();
                    hasInput = true;
                    totalIn = totalIn.add(leg.qty());
                }
                case GOOD_OUTPUT -> {
                    if (leg.disposition() != null) throw TransformationException.dispositionNotAllowed();
                    hasOutput = true;
                    totalGood = totalGood.add(leg.qty());
                }
                case RESIDUAL -> {
                    // XFORM-RESIDUAL-CLASSIFIED-001 — a residual quantity MUST carry a disposition
                    if (leg.disposition() == null) {
                        throw TransformationException.unclassifiedResidual();
                    }
                    hasOutput = true;
                    totalResidual = totalResidual.add(leg.qty());
                }
            }
        }
        // XFORM-ATOMIC-001 — a transformation has at least one input AND at least one output (good or residual);
        // an inputs-consumed-but-nothing-produced record is forbidden.
        if (!hasInput || !hasOutput) {
            throw TransformationException.invalidAmount();
        }
        // XFORM-ACCOUNTED-LOSS-001 — exact conservation to an accounted residual
        if (totalIn.compareTo(totalGood.add(totalResidual)) != 0) {
            throw TransformationException.notConserved(totalIn, totalGood, totalResidual);
        }
        return new Result(baseUnit, totalIn, totalGood, totalResidual);
    }
}
