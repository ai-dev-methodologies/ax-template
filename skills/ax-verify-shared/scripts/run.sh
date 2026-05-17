#!/usr/bin/env bash
# skills/ax-verify-shared/scripts/run.sh
# Tier-2 shared-artifacts axis verifier orchestrator.
# Steps 1-4 are binary; step 5 (OpenAPI lint) is advisory.
# Exit 0 iff steps 1-4 all pass.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo "=== ax-verify-shared: run.sh ==="
echo ""

# Step 1: spec_ref_guard (both practices/rules/** and practices-react/rules/**)
echo "[1] spec_ref_guard (all rules)"
if bash "$REPO_ROOT/practices/evals/spec_ref_guard.sh"; then
    echo "  PASS [spec_ref_guard]"
else
    echo "  FAIL [spec_ref_guard]" >&2
    echo "  hint: invoke /ax-guard-spec-ref for fix-loop" >&2
    exit 1
fi

# Step 2: trio_integrity_guard (all domains)
echo ""
echo "[2] trio_integrity_guard"
if bash "$REPO_ROOT/practices/evals/trio_integrity_guard.sh"; then
    echo "  PASS [trio_integrity_guard]"
else
    echo "  FAIL [trio_integrity_guard]" >&2
    echo "  hint: invoke /ax-guard-trio-integrity; read error code for fix" >&2
    exit 1
fi

# Step 3: AGENTS.md sentinel regeneration check
echo ""
echo "[3] AGENTS.md sentinel"
if bash "$SCRIPT_DIR/regen-agents.sh"; then
    echo "  PASS [agents-sentinel]"
else
    echo "  FAIL [agents-sentinel]" >&2
    echo "  hint: catalog changed without regenerating AGENTS.md; run bash templates/generate_agents.sh" >&2
    exit 1
fi

# Step 4: ADR provenance_class completeness
echo ""
echo "[4] DECISIONS.md provenance_class check"
if bash "$SCRIPT_DIR/check-decisions.sh"; then
    echo "  PASS [decisions-provenance]"
else
    echo "  FAIL [decisions-provenance]" >&2
    echo "  hint: add provenance_class: field to the named ADR" >&2
    exit 1
fi

# Step 5: OpenAPI lint (advisory)
echo ""
echo "[5] OpenAPI lint (advisory)"
if command -v npx &>/dev/null; then
    npx @redocly/cli lint "$REPO_ROOT/contracts/"**/*.yaml 2>/dev/null && \
        echo "  PASS [openapi-lint]" || \
        echo "  INFO [openapi-lint] advisory failure — logged but not blocking"
else
    echo "  SKIP [openapi-lint] npx not available"
fi

echo ""
echo "=== ax-verify-shared: all required steps PASS ==="
exit 0
