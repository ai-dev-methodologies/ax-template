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
#      degraded placeholder),
#   6. that tree was CLEAN at BOTH endpoints — i.e. it was the committed tree of
#      head_sha when the run started AND when its last step finished, and
#   7. it was the SAME tree throughout (tree_stable, with the two endpoints
#      recorded so this guard verifies start == end instead of trusting a flag), and
#   8. it ratcheted against the RELEASE THE REMOTE ACTUALLY HAS — anchor_sha must equal the
#      sha git hands the pre-push hook for the ref being pushed (--expect-anchor-sha).
#
# Why 8 (cross-family review P1-X, 2026-07-30 — the ref is not the tree):
#   Two guards in an R25 run ratchet against "the previous release", which they resolve from
#   refs/remotes/origin/main. THAT IS AN ORDINARY LOCAL REF. `git update-ref` aims it at a
#   synthetic commit whose tree merely DROPS the ratcheting files; both ratchets then take
#   their "first-release bootstrap" skip and R25 passes on a downgrade. Checks 5-7 cannot see
#   it — a ref is not part of the working tree, so no fingerprint covers it. The pre-push hook
#   is the one place holding an authoritative answer (git reads the remote's advertisement, not
#   a local ref), so it passes that sha here and the recorded anchor_sha must equal it.
#   HONEST LIMIT: the binding is supplied only for the push whose REMOTE ref is the anchor's
#   branch (refs/heads/main). A feature-branch push does not advance origin/main and supplies
#   nothing, so check 8 is a no-op there by design.
#
# Why 7 (cross-family review P1, 2026-07-29 — the run is not an instant):
#   head/fingerprint/cleanliness used to be captured ONCE before the first step, and the
#   audit line written after the last one — a measured 2,225-second window on a real full
#   run. Start clean at a HEAD that fails a later step, make the uncommitted fix while an
#   early step runs, let the later step pass on it, revert after the run: every recorded
#   value still says "clean tree at H". verify-completion now samples at every step boundary
#   and at the end; this guard requires all of it to agree.
#   HONEST LIMIT: sampling is at step boundaries, so a change made AND undone inside one
#   step is unobserved. The exposure is one step wide, not zero.
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
#   bash practices/evals/completion_checklist_recency_guard.sh --expect-anchor-sha SHA
#       the audit's recorded anchor_sha must equal SHA — the sha git hands the pre-push hook
#       for the ref being pushed, taken from the REMOTE's advertisement. Passed by the hook
#       only for the anchor branch (refs/heads/main); a ZERO sha (new remote branch) is a
#       no-op. Fixtures supply it via .ax-verify/expected_anchor.txt.
#   bash practices/evals/completion_checklist_recency_guard.sh --fixtures
#   bash practices/evals/completion_checklist_recency_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""
EXPECT_SHA=""
EXPECT_ANCHOR_SHA=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --expect-sha) EXPECT_SHA="$2"; shift 2 ;;
        --expect-sha=*) EXPECT_SHA="${1#--expect-sha=}"; shift ;;
        --expect-anchor-sha) EXPECT_ANCHOR_SHA="$2"; shift 2 ;;
        --expect-anchor-sha=*) EXPECT_ANCHOR_SHA="${1#--expect-anchor-sha=}"; shift ;;
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

python3 - "$SCAN_ROOT" "$EXPECT_SHA" "$EXPECT_ANCHOR_SHA" <<'PYEOF'
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
expect_anchor_arg = sys.argv[3] if len(sys.argv) > 3 else ""
ZERO_SHA = "0" * 40
audit_log = root / ".ax-verify" / "runs.jsonl"
expected_head_file = root / ".ax-verify" / "expected_head.txt"
# Fixture seam for check 9, mirroring expected_head.txt: a fixture that drops this file is
# asserting "the remote advertises THIS sha for the ref being pushed", which is what the
# pre-push hook supplies on a real push. Without it, check 9 is a no-op — so the pre-existing
# fixtures are untouched and the new ones are the only ones that exercise the anchor binding.
expected_anchor_file = root / ".ax-verify" / "expected_anchor.txt"
if expected_anchor_file.is_file():
    expect_anchor_arg = expected_anchor_file.read_text().strip()

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
#    BOTH endpoints are required, not just the opening one: the start value is measured
#    before the first step and the line is written after the last, so a tree that was clean
#    at the start says nothing about the tree the later steps actually verified.
tree_clean_both = (latest.get("tree_clean") is True
                   and latest.get("tree_clean_end") is True)
if not tree_clean_both:
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
        f'(tree_clean={latest.get("tree_clean")!r} at the start, '
        f'tree_clean_end={latest.get("tree_clean_end")!r} when the last step finished), so it '
        f'certifies a tree that differs from the commit being pushed — an uncommitted change '
        f'that makes the run pass does not travel with the push. BOTH endpoints must be clean: '
        f'the start value is measured before the first step, so on its own it says nothing '
        f'about the tree the later steps verified (a legacy line without tree_clean_end is '
        f'refused for exactly that reason). Commit (or stash, or .gitignore) everything, then '
        f're-run `bash practices/scripts/verify-completion.sh` at the commit you are pushing. '
        f'Local iteration is unaffected: only push eligibility requires a clean tree.{hint}'
    )

