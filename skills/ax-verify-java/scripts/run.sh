#!/usr/bin/env bash
# skills/ax-verify-java/scripts/run.sh
# Tier-2 Java/Spring axis verifier orchestrator.
# Steps 1-5 are binary; step 6 is advisory.
# Exit 0 iff steps 1-5 all pass.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo "=== ax-verify-java: run.sh ==="
echo ""

# Step 1: spec_ref_guard scoped to practices/rules/**
echo "[1] spec_ref_guard (practices/rules/)"
if bash "$REPO_ROOT/practices/evals/spec_ref_guard.sh" "$REPO_ROOT/practices/rules"; then
    echo "  PASS [spec_ref_guard]"
else
    echo "  FAIL [spec_ref_guard]" >&2
    echo "  hint: invoke /ax-guard-spec-ref for fix-loop" >&2
    exit 1
fi

# Step 2: evidence_guard scoped to backend/ and practices/rules/**
echo ""
echo "[2] evidence_guard (practices/rules/ + backend/)"
if bash "$REPO_ROOT/practices/evals/evidence_guard.sh" "$REPO_ROOT/practices/rules"; then
    echo "  PASS [evidence_guard]"
else
    echo "  FAIL [evidence_guard]" >&2
    echo "  hint: invoke /ax-guard-evidence for fix-loop" >&2
    exit 1
fi

# Step 3: testPractices
echo ""
echo "[3] ./gradlew testPractices"
if bash "$SCRIPT_DIR/run-gradle.sh" "testPractices"; then
    echo "  PASS [testPractices]"
else
    echo "  FAIL [testPractices]" >&2
    echo "  hint: fix failing @Tag(\"PRACTICES\") test, then re-run" >&2
    exit 1
fi

# Step 4: testAsvs
echo ""
echo "[4] ./gradlew testAsvs"
if bash "$SCRIPT_DIR/run-gradle.sh" "testAsvs"; then
    echo "  PASS [testAsvs]"
else
    echo "  FAIL [testAsvs]" >&2
    echo "  hint: fix failing @Tag(\"ASVS\") test" >&2
    exit 1
fi

# Step 5: testCrud
echo ""
echo "[5] ./gradlew testCrud"
if bash "$SCRIPT_DIR/run-gradle.sh" "testCrud"; then
    echo "  PASS [testCrud]"
else
    echo "  FAIL [testCrud]" >&2
    echo "  hint: fix failing @Tag(\"CRUD\") test" >&2
    exit 1
fi

# Step 6: testPortability (advisory — never blocks)
echo ""
echo "[6] ./gradlew testPortability (advisory)"
if bash "$SCRIPT_DIR/run-gradle.sh" "testPortability" 2>/dev/null; then
    echo "  PASS [testPortability]"
else
    echo "  INFO [testPortability] advisory failure — logged but not blocking"
fi

echo ""
echo "=== ax-verify-java: all required steps PASS ==="
exit 0
