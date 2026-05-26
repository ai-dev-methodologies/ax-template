#!/usr/bin/env bash
# practices/evals/dogfood_finding_expiry_trigger_guard.sh
# R85b (45th hard guard) — mechanises practices/rules/dogfood-finding-must-have-expiry-trigger.md
# (R85). For every entry in docs/dogfood-ledger/*.yaml whose
# `classification` is `scope_deferral`, the `finding` text MUST contain
# at least one expiry-trigger marker so the catalog can mechanically
# detect when a deferral should re-open.
#
# Accepted marker phrases (case-insensitive substring match in the
# `finding` field):
#   - "expiry trigger:"
#   - "re-opens when"
#   - "re-opens before"
#   - "reopens before"
#   - "reopens when"
#   - "defer until"
#   - "deferred until"
#   - "before the fork-receiver"
#   - "before the first"
#   - "before a "
#   - "before the cap"
#   - "before any "
#
# Real_bug entries are not checked (they are closed in the same wave).
# methodology_gap entries are also exempt (those should be addressed
# by changing the methodology, not by indefinite deferral).
#
# Exit codes:
#   0 — every scope_deferral finding has an explicit trigger marker.
#   1 — at least one scope_deferral lacks a trigger marker.
#   2 — usage / environment error (yaml missing, python3 missing).
#
# Usage:
#   bash practices/evals/dogfood_finding_expiry_trigger_guard.sh
#   bash practices/evals/dogfood_finding_expiry_trigger_guard.sh --root DIR
#
# Bash 3.2 compatible (no associative arrays, no ${var,,}).
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "dogfood_finding_expiry_trigger_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "cannot cd to $REPO_ROOT" >&2; exit 2; }

LEDGER_DIR="docs/dogfood-ledger"
[ ! -d "$LEDGER_DIR" ] && exit 0

if ! command -v python3 >/dev/null 2>&1; then
    echo "dogfood_finding_expiry_trigger_guard: python3 not in PATH (required for yaml parsing)" >&2
    exit 2
fi

# Python helper: load each ledger yaml, iterate findings, return violations.
python3 - "$LEDGER_DIR" <<'PY'
import pathlib
import sys

import yaml  # PyYAML — already a dependency for other guards


MARKERS = [
    "expiry trigger:",
    "re-opens when",
    "re-opens before",
    "reopens before",
    "reopens when",
    "defer until",
    "deferred until",
    "before the fork-receiver",
    "before the first",
    "before a ",
    "before the cap",
    "before any ",
]


def has_trigger(finding_text: str) -> bool:
    lower = finding_text.lower()
    return any(marker in lower for marker in MARKERS)


violations = []
ledger_dir = pathlib.Path(sys.argv[1])
for ledger_yaml in sorted(ledger_dir.glob("*.yaml")):
    try:
        doc = yaml.safe_load(ledger_yaml.read_text()) or {}
    except yaml.YAMLError as e:
        print(f"YAML parse error: {ledger_yaml}: {e}", file=sys.stderr)
        sys.exit(2)
    findings = doc.get("findings") or []
    for idx, f in enumerate(findings):
        if not isinstance(f, dict):
            continue
        classification = f.get("classification", "")
        if classification != "scope_deferral":
            continue
        finding_text = f.get("finding", "")
        if not isinstance(finding_text, str):
            continue
        if not has_trigger(finding_text):
            persona = f.get("persona", "??")
            preview = finding_text.split(":", 1)[0][:60]
            violations.append(
                f"{ledger_yaml}: finding[{idx}] persona={persona} "
                f"({preview}…) — no expiry-trigger marker"
            )

if violations:
    print(
        "VIOLATION: dogfood-ledger scope_deferral entries missing expiry-trigger markers (R85):",
        file=sys.stderr,
    )
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        "Accepted markers (any one): 'expiry trigger:', 're-opens when/before', "
        "'reopens when/before', 'defer until', 'deferred until', "
        "'before the fork-receiver', 'before the first', 'before a ', "
        "'before the cap', 'before any '.",
        file=sys.stderr,
    )
    print(
        f"dogfood_finding_expiry_trigger_guard: {len(violations)} violation(s) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)

print(
    "dogfood_finding_expiry_trigger_guard: every scope_deferral has an explicit expiry-trigger marker"
)
sys.exit(0)
PY
