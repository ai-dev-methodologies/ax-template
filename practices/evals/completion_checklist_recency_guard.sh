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
#   9. the anchor was the SAME COMMIT at both ends of the run (anchor_stable +
#      anchor_sha == anchor_sha_end) — the ref is writable while the guards run,
#  10. the line's FIELD SET is exactly what the committed writer emits (and no key is
#      duplicated), with the pin cross-checked against verify-completion.sh itself,
#  11. the recorded tree_fingerprint is RECOMPUTED from the tree and must match,
#  12. the repository carries no git REPLACEMENT REFS, and
#  13. the summary is corroborated by the run's own per-step ledger (.ax-verify/last_run.jsonl:
#      present, same head, same tree, no non-PASS step).
#
# Why 9-13 (cross-family review ROUND 4, 2026-07-30 — the ratchet's own runtime was
# unauthenticated; TD-2026-07-30-P1-anchor-runtime):
#   Checks 1-8 read the LINE and compare its fields to each other and to git. None of them asked
#   who WROTE the line, whether git was interpreting real objects, or whether the anchor ref held
#   still while the run happened. Each of those is an INPUT, and an input the gate trusts must be
#   authenticated AT THE MOMENT OF USE:
#     · 9  — the anchor ref can be aimed at a commit lacking the ratcheting files for exactly the
#            minutes the guards run (they bootstrap-skip) and restored before the line is written;
#            check 8 is satisfied by the honest recorded value, so only the producer can see it.
#     · 10 — `.ax-verify/runs.jsonl` is an ordinary text file. `echo '{…}' >>` with the pushed sha
#            and the advertised anchor satisfied every earlier check. Pinning the exact shape (and
#            refusing duplicate keys, which a lenient parser resolves in the writer's favour) makes
#            a hand-authored line reproduce the writer exactly or fail.
#     · 11 — the recorded fingerprint was never checked to BE a fingerprint: "x" passed check 5.
#     · 12 — `git replace` keeps shas identical and swaps the objects every git read sees.
#     · 13 — one line is one claim; the per-step ledger is a second artifact that must agree.
#   HONEST LIMIT, stated because the alternative is theater: none of this makes forgery
#   IMPOSSIBLE. Everything recomputable is also reproducible by an attacker who runs the same
#   commands. An HMAC would close it and is deliberately NOT shipped — a public fork-base catalog
#   has nowhere to keep a key, and a key committed next to the data authenticates nothing. What
#   these checks close is the cheap, observed shape: the appended hand-authored line.
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

# P1-1 (ROUND 4, TD-2026-07-30-P1-anchor-runtime): every git read below — including the ones in
# the python subprocesses — must see the real object graph. `git replace` keeps shas identical
# while swapping the objects, and this guard's whole job is to compare recorded shas to git.
export GIT_NO_REPLACE_OBJECTS=1

python3 - "$SCAN_ROOT" "$EXPECT_SHA" "$EXPECT_ANCHOR_SHA" "$REPO_ROOT" <<'PYEOF'
import sys
import pathlib
import json
import os
import re
import subprocess
import datetime
import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

root = pathlib.Path(sys.argv[1])
expect_sha_arg = sys.argv[2] if len(sys.argv) > 2 else ""
expect_anchor_arg = sys.argv[3] if len(sys.argv) > 3 else ""
# The repo THIS GUARD lives in (never the scanned root). Used only to locate the committed
# writer + fingerprint helper for the schema cross-check and the recompute, so that a fixture
# root cannot supply its own definition of what a genuine audit line looks like.
guard_repo = pathlib.Path(sys.argv[4]) if len(sys.argv) > 4 else None
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
#    DUPLICATE KEYS ARE REFUSED (P1-4, ROUND 4). `json.loads` keeps the LAST occurrence silently,
#    so `{"tree_clean":false, … ,"tree_clean":true}` reads as green while a human reading the
#    line sees the honest value first. The reviewer's note is narrower still — a duplicated
#    *_end field lets a placeholder pass — but the general shape is the same: a lenient parser
#    turns one record into two claims and lets the writer choose which one is audited.
def _no_dup_pairs(pairs):
    seen = set()
    for k, _v in pairs:
        if k in seen:
            emit_fail(
                "AUDIT_LINE_DUPLICATE_KEY",
                f'the latest audit line repeats the key {k!r}. JSON parsers keep the LAST '
                f'occurrence, so a duplicated field is a line that says two different things and '
                f'lets the writer pick which one is audited (a second "tree_clean" or a second '
                f'"*_end" is exactly that). verify-completion.sh emits each key once; a line that '
                f'does not is not a line it wrote.'
            )
        seen.add(k)
    return dict(pairs)

