#!/usr/bin/env bash
# practices/evals/completion_checklist_recency_guard.sh — R25 49th hard guard.
#
# Closes the R25 catalog enforcement-loop gap: the catalog can declare a
# verify-completion.sh contract, but without auditing recency an AI agent could
# silently skip the contract and still ship work. This guard inspects the
# .ax-verify/runs.jsonl audit log written by verify-completion.sh and verifies
# that the LATEST log line:
#   1. exists at all
#   2. references the current HEAD sha (i.e. verify-completion ran AFTER the
#      last commit, not before)
#   3. has exit == 0 (last verify-completion run was PASS, no outstanding RED)
#
# Rule of construction (R25 brief): "verify-completion.sh 실행 안 한 채로 commit
# 하면 trip" — so this guard is what backstops pre-commit / pre-push hook
# coverage. The guard does NOT run verify-completion itself (cycle); it ONLY
# audits the artifact verify-completion produces.
#
# Exit codes: 0 PASS · 1 violation · 2 usage error.
#
# Usage:
#   bash practices/evals/completion_checklist_recency_guard.sh           # live repo
#   bash practices/evals/completion_checklist_recency_guard.sh --fixtures
#   bash practices/evals/completion_checklist_recency_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "completion_checklist_recency_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/completion_checklist_recency"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "completion_checklist_recency_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2
    fi

    pass=0
    fail=0

    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [completion_checklist_recency/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [completion_checklist_recency/$(basename "$sub")] — expected exit 0 on PASS fixture"
            fail=$((fail + 1))
        fi
    done

    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "FAIL [completion_checklist_recency/$(basename "$sub")] — expected exit 1 on FAIL fixture"
            fail=$((fail + 1))
        else
            echo "PASS [completion_checklist_recency/$(basename "$sub")]"
            pass=$((pass + 1))
        fi
    done

    echo ""
    echo "completion_checklist_recency_guard: fixtures $pass PASS / $fail FAIL"
    if [ "$fail" -gt 0 ]; then exit 1; fi
    exit 0
fi

# ── Live mode (or --root override) ────────────────────────────────────────────
SCAN_ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
if [ ! -d "$SCAN_ROOT" ]; then
    echo "completion_checklist_recency_guard: root not found: $SCAN_ROOT" >&2
    exit 2
fi

python3 - "$SCAN_ROOT" <<'PYEOF'
import sys
import pathlib
import json
import os
import subprocess
import datetime
import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

root = pathlib.Path(sys.argv[1])
audit_log = root / ".ax-verify" / "runs.jsonl"
expected_head_file = root / ".ax-verify" / "expected_head.txt"

ts = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

def emit_fail(code, msg):
    print(f"VIOLATION [completion_checklist_recency]: {code} — {msg}")
    print(f'{{"signal":"completion_checklist.recency_fail","code":"{code}","ts":"{ts}"}}')
    sys.exit(1)

# 1. Audit log must exist with at least one line.
if not audit_log.is_file():
    emit_fail(
        "AUDIT_LOG_MISSING",
        f"{audit_log.relative_to(root)} not found. "
        "Run `bash practices/scripts/verify-completion.sh` at least once after every commit. "
        "Iron Law (R25): no audit line ⇒ task NOT done."
    )

lines = [l for l in audit_log.read_text().splitlines() if l.strip()]
if not lines:
    emit_fail(
        "AUDIT_LOG_EMPTY",
        f"{audit_log.relative_to(root)} exists but contains no entries."
    )

# 2. Parse latest entry; must be valid JSON with required keys.
try:
    latest = json.loads(lines[-1])
except json.JSONDecodeError as e:
    emit_fail("AUDIT_LINE_MALFORMED", f"latest line is not valid JSON: {e}")

required = ("ts", "head_sha", "exit", "pass", "warn_advisory", "hard_fail", "skip")
missing = [k for k in required if k not in latest]
if missing:
    emit_fail("AUDIT_LINE_INCOMPLETE", f"latest line missing keys: {missing}")

# 3. head_sha must match current HEAD (audit ran AFTER the last commit).
#    Fixture mode: if expected_head.txt is present, use it; else read .git HEAD.
expected_head = None
if expected_head_file.is_file():
    expected_head = expected_head_file.read_text().strip()
else:
    # Try git in this root.
    try:
        out = subprocess.check_output(
            ["git", "-C", str(root), "rev-parse", "HEAD"],
            stderr=subprocess.DEVNULL,
        ).decode().strip()
        expected_head = out
    except Exception:
        expected_head = None

if expected_head is None:
    # Not a git repo and no fixture marker — treat as PASS (e.g. tarball release).
    print(f'{{"signal":"completion_checklist.recency_skip","reason":"no_git_no_fixture","ts":"{ts}"}}')
    sys.exit(0)

if latest["head_sha"] != expected_head:
    emit_fail(
        "AUDIT_STALE_HEAD",
        f'latest audit was for head_sha={latest["head_sha"][:12]} but current HEAD is '
        f'{expected_head[:12]}. Re-run `bash practices/scripts/verify-completion.sh` '
        f'after every commit.'
    )

# 4. Latest entry must be PASS (exit == 0, hard_fail == 0).
if latest["exit"] != 0 or latest["hard_fail"] > 0:
    emit_fail(
        "AUDIT_LAST_RUN_FAILED",
        f'latest verify-completion.sh run was FAIL (exit={latest["exit"]}, '
        f'hard_fail={latest["hard_fail"]}). Iron Law (R25): task not done until '
        f'verify-completion exits 0.'
    )

# All conditions satisfied.
print(f'{{"signal":"completion_checklist.recency_pass","head_sha":"{expected_head[:12]}","ts":"{ts}"}}')
sys.exit(0)
PYEOF
