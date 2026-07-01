// MultiTokenConfig.java — N1 falsification fixture for private_boundary_guard [R26]
// This file demonstrates that a placeholder decoy token does NOT suppress a
// real secret when both appear on the same line.
//
// EXPECTED RESULT: exit 1 (Layer 2 violation — real AKIA key detected)
//
// The two AKIA tokens appear on the same line:
//   AKIAEXAMPLEXXXXXXXXX  → placeholder (contains EXAMPLE) — allowlist suppresses THIS one
//   AKIA0123456789ABCDEF  → real AWS access key — NOT suppressed
//
// With old head -1 logic: only the first token (AKIAEXAMPLEXXXXXXXXX) is checked.
//   It contains EXAMPLE → the whole line is suppressed → exit 0 (SILENT BYPASS — BUG).
//
// With correct token-loop logic: BOTH tokens are checked.
//   First: AKIAEXAMPLEXXXXXXXXX → suppressed. Second: AKIA0123456789ABCDEF → NOT suppressed.
//   One real token found → report_violation → exit 1 (CORRECT).
package com.example.config;

public class MultiTokenConfig {
    // DO NOT USE IN PRODUCTION — these are test credentials demonstrating the N1 bug
    // First key is a placeholder; second key is a real-looking key on the same line:
    private static final String[] TEST_KEYS = {"AKIAEXAMPLEXXXXXXXXX", "AKIA0123456789ABCDEF"};
}