try:
    latest = json.loads(lines[-1], object_pairs_hook=_no_dup_pairs)
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
            ["git", "--no-replace-objects", "-C", str(root), "rev-parse", "HEAD"],
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
            ["git", "--no-replace-objects", "-C", str(root), "status", "--porcelain", "-uall"],
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

# ── P1-4 (cross-family review ROUND 4, 2026-07-30 — A RECORD IS A CLAIM, NOT EVIDENCE) ──
# Checks 1-9 read the audit line's FIELDS and compare them to each other and to git. Not one of
# them asked WHO WROTE THE LINE. `.ax-verify/runs.jsonl` is an ordinary append-only text file in
# the working tree; `echo '{…}' >> .ax-verify/runs.jsonl` with the pushed sha and the remote's
# advertised anchor satisfied every check above, because every value they compare was supplied by
# the same author.
#
# WHAT CAN AND CANNOT BE FIXED HERE, stated plainly rather than dressed up:
#   · Anything RECOMPUTABLE is recomputed (checks 12/13) — the tree fingerprint from the working
#     tree, head/refs from git. A forged "x" dies. But recomputable also means FORGEABLE by an
#     attacker willing to run the same command, so this defeats sloppy forgery, not determined
#     forgery.
#   · Anything the run PRODUCES ON THE SIDE is required to exist and to agree (check 14): the
#     per-step ledger .ax-verify/last_run.jsonl must be present, must be about the same head and
#     the same tree, and must not contradict a green summary. The forger now has to fabricate a
#     consistent SET of artifacts, not one line.
#   · The SHAPE is pinned to the writer (check 11), so a hand-authored line drifts and fails.
#   · WHAT IS NOT DONE, and why: an HMAC/signature would make forgery infeasible rather than
#     merely inconvenient, and it is deliberately NOT implemented — there is nowhere to keep a
#     key in a PUBLIC fork-base catalog, and a key committed beside the data it authenticates
#     authenticates nothing. Shipping the ceremony without the secret would be theater. The
#     residual is therefore REAL and stated: a party with write access to the repo who is willing
#     to run the same commands the runner runs can still fabricate a passing record. What this
#     closes is the cheap version — the hand-authored line.
AUDIT_SCHEMA_KEYS = (
    "ts", "head_sha", "exit", "pass", "warn_advisory", "hard_fail", "skip", "full_run",
    "tree_fingerprint", "tree_clean", "head_sha_end", "tree_fingerprint_end", "tree_clean_end",
    "tree_stable", "tree_samples", "anchor_sha", "anchor_kind", "anchor_sha_end", "anchor_stable",
)

