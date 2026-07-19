#!/usr/bin/env bash
# practices/consumer-proof/engine/fixtures/harness-config-pin/prove-config-pin.sh
#
# RED-ABLE PROOF for the consumer-proof harness reliability gap:
#
#   ESLint 9's flat config silently exits 0 with "File ignored because
#   outside of base path." when `lint_json()` in
#   practices/consumer-proof/run-consumer-proof.sh is invoked the way it
#   invokes eslint (`cd $REACT_DIR && npx eslint <relpath>`) against a
#   fixture that lives OUTSIDE $REACT_DIR — e.g. a future out-of-tree
#   scenario fixture tree under practices/consumer-proof/scenarios/**. That
#   is a SILENT FALSE-CLEAN: an unblocked violating variant would be
#   credited as "passing" with zero indication anything was skipped.
#
#   The fix: lint_json() MUST run with cwd = $REPO_ROOT (an ancestor of every
#   fixture tree in this repo) AND resolve every target to an absolute path
#   AND pin --config explicitly to $REACT_DIR/eslint.config.mjs, so the
#   base-path check always succeeds regardless of the fixture's location.
#
# This script does NOT hand-copy the fix — it EXTRACTS the live lint_json()
# function body straight out of the current run-consumer-proof.sh and
# exercises it against a genuinely out-of-tree violating fixture (this
# directory, a sibling of $REACT_DIR, not a descendant). If a future edit to
# run-consumer-proof.sh ever drops the --config pin, this script goes RED —
# either because the extracted function no longer mentions --config
# (structural gate) or because the behavioral re-run silently "passes" a
# violating fixture again (behavioral gate).
#
# Usage: bash practices/consumer-proof/engine/fixtures/harness-config-pin/prove-config-pin.sh
# Exit:  0 = the pin holds (fix present + behaviorally effective)
#        1 = REGRESSION — the pin was removed or is no longer effective
#        2 = ENVIRONMENT ERROR (node/npm missing, deps not installed, etc.)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"
HARNESS="$REPO_ROOT/practices/consumer-proof/run-consumer-proof.sh"
REACT_DIR="$REPO_ROOT/practices/consumer-proof/react"

fail() { echo "FAIL: $1"; exit 1; }
enverr() { echo "ENVERR: $1"; exit 2; }

[ -f "$HARNESS" ] || enverr "harness script not found: $HARNESS"
[ -d "$REACT_DIR/node_modules" ] || enverr "react deps not installed at $REACT_DIR (run: cd $REACT_DIR && npm install)"
command -v node >/dev/null 2>&1 || enverr "node not on PATH"
command -v npx >/dev/null 2>&1 || enverr "npx not on PATH"

# ── extract the LIVE lint_json() function body from the real harness file ───
FUNC_FILE="$(mktemp "${TMPDIR:-/tmp}/axcproof-lintjson.XXXXXX")"
LINT_OUT="$(mktemp "${TMPDIR:-/tmp}/axcproof-lintout.XXXXXX")"
LINT_ERR="$(mktemp "${TMPDIR:-/tmp}/axcproof-linterr.XXXXXX")"
OUT_OF_TREE_FIXTURE="$SCRIPT_DIR/out-of-tree-violating.tsx"
trap 'rm -f "$FUNC_FILE" "$LINT_OUT" "$LINT_ERR" "$OUT_OF_TREE_FIXTURE"' EXIT
awk '/^lint_json\(\) \{/{flag=1} flag{print} /^\}/{if(flag){exit}}' "$HARNESS" > "$FUNC_FILE"

[ -s "$FUNC_FILE" ] || fail "could not extract lint_json() from $HARNESS (function renamed/removed?)"

# STRUCTURAL GATE — the extracted function must still pin --config.
if ! grep -qF -- '--config' "$FUNC_FILE"; then
    echo "  extracted lint_json():"
    sed 's/^/    /' "$FUNC_FILE"
    fail "lint_json() no longer pins --config — the out-of-tree false-clean regression is back"
fi

# ── behavioral gate: run the LIVE function against a genuinely out-of-tree
# violating fixture (this directory is a SIBLING of $REACT_DIR, not nested
# under it — exactly the shape a future out-of-tree scenario would have). ──
cat > "$OUT_OF_TREE_FIXTURE" <<'TSX'
// VIOLATING — ax/no-array-mutate-on-state (deliberately placed OUTSIDE
// $REACT_DIR to reproduce the ESLint 9 flat-config base-path false-clean).
'use client'
import { useState } from 'react'

export default function TodoList() {
  const [items, setItems] = useState<string[]>([])

  function add(next: string) {
    items.push(next) // mutateMethodOnState
    setItems(items)
  }

  return (
    <ul>
      {items.map((i) => (
        <li key={i}>{i}</li>
      ))}
    </ul>
  )
}
TSX

# shellcheck disable=SC1090
source "$FUNC_FILE"
LINT_RC=0
lint_json "$OUT_OF_TREE_FIXTURE"

if [ "$LINT_RC" -ne 1 ]; then
    echo "  eslint output:"
    sed 's/^/    /' "$LINT_OUT" "$LINT_ERR" 2>/dev/null
    fail "out-of-tree violating fixture was NOT blocked (lint_json exit=$LINT_RC, expected 1) — false-clean regression reproduced"
fi
if ! grep -qF -- 'ax/no-array-mutate-on-state' "$LINT_OUT"; then
    echo "  eslint json output:"
    sed 's/^/    /' "$LINT_OUT"
    fail "lint_json exited 1 but the intended signature ax/no-array-mutate-on-state is absent (wrong-cause block, not a real proof)"
fi

echo "PASS: lint_json() in run-consumer-proof.sh still pins --config, and correctly"
echo "      blocks (exit 1, signature ax/no-array-mutate-on-state) a violating"
echo "      fixture placed OUTSIDE \$REACT_DIR — the base-path false-clean does"
echo "      NOT reproduce with the current harness."

# ── supplementary (non-gating) demonstration of the vulnerability class the
# fix guards against: the UNPINNED invocation (no --config) DOES silently
# pass the very same fixture. Informational only — never fails the proof,
# since it documents upstream ESLint behavior, not this repo's contract. ──
echo
echo "── supplementary: reproducing the UNPINNED (pre-fix) false-clean for context ──"
UNPINNED_OUT="$(mktemp "${TMPDIR:-/tmp}/axcproof-unpinned.XXXXXX")"
unpinned_rc=0
( cd "$REACT_DIR" && npx eslint --format json "$OUT_OF_TREE_FIXTURE" ) >"$UNPINNED_OUT" 2>&1 || unpinned_rc=$?
if [ "$unpinned_rc" -eq 0 ] && grep -qi 'outside of base path' "$UNPINNED_OUT" 2>/dev/null; then
    echo "  confirmed: unpinned invocation silently exits 0 (\"outside of base path\")"
    echo "  — this is exactly what --config prevents."
else
    echo "  (unpinned invocation did not reproduce the base-path false-clean on this"
    echo "   eslint version; not gating — the pinned function above is the contract.)"
fi
rm -f "$UNPINNED_OUT"

exit 0
