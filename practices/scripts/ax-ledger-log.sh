#!/usr/bin/env bash
# practices/scripts/ax-ledger-log.sh
# ───────────────────────────────────────────────────────────────────────────────
# ax-ledger CAPTURE — append one structured usage event to the per-project ledger.
#
# Every meaningful interaction with ax-template's enforcement leaves a trace here so it can be
# reviewed (복기) and turned into catalog improvement. The ledger is per-PROJECT (the forked repo) and
# tags every event with the acting user — so when a fork-receiver uses ax-template, progress AND
# rule-violating requests are all recorded, sliced by {project, user}, reviewable in the same session
# or later.
#
# Usage:
#   bash practices/scripts/ax-ledger-log.sh <kind> [key=value ...]
#
#   kind ∈
#     progress         — a step advanced (domain added, test{Domain} GREEN, spec item bound, …)
#     gate_run         — a verification gate ran (carry outcome=pass|fail, pass=, fail=)
#     violation        — an enforced rule/guard BLOCKED something (carry gate=, rule=, detail=)
#     bypass_attempt   — someone tried to skip a gate (--no-verify / --skip / manual override)
#     request_rejected — a request was refused because it would break an enforced rule/method
#
#   common keys: gate, rule, outcome, severity (info|warn|block), detail, actor (user|agent)
#
# Writes to $AX_LEDGER_DIR (default <repo>/.ax-ledger)/events.jsonl. Never fails the caller: a
# logging error must not block real work, so this script always exits 0.
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LEDGER_DIR="${AX_LEDGER_DIR:-$REPO_ROOT/.ax-ledger}"

kind="${1:-progress}"
shift 2>/dev/null || true

mkdir -p "$LEDGER_DIR" 2>/dev/null || { echo "[ax-ledger] cannot create $LEDGER_DIR — skipping"; exit 0; }

project="$(git -C "$REPO_ROOT" remote get-url origin 2>/dev/null || basename "$REPO_ROOT")"
user="$(git -C "$REPO_ROOT" config user.email 2>/dev/null || echo "unknown")"
head="$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo "none")"
ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

python3 - "$LEDGER_DIR/events.jsonl" "$ts" "$project" "$user" "$head" "$kind" "$@" <<'PY' 2>/dev/null || true
import sys, json, os
path, ts, project, user, head, kind, *kvs = sys.argv[1:]
ev = {"ts": ts, "project": project, "user": user, "head_sha": head, "kind": kind,
      "severity": "info", "reviewed": False}
for kv in kvs:
    if "=" in kv:
        k, v = kv.split("=", 1)
        ev[k] = v
with open(path, "a", encoding="utf-8") as f:
    f.write(json.dumps(ev, ensure_ascii=False) + "\n")
print(f"[ax-ledger] {kind} recorded ({ev.get('gate', ev.get('rule', '-'))}) → {os.path.relpath(path)}")
PY
exit 0
