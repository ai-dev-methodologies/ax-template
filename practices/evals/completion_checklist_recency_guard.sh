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
#   4. has full_run == true (a --step partial run writes full_run=false and
#      must NOT satisfy the completion contract — closes the dogfood-confirmed
#      P2 where a single trivial step was indistinguishable from a full PASS)
#   5. identifies the TREE it verified (tree_fingerprint present and not a
#      degraded placeholder), and
#   6. that tree was CLEAN — i.e. it was the committed tree of head_sha.
#
# Why 5+6 (cross-family review P1, 2026-07-29 — this needs no --resume at all):
#   head_sha does not identify the code that was verified. R25 is routinely run on a
#   dirty tree, so one head covers arbitrarily many trees, and a push ships the COMMIT:
#     1. committed HEAD H fails frontend lint
#     2. an UNCOMMITTED fix makes a full R25 run pass  → {head_sha:H, exit:0, full_run:true}
#     3. the fix is stashed/reverted — nothing re-runs, the audit line is untouched
#     4. `git push` H → this guard used to accept that line
#   The pushed tree was never verified; the verified tree was never pushed. Requiring the
#   evidence to come from the clean tree of the pushed sha closes it, because a clean tree
#   at sha S IS the tree of S.
#   Scope, deliberately: only PUSH eligibility tightens. Dirty-tree runs remain fully
#   usable locally and for `--resume` (verify-completion binds those by fingerprint), so
#   the iteration loop is unchanged — what changes is that shipping requires re-running
#   the contract once the work is committed.
#   HONEST LIMIT: git-ignored paths (node_modules/, build/) are outside git's model and
#   cannot be pinned by any of this; "clean" means identical to the commit in every path
#   git tracks or would track.
#
# Rule of construction (R25 brief): "verify-completion.sh 실행 안 한 채로 commit
# 하면 trip" — so this guard is what backstops pre-commit / pre-push hook
# coverage. The guard does NOT run verify-completion itself (cycle); it ONLY
# audits the artifact verify-completion produces.
#
# Exit codes: 0 PASS · 1 violation · 2 usage error.
#
# Usage:
#   bash practices/evals/completion_checklist_recency_guard.sh           # live repo (HEAD)
#   bash practices/evals/completion_checklist_recency_guard.sh --expect-sha SHA
#       audit must match SHA instead of the checkout's HEAD — used by the
#       pre-push hook to verify the EXACT sha being pushed (a non-checked-out
#       branch push must not ride on the current branch's audit)
#   bash practices/evals/completion_checklist_recency_guard.sh --fixtures
#   bash practices/evals/completion_checklist_recency_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""
EXPECT_SHA=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --expect-sha) EXPECT_SHA="$2"; shift 2 ;;
        --expect-sha=*) EXPECT_SHA="${1#--expect-sha=}"; shift ;;
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

python3 - "$SCAN_ROOT" "$EXPECT_SHA" <<'PYEOF'
import sys
import pathlib
import json
import os
import subprocess
import datetime
import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

root = pathlib.Path(sys.argv[1])
expect_sha_arg = sys.argv[2] if len(sys.argv) > 2 else ""
audit_log = root / ".ax-verify" / "runs.jsonl"
expected_head_file = root / ".ax-verify" / "expected_head.txt"

ts = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

def emit_fail(code, msg):
    # ax-ledger: a blocked push (no fresh verify at HEAD) IS a bypass attempt — record it (never fails)
    try:
        import subprocess
        subprocess.run(["bash", str(root / "practices" / "scripts" / "ax-ledger-log.sh"),
                        "bypass_attempt", "gate=completion_checklist_recency",
                        f"detail={code}", "severity=block"], capture_output=True, timeout=10)
    except Exception:
        pass
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