# 10. The ANCHOR must have been the same commit at BOTH ends of the run (P1-2, ROUND 4).
#     The ref the ratchets measure against is an ordinary local ref: aim it at an ancient root
#     commit for the minutes the guards run — they find their own files absent and take the
#     first-release bootstrap skip — then restore it before the audit line is written. Check 9
#     compares the RECORDED anchor to the remote and is perfectly satisfied by that, because the
#     recorded value is the honest one. Only the producer can see the drift, so it reports both
#     endpoints and this check verifies the relation. Fail closed on absent fields: a line
#     without them came from a producer that never looked.
anchor_end = latest.get("anchor_sha_end")
anchor_stable = latest.get("anchor_stable")
if anchor_stable is not True or anchor_end != latest.get("anchor_sha"):
    emit_fail(
        "AUDIT_ANCHOR_MOVED_MIDRUN",
        f'the audit line does not show a settled release anchor for the whole run '
        f'(anchor_stable={anchor_stable!r}, anchor_sha={str(latest.get("anchor_sha"))[:12]} → '
        f'anchor_sha_end={str(anchor_end)[:12]}). refs/remotes/origin/main is an ordinary local '
        f'ref, so it can be pointed at a commit that merely LACKS the ratcheting files for the '
        f'minutes the guards run — every ratchet then takes its first-release bootstrap skip — '
        f'and restored before this line is written. Both readings cannot be the release being '
        f'extended. Re-run `bash practices/scripts/verify-completion.sh` on a settled repository. '
        f'(Missing fields mean the line predates this binding — re-run.)'
    )

# 11. SCHEMA PINNING — the line's field set must be exactly what the writer emits.
#     A hand-authored record is written by hand: it carries the fields its author knew to
#     include. Pinning the exact set turns every future field into a forgery detector, and turns
#     "I copied an old line and edited the shas" into a failure. The pin is cross-checked against
#     the COMMITTED writer, so the two cannot drift apart silently — if verify-completion.sh
#     gains a field and this list does not, the guard BLOCKS and names the drift instead of
#     quietly accepting a shape nobody pinned.
line_keys = set(latest.keys())
pinned = set(AUDIT_SCHEMA_KEYS)
if line_keys != pinned:
    emit_fail(
        "AUDIT_LINE_SCHEMA_MISMATCH",
        f'the latest audit line\'s field set is not the one verify-completion.sh emits '
        f'(unexpected: {sorted(line_keys - pinned)}, missing: {sorted(pinned - line_keys)}). '
        f'.ax-verify/runs.jsonl is an ordinary text file — appending a well-shaped line by hand '
        f'is the cheapest forgery there is, and every check above compares values supplied by the '
        f'same author. Pinning the exact shape means a hand-authored line has to reproduce the '
        f'writer exactly, and any field added to the writer later becomes a new detector. '
        f'Re-run `bash practices/scripts/verify-completion.sh` to get a genuine line.'
    )
if guard_repo is not None:
    writer = guard_repo / "practices" / "scripts" / "verify-completion.sh"
    if writer.is_file():
        # The audit printf is the schema. Extract the key names from the FORMAT STRING of the
        # line that appends to $AUDIT_LOG, so the pin above is checked against the code that
        # actually writes, not against a comment.
        wtext = writer.read_text(errors="replace")
        m = re.search(r"printf\s+'(\{\"ts\".*?)\\n'", wtext, re.S)
        if m:
            emitted = tuple(re.findall(r'"([a-z_]+)":', m.group(1)))
            if tuple(AUDIT_SCHEMA_KEYS) != emitted:
                emit_fail(
                    "AUDIT_WRITER_SCHEMA_DRIFT",
                    f'this guard pins the audit-line shape as {list(AUDIT_SCHEMA_KEYS)} but the '
                    f'committed writer (practices/scripts/verify-completion.sh) emits {list(emitted)}. '
                    f'The pin exists so a hand-authored line cannot pass as genuine; a pin that no '
                    f'longer matches the writer either rejects every honest run or accepts a shape '
                    f'nobody reviewed. Update BOTH in the same commit.'
                )