# 8. The tree must have been the SAME tree throughout. Checks 6+7 are endpoints, and a run is
#    not an instant — a full run here is tens of minutes wide. An edit made after the start
#    snapshot and undone before the closing one leaves both endpoints identical and perfectly
#    clean while the steps in between verified code no commit contains. verify-completion
#    therefore samples the tree at every step boundary and reports whether they all agreed;
#    the endpoints are recorded too so this check VERIFIES the relation (start == end) rather
#    than trusting tree_stable alone. Fail closed on absent fields (a pre-sampling producer).
#    HONEST LIMIT (inherited from the producer): sampling is at step boundaries, so a change
#    made and undone WITHIN one step is still unobserved. The window is one step, not zero.
head_end = latest.get("head_sha_end")
fp_end = latest.get("tree_fingerprint_end")
samples = latest.get("tree_samples")
#    The fingerprint comparison deliberately does NOT re-check that fp_end is a string:
#    check 6 already guarantees tree_fp is a usable one, so equality carries the type. Written
#    this way so that neutering check 6 (fixture_kill_proof [87] does exactly that) fails its
#    fixture through check 6's ABSENCE rather than through this check firing on the side —
#    otherwise that fixture would look vacuous while the real coverage question is hidden.
endpoints_agree = (head_end == latest["head_sha"] and head_end == expected_head
                   and fp_end == tree_fp)
tree_settled = (latest.get("tree_stable") is True and endpoints_agree
                and isinstance(samples, int) and samples >= 2)
if not tree_settled:
    emit_fail(
        "AUDIT_TREE_MUTATED_MIDRUN",
        f'the audit line does not show a settled tree for the WHOLE run '
        f'(tree_stable={latest.get("tree_stable")!r}, samples={samples!r}, '
        f'head_sha={latest["head_sha"][:12]} → head_sha_end={str(head_end)[:12]}, '
        f'tree={str(tree_fp)[:12]} → tree_end={str(fp_end)[:12]}). A run spans tens of '
        f'minutes: an uncommitted edit made after it started and reverted before it finished '
        f'leaves both endpoints looking pristine while the steps in between verified code the '
        f'commit does not contain. Re-run `bash practices/scripts/verify-completion.sh` at the '
        f'commit you are pushing and leave the tree alone until it finishes. (Missing fields '
        f'mean the line came from a producer that did not sample across the run — re-run.)'
    )

# 9. The run must have ratcheted against the RELEASE THE REMOTE ACTUALLY HAS.
#    (P1-X layer 3, cross-family review ROUND 3, 2026-07-30 — the ref is not the tree.)
#    Two guards in an R25 run ratchet against "the previous release", resolved from
#    refs/remotes/origin/main. That is an ORDINARY LOCAL REF: `git update-ref` aims it anywhere,
#    including at a synthetic commit whose tree merely DROPS the ratcheting files — at which
#    point every ratchet takes its "first-release bootstrap" skip and R25 passes on a downgrade.
#    Checks 5-8 above cannot see it, because a ref is not part of the working tree and the
#    fingerprint only hashes the tree.
#    The pre-push hook is the one place with an AUTHORITATIVE answer: git hands it the remote sha
#    from the remote's own advertisement, not from any local ref. So the hook passes that sha
#    here and the recorded anchor_sha must EQUAL it.
#    Fail-closed on a MISSING field: a line without anchor_sha came from a producer that did not
#    record which release it measured against, which is precisely the state the attack wants.
#    LEGITIMATE EXCEPTION, and it is stated in the failure text: the remote moved between the R25
#    run and the push (someone else pushed). The honest resolution is `git fetch` + re-run R25 —
#    the ratchet was measured against a release that is no longer the one being extended.
#    SCOPE, disclosed: the hook only supplies this for a push whose REMOTE ref is the anchor's
#    branch (refs/heads/main). Pushing a feature branch supplies nothing and this check is a
#    no-op there — correct, because origin/main is not what that push advances, and the check
#    fires on the push that actually publishes the release.
if expect_anchor_arg and expect_anchor_arg != ZERO_SHA:
    recorded_anchor = latest.get("anchor_sha")
    if not isinstance(recorded_anchor, str) or not recorded_anchor:
        emit_fail(
            "AUDIT_ANCHOR_UNBOUND",
            f'the latest audit line does not record which release the ratcheting guards measured '
            f'against (anchor_sha={recorded_anchor!r}). The remote advertises '
            f'{expect_anchor_arg[:12]} for this ref, and there is nothing to compare it to. '
            f'refs/remotes/origin/main is an ordinary local ref that `git update-ref` can aim at '
            f'a synthetic commit lacking the ratcheting files, which turns every ratchet into a '
            f'bootstrap skip — so an unrecorded anchor is refused rather than assumed honest. '
            f'Re-run `bash practices/scripts/verify-completion.sh` at the commit you are pushing.'
        )
    if recorded_anchor != expect_anchor_arg:
        emit_fail(
            "AUDIT_ANCHOR_FORGED",
            f'the latest R25 run ratcheted against anchor_sha={recorded_anchor[:12]} '
            f'(kind={latest.get("anchor_kind")!r}), but the REMOTE advertises '
            f'{expect_anchor_arg[:12]} for the ref being pushed. git took that second value from '
            f'the remote itself, so it is authoritative; the first came from a local ref that '
            f'anything can rewrite.\n'
            f'  · If refs/remotes/origin/main was pointed somewhere else during the run, this is '
            f'the forgery this check exists for: the guards ratcheted against a commit the remote '
            f'does not have.\n'
            f'  · If the remote simply MOVED since your run (someone else pushed), that is the '
            f'legitimate case and the honest resolution is the same: `git fetch origin` and '
            f're-run `bash practices/scripts/verify-completion.sh`. The ratchet was measured '
            f'against a release that is no longer the one you are extending.'
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