# 3. head_sha must match the sha under audit (audit ran AFTER the last commit).
#    Priority: fixture expected_head.txt > --expect-sha (pre-push per-ref
#    verification of the EXACT pushed sha) > this root's git HEAD.
expected_head = None
if expected_head_file.is_file():
    expected_head = expected_head_file.read_text().strip()
elif expect_sha_arg:
    expected_head = expect_sha_arg
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

# 5. Latest entry must be a FULL checklist run. A `--step <id>` partial run
#    writes full_run=false; legacy lines without the field are also rejected
#    (fail-closed) — any run at the current HEAD re-runs the new script anyway.
if latest.get("full_run") is not True:
    emit_fail(
        "AUDIT_PARTIAL_RUN",
        f'latest audit line is not a full checklist run (full_run='
        f'{latest.get("full_run")!r}). A --step partial run does not satisfy '
        f'the completion contract. Re-run `bash practices/scripts/'
        f'verify-completion.sh` with no --step filter.'
    )

# 6. The evidence must identify the TREE it verified. A line without a usable
#    fingerprint predates this binding, or came from a run that could not tell what it
#    was looking at ("nogit" = no git working tree, "unverifiable-*" = the fingerprint
#    helper failed). Fail closed: re-running the contract is always available.
tree_fp = latest.get("tree_fingerprint")
tree_fp_usable = (isinstance(tree_fp, str) and bool(tree_fp)
                  and tree_fp != "nogit" and not tree_fp.startswith("unverifiable-"))
if not tree_fp_usable:
    emit_fail(
        "AUDIT_TREE_UNIDENTIFIED",
        f'latest audit line does not identify the working tree it verified '
        f'(tree_fingerprint={tree_fp!r}). head_sha alone is satisfied by ANY tree at that '
        f'commit, so it cannot show that the code being pushed is the code that passed. '
        f'Re-run `bash practices/scripts/verify-completion.sh` at the commit you are pushing.'
    )

# 7. That tree must have been the COMMITTED tree of head_sha. This is the push-evidence
#    rule: a commit is what ships, so evidence gathered from a working tree that differs
#    from the commit is evidence about code the receiver will never get.
if latest.get("tree_clean") is not True:
    hint = ""
    try:
        dirty = subprocess.check_output(
            ["git", "-C", str(root), "status", "--porcelain", "-uall"],
            stderr=subprocess.DEVNULL,
        ).decode().splitlines()
        if dirty:
            shown = "\n    ".join(dirty[:10])
            more = f"\n    … and {len(dirty) - 10} more" if len(dirty) > 10 else ""
            hint = f"\n  Currently uncommitted/untracked here:\n    {shown}{more}"
    except Exception:
        pass
    emit_fail(
        "AUDIT_DIRTY_TREE_EVIDENCE",
        f'the latest verify-completion.sh run was performed on a DIRTY working tree '
        f'(tree_clean={latest.get("tree_clean")!r}), so it certifies a tree that differs '
        f'from the commit being pushed — an uncommitted change that makes the run pass does '
        f'not travel with the push. Commit (or stash, or .gitignore) everything, then re-run '
        f'`bash practices/scripts/verify-completion.sh` at the commit you are pushing. '
        f'Local iteration is unaffected: only push eligibility requires a clean tree.{hint}'
    )

# All conditions satisfied.
# Defensive slice: check 6 guarantees tree_fp is a usable string here, so this cannot be a
# second detection path. It is written this way so that neutering check 6 (fixture_kill_proof
# [87] does exactly that) fails the fixture through check 6's ABSENCE, not through a
# TypeError raised by this success line — an incidental crash would make the kill-proof
# report the fixture as vacuous while actually hiding the real coverage question.
fp_short = tree_fp[:12] if isinstance(tree_fp, str) else "unknown"
print(f'{{"signal":"completion_checklist.recency_pass","head_sha":"{expected_head[:12]}",'
      f'"tree":"clean","tree_fingerprint":"{fp_short}","ts":"{ts}"}}')
sys.exit(0)
PYEOF
