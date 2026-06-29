#!/usr/bin/env bash
# practices/evals/broadleaf_no_port_guard.sh — Broadleaf-absorption LICENSE-SAFETY guard.
#
# THE INVARIANT (binary): ax-template ABSORBS Broadleaf INVARIANTS into independent
# code; it MUST NEVER PORT Broadleaf SOURCE into its own implementation tree. Broadleaf
# is licensed under the Broadleaf Fair Use License Agreement v1.0 (NOT an OSI/permissive
# license) — its source cannot be relicensed or redistributed inside this fork-base
# template. So our OWN implementation source (backend/src, frontend/src) MUST contain
# ZERO Broadleaf mention (case-insensitive) — strengthened 2026-06-29 from "no ported source" to
# "no name reference at all":
#   (a) no `import org.broadleafcommerce...`        (ported dependency)
#   (b) no `package org.broadleafcommerce...`       (a verbatim ported file)
#   (c) no Broadleaf Fair Use License header string (every Broadleaf source file carries it)
#   (d) no bare "Broadleaf" name even in a comment / Javadoc / SQL header (provenance hint)
#
# Short single-line Broadleaf quotes in practices/rules/*.md `evidence:` blocks and in specs/*.yaml
# notes are INTENTIONAL citations (fair-use grounding) and are NOT scanned — this guard targets ONLY
# the IMPLEMENTATION tree, which must read as standalone code with no trace of the absorption source.
#
# Motivation (2026-06-25): the Broadleaf-absorption program reads the Broadleaf clone
# (kept OUTSIDE git) for understanding. This guard mechanically enforces the license
# discipline so a future agent cannot silently copy-paste Broadleaf source into our code.
#
# Usage:
#   bash practices/evals/broadleaf_no_port_guard.sh
#   bash practices/evals/broadleaf_no_port_guard.sh --root DIR   # fixture mode (scan DIR)
# Exit 0 = no ported Broadleaf source. Exit 1 = ported source found (BLOCK).

set -u

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        *) echo "broadleaf_no_port_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# Forbidden = ANY Broadleaf mention (case-insensitive) in the implementation tree. This subsumes the
# ported-source markers (import/package org.broadleafcommerce, Fair Use License header) AND now also
# forbids bare NAME references even in comments/Javadoc/SQL — the shipped implementation code must read
# as standalone; the absorption provenance lives only in docs/rules/parity (fair-use, not scanned).
PATTERNS='[Bb]roadleaf'

if [ -n "$ROOT_OVERRIDE" ]; then
    # Fixture mode: scan the whole override root.
    [ -d "$ROOT_OVERRIDE" ] || { echo "broadleaf_no_port_guard: root not found: $ROOT_OVERRIDE" >&2; exit 2; }
    SCAN_DIRS="$ROOT_OVERRIDE"
else
    # Live mode: scan ONLY our implementation source trees (not rules/specs/docs).
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
    cd "$REPO_ROOT" || { echo "broadleaf_no_port_guard: cannot cd repo root" >&2; exit 2; }
    SCAN_DIRS=""
    [ -d backend/src ] && SCAN_DIRS="$SCAN_DIRS backend/src"
    [ -d frontend/src ] && SCAN_DIRS="$SCAN_DIRS frontend/src"
    [ -n "$SCAN_DIRS" ] || { echo "broadleaf_no_port_guard: no implementation source trees found" >&2; exit 2; }
fi

# grep -rlE: list files containing any forbidden pattern. Restrict to source/text files.
HITS="$(grep -rlE "$PATTERNS" $SCAN_DIRS 2>/dev/null || true)"

if [ -n "$HITS" ]; then
    echo "broadleaf_no_port_guard: FAIL — Broadleaf mention detected in the implementation tree:" >&2
    grep -rinE "$PATTERNS" $SCAN_DIRS 2>/dev/null | sed 's/^/  /' >&2
    echo "" >&2
    echo "  The implementation tree (backend/src, frontend/src) must contain ZERO Broadleaf reference —" >&2
    echo "  not just no ported source (Broadleaf is under the Fair Use License v1.0, not OSI/permissive)," >&2
    echo "  but no name mention even in comments/Javadoc/SQL. Absorb the INVARIANT into standalone code and" >&2
    echo "  describe it generically; the absorption provenance lives only in docs/rules/parity (fair-use)." >&2
    exit 1
fi

echo "broadleaf_no_port_guard: PASS — no Broadleaf reference in the implementation tree (scanned:$SCAN_DIRS)"
exit 0
