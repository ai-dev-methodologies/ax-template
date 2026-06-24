#!/usr/bin/env bash
# FIXTURE proof script — a faithful, self-contained miniature of the real
# practices/scripts/ax-prove-evidence-gate-blocks-agent.sh. It TOGGLES correctly:
# fabricated evidence (placeholder + empty url) -> BLOCK -> real citation -> PASS,
# with the same 'blocked_rc -ne 1' fail-guard and a reference to evidence_guard.sh.
# agent_block_proof_guard's pass fixture asserts this proof toggles and is non-vacuous.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GUARD="$REPO_ROOT/practices/evals/evidence_guard.sh"

LEDGER="${AX_LEDGER_DIR:-$REPO_ROOT/.ax-ledger}/events.jsonl"
mkdir -p "$(dirname "$LEDGER")"
log_agent() { printf '{"kind": "%s", "actor": "agent"}\n' "$1" >> "$LEDGER"; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
RULES="$TMP/practices/rules"
mkdir -p "$RULES"
RULE="$RULES/demo.md"

# 1. agent writes a rule with FABRICATED evidence (placeholder citation + empty url)
printf -- '---\nevidence:\n  - source_type: external\n    citation: "(replace with the standard / docs you actually consulted)"\n    url: ""\n---\nbody\n' > "$RULE"

# 2. real-shaped guard must BLOCK (exit 1)
set +e
bash "$GUARD" "$TMP/practices" >/dev/null 2>&1
blocked_rc=$?
set -e
if [ "$blocked_rc" -ne 1 ]; then
    echo "[FAIL] guard did NOT block (exit $blocked_rc, expected 1)"
    exit 1
fi
log_agent violation

# 3. agent corrects to a real external citation + url
printf -- '---\nevidence:\n  - source_type: external\n    citation: "RFC 9457: Problem Details, Section 3"\n    url: "https://www.rfc-editor.org/rfc/rfc9457#section-3"\n---\nbody\n' > "$RULE"

# 4. same guard must now PASS (exit 0)
set +e
bash "$GUARD" "$TMP/practices" >/dev/null 2>&1
fixed_rc=$?
set -e
if [ "$fixed_rc" -ne 0 ]; then
    echo "[FAIL] guard did NOT pass the fix (exit $fixed_rc, expected 0)"
    exit 1
fi
log_agent progress

echo "PROVEN (fixture): evidence block -> pass held"
exit 0
