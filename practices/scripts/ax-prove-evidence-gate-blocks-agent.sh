#!/usr/bin/env bash
# ax-prove-evidence-gate-blocks-agent.sh — falsification test for the EVIDENCE surface.
#
# Sibling of ax-prove-gate-blocks-agent.sh. That proof exercises the RFC-9457
# controller gate (run-all-guards surface); THIS one exercises the catalog's #1
# anti-hallucination gate — evidence_guard, the flagship of the pre-commit
# commit-blocking surface (BACKLOG P2-3 surface-coverage map).
#
# The headline thesis is "gates mechanically constrain AI agents". The single
# highest-stakes way an AI agent breaks the catalog is by INVENTING a rule with
# no external anchor (a fabricated/placeholder `evidence:` block). This proof
# shows that fabrication is mechanically BLOCKED, then PASSES once the agent
# anchors the rule to a real source — deterministically and self-contained:
#   1. An "agent" writes a rule whose evidence is a _template.md placeholder
#      citation + empty url (a real fabrication) into a throwaway mktemp catalog
#      — the real practices/ catalog is never touched.
#   2. The REAL guard (evidence_guard.sh <tmp-catalog>) runs and MUST exit 1
#      (BLOCK). We log a `violation ... actor=agent`.
#   3. The "agent" corrects it to a real external citation + URL (RFC 9457); the
#      SAME guard MUST now exit 0 (PASS). We log a `progress ... actor=agent`.
#   4. The script exits 0 only if the block→pass transition held AND the pair
#      landed on disk (logging must not fail silently).
#
# Vision-compatible: maintainer-run, opt-in, no fork-receiver CI is forced. It
# writes only to the gitignored .ax-ledger trace plus a temp tree.
#
# Exit codes: 0 — block-then-pass held (evidence thesis proven at HEAD);
#             1 — the guard did not block the fabrication, or did not pass the fix.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GUARD="$REPO_ROOT/practices/evals/evidence_guard.sh"
LOG="$REPO_ROOT/practices/scripts/ax-ledger-log.sh"

LEDGER="${AX_LEDGER_DIR:-$REPO_ROOT/.ax-ledger}/events.jsonl"
# NOTE: `grep -c` PRINTS "0" and exits 1 on zero matches — a bare `grep -c ... || echo 0`
# would emit "0\n0" and break the [ -lt ] arithmetic below (BACKLOG P3-46).
agent_events() {
    local n
    [ -f "$LEDGER" ] || { echo 0; return; }
    n="$(grep -c '"actor": "agent"' "$LEDGER" 2>/dev/null || true)"
    echo "${n:-0}"
}
BEFORE="$(agent_events)"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# An isolated single-rule catalog. evidence_guard takes an absolute path as a
# catalog-dir override and checks rules/*.md there (its templates/ walk still
# targets the real repo, which passes — only the rule check is under test).
RULES="$TMP/practices/rules"
mkdir -p "$RULES"
RULE="$RULES/demo-fabricated.md"

# ── 1. the agent writes a rule with FABRICATED evidence (placeholder + empty url) ──
cat > "$RULE" <<'MD'
---
rule_id: DEMO-FABRICATED-001
evidence:
  - source_type: external
    citation: "(replace with the standard / docs you actually consulted)"
    url: ""
---

# Demo rule (fabricated evidence)

An agent invented this rule but never anchored it to a real external source.
MD

echo "[1] agent wrote a rule with a placeholder citation + empty url (fabricated evidence)"

# ── 2. the REAL guard must BLOCK it (exit 1) ──
set +e
bash "$GUARD" "$TMP/practices" >/dev/null 2>&1
blocked_rc=$?
set -e
if [ "$blocked_rc" -ne 1 ]; then
    echo "[FAIL] guard did NOT block the fabrication (exit $blocked_rc, expected 1) — evidence thesis unproven"
    exit 1
fi
echo "[2] guard BLOCKED it (exit 1) ✓"
bash "$LOG" violation gate=evidence-anchoring rule=EVIDENCE actor=agent severity=block \
    detail="agent wrote a rule with placeholder citation + empty url; evidence_guard BLOCKED" >/dev/null 2>&1 || true

# ── 3. the agent CORRECTS it to a real external citation + URL ──
cat > "$RULE" <<'MD'
---
rule_id: DEMO-FABRICATED-001
evidence:
  - source_type: external
    citation: "RFC 9457: Problem Details for HTTP APIs (IETF, 2023), Section 3 — Members of a Problem Details Object"
    url: "https://www.rfc-editor.org/rfc/rfc9457#section-3"
---

# Demo rule (anchored evidence)

Now anchored to a real external citation + URL.
MD

echo "[3] agent corrected the evidence to a real external citation + URL"

# ── 4. the SAME guard must now PASS (exit 0) ──
set +e
bash "$GUARD" "$TMP/practices" >/dev/null 2>&1
fixed_rc=$?
set -e
if [ "$fixed_rc" -ne 0 ]; then
    echo "[FAIL] guard did NOT pass the corrected rule (exit $fixed_rc, expected 0)"
    exit 1
fi
echo "[4] guard PASSED the correction (exit 0) ✓"
bash "$LOG" progress gate=evidence-anchoring outcome=pass actor=agent \
    detail="agent anchored the rule to RFC 9457; evidence_guard PASS after block" >/dev/null 2>&1 || true

# ── 5. self-verify the proof actually landed on disk (logging must not fail silently) ──
AFTER="$(agent_events)"
if [ "$AFTER" -lt "$((BEFORE + 2))" ]; then
    echo "[FAIL] the block→correct pair was NOT recorded to $LEDGER (before=$BEFORE after=$AFTER)"
    exit 1
fi

echo
echo "PROVEN: an agent's fabricated-evidence rule was mechanically BLOCKED by evidence_guard, then PASSED"
echo "after the agent anchored it to a real source, and the violation+progress pair is recorded for"
echo "actor=agent at HEAD ($(git -C "$REPO_ROOT" rev-parse --short HEAD))."
echo "  ledger: $LEDGER  (actor=agent events: $BEFORE → $AFTER)"
