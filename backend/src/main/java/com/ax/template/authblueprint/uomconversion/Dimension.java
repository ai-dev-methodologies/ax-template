package com.ax.template.authblueprint.uomconversion;

/**
 * dimensional-uom-conversion-l0 physical dimensions (UOMCONV-COMPAT-001). Per NIST SP 811 §7.14,
 * a quantity's dimension is a structured exponent over the base quantities; two units are
 * inter-convertible by a PURE RATIO only when they share a Dimension. A conversion between
 * DIFFERENT dimensions is not a unit ratio — it requires a bridging material property
 * (density / unit-weight). This fixed set is the catalog reference; a fork-receiver may extend it
 * without touching the governance contract (the compatibility precondition + recorded bridge).
 */
public enum Dimension {
    /** Length (m). */
    LENGTH,
    /** Mass (kg). */
    MASS,
    /** Volume (L) — a derived length dimension, kept distinct here as a conversion dimension. */
    VOLUME,
    /** Count / amount of discrete items (each). */
    COUNT
}