# 12. RECOMPUTE the tree fingerprint rather than believing it.
#     The record says which working tree was verified. Until now nothing checked that the value
#     was a fingerprint at all — "x" satisfied check 6 (a non-empty string that is not "nogit"
#     and does not start with "unverifiable-"). Recomputing it from the tree in front of us kills
#     that. Skipped for fixture roots (which declare expected_head.txt and are not git trees).
#     HONEST LIMIT, and it is a real one: on a CLEAN tree the fingerprint's two inputs are empty,
#     so the digest is a CONSTANT shared by every clean tree of every commit. Since push
#     eligibility already requires a clean tree (check 7), this recompute proves the record was
#     produced by the algorithm and that the tree is as clean as claimed — it does NOT
#     independently identify the code. head_sha, re-read from git in check 3, carries that.
#     CONSEQUENCE, disclosed: a tree that has been modified since the certifying run no longer
#     matches, so `push` after starting new edits is refused with "re-run or stash". That is the
#     conservative direction — the alternative is a gate that accepts a value it never checked.
#     A MEASUREMENT MUST NOT DISTURB WHAT IT MEASURES: the helper is executed as a SUBPROCESS,
#     never imported. `import tree_fingerprint` writes practices/scripts/lib/__pycache__/*.pyc
#     into the very tree being fingerprinted, so the recompute would report a mismatch it caused
#     itself — observed the first time this check ran (the sandbox tree went dirty with a .pyc
#     and every honest push was refused). Running the file as a script leaves no cache.
if not expected_head_file.is_file() and guard_repo is not None:
    fp_helper = guard_repo / "practices" / "scripts" / "lib" / "tree_fingerprint.py"
    # FAIL CLOSED on a missing helper (P1-3(c) applied to this file too): deleting
    # tree_fingerprint.py would otherwise turn the recompute into a silent skip, which is the
    # same "delete the thing that checks you" shape the anchor-critical path lists close. It is
    # ON those lists, so its deletion is separately blocking at the anchor; here it blocks at the
    # push. Guarded by the git test so the non-git roots ([87]'s relocated copies) still run.
    if not fp_helper.is_file() and (guard_repo / ".git").exists():
        emit_fail(
            "AUDIT_FINGERPRINT_UNVERIFIABLE",
            f'practices/scripts/lib/tree_fingerprint.py is absent, so the recorded '
            f'tree_fingerprint cannot be recomputed and the push gate would be trusting a value '
            f'it never checked. The helper is anchor-critical: a gate that cannot verify its own '
            f'evidence blocks rather than skipping.'
        )
    if fp_helper.is_file():
        try:
            recomputed = subprocess.check_output(
                [sys.executable, str(fp_helper), str(root)],
                stderr=subprocess.DEVNULL,
            ).decode().strip()
        except Exception as e:
            emit_fail(
                "AUDIT_FINGERPRINT_UNVERIFIABLE",
                f'practices/scripts/lib/tree_fingerprint.py could not be run ({e}), so the '
                f'recorded tree_fingerprint cannot be recomputed. The helper is anchor-critical; '
                f'a push gate that cannot recompute its evidence blocks rather than trusting it.'
            )
        if recomputed and recomputed != "nogit" and recomputed != tree_fp:
            emit_fail(
                "AUDIT_FINGERPRINT_MISMATCH",
                f'the recorded tree_fingerprint ({str(tree_fp)[:12]}) is not the fingerprint of '
                f'the tree that is here now ({recomputed[:12]}). Either the line was written by '
                f'hand — .ax-verify/runs.jsonl is an ordinary text file, and until this check '
                f'existed any non-empty string satisfied the "tree identified" test — or the tree '
                f'has changed since the run that certified it, in which case the certificate is '
                f'about code that is no longer here. Both resolve the same way: re-run '
                f'`bash practices/scripts/verify-completion.sh` at the commit you are pushing '
                f'(or put the tree back the way the run found it).'
            )

# 13. Git REPLACEMENT REFS make every sha comparison above meaningless (P1-1, ROUND 4).
#     `git replace <real> <fabricated>` keeps shas identical and swaps the object every ordinary
#     git command reads. This guard compares recorded shas against git; the ratcheting guards
#     walk history through git. All of it can be answered from a fabricated graph. Every call
#     here runs with GIT_NO_REPLACE_OBJECTS=1, and a tree carrying such refs at all is refused —
#     a released tree has no reason to have them, and "we read past them" is a claim about every
#     call site forever.
try:
    _replace = subprocess.check_output(
        ["git", "--no-replace-objects", "-C", str(root), "for-each-ref",
         "--format=%(refname)", "refs/replace/"],
        stderr=subprocess.DEVNULL,
    ).decode().strip()
