package com.ax.template.authblueprint.uomconversion;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * dimensional-uom-conversion-l0 unit table (UOMCONV-COMPAT-001). Each unit carries its
 * {@link Dimension} and its factor TO the dimension's base unit (NIST SP 811 §7.1 — a value is a
 * number times a unit; the in-dimension ratio between two units is the quotient of their base
 * factors). A SAME_DIMENSION conversion is the pure ratio {@code from.toBaseFactor / to.toBaseFactor};
 * a CROSS_DIMENSION conversion goes through a material bridge applied at the base-unit level.
 * Base units: LENGTH=m, MASS=kg, VOLUME=L, COUNT=each. The set is the catalog reference; a
 * fork-receiver may extend it (the governance contract is the dimension/bridge discipline, not
 * this enumeration).
 */
public enum Unit {
    // LENGTH — base m
    M(Dimension.LENGTH, "1"),
    CM(Dimension.LENGTH, "0.01"),
    MM(Dimension.LENGTH, "0.001"),
    // MASS — base kg
    KG(Dimension.MASS, "1"),
    G(Dimension.MASS, "0.001"),
    MG(Dimension.MASS, "0.000001"),
    // VOLUME — base L
    L(Dimension.VOLUME, "1"),
    ML(Dimension.VOLUME, "0.001"),
    // COUNT — base each
    EACH(Dimension.COUNT, "1");

    private final Dimension dimension;
    private final BigDecimal toBaseFactor;

    Unit(Dimension dimension, String toBaseFactor) {
        this.dimension = dimension;
        this.toBaseFactor = new BigDecimal(toBaseFactor);
    }

    public Dimension dimension() {
        return dimension;
    }

    /** The multiplier that converts a quantity in THIS unit to the dimension's base unit. */
    public BigDecimal toBaseFactor() {
        return toBaseFactor;
    }

    /** Resolve a unit by its uppercase code; empty when unknown (→ 422 UNKNOWN_UNIT). */
    public static Optional<Unit> byCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        for (Unit u : values()) {
            if (u.name().equalsIgnoreCase(code)) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }
}
