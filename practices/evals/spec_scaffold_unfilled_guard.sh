#!/usr/bin/env bash
# practices/evals/spec_scaffold_unfilled_guard.sh — ax-plan G6 forcing wire.
#
# THE FORCING WIRE. /ax-scaffold emits a Spec Trio SKELETON whose spec files carry
# the literal scaffold marker `# TODO: Add` (and empty `items: []`). This guard
# BLOCKS the build while ANY specs/*-l0.yaml or specs/*-frontend-l0.yaml still
# carries that marker — i.e. a domain was scaffolded but NOT planned. The only way
# to clear it is to run `/ax-plan <domain>`, which fills the spec with real items +
# 1:1 RED @Tag stubs (removing the marker). You therefore cannot skip /ax-plan: a
# scaffolded-but-unplanned domain keeps the catalog RED.
#
# This complements the two promoted hard gates without overlapping them:
#   - domain_spec_trio_guard.sh       — the Trio FILES exist per domain_mode
#   - spec_item_verification_binding  — every applicable ITEM resolves a binding
#   - THIS guard                      — no spec is left UNFILLED (scaffold marker)
# An empty skeleton passes the first two vacuously (0 items → nothing unbound);
# this guard is what makes the empty skeleton fail, forcing the plan.
#
# Marker rationale: matches ONLY `# TODO: Add` (the exact string new-domain.sh
# writes). It deliberately does NOT match bare `TBD`/`FIXME`, which appear in the
# `introduced_at:` provenance prose of complete specs (caching-l0, dsr-l0, ...).
#
# Usage:   bash practices/evals/spec_scaffold_unfilled_guard.sh [--root DIR]
# Exit 0 = no unfilled scaffold specs. Exit 1 = at least one unplanned scaffold.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

while [ $# -gt 0 ]; do
    case "$1" in
        --root) REPO_ROOT="$2"; shift 2 ;;
        --root=*) REPO_ROOT="${1#--root=}"; shift ;;
        *) echo "spec_scaffold_unfilled_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SPECS_DIR="$REPO_ROOT/specs"
if [ ! -d "$SPECS_DIR" ]; then
    echo "spec_scaffold_unfilled_guard: specs dir '$SPECS_DIR' not found — nothing to check"
    exit 0
fi

violations=0
checked=0
for spec in "$SPECS_DIR"/*-l0.yaml; do
    [ -e "$spec" ] || continue
    checked=$((checked + 1))
    if grep -qE '^[[:space:]]*#?[[:space:]]*TODO:[[:space:]]*Add' "$spec" 2>/dev/null; then
        rel="${spec#$REPO_ROOT/}"
        domain="$(basename "$spec" | sed -E 's/(-frontend)?-l0\.yaml$//')"
        echo "VIOLATION: $rel is an UNFILLED ax-scaffold skeleton (carries '# TODO: Add')."
        echo "  Run  /ax-plan $domain  to fill+trace it (real items + 1:1 RED @Tag stubs),"
        echo "  or delete the file if this domain_mode does not need it."
        violations=$((violations + 1))
    fi
done

echo "[spec_scaffold_unfilled] checked $checked spec file(s)"
if [ "$violations" -gt 0 ]; then
    echo "spec_scaffold_unfilled_guard: $violations unfilled scaffold spec(s) — BLOCKED. Run /ax-plan." >&2
    exit 1
fi
echo "spec_scaffold_unfilled_guard: PASS — no scaffolded-but-unplanned specs (every spec is filled)"
exit 0
