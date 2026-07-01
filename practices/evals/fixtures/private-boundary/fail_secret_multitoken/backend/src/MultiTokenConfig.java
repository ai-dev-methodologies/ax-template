// MultiTokenConfig.java — N1 falsification fixture for private_boundary_guard [R26]
// This file demonstrates that a placeholder decoy token does NOT suppress a
// real secret when both appear on the same line.
//
// EXPECTED RESULT: exit 1 (Layer 2 violation — real AKIA key detected)
//
// The TEST_KEYS line below contains two AKIA tokens:
//   First  → AKIAEXAMPLEXXXXXXXXX  (placeholder, contains EXAMPLE)
//   Second → [see TEST_KEYS] (real AWS access key — NOT written in comments here)
//
// With OLD head -1 logic: only the first token (AKIAEXAMPLEXXXXXXXXX) is checked.
//   It contains EXAMPLE → the whole line is suppressed → exit 0 (SILENT BYPASS — BUG).
//   The real key on the same line is never examined.
//
// With CORRECT token-loop logic: BOTH tokens are checked.
//   First:  AKIAEXAMPLEXXXXXXXXX → allowlist match → suppressed.
//   Second: real key             → no allowlist match → all_placeholder=false → violation.
//   One real token found → report_violation → exit 1 (CORRECT).
package com.example.config;

public class MultiTokenConfig {
    // Placeholder decoy first, real key second — both on the same line:
    private static final String[] TEST_KEYS = {"AKIAEXAMPLEXXXXXXXXX", "AKIA0123456789ABCDEF"};
}
