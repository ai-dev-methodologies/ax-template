// PragmaBypass.java — n1 falsification fixture for private_boundary_guard [R26]
// This file demonstrates that pragma: allow-secret does NOT suppress secrets
// in production code paths (backend/src/).
//
// EXPECTED RESULT: exit 1 (Layer 2 violation — real AKIA key in code path)
//
// With old unrestricted pragma logic: the pragma comment is honored everywhere.
//   The real AKIA key is suppressed → exit 0 (BYPASS — BUG).
//
// With correct path-restricted pragma logic: backend/src is NOT a doc path.
//   pragma is ignored in code paths → violation reported → exit 1 (CORRECT).
//
// pragma: allow-secret is ONLY valid in:
//   docs/ or practices/rules/ or *.md files (documentation paths)
// It is IGNORED in:
//   backend/src/, frontend/src/, specs/, contracts/ (code/spec paths)
package com.example.config;

public class PragmaBypass {
    // Attempting to use pragma to hide a real secret in production code — NOT allowed
    static final String AWS_KEY = "AKIA0123456789ABCDEF"; // pragma: allow-secret
}
