#!/usr/bin/env bash
# verify/cold-start-test.sh — cold-start readiness check for a blueprint.
#
# Usage:  bash verify/cold-start-test.sh <blueprint_name>
#
# Simulates a context-0 agent that receives only the minimum file set for a
# blueprint and validates:
#   1. All files in the minimum set exist and are non-empty (readable + nonzero size)
#   2. docs/blueprints/<blueprint>/progress.md has a parseable 'next phase' anchor
#      (or a '## COMPLETE' / '**Status**: COMPLETE' marker)
#
# Per-blueprint minimum file sets:
#   payment  — 8 files (hardcoded in cold_start_files_for_payment)
#   <other>  — default set (CLAUDE.md + README.md + practices/AGENTS.md +
#              docs/blueprints/<name>/plan.md + docs/blueprints/<name>/progress.md)
#
# Output:
#   One line per file: "  PASS  <path>" or "  FAIL  <path>  (<reason>)"
#   Final summary: cold-start-test: <blueprint> — N/M files readable, next-phase parseable: yes/no
#
# Exit:   0 if all files readable + nonzero AND next-phase anchor parseable
#         1 if any file missing/empty OR anchor not parseable
#         2 if invocation error (missing arg, empty arg)

set -uo pipefail

# ── Argument parsing ─────────────────────────────────────────────────────────

if [ $# -ne 1 ]; then
    echo "Usage: $0 <blueprint_name>" >&2
    echo "Example: $0 payment" >&2
    exit 2
fi

BLUEPRINT="$1"

if [ -z "$BLUEPRINT" ]; then
    echo "ERROR: blueprint name is empty" >&2
    exit 2
fi

# Resolve repo root (script may be run from any subdir).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# ── Per-blueprint minimum file sets ─────────────────────────────────────────

cold_start_files_for_payment() {
    echo "docs/blueprints/payment/plan.md"
    echo "docs/blueprints/payment/progress.md"
    echo "specs/payment-l0.yaml"
    echo "contracts/payment-openapi.yaml"
    echo "blueprints/payment-manifest.yaml"
    echo "practices/AGENTS.md"
    echo "CLAUDE.md"
    echo "README.md"
}

cold_start_files_default() {
    local name="$1"
    echo "CLAUDE.md"
    echo "README.md"
    echo "practices/AGENTS.md"
    echo "docs/blueprints/${name}/plan.md"
    echo "docs/blueprints/${name}/progress.md"
}

# Select file set for the given blueprint.
case "$BLUEPRINT" in
    payment)
        FILE_LIST="$(cold_start_files_for_payment)"
        ;;
    *)
        # Unknown blueprints use the default set (no hard error — blueprint may
        # be early-stage and not registered yet). Exit 1 is expected when files
        # are missing; exit 2 is reserved for invocation errors only.
        FILE_LIST="$(cold_start_files_default "$BLUEPRINT")"
        ;;
esac

PROGRESS_PATH="docs/blueprints/${BLUEPRINT}/progress.md"

# ── Header ───────────────────────────────────────────────────────────────────

echo "cold-start-test: ${BLUEPRINT}"
echo "simulating context-0 agent: minimum file set check + next-phase anchor"
echo ""

# ── File checks ──────────────────────────────────────────────────────────────

TOTAL=0
PASS=0
FAIL=0
declare -a FAILURES

while IFS= read -r filepath; do
    [ -z "$filepath" ] && continue
    TOTAL=$((TOTAL + 1))

    if [ -s "$filepath" ]; then
        echo "  PASS  $filepath"
        PASS=$((PASS + 1))
    else
        if [ -e "$filepath" ]; then
            reason="exists but empty"
        else
            reason="not found"
        fi
        echo "  FAIL  $filepath  ($reason)"
        FAILURES+=("$filepath ($reason)")
        FAIL=$((FAIL + 1))
    fi
done <<< "$FILE_LIST"

# ── Next-phase anchor check ───────────────────────────────────────────────────
#
# Parse progress.md for one of:
#   A) A line starting with "- **P" followed by a digit (phase anchor)
#      e.g.:  - **P0.7 started**: ...  or  - **P3.0 complete**: ...
#   B) A "**Status**: COMPLETE" or "## COMPLETE" marker (blueprint finished)
#
# We extract the LAST such line to get the most recent phase reference.

echo ""
ANCHOR_PARSEABLE="no"
ANCHOR_LINE=""

if [ ! -f "$PROGRESS_PATH" ]; then
    echo "  FAIL  next-phase anchor: $PROGRESS_PATH not found"
    FAILURES+=("next-phase anchor: $PROGRESS_PATH not found")
    FAIL=$((FAIL + 1))
elif [ ! -s "$PROGRESS_PATH" ]; then
    echo "  FAIL  next-phase anchor: $PROGRESS_PATH is empty"
    FAILURES+=("next-phase anchor: $PROGRESS_PATH is empty")
    FAIL=$((FAIL + 1))
else
    # Check for COMPLETE marker first.
    if grep -qE '(\*\*Status\*\*: COMPLETE|^## COMPLETE)' "$PROGRESS_PATH" 2>/dev/null; then
        ANCHOR_LINE="Status: COMPLETE (blueprint finished)"
        ANCHOR_PARSEABLE="yes"
    else
        # Look for phase anchor lines: "- **P<digit>" anywhere in file.
        ANCHOR_LINE="$(grep -E '^\- \*\*P[0-9]' "$PROGRESS_PATH" 2>/dev/null | tail -1 || true)"
        if [ -n "$ANCHOR_LINE" ]; then
            ANCHOR_PARSEABLE="yes"
        fi
    fi

    if [ "$ANCHOR_PARSEABLE" = "yes" ]; then
        echo "  PASS  next-phase anchor: ${ANCHOR_LINE}"
    else
        echo "  FAIL  next-phase anchor: not parseable in $PROGRESS_PATH"
        echo "        (expected a line matching '- **P<N>' or '**Status**: COMPLETE')"
        FAILURES+=("next-phase anchor: not parseable in $PROGRESS_PATH")
        FAIL=$((FAIL + 1))
    fi
fi

# ── Summary ───────────────────────────────────────────────────────────────────

echo ""
echo "cold-start-test: ${BLUEPRINT} — ${PASS}/${TOTAL} files readable, next-phase parseable: ${ANCHOR_PARSEABLE}"

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo "Failures:"
    for f in "${FAILURES[@]}"; do
        echo "  - $f"
    done
    exit 1
fi

exit 0