except Exception:
    _replace = ""
if _replace:
    emit_fail(
        "AUDIT_REPLACE_REFS_PRESENT",
        f'this repository carries git replacement refs ({_replace.splitlines()}). They keep every '
        f'sha identical while swapping the OBJECT that rev-list/show/ls-tree/merge-base read, so '
        f'the ratcheting guards\' ancestry and bootstrap checks — and the sha comparisons in this '
        f'guard — can all be satisfied out of a fabricated history while the audit line records '
        f'authentic shas. Remove them (`git replace -d <ref>`) and re-run R25.'
    )

# 14. The summary must be corroborated by the run's OWN per-step ledger.
#     One line is one claim. verify-completion also publishes .ax-verify/last_run.jsonl — a
#     record per step, written incrementally as the run proceeds and bound to the head and tree
#     that produced it. Requiring the two artifacts to exist AND agree means a forger must
#     fabricate a consistent set rather than append a line. It is not unforgeable (nothing here
#     is, without a key — see the note above); it raises the cost and it catches every
#     "append one green line" attempt, which is the shape actually observed.
#     Fail closed: a green summary with no step ledger is a summary of nothing.
run_ledger = root / ".ax-verify" / "last_run.jsonl"
if not run_ledger.is_file():
    emit_fail(
        "AUDIT_RUN_LEDGER_MISSING",
        f'.ax-verify/last_run.jsonl is absent, so the summary line has no per-step corroboration. '
        f'verify-completion.sh publishes a record for every step it runs; a green summary with no '
        f'step ledger is a claim with nothing behind it (and it is what an appended line looks '
        f'like). Re-run `bash practices/scripts/verify-completion.sh`.'
    )
ledger_lines = [l for l in run_ledger.read_text().splitlines() if l.strip()]
if not ledger_lines:
    emit_fail(
        "AUDIT_RUN_LEDGER_EMPTY",
        f'.ax-verify/last_run.jsonl exists but records no steps, so nothing corroborates the '
        f'summary line.'
    )
for raw in ledger_lines:
    try:
        rec = json.loads(raw)
    except json.JSONDecodeError as e:
        emit_fail("AUDIT_RUN_LEDGER_MALFORMED",
                  f'.ax-verify/last_run.jsonl has a line that is not valid JSON: {e}')
    for field in ("step_id", "status", "head_sha", "tree_fingerprint"):
        if field not in rec:
            emit_fail("AUDIT_RUN_LEDGER_INCOMPLETE",
                      f'a step record in .ax-verify/last_run.jsonl is missing {field!r}: {raw[:120]}')
    if rec["head_sha"] != latest["head_sha"]:
        emit_fail(
            "AUDIT_RUN_LEDGER_HEAD_MISMATCH",
            f'step {rec["step_id"]!r} in .ax-verify/last_run.jsonl was recorded at head '
            f'{str(rec["head_sha"])[:12]} but the summary line claims '
            f'{latest["head_sha"][:12]}. The two artifacts describe different runs, so neither '
            f'corroborates the other. Re-run the contract at the commit you are pushing.'
        )
    if rec["tree_fingerprint"] != tree_fp:
        emit_fail(
            "AUDIT_RUN_LEDGER_TREE_MISMATCH",
            f'step {rec["step_id"]!r} was recorded against tree '
            f'{str(rec["tree_fingerprint"])[:12]} but the summary line claims {str(tree_fp)[:12]}. '
            f'Check 8 already requires the tree to have been settled for the whole run, so a step '
            f'bound to a different tree means the two artifacts did not come from one run.'
        )
    if rec["status"] not in ("PASS",):
        emit_fail(
            "AUDIT_RUN_LEDGER_STATUS_CONFLICT",
            f'step {rec["step_id"]!r} is recorded as {rec["status"]!r} in '
            f'.ax-verify/last_run.jsonl while the summary line claims exit=0 / hard_fail=0. A '
            f'step that did not pass cannot be part of a run that certifies a push.'
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
