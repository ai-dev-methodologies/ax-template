#!/usr/bin/env bash
# ax-prove-gate-blocks-agent.sh — the project's FALSIFICATION TEST.
#
# The catalog's headline thesis is "gates mechanically constrain AI agents":
# an agent writes rule-violating code, an enforced guard BLOCKS it, the agent
# corrects, and the loop is recorded. Until now that loop had fired ZERO times
# on disk for actor=agent (every .ax-ledger event was the maintainer's own).
#
# This script produces the missing proof DETERMINISTICALLY and self-contained:
#   1. An "agent" writes a DemoController whose @ExceptionHandler returns
#      Map<String,String> (a real RFC-9457 violation) into a throwaway mktemp
#      tree — the real backend is never touched.
#   2. The REAL guard (controller_problemdetail_guard.sh --root $TMP) runs and
#      MUST exit 1 (BLOCK). We log a `violation ... actor=agent`.
#   3. The "agent" corrects the return type to ProblemDetail; the SAME guard
#      MUST now exit 0 (PASS). We log a `progress ... actor=agent`.
#   4. The script exits 0 only if the block→pass transition held.
#
# It is vision-compatible: maintainer-run, opt-in, no fork-receiver CI is
# forced. It writes only to the gitignored .ax-ledger trace plus a temp tree.
#
# Exit codes: 0 — block-then-pass held (thesis proven at HEAD);
#             1 — the guard did not block the violation, or did not pass the fix.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GUARD="$REPO_ROOT/practices/evals/controller_problemdetail_guard.sh"
LOG="$REPO_ROOT/practices/scripts/ax-ledger-log.sh"

LEDGER="${AX_LEDGER_DIR:-$REPO_ROOT/.ax-ledger}/events.jsonl"
# NOTE: `grep -c` PRINTS "0" and exits 1 on zero matches — a bare `grep -c ... || echo 0`
# would emit "0\n0" and break downstream arithmetic (BACKLOG P3-46 sibling site).
agent_events() {
    local n
    [ -f "$LEDGER" ] || { echo 0; return; }
    n="$(grep -c '"actor": "agent"' "$LEDGER" 2>/dev/null || true)"
    echo "${n:-0}"
}
BEFORE="$(agent_events)"

# BACKLOG P2-67: mktemp is resolved to an ABSOLUTE path (a PATH-earlier shim can return a
# directory the attacker owns — this catalog ships exactly such a shim in
# resume_provenance_guard.sh) and the returned directory is verified: a real directory, owned by
# this euid, with no group/other write. This harness builds SANDBOXES in it and runs gates there.
_AX_MK="$(PATH=/usr/bin:/bin:/usr/local/bin command -v mktemp 2>/dev/null || true)"
case "$_AX_MK" in /*) ;; *) echo "$(basename "$0"): mktemp did not resolve to an absolute path" >&2; exit 2 ;; esac
TMP="$("$_AX_MK" -d "${TMPDIR:-/tmp}/ax-prove.XXXXXXXX")"
_AX_ST="$(stat -f '%u %Lp' "$TMP" 2>/dev/null)" || _AX_ST=""
[ -n "$_AX_ST" ] || _AX_ST="$(stat -c '%u %a' "$TMP" 2>/dev/null)" || _AX_ST=""
case "$_AX_ST" in
    [0-9]*" "[0-7][0-7][0-7]|[0-9]*" "[0-7][0-7][0-7][0-7]) ;;
    *) echo "$(basename "$0"): the owner/mode of $TMP could not be read (stat said '${_AX_ST:-<nothing>}')" >&2; exit 2 ;;
esac
if [ ! -d "$TMP" ] || [ -L "$TMP" ] || [ "${_AX_ST%% *}" != "${EUID:-$(id -u)}" ] \
   || [ $(( 8#${_AX_ST##* } & 8#22 )) -ne 0 ]; then
    echo "$(basename "$0"): refusing a temp dir that is not a private directory owned by this uid ($TMP, stat '$_AX_ST')" >&2
    exit 2
fi
trap 'rm -rf "$TMP"' EXIT

PKG="$TMP/backend/src/main/java/com/ax/template/authblueprint/demo"
mkdir -p "$PKG"
CTRL="$PKG/DemoController.java"

# ── 1. the agent writes a VIOLATING @ExceptionHandler (returns Map, not RFC 9457) ──
cat > "$CTRL" <<'JAVA'
package com.ax.template.authblueprint.demo;

import java.util.Map;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @ExceptionHandler(IllegalStateException.class)
    public Map<String, String> handle(IllegalStateException ex) {
        return Map.of("error", ex.getMessage());
    }
}
JAVA

echo "[1] agent wrote a violating @ExceptionHandler (returns Map<String,String>)"

# ── 2. the REAL guard must BLOCK it (exit 1) ──
set +e
bash "$GUARD" --root "$TMP" >/dev/null 2>&1
blocked_rc=$?
set -e
if [ "$blocked_rc" -ne 1 ]; then
    echo "[FAIL] guard did NOT block the violation (exit $blocked_rc, expected 1) — thesis unproven"
    exit 1
fi
echo "[2] guard BLOCKED it (exit 1) ✓"
bash "$LOG" violation gate=controller-problemdetail rule=RFC9457 actor=agent severity=block \
    detail="agent wrote non-ProblemDetail @ExceptionHandler (Map<String,String>); guard BLOCKED" >/dev/null 2>&1 || true

# ── 3. the agent CORRECTS the return type to ProblemDetail ──
cat > "$CTRL" <<'JAVA'
package com.ax.template.authblueprint.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handle(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
JAVA

echo "[3] agent corrected the return type to ProblemDetail"

# ── 4. the SAME guard must now PASS (exit 0) ──
set +e
bash "$GUARD" --root "$TMP" >/dev/null 2>&1
fixed_rc=$?
set -e
if [ "$fixed_rc" -ne 0 ]; then
    echo "[FAIL] guard did NOT pass the corrected code (exit $fixed_rc, expected 0)"
    exit 1
fi
echo "[4] guard PASSED the correction (exit 0) ✓"
bash "$LOG" progress gate=controller-problemdetail outcome=pass actor=agent \
    detail="agent corrected @ExceptionHandler to ProblemDetail; guard PASS after block" >/dev/null 2>&1 || true

# ── 5. self-verify the proof actually landed on disk (logging must not fail silently) ──
AFTER="$(agent_events)"
if [ "$AFTER" -lt "$((BEFORE + 2))" ]; then
    echo "[FAIL] the block→correct pair was NOT recorded to $LEDGER (before=$BEFORE after=$AFTER)"
    exit 1
fi

echo
echo "PROVEN: an agent's rule-violating change was mechanically BLOCKED, then PASSED after correction,"
echo "and the violation+progress pair is recorded for actor=agent at HEAD ($(git -C "$REPO_ROOT" rev-parse --short HEAD))."
echo "  ledger: $LEDGER  (actor=agent events: $BEFORE → $AFTER)"
