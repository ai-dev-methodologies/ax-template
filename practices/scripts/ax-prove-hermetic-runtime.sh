#!/usr/bin/env bash
# practices/scripts/ax-prove-hermetic-runtime.sh
#   FALSIFICATION PROOF for ROUND 5 (cross-family reviewer, 2026-07-30;
#   TD-2026-07-30-P1-hermetic-runtime): THE RATCHET MUST NOT INHERIT ITS OWN RUNTIME.
#
# Round 4 proved that the helper's own NAMES cannot be injected. Round 5 attacks one level down —
# the RUNTIME the gate is made of:
#     · the git REPOSITORY is chosen by the environment  (GIT_DIR / GIT_WORK_TREE …)
#     · the COMMANDS are chosen by the environment       (exported `git` / `cd` / `pwd` / `python3`)
#     · the gate's own IMPLEMENTATION is a mutable file  (tree_fingerprint.py rewritten to lie)
# Each is an INPUT, and an input a gate trusts must be authenticated at the moment of use.
#
# WHAT IT PROVES (every attack is run against the LIVE push gate, in a throwaway sandbox):
#   (A) GIT_DIR/GIT_WORK_TREE aimed at a CLEAN shadow checkout of the same commit, while the tree
#       being pushed is DIRTY and the record claims it was clean       → the push gate REFUSES.
#       Attributable: the same sandbox with the hermetic scrub NEUTERED accepts it, so the scrub
#       is what refuses — not some incidental property of the sandbox.
#   (B) an exported `git` function                                     → HELPER_FUNCTION_INJECTED
#   (C) an exported `cd` + `pwd` pair                                  → HELPER_FUNCTION_INJECTED
#   (D) an exported `python3` that returns 0 — measured pre-fix to turn a FAILING recency guard
#       into exit 0, i.e. an entire python gate skipped              → HELPER_FUNCTION_INJECTED
#   (E1) tree_fingerprint.py rewritten to print a constant, UNCOMMITTED → RATCHET_TOOLCHAIN_MODIFIED
#   (E2) the same tamper COMMITTED, so the toolchain matches HEAD: the recompute is performed with
#        the PREVIOUS RELEASE's copy of the helper, so the lie still does not survive
#                                                                      → AUDIT_FINGERPRINT_*
#   (F) NEGATIVE CONTROL: the same sandbox with no attack at all PASSES (exit 0) and reaches NONE
#       of those codes — which is what makes (A)-(E) attributable to the attacks.
#
# ROUND 6 (TD-2026-07-30-P1-preflight-and-raw-bytes) adds the four classes the reviewer opened
# next. Two invariants name them: (alpha) NOTHING OVERRIDABLE MAY EXECUTE BEFORE THE SCRUB THAT
# DETECTS OVERRIDES; (beta) ANY CLAIM ABOUT BYTES MUST BE MADE ON RAW BYTES.
#   (G) BASH_FUNC_set / BASH_FUNC_[ injected through the ENVIRONMENT — the two commands round 5
#       ran BEFORE its own scrub                                    → HERMETIC_PREFLIGHT_HOSTILE
#       (G') the same attack with the round-6 preflight removed lands again (exit 0).
#   (H) a symlink named python3 pointing at /usr/bin/true, which satisfied every -f/-x/absolute
#       test and skipped the whole python body                      → HERMETIC_TOOL_UNAUTHENTIC
#       (H') the same attack with the SMOKE TEST removed lands again. SCOPE (round 8 / P1-B):
#       this refuses a MIS-RESOLVED tool, not a hostile PATH wrapper — the challenge is fixed
#       and public, so a wrapper forwards it. See TD-2026-07-30-(ratchet-threat-model).
#   (I) PYTHONPATH → sitecustomize.py calling os._exit(0)           → the gate reaches its honest
#       non-zero verdict anyway (scrubbed env + `-I -S`).
#   (J) a `filter.<n>.clean` that echoes the committed copy, so a TAMPERED toolchain file reads
#       byte-identical through git                                  → GIT_FILTERS_PRESENT /
#       RATCHET_TOOLCHAIN_MODIFIED, and (J') with the raw-byte comparison reverted to
#       `git diff --quiet` the tamper is accepted again (rc 0).
#   (K) a COMMITTED push-only bypass inside the recency guard (`[ -n "$EXPECT_SHA" ] && exit 0`),
#       which every HEAD-anchored self-check certifies because a committed backdoor IS HEAD
#                                                                   → RATCHET_PRIOR_RELEASE_GUARD_FAILED
#       (K') with the prior-release re-run removed the bypass passes the hook again (exit 0).
#
# ROUND 7 (TD-2026-07-30-P1-privileged-startup / P1-3) restates (alpha) honestly: an IN-SCRIPT
# sensor cannot see a STARTUP FILE, because bash runs $BASH_ENV before the script's first line.
#   (M) a SELF-ERASING $BASH_ENV — `builtin unset BASH_ENV ENV; function set(){ exit 0; }` —
#       leaves nothing for the round-6 preflight to find (no BASH_FUNC_* entry, no BASH_ENV in
#       the environ) and turns the gate's honest non-zero into exit 0. Closed by re-executing
#       every entry under bash PRIVILEGED MODE (-p), which processes neither $BASH_ENV/$ENV nor
#       inherited functions                                          → the honest verdict stands.
#       (M') with the privileged re-exec removed the payload lands again (exit 0).
#       (M'') presetting the loop marker AX_PRIV_REEXEC ABORTS       → HERMETIC_PRIVILEGED_UNREACHABLE
#             (a marker an attacker can set may never mean "already privileged").
#   (N) the two SOURCED libs ran `export` — an ordinary command lookup — BEFORE the round-6
#       preflight, so an exported export() exits them at 0. They now ASSERT privileged mode as
#       their first executable text (a sourced file cannot re-exec without replacing its caller).
#       (N') with the round-6 shape restored the exported export() lands again (exit 0).
#   (O) `filter.<n>.process` — git's long-running filter protocol — was outside round 6's
#       `clean|smudge` pattern and converts content identically: an equal-length swap of an
#       ordinary tracked file's bytes makes `git status --porcelain` EMPTY, so the fingerprint's
#       raw-byte loop never opens it        → GIT_FILTERS_PRESENT / GIT_RAW_INDEX_DIVERGENCE.
#       (O') with the keyword subset restored and the raw sweep removed the mask lands (exit 0).
#
# ROUND 8 (TD-2026-07-31-(P1-worktree-representation) / P1-A) attacks invariant (beta) from the one
# side round 7 left open: it measured BYTES, but only for paths whose ON-DISK SHAPE it expected,
# and `continue`d past the rest. Two representations hide bytes that a push ships, and both are
# reachable with one `git update-index` — no environment control, squarely inside the declared
# threat model. MEASURED pre-fix: a clean tree, a symlink swap and a tracked-but-absent path all
# produced the IDENTICAL fingerprint 0a815065… (the clean-tree constant).
#   (P) an index-REGULAR tracked path, marked --assume-unchanged, deleted and replaced by a
#       SYMLINK to a benign file OUTSIDE the repository. `git status --porcelain` is EMPTY and the
#       index still says mode 100644          → GIT_INDEX_FLAGS_SET / GIT_WORKTREE_TYPE_MISMATCH.
#       (P') with the round-8 refusals removed from BOTH implementations it lands again (exit 0).
#   (Q) a tracked path marked --skip-worktree and deleted: status empty, the build omits the file,
#       both sweeps ignored the absent path      → GIT_INDEX_FLAGS_SET / GIT_TRACKED_PATH_ABSENT.
#       (Q') same neuter, lands again (exit 0).
#   (R) LAYER INDEPENDENCE: with ONLY the index-bit refusal neutered, (P) and (Q) must still be
#       refused on the REPRESENTATION codes. Without this the representation layer would be dead
#       code behind the bit check — a sparse checkout sets skip-worktree, so the bit is present in
#       the ordinary case and would mask whether the backstop works at all.
#   (S) OVER-CORRECTION CONTROL: a sandbox carrying an UNINITIALIZED gitlink must still PASS. All
#       three gitlinks in this catalog are empty post-clone fixture directories; a refusal there
#       would break every fresh clone to close nothing.
#
# ROUND 9 (TD-2026-07-30-(P1-representation-parity) / P1-1, P1-2, (c), (d), (e)) attacks the parity
# round 8 left: it SEPARATED the shapes and then compared only BLOB BYTES, so three representation
# facts that no digest here carries were still accepted. None needs environment control.
#   (T) an index-100644 path that is EXECUTABLE on disk, under `core.fileMode=false`: status EMPTY,
#       bytes identical, fingerprint = the clean-tree constant, and R25's direct `./gradlew`
#       invocations run a file the push records as non-executable → GIT_EXEC_BIT_DIVERGENCE.
#   (U) the MIRROR: index 100755, NOT executable on disk           → GIT_EXEC_BIT_DIVERGENCE.
#       (T2)/(U2) with the round-9 refusals removed, both land again (exit 0).
#   (V) a gitlink with NO gitdir whose directory is POPULATED. Round 8's exemption tested for the
#       absence of `<gitlink>/.git` and never required the directory to be EMPTY, so a committed
#       step can `bash vendor/sub/check.sh` a file that a fresh clone of the pushed commit never
#       receives                                     → GIT_GITLINK_UNINITIALIZED_POPULATED.
#       (V2) with the round-9 refusals removed it lands again (exit 0).
#   (W) two index entries differing only in CASE that lstat to ONE inode → GIT_CASEFOLD_ALIAS.
#       SCOPE, stated: with divergent blobs the alias also shows as a modification, so the
#       clean-tree precondition already refuses the ordinary form; this is defense in depth that
#       names the fault rather than the symptom. (W2) is its pre-round-9 twin.
#   (X) ROUND 9 / (c): the MIRROR of (P) — an index-SYMLINK path that is a REGULAR FILE on disk.
#       Round 8 implemented this direction and never exercised it; run with ONLY the index-bit
#       refusal neutered so the code under test is the representation backstop
#                                                    → GIT_WORKTREE_TYPE_MISMATCH.
#   (Y) ROUND 9 / (c): an INITIALIZED submodule moved off the recorded commit — round 8's other
#       implemented-but-undriven branch          → GIT_GITLINK_DIVERGENCE.
#   ROUND 9 / (d): a per-case SETUP FAILURE is now LOUD (harness exit 2). It used to be
#       `r8_apply … || return 0`, which turned a case whose attack could not even be applied into a
#       SILENT PASS — a proof harness that can skip its own cases proves nothing.
#
# ROUND 10 (TD-2026-07-31-(P1-casefold-prefix)) attacks the round-9 casefold check itself: it was
# LEAF-ONLY. Both implementations grouped COMPLETE folded paths, so two entries whose LEAF names
# differ never met even when a shared DIRECTORY component was the alias.
#   (Z) index `A/check.sh` (running `cat A/helper`) + index `a/helper`; on APFS `A` and `a` are ONE
#       directory inode. Status EMPTY, the local read succeeds, and MEASURED against d567c37 the
#       fingerprint returned the clean-tree constant with an EMPTY 12c violation set — while the
#       pushed tree serves no `A/helper` to a case-sensitive receiver → GIT_CASEFOLD_DIR_ALIAS.
#       (Z2) with the refusal removed from BOTH implementations it lands again (exit 0).
#   (Z3)/(Z4) ROUND 10 / P2 — PER-IMPLEMENTATION ATTRIBUTION. The round-9 twins disabled the 12c
#       sweep and the fingerprint helper TOGETHER, so they proved the pair load-bearing and not
#       either one. Every alias/representation class now also runs with exactly ONE side neutered:
#       (Z3)(T3)(V3)(W3) sweep-only → the helper still refuses (AUDIT_FINGERPRINT_UNVERIFIABLE,
#       the recompute runs the prior release's copy), (Z4)(T4)(V4)(W4) helper-only → the sweep
#       still refuses on its own code.
#   (Z5) THE FALSE-POSITIVE CONTROL: genuinely DISTINCT `A/` and `a/` directories must NOT be
#       refused, because the check is a measurement of (st_dev, st_ino) and a case-sensitive
#       fork-receiver must be unaffected. Built on a case-sensitive filesystem when one is
#       reachable ($WORK on Linux, or AX_PROVE_CS_DIR); on a case-insensitive one the topology
#       cannot exist, so the harness runs the shared-prefix arm and SAYS WHICH ARM RAN — the
#       premise of every alias case is asserted at gate time (r9_premise), never assumed.
#
# ROUND 11 (TD-2026-08-01-(P1-unicode-prefix-fold)) attacks the FOLD the round-10 prefix map is
# KEYED with. It was `bytes.lower()`: ASCII-only and normalization-blind, so two aliases the
# filesystem serves from ONE inode were never compared — and neither needs any environment
# control, both are committed content that arrives in an ordinary clone.
#   (AA) index `é/check.sh` (NFC c3a9, running `cat "é/helper"`) + index `e◌́/helper` (NFD 65cc81).
#       MEASURED at beee364: status EMPTY, `bash é/check.sh` → PASS, BOTH implementations silent,
#       fingerprint = the clean-tree constant — while the pushed tree records only `e◌́/helper`,
#       which a normalization-SENSITIVE receiver does not serve as `é/helper`
#                                                    → GIT_CASEFOLD_DIR_ALIAS.
#   (AB) the same hole with NON-ASCII CASE: `É/` (c389) ≡ `é/` (c3a9), which `bytes.lower()` never
#       touched                                      → GIT_CASEFOLD_DIR_ALIAS.
#   (AA2)/(AB2) pre-round-11 twins: the neuter puts `bytes.lower()` BACK as the key in BOTH
#       implementations (it does NOT delete the round-10 report), so what lands again is
#       attributable to the FOLD alone. (AA3)/(AA4)/(AB3)/(AB4) are the per-implementation splits.
#   (AC) NO REGRESSION: the round-10 ASCII topology is still refused by the widened fold.
#   (AA5) the round-11 FALSE-POSITIVE control on a REAL case-sensitive volume: genuinely DISTINCT
#       `É/` and `é/` must NOT be refused. (AD)/(AE) close the rest: (AD) asserts the two
#       implementations' folds agree over every prefix of every tracked path plus an adversarial
#       corpus, and (AE) drives the shipped grouping code with SYNTHETIC inodes for the one control
#       this platform cannot build — MEASURED with hdiutil, case-insensitive APFS, CASE-SENSITIVE
#       APFS, case-sensitive HFS+, ExFAT and FAT32 are ALL normalization-INSENSITIVE, so a
#       distinct-inode NFC/NFD pair does not exist here. (AE) says SIMULATED in its own output.
#
# ROUND 12 (TD-2026-08-01-(P1-ignorable-fold)) attacks the THIRD equivalence axis the round-11 fold
# still preserved: IGNORABLE FORMAT CHARACTERS. Case-insensitive HFS+ folds designated formatting
# controls to ZERO and skips them (Apple TN1150, `FastUnicodeCompare`), so two spellings differing
# only by an INVISIBLE character are one directory — and round 11's canonical caseless key kept
# them apart. Committed path names only; no environment control.
#   (AF) index `SAFE/check.sh` (running `cat SAFE/helper`) + index `SAFE<U+200C ZWNJ>/helper`. The
#       checkout is clean, the local check PASSES, and the pushed tree carries no literal
#       `SAFE/helper` for a receiver that treats U+200C as significant
#                                                    → GIT_CASEFOLD_DIR_ALIAS.
#   (AG) the same hole with U+202E RIGHT-TO-LEFT OVERRIDE — a BIDI control, checked rather than
#       assumed to be ignorable                       → GIT_CASEFOLD_DIR_ALIAS.
#   (AF2)/(AG2) pre-round-12 twins: the neuter removes ONLY the ignorable strip and leaves the
#       round-11 fold live, so what lands again is attributable to the STRIP alone.
#       (AF3)/(AF4)/(AG3)/(AG4) are the per-implementation splits.
#   (AH) the round-12 FALSE-POSITIVE control, and it is LIVE, not simulated: both APFS variants
#       serve the two spellings from DISTINCT inodes, so the ordinary sandbox IS the control and
#       must NOT be refused. (AI) asserts the four measurable claims the strip set rests on and
#       drives the shipped grouping code with synthetic inodes as the RED arm's fallback.
#   BOTH SIDES OF THIS ROUND ARE REAL: the harness ATTACHES a case-insensitive HFS+ image
#   (`hdiutil create -fs HFS+`) for the folding side, and measures the non-folding side in place.
#
# Nothing outside the throwaway directory is touched; the live tree is only ever READ.
# Exit: 0 all attacks blocked · 1 at least one attack open · 2 harness error.
set -uo pipefail

SCRIPT_DIR="$(builtin cd "$(dirname "${BASH_SOURCE[0]}")" && builtin pwd)"
REPO_ROOT="$(builtin cd "$SCRIPT_DIR/../.." && builtin pwd -P)"

RECENCY_REL="practices/evals/completion_checklist_recency_guard.sh"
FP_REL="practices/scripts/lib/tree_fingerprint.py"
COPY_RELS=(
    "$RECENCY_REL"
    "$FP_REL"
    "practices/scripts/lib/release_anchor.sh"
    "practices/scripts/verify-completion.sh"
    "practices/scripts/ax-ledger-log.sh"
    "practices/evals/evidence_quote_spotcheck_guard.sh"
    "practices/evals/manifest_snapshot_integrity_guard.sh"
    "practices/evals/run-all-guards.sh"
    ".githooks/pre-push"
    ".githooks/pre-push-lib.sh"
)
for rel in "${COPY_RELS[@]}"; do
    [ -f "$REPO_ROOT/$rel" ] || { echo "ax-prove-hermetic-runtime: missing $rel" >&2; exit 2; }
done
command -v git >/dev/null 2>&1 || { echo "ax-prove-hermetic-runtime: git required" >&2; exit 2; }

WORK="$(mktemp -d)"
# AX_PROVE_KEEP=1 preserves the sandboxes and their per-case gate logs so a round's TRANSCRIPTS can
# be quoted rather than paraphrased (the reviewer asks for the codes, not for a summary of them).
# It changes nothing the harness measures; the default remains "leave nothing behind".
ax_cleanup() { [ -n "${AX_PROVE_KEEP:-}" ] && { echo "  (sandboxes kept: $*)"; return 0; }
               rm -rf "$@"; }
# ROUND 12: one arm may run on a filesystem this harness ATTACHES (a case-insensitive HFS+ image —
# the only filesystem measured to fold ignorable format characters). Detaching must happen BEFORE
# the rm, or the rm walks a mounted volume. AX_PROVE_KEEP keeps the mount too, and says where.
CS_ROOT=""; IGN_MNT=""
ax_teardown() {
    if [ -n "$IGN_MNT" ]; then
        if [ -n "${AX_PROVE_KEEP:-}" ]; then echo "  (ignorable-folding volume left mounted: $IGN_MNT)"
        else hdiutil detach -force "$IGN_MNT" >/dev/null 2>&1; fi
    fi
    ax_cleanup "$WORK" ${CS_ROOT:+"$CS_ROOT"}
}
trap ax_teardown EXIT
FAIL=0
note() { echo "  $*"; }
violation() { echo "  VIOLATION: $*" >&2; FAIL=1; }

GIT_ID=(-c user.email=ax@example.invalid -c user.name=ax)

# prefix_neuter <sb> — rebuild the sandbox's copies as the PRE-ROUND-5 shape: the hermetic
# scrubs, the git-context binding and the independent status read are removed. Every anchor is
# asserted to occur exactly once, so this goes STALE LOUDLY rather than silently proving nothing.
prefix_neuter() {
    python3 - "$1/repo/practices/evals/completion_checklist_recency_guard.sh" \
              "$1/repo/practices/scripts/lib/tree_fingerprint.py" <<'PY'
import sys
guard, fp = sys.argv[1], sys.argv[2]
edits = {
    guard: [
        ('for _ax_hn in ${!GIT_@}; do unset "$_ax_hn" 2>/dev/null || true; done',
         ': # PRE-ROUND-5: the GIT_* family was inherited'),
        ('unset BASH_ENV ENV GIT_DIR GIT_WORK_TREE GIT_COMMON_DIR GIT_OBJECT_DIRECTORY GIT_INDEX_FILE \\',
         'unset BASH_ENV ENV \\'),
        ('GIT_ENV = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}',
         'GIT_ENV = dict(os.environ)'),
        ('    live_git_root = os.path.realpath(top_out) == os.path.realpath(str(root))',
         '    live_git_root = True'),
        ('    if [ -z "$_ax_hcan" ] || [ "$_ax_htop" != "$_ax_hcan" ]; then',
         '    if false; then'),
        ('    if st_out:', '    if False:'),
    ],
    fp: [
        ('    env = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}',
         '    env = dict(os.environ)'),
        ('    if not top or os.path.realpath(top) != os.path.realpath(repo):', '    if False:'),
    ],
}
for path, pairs in edits.items():
    text = open(path, encoding="utf-8").read()
    for anchor, value in pairs:
        n = text.count(anchor)
        if n != 1:
            print(f"anchor occurs {n}x (expected 1) in {path}: {anchor[:60]!r}", file=sys.stderr)
            sys.exit(3)
        text = text.replace(anchor, value)
    open(path, "w", encoding="utf-8").write(text)
PY
}

# build_sb <dir> [prefix] — a git work tree carrying the LIVE gate, with an honest origin/main, plus the
# audit artifacts a genuine R25 run would have left. The audit line is written by this harness
# (that is the point: it is an ordinary text file) but every value in it is HONEST, so the
# unattacked control PASSES and each attack is the only difference.
build_sb() {
    local sb="$1" prefix="${2:-}" fp head
    mkdir -p "$sb/repo" || return 2
    ( builtin cd "$sb/repo" && git init -q . ) >/dev/null 2>&1 || return 2
    local rel
    for rel in "${COPY_RELS[@]}"; do
        mkdir -p "$sb/repo/$(dirname "$rel")" || return 2
        cp "$REPO_ROOT/$rel" "$sb/repo/$rel" || return 2
    done
    # A no-op gradle wrapper so the push hook's regression STAGE can complete in the sandbox: the
    # thing under test is the R25 stage above it, and a missing backend/ would make every hook run
    # exit 1 for a reason that has nothing to do with the attack (ROUND 6).
    mkdir -p "$sb/repo/backend" || return 2
    printf '#!/bin/sh\nexit 0\n' > "$sb/repo/backend/gradlew" || return 2
    chmod +x "$sb/repo/backend/gradlew" || return 2
    # The PRE-fix shape is committed as the sandbox's whole history, so the "must reproduce" probe
    # is a genuine pre-round-5 world rather than a live tree with a patch on top.
    if [ -n "$prefix" ]; then prefix_neuter "$sb" || return 3; fi
    ( builtin cd "$sb/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "gate" ) >/dev/null 2>&1 || return 2
    # an origin/main that HAS this state — the anchor the recompute reads its implementation from
    git init -q --bare "$sb/remote.git" >/dev/null 2>&1 || return 2
    ( builtin cd "$sb/repo" && git remote add origin "$sb/remote.git" \
      && git push -q origin HEAD:refs/heads/main && git fetch -q origin ) >/dev/null 2>&1 || return 2
    ( builtin cd "$sb/repo" && git update-ref refs/remotes/origin/main HEAD ) >/dev/null 2>&1 || return 2
    return 0
}

# write_audit <sb> [fingerprint-override]
write_audit() {
    local sb="$1" fp_override="${2:-}" head anchor fp
    head="$(git -C "$sb/repo" rev-parse HEAD)"
    anchor="$(git -C "$sb/repo" rev-parse refs/remotes/origin/main)"
    fp="${fp_override:-$(python3 "$sb/repo/$FP_REL" "$sb/repo" 2>/dev/null)}"
    mkdir -p "$sb/repo/.ax-verify"
    printf '{"ts":"2026-07-30T00:00:00Z","head_sha":"%s","exit":0,"pass":1,"warn_advisory":0,"hard_fail":0,"skip":0,"full_run":true,"tree_fingerprint":"%s","tree_clean":true,"head_sha_end":"%s","tree_fingerprint_end":"%s","tree_clean_end":true,"tree_stable":true,"tree_samples":3,"anchor_sha":"%s","anchor_kind":"origin/main","anchor_sha_end":"%s","anchor_stable":true}\n' \
        "$head" "$fp" "$head" "$fp" "$anchor" "$anchor" > "$sb/repo/.ax-verify/runs.jsonl"
    printf '{"step_id":"gate","status":"PASS","head_sha":"%s","tree_fingerprint":"%s"}\n' \
        "$head" "$fp" > "$sb/repo/.ax-verify/last_run.jsonl"
    # .ax-verify must not itself make the tree dirty — the gate now reads `git status` directly.
    # ROUND 10 / P2: ONLY .gitignore is staged. `git add -A` here re-read every path from disk, and
    # on a case-insensitive filesystem that HEALS a casefold premise — the two aliased entries
    # collapse onto the one file's bytes, so the case that was supposed to carry two divergent
    # blobs carried one, and nothing said so. The setups commit their own state; nothing else is
    # pending at this point.
    printf '.ax-verify/\n' > "$sb/repo/.gitignore"
    ( builtin cd "$sb/repo" && git add -- .gitignore \
      && git "${GIT_ID[@]}" commit -q -m "ignore audit dir" ) >/dev/null 2>&1
    # rewrite with the post-commit head/fingerprint
    head="$(git -C "$sb/repo" rev-parse HEAD)"
    fp="${fp_override:-$(python3 "$sb/repo/$FP_REL" "$sb/repo" 2>/dev/null)}"
    printf '{"ts":"2026-07-30T00:00:00Z","head_sha":"%s","exit":0,"pass":1,"warn_advisory":0,"hard_fail":0,"skip":0,"full_run":true,"tree_fingerprint":"%s","tree_clean":true,"head_sha_end":"%s","tree_fingerprint_end":"%s","tree_clean_end":true,"tree_stable":true,"tree_samples":3,"anchor_sha":"%s","anchor_kind":"origin/main","anchor_sha_end":"%s","anchor_stable":true}\n' \
        "$head" "$fp" "$head" "$fp" "$anchor" "$anchor" > "$sb/repo/.ax-verify/runs.jsonl"
    printf '{"step_id":"gate","status":"PASS","head_sha":"%s","tree_fingerprint":"%s"}\n' \
        "$head" "$fp" > "$sb/repo/.ax-verify/last_run.jsonl"
}

run_gate() {   # run_gate <sb> <log> [env assignments...] — the LIVE recency guard, as the hook calls it
    local sb="$1" log="$2"; shift 2
    ( builtin cd "$sb/repo" && env "$@" bash "$sb/repo/$RECENCY_REL" ) > "$log" 2>&1
}

# ── ROUND 6 neuters. Same contract as prefix_neuter: every anchor must occur EXACTLY ONCE, so a
# refactor makes this harness fail loudly instead of silently proving nothing.
round6_neuter() {   # round6_neuter <sb> <what>   what ∈ preflight|smoketest|rawbytes|priorrelease
    "${AX_PY_BIN:-python3}" - "$1/repo" "$2" <<'PY'
import re, sys, pathlib
repo, what = pathlib.Path(sys.argv[1]), sys.argv[2]
GUARD = repo / "practices/evals/completion_checklist_recency_guard.sh"
ANCHOR = repo / "practices/scripts/lib/release_anchor.sh"
HOOK = repo / ".githooks/pre-push"
edits = {
    # (G') remove the pure-keyword preflight: the round-5 world, where `set -uo pipefail` and the
    #      bootstrap's own `[ … ]` executed before anything looked at the runtime.
    "preflight": [(GUARD, [('_AX_PF_ENV="$(/usr/bin/env)"', '_AX_PF_ENV="AX_NEUTERED"')])],
    # (H') remove the interpreter self-report: back to "absolute + -x is good enough".
    "smoketest": [(GUARD, [('            "AXPY 3 "*) AX_PY_BIN="$_ax_hb" ;;',
                           '            *) AX_PY_BIN="$_ax_hb" ;;\n'
                           '            "AXPY 3 "*) AX_PY_BIN="$_ax_hb" ;;')])],
    # (J') revert the toolchain byte comparison to the filter-honouring `git diff --quiet`.
    # A MINIMAL anchor on purpose: the surrounding lines legitimately move (the absent-on-both-
    # sides exemption landed between rounds and broke a multi-line anchor). This one line IS the
    # comparison — reverting it to the filter-honouring `git diff --quiet` is the pre-round-6 world.
    "rawbytes": [(ANCHOR, [
        ('    ax_ratchet_filters_absent "$repo" "$label" "$@" || bad=1', '    :'),
        ('        [ -n "$want" ] && [ "$want" = "$have" ] && continue',
         "\n".join([
            '        ax_git "$repo" diff --quiet "$rev" -- "$rel" >/dev/null 2>&1',
            '        rc=$?',
            '        [ "$rc" -eq 0 ] && continue']))])],
    # (K') remove the prior-release re-run from the hook.
    "priorrelease": [(HOOK, [('    pp_anchor_recency_gate "$ANCHOR_REV" "$local_sha" || exit 1',
                              '    : # PRE-ROUND-6: the hook trusted the tree copy of the guard')])],
}
for path, pairs in edits[what]:
    text = path.read_text(encoding="utf-8")
    for a, b in pairs:
        n = text.count(a)
        if n != 1:
            print(f"neuter anchor occurs {n}x (expected 1) in {path}: {a[:60]!r}", file=sys.stderr)
            sys.exit(3)
        text = text.replace(a, b)
    path.write_text(text, encoding="utf-8")
# ROUND 7 CORRECTION: "pre-round-6" also means PRE-ROUND-7 — the privileged re-exec did not exist
# then either. Leaving it in place makes (G') structurally unreproducible (privileged mode alone
# blocks the exported-`set` import), which would silently turn a non-vacuity proof into a
# tautology. The pre-fix world must be the world as it actually was.
if what == "preflight":
    pat = (r"^case \$- in\n    \*p\*\) ;;\n    \*\) case \"\$\{AX_PRIV_REEXEC-\}\""
           r".*?\nunset AX_PRIV_REEXEC\n")
    body = GUARD.read_text(encoding="utf-8")
    hits = re.findall(pat, body, re.M | re.S)
    if len(hits) != 1:
        print(f"round-7 block occurs {len(hits)}x (expected 1) in {GUARD}", file=sys.stderr)
        sys.exit(3)
    GUARD.write_text(re.sub(pat, "", body, count=1, flags=re.M | re.S), encoding="utf-8")
PY
    local rc=$?
    [ "$rc" -ne 0 ] && return "$rc"
    # The pre-round-6 shape is COMMITTED and published as the sandbox's origin/main, so the
    # sandbox is a self-consistent OLD WORLD rather than a new world with an uncommitted edit —
    # otherwise the toolchain-authenticity check fires on the neuter itself and the reproduction
    # would be measuring the wrong thing.
    ( builtin cd "$1/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "pre-round-6" \
      && git update-ref refs/remotes/origin/main HEAD \
      && git push -q -f origin HEAD:refs/heads/main ) >/dev/null 2>&1
    return 0
}

# ── ROUND 7 neuters (TD-2026-07-30-P1-privileged-startup / P1-3). Same contract as the round-5/6
# neuters: EXACTLY ONE match per anchor, so a refactor makes this harness fail loudly.
round7_neuter() {   # round7_neuter <sb> <what>   what ∈ privileged|sourcedpriv|filterproc
    "${AX_PY_BIN:-python3}" - "$1/repo" "$2" <<'PY'
import re, sys, pathlib
repo, what = pathlib.Path(sys.argv[1]), sys.argv[2]
GUARD = repo / "practices/evals/completion_checklist_recency_guard.sh"
ANCHOR = repo / "practices/scripts/lib/release_anchor.sh"
FP = repo / "practices/scripts/lib/tree_fingerprint.py"
BANNER = "# ── ROUND 6 / P1-1: PURE-KEYWORD PREFLIGHT"

def sub_once(path, pattern, repl, flags=re.M):
    text = path.read_text(encoding="utf-8")
    hits = re.findall(pattern, text, flags)
    if len(hits) != 1:
        print(f"round7 neuter anchor occurs {len(hits)}x (expected 1) in {path}: {pattern[:70]!r}",
              file=sys.stderr)
        sys.exit(3)
    path.write_text(re.sub(pattern, repl, text, count=1, flags=flags), encoding="utf-8")

def plain_once(path, a, b):
    text = path.read_text(encoding="utf-8")
    if text.count(a) != 1:
        print(f"round7 neuter anchor occurs {text.count(a)}x (expected 1) in {path}: {a[:70]!r}",
              file=sys.stderr)
        sys.exit(3)
    path.write_text(text.replace(a, b, 1), encoding="utf-8")

if what == "privileged":
    # (M') the round-6 world: no privileged re-exec, so $BASH_ENV is processed before line 1.
    sub_once(GUARD, r"^case \$- in\n    \*p\*\) ;;\n    \*\) case \"\$\{AX_PRIV_REEXEC-\}\".*?\nunset AX_PRIV_REEXEC\n",
             "", re.M | re.S)
elif what == "sourcedpriv":
    # (N') the round-6 world for a SOURCED lib: no privileged assertion, and `export` — an
    # ordinary command lookup — back ahead of the pure-keyword preflight, which is where round 6
    # actually left it (release_anchor.sh:251).
    sub_once(ANCHOR, r"^case \$- in\n    \*p\*\) ;;\n    \*\) _AX_PV_NULL=[^\n]*HERMETIC_PRIVILEGED_UNREACHABLE[^\n]*\nesac\n",
             "", re.M | re.S)
    plain_once(ANCHOR, BANNER, "export GIT_NO_REPLACE_OBJECTS=1\n" + BANNER)
elif what == "filterproc":
    # (O') the round-6 world for content filters: the clean|smudge KEYWORD SUBSET, and no
    # raw-bytes sweep on either the guard side or the fingerprint side.
    plain_once(GUARD, 'r"^filter\\.",', 'r"^filter\\..*\\.(clean|smudge)$",')
    plain_once(GUARD, "            if _h.hexdigest().encode() != _blob:", "            if False:")
    plain_once(FP, 'r"^filter\\."],', 'r"^filter\\..*\\.(clean|smudge)$"],')
    plain_once(FP, "    if diverged:", "    if False:")
else:
    print(f"unknown round7 neuter: {what}", file=sys.stderr); sys.exit(3)
PY
    local rc=$?
    [ "$rc" -ne 0 ] && return "$rc"
    ( builtin cd "$1/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "pre-round-7" \
      && git update-ref refs/remotes/origin/main HEAD \
      && git push -q -f origin HEAD:refs/heads/main ) >/dev/null 2>&1
    return 0
}

# ── ROUND 8 neuters (TD-2026-07-31-(P1-worktree-representation) / P1-A). Same contract as the
# round-5/6/7 neuters: EXACTLY ONE match per anchor, so a refactor makes this harness fail loudly
# instead of silently proving nothing. `all` restores the round-7 world (every round-8 refusal
# gone, so the sweeps `continue` past the unexpected representation exactly as they did); `bits`
# removes ONLY the index-flag refusal, which is what makes the representation layer separately
# falsifiable.
round8_neuter() {   # round8_neuter <sb> <what>   what ∈ all|bits
    "${AX_PY_BIN:-python3}" - "$1/repo" "$2" <<'PY'
import sys, pathlib
repo, what = pathlib.Path(sys.argv[1]), sys.argv[2]
GUARD = repo / "practices/evals/completion_checklist_recency_guard.sh"
FP = repo / "practices/scripts/lib/tree_fingerprint.py"
pairs = {
    "all": [
        (GUARD, "        if _flagged:"), (GUARD, "        if _mistyped:"),
        (GUARD, "        if _absent:"), (GUARD, "        if _glbad:"),
        (FP, "    if flagged:"), (FP, "    if mistyped:"),
        (FP, "    if unreadable:"), (FP, "    if gitlinks:"),
    ],
    "bits": [(GUARD, "        if _flagged:"), (FP, "    if flagged:")],
}
for path, anchor in pairs[what]:
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        print(f"round8 neuter anchor occurs {n}x (expected 1) in {path}: {anchor!r}",
              file=sys.stderr)
        sys.exit(3)
    dead = anchor[:len(anchor) - len(anchor.lstrip())] + "if False:"
    path.write_text(text.replace(anchor, dead, 1), encoding="utf-8")
PY
    local rc=$?
    [ "$rc" -ne 0 ] && return "$rc"
    # Committed and published as the sandbox's origin/main, so the sandbox is a self-consistent
    # OLD WORLD rather than a new world with an uncommitted edit — otherwise the toolchain
    # authenticity check fires on the neuter itself and the reproduction measures the wrong thing.
    ( builtin cd "$1/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "pre-round-8" \
      && git update-ref refs/remotes/origin/main HEAD \
      && git push -q -f origin HEAD:refs/heads/main ) >/dev/null 2>&1
    return 0
}

# r8_sentinel <sb> — commit an ordinary tracked file carrying the MALICIOUS bytes, and leave a
# BENIGN twin outside the repository for the symlink to point at. The target is deliberately NOT
# a toolchain path: 12b already compares those nine on raw bytes, so attacking one would be caught
# by a check that predates this round and the reproduction would prove nothing. Everything ELSE in
# the tree — which is what R25 actually builds and lints — had no such cover.
R8_REL="practices/verification-checklist.yaml"
r8_sentinel() {
    local sb="$1"
    mkdir -p "$sb/repo/$(dirname "$R8_REL")" || return 2
    printf 'steps: [MALICIOUS]\n' > "$sb/repo/$R8_REL" || return 2
    printf 'steps: [benign]\n' > "$sb/benign.yaml" || return 2
    ( builtin cd "$sb/repo" && git add "$R8_REL" \
      && git "${GIT_ID[@]}" commit -q -m sentinel ) >/dev/null 2>&1 || return 2
    return 0
}

# proc_filter_prepare <sb> <tracked-rel> — write git's LONG-RUNNING FILTER (protocol v2) and the
# .gitattributes that attaches it, but do NOT enable it yet. Split in two on purpose: the
# attribute commit MOVES HEAD, so it must land before write_audit or the gate would fail with
# AUDIT_STALE_HEAD and the scenario would be measuring the wrong refusal.
proc_filter_prepare() {
    local sb="$1" rel="$2"
    cat > "$sb/procfilter.py" <<'PYF'
import sys, os
IN, OUT = sys.stdin.buffer, sys.stdout.buffer
MASK = open(os.environ["AX_MASK_FILE"], "rb").read()
def rd():
    h = IN.read(4)
    if not h: return None
    n = int(h, 16)
    return b"" if n == 0 else IN.read(n - 4)
def wr(d=None):
    OUT.write(b"0000" if d is None else (b"%04x" % (len(d) + 4)) + d)
def rlist():
    out = []
    while True:
        p = rd()
        if p is None or p == b"": return out
        out.append(p)
rlist()
wr(b"git-filter-server\n"); wr(b"version=2\n"); wr(); OUT.flush()
rlist()
wr(b"capability=clean\n"); wr(b"capability=smudge\n"); wr(); OUT.flush()
while True:
    hdrs = rlist()
    if not hdrs: break
    rlist()
    wr(b"status=success\n"); wr(); OUT.flush()
    wr(MASK); wr(); wr(); OUT.flush()
PYF
    git -C "$sb/repo" show "HEAD:$rel" > "$sb/mask.bin" 2>/dev/null || return 2
    printf '%s filter=axproc\n' "$rel" > "$sb/repo/.gitattributes" || return 2
    ( builtin cd "$sb/repo" && git add .gitattributes \
      && git "${GIT_ID[@]}" commit -q -m "attrs" ) >/dev/null 2>&1 || return 2
    return 0
}

# proc_filter_enable <sb> — the one config key that is the whole attack surface: `.process`, which
# round 6's `clean|smudge` pattern did not match.
proc_filter_enable() {
    git -C "$1/repo" config filter.axproc.process "${AX_PY_BIN:-python3} $1/procfilter.py" || return 2
    return 0
}

# A sandbox whose HONEST verdict is NON-ZERO: the audit log is removed, so the gate must say
# AUDIT_LOG_MISSING. That is the only way to measure an attack whose payload is "make the gate
# exit 0" — against a passing control, success and subversion are the same number.
build_sb_red() {
    local sb="$1" neuter="${2:-}"
    build_sb "$sb" || return 2
    write_audit "$sb"
    [ -n "$neuter" ] && { round6_neuter "$sb" "$neuter" || return 3; }
    rm -f "$sb/repo/.ax-verify/runs.jsonl" "$sb/repo/.ax-verify/last_run.jsonl"
    return 0
}

echo "=== ax-prove-hermetic-runtime — ROUND 5 (runtime) + ROUND 6 (preflight / raw bytes) + ROUND 7 (privileged startup / filter.process) + ROUND 8 (worktree representation) ==="

# ── (F) NEGATIVE CONTROL, first: an unattacked sandbox must actually PASS ─────────────
SB_OK="$WORK/control"; build_sb "$SB_OK" || { echo "harness setup failed (control)" >&2; exit 2; }
write_audit "$SB_OK"
run_gate "$SB_OK" "$WORK/f.log"; F_RC=$?
note "(F) negative control (no attack)          : exit=$F_RC (want 0)"
if [ "$F_RC" -ne 0 ]; then
    violation "the UNATTACKED sandbox does not pass, so every 'blocked' below could be an artefact" \
              "of the harness rather than of the attack. Fix the harness, not the gate."
    head -3 "$WORK/f.log" >&2
    echo "ax-prove-hermetic-runtime: FAIL — control broken" >&2
    exit 1
fi
if grep -qE "GIT_CONTEXT_REDIRECTED|HELPER_FUNCTION_INJECTED|RATCHET_TOOLCHAIN_MODIFIED|AUDIT_TREE_DIRTY_NOW|AUDIT_FINGERPRINT" "$WORK/f.log"; then
    violation "the unattacked control already reports one of the attack codes."
fi

# ── (A) GIT CONTEXT REDIRECTION ──────────────────────────────────────────────────────
# The tree being audited is DIRTY; a CLEAN shadow checkout of the same commit is handed to git
# through the environment. Pre-fix, every read (status, fingerprint, toplevel) described the
# shadow, so the dirty tree certified itself clean.
SB_A="$WORK/redirect"; build_sb "$SB_A" || { echo "harness setup failed (A)" >&2; exit 2; }
write_audit "$SB_A"
git clone -q "$SB_A/repo" "$SB_A/shadow" >/dev/null 2>&1 || { echo "harness setup failed (A/clone)" >&2; exit 2; }
printf 'uncommitted\n' > "$SB_A/repo/DIRTY.txt"          # the tree that will be pushed is dirty
run_gate "$SB_A" "$WORK/a.log" GIT_DIR="$SB_A/shadow/.git" GIT_WORK_TREE="$SB_A/shadow"; A_RC=$?
note "(A) GIT_DIR/GIT_WORK_TREE → clean shadow  : exit=$A_RC (want non-zero)"
if [ "$A_RC" -eq 0 ]; then
    violation "a DIRTY tree certified itself through a redirected git context (exit 0). Every" \
              "head/status/fingerprint answer then describes a repository nobody is pushing."
    head -3 "$WORK/a.log" >&2
fi
# Attributability: neuter the two scrubs (shell + python) in a COPY and the same attack lands.
SB_A2="$WORK/redirect-prefix"
build_sb "$SB_A2" prefix || { echo "harness setup failed (A2 — neuter anchors stale?)" >&2; exit 2; }
write_audit "$SB_A2"
git clone -q "$SB_A2/repo" "$SB_A2/shadow" >/dev/null 2>&1
printf 'uncommitted\n' > "$SB_A2/repo/DIRTY.txt"
run_gate "$SB_A2" "$WORK/a2.log" GIT_DIR="$SB_A2/shadow/.git" GIT_WORK_TREE="$SB_A2/shadow"; A2_RC=$?
note "(A') same attack, ROUND-5 additions removed: exit=$A2_RC (want 0 = reproduces)"
if [ "$A2_RC" -ne 0 ]; then
    violation "in a pre-round-5 sandbox the attack did NOT reproduce (exit=$A2_RC), so (A) proves" \
              "nothing about the round-5 additions — something else is doing the blocking, and" \
              "this harness has gone stale. Fix the harness, not the gate."
    head -3 "$WORK/a2.log" >&2
fi

# ── (B)/(C)/(D) EXPORTED FUNCTION INJECTION THROUGH COMMAND DEPENDENCIES ─────────────
inject_case() {   # inject_case <tag> <label> <attack-body>
    local tag="$1" label="$2" body="$3" rc
    local sb="$WORK/inject-$tag"
    build_sb "$sb" || { echo "harness setup failed ($tag)" >&2; exit 2; }
    write_audit "$sb"
    { printf 'set -uo pipefail\n'; printf '%s\n' "$body"; printf 'builtin cd "$1" && bash "%s"\n' "$RECENCY_REL"; } \
        > "$WORK/attack-$tag.sh"
    bash "$WORK/attack-$tag.sh" "$sb/repo" > "$WORK/$tag.log" 2>&1; rc=$?
    note "($tag) $label: exit=$rc"
    # ROUND 6: the pure-keyword preflight refuses ANY exported function BEFORE the round-5
    # dependency-list scrub can name it, so these cases now report HERMETIC_PREFLIGHT_HOSTILE.
    # Either code is a refusal; what must never happen is exit 0.
    if ! grep -qE "HELPER_FUNCTION_INJECTED|HERMETIC_PREFLIGHT_HOSTILE" "$WORK/$tag.log" || [ "$rc" -eq 0 ]; then
        violation "an exported function named after a command the gate INVOKES was not refused" \
                  "(exit=$rc). bash imports exported functions across \`bash script.sh\`, so the" \
                  "caller supplies the program every check below runs."
        head -3 "$WORK/$tag.log" >&2
    fi
}
inject_case B "exported \`git\`                      " \
    'git() { case " $* " in *"rev-parse"*) echo 0000000000000000000000000000000000000000; return 0 ;; esac; command git "$@"; }
export -f git'
inject_case C "exported \`cd\` + \`pwd\`               " \
    'cd() { builtin cd "$@" 2>/dev/null || return 0; }
pwd() { echo /foreign/repo; }
export -f cd pwd'
inject_case D "exported \`python3\` returning 0       " \
    'python3() { return 0; }
export -f python3'

# ── (E1) THE FINGERPRINT HELPER, TAMPERED AND UNCOMMITTED ────────────────────────────
SB_E1="$WORK/tamper-uncommitted"; build_sb "$SB_E1" || { echo "harness setup failed (E1)" >&2; exit 2; }
write_audit "$SB_E1"
printf '#!/usr/bin/env python3\nprint("nogit")\n' > "$SB_E1/repo/$FP_REL"
run_gate "$SB_E1" "$WORK/e1.log"; E1_RC=$?
note "(E1) helper rewritten to lie, uncommitted : exit=$E1_RC (want non-zero)"
if [ "$E1_RC" -eq 0 ] || ! grep -q "RATCHET_TOOLCHAIN_MODIFIED" "$WORK/e1.log"; then
    violation "an uncommitted rewrite of tree_fingerprint.py was accepted (exit=$E1_RC). Its MODE" \
              "is unchanged — it is still a regular file — which is all the pre-round-5 checks" \
              "asked. \`print(\"nogit\")\` then disabled the recompute entirely."
    head -3 "$WORK/e1.log" >&2
fi

# ── (E2) THE SAME TAMPER, COMMITTED ─────────────────────────────────────────────────
# Now the working tree matches HEAD, so (E1)'s check is satisfied. The recompute is performed with
# the copy of the helper carried by the PREVIOUS RELEASE (origin/main), which is not the tampered
# one — the verifier no longer shares the runner's implementation.
SB_E2="$WORK/tamper-committed"; build_sb "$SB_E2" || { echo "harness setup failed (E2)" >&2; exit 2; }
write_audit "$SB_E2"
printf '#!/usr/bin/env python3\nimport sys\nprint("%s")\n' \
    "$(python3 -c 'print("d"*64)')" > "$SB_E2/repo/$FP_REL"
( builtin cd "$SB_E2/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "tamper" ) >/dev/null 2>&1
# re-point the record at the new head, still claiming the constant the tampered helper prints
write_audit "$SB_E2" "$(python3 -c 'print("d"*64)')"
run_gate "$SB_E2" "$WORK/e2.log"; E2_RC=$?
note "(E2) same tamper, COMMITTED               : exit=$E2_RC (want non-zero)"
if [ "$E2_RC" -eq 0 ] || ! grep -qE "AUDIT_FINGERPRINT_(MISMATCH|UNVERIFIABLE)" "$WORK/e2.log"; then
    violation "a COMMITTED rewrite of tree_fingerprint.py made the gate recompute the very" \
              "constant the record claims (exit=$E2_RC): the writer and the verifier were the" \
              "same compromised implementation. The recompute must use the previous release's copy."
    head -3 "$WORK/e2.log" >&2
fi

# ══ ROUND 6 ═══════════════════════════════════════════════════════════════════════════
# ── (G) EXPORTED COMMANDS THAT RAN BEFORE THE SCRUB (invariant alpha) ────────────────
preflight_case() {   # preflight_case <tag> <envvar> <label>
    local tag="$1" var="$2" label="$3" rc
    local sb="$WORK/pf-$tag"
    build_sb_red "$sb" || { echo "harness setup failed ($tag)" >&2; exit 2; }
    ( builtin cd "$sb/repo" && env "$var" bash "$sb/repo/$RECENCY_REL" ) > "$WORK/$tag.log" 2>&1; rc=$?
    note "(G/$tag) $label: exit=$rc (want non-zero)"
    if [ "$rc" -eq 0 ] || ! grep -q "HERMETIC_PREFLIGHT_HOSTILE" "$WORK/$tag.log"; then
        violation "an exported \`${tag}\` survived into the gate (exit=$rc). It is invoked BEFORE the" \
                  "round-5 scrub could look at it, so the scrub's dependency list is irrelevant."
        head -3 "$WORK/$tag.log" >&2
    fi
}
preflight_case set 'BASH_FUNC_set%%=() { exit 0; }' "exported \`set\`  (round-5: pre-push exit 0)  "
preflight_case bracket 'BASH_FUNC_[%%=() { exit 0; }' "exported \`[\`    (round-5: guard exit 0)    "
SB_G2="$WORK/pf-neutered"; build_sb_red "$SB_G2" preflight || { echo "harness setup failed (G')" >&2; exit 2; }
( builtin cd "$SB_G2/repo" && env 'BASH_FUNC_set%%=() { exit 0; }' bash "$SB_G2/repo/$RECENCY_REL" ) \
    > "$WORK/g2.log" 2>&1; G2_RC=$?
note "(G') same attack, ROUND-6 preflight removed: exit=$G2_RC (want 0 = reproduces)"
if [ "$G2_RC" -ne 0 ]; then
    violation "in a pre-round-6 sandbox the exported-\`set\` attack did NOT reproduce (exit=$G2_RC)," \
              "so (G) proves nothing about the preflight. The harness has gone stale."
fi

# ── (H) A PATH IS NOT EVEN A SMOKE TEST ─────────────────────────────────────────────
# CORRECTED SCOPE (reviewer ROUND 8 / P1-B): this proves a MIS-RESOLVED interpreter is
# refused. It does NOT prove the interpreter is authenticated — a hostile wrapper forwards the
# fixed public challenge to the real binary (reviewer round 7, P1-2), and PATH executables are
# declared trusted in TD-2026-07-30-(ratchet-threat-model).
mkdir -p "$WORK/fakebin"
ln -sf /usr/bin/true "$WORK/fakebin/python3" 2>/dev/null || cp /usr/bin/true "$WORK/fakebin/python3"
SB_H="$WORK/py-true"; build_sb_red "$SB_H" || { echo "harness setup failed (H)" >&2; exit 2; }
run_gate "$SB_H" "$WORK/h.log" PATH="$WORK/fakebin:$PATH"; H_RC=$?
note "(H) python3 → /usr/bin/true symlink       : exit=$H_RC (want non-zero)"
if [ "$H_RC" -eq 0 ] || ! grep -q "HERMETIC_TOOL_UNAUTHENTIC" "$WORK/h.log"; then
    violation "a symlink named python3 pointing at /usr/bin/true was accepted as the interpreter" \
              "(exit=$H_RC). -f/-x FOLLOW SYMLINKS and assert nothing; running the program is"
              "the smoke test that catches a stub. It is not authentication."
    head -3 "$WORK/h.log" >&2
fi
SB_H2="$WORK/py-true-neutered"; build_sb_red "$SB_H2" smoketest || { echo "harness setup failed (H')" >&2; exit 2; }
run_gate "$SB_H2" "$WORK/h2.log" PATH="$WORK/fakebin:$PATH"; H2_RC=$?
note "(H') same attack, self-report removed      : exit=$H2_RC (want 0 = reproduces)"
if [ "$H2_RC" -ne 0 ]; then
    violation "the /usr/bin/true interpreter did NOT reproduce with the smoke test removed" \
              "(exit=$H2_RC); (H) is therefore not attributable to it. Harness stale."
fi

# ── (I) PYTHON STARTUP HOOK ─────────────────────────────────────────────────────────
mkdir -p "$WORK/pyhook"; printf 'import os\nos._exit(0)\n' > "$WORK/pyhook/sitecustomize.py"
SB_I="$WORK/pyhook-sb"; build_sb_red "$SB_I" || { echo "harness setup failed (I)" >&2; exit 2; }
run_gate "$SB_I" "$WORK/i.log" PYTHONPATH="$WORK/pyhook"; I_RC=$?
note "(I) PYTHONPATH sitecustomize os._exit(0)  : exit=$I_RC (want non-zero)"
if [ "$I_RC" -eq 0 ] || ! grep -q "AUDIT_LOG_MISSING" "$WORK/i.log"; then
    violation "a sitecustomize.py reached via PYTHONPATH silenced the gate (exit=$I_RC): the whole" \
              "python body is skipped before it runs a line of ours."
    head -3 "$WORK/i.log" >&2
fi

# ── (J) BYTE CLAIMS THROUGH A CLEAN FILTER (invariant beta) ─────────────────────────
SB_J="$WORK/filter"; build_sb "$SB_J" || { echo "harness setup failed (J)" >&2; exit 2; }
git -C "$SB_J/repo" show "HEAD:$FP_REL" > "$SB_J/committed_fp.py" 2>/dev/null
git -C "$SB_J/repo" config filter.axmask.clean "cat $SB_J/committed_fp.py"
mkdir -p "$SB_J/repo/.git/info"; printf '%s filter=axmask\n' "$FP_REL" > "$SB_J/repo/.git/info/attributes"
printf '\n# TAMPERED — attacker payload\n' >> "$SB_J/repo/$FP_REL"
( builtin cd "$SB_J/repo" && bash -p -c 'source practices/scripts/lib/release_anchor.sh
ax_ratchet_toolchain_authentic "$PWD" probe HEAD $(ax_ratchet_toolchain_paths)' ) > "$WORK/j.log" 2>&1; J_RC=$?
note "(J) clean filter masks a tampered file    : exit=$J_RC (want non-zero)"
if [ "$J_RC" -eq 0 ] || ! grep -qE "GIT_FILTERS_PRESENT|RATCHET_TOOLCHAIN_MODIFIED" "$WORK/j.log"; then
    violation "a \`filter.<n>.clean\` that echoes the committed copy made a TAMPERED toolchain file" \
              "compare byte-identical to its blob (exit=$J_RC). Byte claims must be raw."
    head -3 "$WORK/j.log" >&2
fi
SB_J2="$WORK/filter-neutered"; build_sb "$SB_J2" || { echo "harness setup failed (J')" >&2; exit 2; }
round6_neuter "$SB_J2" rawbytes || { echo "harness setup failed (J'/neuter stale)" >&2; exit 2; }
git -C "$SB_J2/repo" show "HEAD:$FP_REL" > "$SB_J2/committed_fp.py" 2>/dev/null
git -C "$SB_J2/repo" config filter.axmask.clean "cat $SB_J2/committed_fp.py"
mkdir -p "$SB_J2/repo/.git/info"; printf '%s filter=axmask\n' "$FP_REL" > "$SB_J2/repo/.git/info/attributes"
printf '\n# TAMPERED — attacker payload\n' >> "$SB_J2/repo/$FP_REL"
( builtin cd "$SB_J2/repo" && bash -p -c 'source practices/scripts/lib/release_anchor.sh
ax_ratchet_toolchain_authentic "$PWD" probe HEAD $(ax_ratchet_toolchain_paths)' ) > "$WORK/j2.log" 2>&1; J2_RC=$?
note "(J') same tamper, raw comparison reverted  : exit=$J2_RC (want 0 = reproduces)"
if [ "$J2_RC" -ne 0 ]; then
    violation "with the byte comparison reverted to \`git diff --quiet\` the filter mask did NOT" \
              "reproduce (exit=$J2_RC); (J) is not attributable to the raw-byte fix. Harness stale."
    head -3 "$WORK/j2.log" >&2
fi

# ── (K) A COMMITTED PUSH-ONLY BYPASS (the self-authentication circle) ────────────────
backdoor() {   # backdoor <sb> — commit the reviewer's push-only bypass into the tree's guard
    "${AX_PY_BIN:-python3}" - "$1/repo/$RECENCY_REL" <<'PY'
import sys, pathlib
p = pathlib.Path(sys.argv[1]); s = p.read_text(encoding="utf-8")
needle = "\n".join([
    '        *) echo "completion_checklist_recency_guard: unknown arg: $1" >&2; exit 2 ;;',
    '    esac',
    'done',
    ''])
assert s.count(needle) == 1, "backdoor anchor stale"
patch = needle + "\n".join(['', 'if [ -n "$EXPECT_SHA" ]; then exit 0; fi', ''])
p.write_text(s.replace(needle, patch), encoding="utf-8")
PY
    ( builtin cd "$1/repo" && git add -A && git "${GIT_ID[@]}" commit -q -m "backdoor" ) >/dev/null 2>&1
}
push_spec() {   # push_spec <sb> — the stdin git feeds the hook for a push that advances main
    printf 'refs/heads/main %s refs/heads/main %s
' \
        "$(git -C "$1/repo" rev-parse HEAD)" "$(git -C "$1/repo" rev-parse refs/remotes/origin/main)"
}
SB_K="$WORK/backdoor"; build_sb "$SB_K" || { echo "harness setup failed (K)" >&2; exit 2; }
write_audit "$SB_K"
backdoor "$SB_K" || { echo "harness setup failed (K/anchor stale)" >&2; exit 2; }
push_spec "$SB_K" | ( builtin cd "$SB_K/repo" && bash "$SB_K/repo/.githooks/pre-push" origin "$SB_K/repo" ) \
    > "$WORK/k.log" 2>&1; K_RC=$?
note "(K) COMMITTED push-only bypass in the guard: exit=$K_RC (want non-zero)"
if [ "$K_RC" -eq 0 ] || ! grep -q "RATCHET_PRIOR_RELEASE_GUARD_FAILED" "$WORK/k.log"; then
    violation "a COMMITTED \`[ -n \"\$EXPECT_SHA\" ] && exit 0\` inside the recency guard passed the" \
              "push gate (exit=$K_RC). Every self-check anchored on HEAD certifies it, because a" \
              "committed backdoor IS HEAD; the prior release's copy must be the one that runs."
    head -5 "$WORK/k.log" >&2
fi
SB_K2="$WORK/backdoor-neutered"; build_sb "$SB_K2" || { echo "harness setup failed (K')" >&2; exit 2; }
write_audit "$SB_K2"
round6_neuter "$SB_K2" priorrelease || { echo "harness setup failed (K'/neuter stale)" >&2; exit 2; }
backdoor "$SB_K2" || { echo "harness setup failed (K'/anchor stale)" >&2; exit 2; }
push_spec "$SB_K2" | ( builtin cd "$SB_K2/repo" && bash "$SB_K2/repo/.githooks/pre-push" origin "$SB_K2/repo" ) \
    > "$WORK/k2.log" 2>&1; K2_RC=$?
note "(K') same bypass, prior-release re-run gone: exit=$K2_RC (want 0 = reproduces)"
if [ "$K2_RC" -ne 0 ]; then
    violation "with the prior-release re-run removed the committed bypass did NOT reproduce" \
              "(exit=$K2_RC); (K) is not attributable to it. Harness stale."
    head -5 "$WORK/k2.log" >&2
fi

# ── (L) POSITIVE CONTROL FOR THE NEW PUSH PATH ──────────────────────────────────────
# (K) shows the prior-release re-run REFUSING. This shows it AGREEING on an honest tree — without
# which "it blocks" could just mean "it always blocks", and the gate would be unshippable.
SB_L="$WORK/push-ok"; build_sb "$SB_L" || { echo "harness setup failed (L)" >&2; exit 2; }
write_audit "$SB_L"
push_spec "$SB_L" | ( builtin cd "$SB_L/repo" && bash "$SB_L/repo/.githooks/pre-push" origin "$SB_L/repo" ) \
    > "$WORK/l.log" 2>&1; L_RC=$?
note "(L) honest push through the FULL hook     : exit=$L_RC (want 0)"
if [ "$L_RC" -ne 0 ] || ! grep -q "previous release's recency guard also PASSES" "$WORK/l.log"; then
    violation "the round-6 prior-release re-run does not agree on an HONEST tree (exit=$L_RC), so" \
              "(K)'s refusal is not evidence of anything: a gate that always blocks blocks nothing."
    head -8 "$WORK/l.log" >&2
fi

# ══ ROUND 7 (TD-2026-07-30-P1-privileged-startup / P1-3) ════════════════════════════
# INVARIANT (alpha) restated honestly: an IN-SCRIPT sensor cannot see a startup file, because
# bash runs $BASH_ENV BEFORE the script's first line. Round 6 answered "what executes first
# INSIDE the file"; round 7 answers "what executes first AT ALL".

# ── (M) SELF-ERASING $BASH_ENV ──────────────────────────────────────────────────────
# The payload defines a NON-EXPORTED function (so no BASH_FUNC_* entry exists) and then unsets
# BASH_ENV (so the environ the round-6 preflight reads is clean). Nothing is left to detect; the
# first `set -uo pipefail` the gate runs is the attacker's.
printf 'builtin unset BASH_ENV ENV\nfunction set(){ builtin exit 0; }\n' > "$WORK/bashenv_payload.sh"
SB_M="$WORK/bashenv"; build_sb_red "$SB_M" || { echo "harness setup failed (M)" >&2; exit 2; }
run_gate "$SB_M" "$WORK/m.log" BASH_ENV="$WORK/bashenv_payload.sh"; M_RC=$?
note "(M) self-erasing BASH_ENV shadowing \`set\`  : exit=$M_RC (want non-zero)"
if [ "$M_RC" -eq 0 ] || ! grep -qE "AUDIT_LOG_MISSING|HERMETIC_" "$WORK/m.log"; then
    violation "a \$BASH_ENV that erases its own trace turned the gate's honest non-zero verdict" \
              "into exit=$M_RC. bash sources that file before line 1, so no in-script preflight" \
              "can see it — the entry must re-exec itself into bash privileged mode."
    head -5 "$WORK/m.log" >&2
fi
SB_M2="$WORK/bashenv-neutered"; build_sb_red "$SB_M2" || { echo "harness setup failed (M')" >&2; exit 2; }
round7_neuter "$SB_M2" privileged || { echo "harness setup failed (M'/neuter stale)" >&2; exit 2; }
run_gate "$SB_M2" "$WORK/m2.log" BASH_ENV="$WORK/bashenv_payload.sh"; M2_RC=$?
note "(M') same payload, privileged re-exec gone: exit=$M2_RC (want 0 = reproduces)"
if [ "$M2_RC" -ne 0 ]; then
    violation "with the round-7 privileged re-exec removed the BASH_ENV payload did NOT reproduce" \
              "(exit=$M2_RC); (M) is not attributable to it. Harness stale."
    head -5 "$WORK/m2.log" >&2
fi
# (M'') the loop marker is NOT a skip switch: presetting it must ABORT, not proceed unprivileged.
run_gate "$SB_M" "$WORK/m3.log" AX_PRIV_REEXEC=1; M3_RC=$?
note "(M'') AX_PRIV_REEXEC preset by the attacker: exit=$M3_RC (want non-zero)"
if [ "$M3_RC" -eq 0 ] || ! grep -q "HERMETIC_PRIVILEGED_UNREACHABLE" "$WORK/m3.log"; then
    violation "presetting AX_PRIV_REEXEC skipped the privileged re-exec instead of aborting" \
              "(exit=$M3_RC). A marker an attacker can set must never be trusted to mean 'already" \
              "privileged' — it may only mean 'a re-exec was attempted', which is a refusal."
    head -5 "$WORK/m3.log" >&2
fi

# ── (N) THE TWO SOURCED LIBS — `export` AHEAD OF THE PREFLIGHT ──────────────────────
# Round 6 claimed its preflight was "the first executable text of all 8 entries". For the two
# SOURCED libs that was FALSE: `export GIT_NO_REPLACE_OBJECTS=1` ran first, and `export` is an
# ordinary command lookup. A sourced file cannot re-exec without replacing its caller, so it
# ASSERTS privileged mode instead — a non-privileged caller is refused outright.
SB_N="$WORK/sourced-export"; build_sb "$SB_N" || { echo "harness setup failed (N)" >&2; exit 2; }
( builtin cd "$SB_N/repo" && env "BASH_FUNC_export%%=() { builtin exit 0; }" \
    bash -c 'source practices/scripts/lib/release_anchor.sh; echo SOURCED_OK' ) \
    > "$WORK/n.log" 2>&1; N_RC=$?
note "(N) exported export() + non-privileged caller: exit=$N_RC (want non-zero)"
if [ "$N_RC" -eq 0 ] || grep -q "SOURCED_OK" "$WORK/n.log"; then
    violation "release_anchor.sh executed inside a shell that had already run attacker code" \
              "(exit=$N_RC). Its first executable text must assert bash privileged mode, and the" \
              "pre-preflight \`export\` must sit BELOW that assertion."
    head -5 "$WORK/n.log" >&2
fi
SB_N2="$WORK/sourced-export-neutered"; build_sb "$SB_N2" || { echo "harness setup failed (N')" >&2; exit 2; }
round7_neuter "$SB_N2" sourcedpriv || { echo "harness setup failed (N'/neuter stale)" >&2; exit 2; }
( builtin cd "$SB_N2/repo" && env "BASH_FUNC_export%%=() { builtin exit 0; }" \
    bash -c 'source practices/scripts/lib/release_anchor.sh; echo SOURCED_OK' ) \
    > "$WORK/n2.log" 2>&1; N2_RC=$?
note "(N') same attack, round-6 shape restored  : exit=$N2_RC (want 0 = reproduces)"
if [ "$N2_RC" -ne 0 ]; then
    violation "with the privileged assertion removed and \`export\` back above the preflight, the" \
              "exported export() did NOT reproduce (exit=$N2_RC); (N) is not attributable. Stale."
    head -5 "$WORK/n2.log" >&2
fi

# ── (O) filter.<n>.process — THE KEYWORD SUBSET (invariant beta, second pass) ───────
# Round 6 refused `filter.<n>.clean|smudge`. git's LONG-RUNNING FILTER PROTOCOL is declared as
# `filter.<n>.process` and converts content identically. With only that key set, a tracked file
# whose disk bytes differ from its committed blob makes `git status --porcelain` EMPTY — so the
# fingerprint's raw-byte loop never opens it and the push gate certifies a tree it never read.
# THE TARGET IS AN ORDINARY TRACKED FILE, not a toolchain path: round 6 already ran `check-attr
# filter` over the nine ratchet-critical paths, so attacking one of those would be caught by a
# check that predates this round and the reproduction would prove nothing. Everything ELSE in the
# tree — which is what R25 actually tests — had no such cover.
# WHY THE TWO VERSIONS ARE THE SAME LENGTH: git's ie_modified() returns "modified" from the stat
# comparison alone when the recorded size differs and is non-zero, WITHOUT consulting the filter.
# An attacker therefore pads; measured, an equal-length swap makes `git status --porcelain` empty
# while a length-changing one does not. Using an unequal length here would have made the scenario
# pass for the wrong reason.
TAMPER_REL="sentinel.bin"
SB_O="$WORK/procfilter"; build_sb "$SB_O" || { echo "harness setup failed (O)" >&2; exit 2; }
printf 'HOSTILE-PAYLOAD!\n' > "$SB_O/repo/$TAMPER_REL"
( builtin cd "$SB_O/repo" && git add "$TAMPER_REL" && git "${GIT_ID[@]}" commit -q -m sentinel ) >/dev/null 2>&1 \
    || { echo "harness setup failed (O/sentinel)" >&2; exit 2; }
proc_filter_prepare "$SB_O" "$TAMPER_REL" || { echo "harness setup failed (O/filter)" >&2; exit 2; }
write_audit "$SB_O"
proc_filter_enable "$SB_O" || { echo "harness setup failed (O/enable)" >&2; exit 2; }
printf 'BENIGN-COMMITTED\n' > "$SB_O/repo/$TAMPER_REL"
run_gate "$SB_O" "$WORK/o.log" AX_MASK_FILE="$SB_O/mask.bin"; O_RC=$?
note "(O) filter.<n>.process masks a tampered file: exit=$O_RC (want non-zero)"
if [ "$O_RC" -eq 0 ] || ! grep -qE "GIT_FILTERS_PRESENT|GIT_RAW_INDEX_DIVERGENCE|RATCHET_TOOLCHAIN_MODIFIED" "$WORK/o.log"; then
    violation "a \`filter.<n>.process\` driver made a tampered tracked file invisible to the push" \
              "gate (exit=$O_RC). The refusal must cover ANY key under a filter driver, and the" \
              "byte comparison must be made without asking git what the file contains."
    head -5 "$WORK/o.log" >&2
fi
SB_O2="$WORK/procfilter-neutered"; build_sb "$SB_O2" || { echo "harness setup failed (O')" >&2; exit 2; }
round7_neuter "$SB_O2" filterproc || { echo "harness setup failed (O'/neuter stale)" >&2; exit 2; }
printf 'HOSTILE-PAYLOAD!\n' > "$SB_O2/repo/$TAMPER_REL"
( builtin cd "$SB_O2/repo" && git add "$TAMPER_REL" && git "${GIT_ID[@]}" commit -q -m sentinel ) >/dev/null 2>&1 \
    || { echo "harness setup failed (O'/sentinel)" >&2; exit 2; }
proc_filter_prepare "$SB_O2" "$TAMPER_REL" || { echo "harness setup failed (O'/filter)" >&2; exit 2; }
write_audit "$SB_O2"
proc_filter_enable "$SB_O2" || { echo "harness setup failed (O'/enable)" >&2; exit 2; }
printf 'BENIGN-COMMITTED\n' > "$SB_O2/repo/$TAMPER_REL"
run_gate "$SB_O2" "$WORK/o2.log" AX_MASK_FILE="$SB_O2/mask.bin"; O2_RC=$?
note "(O') same tamper, keyword subset restored : exit=$O2_RC (want 0 = reproduces)"
if [ "$O2_RC" -ne 0 ]; then
    violation "with the clean|smudge keyword subset restored and the raw sweep removed, the" \
              "\`.process\` mask did NOT reproduce (exit=$O2_RC); (O) is not attributable. Stale."
    head -5 "$WORK/o2.log" >&2
fi

# ══ ROUND 8 (TD-2026-07-31-(P1-worktree-representation) / P1-A) ═══════════════════════
# Invariant (beta) again, from the side round 7 left open: a claim about bytes must be made on raw
# bytes — AND a path whose on-disk REPRESENTATION is not the one the index records is a path about
# which this gate has no byte claim to make. Round 7 spelled that `continue`.

# r8_apply <sb> <kind>  kind ∈ symlink|absent — the attack, applied AFTER write_audit so that HEAD
# and the recorded fingerprint are the honest ones a real run would have produced.
r8_apply() {
    local sb="$1" kind="$2"
    case "$kind" in
        symlink)
            git -C "$sb/repo" update-index --assume-unchanged "$R8_REL" || return 2
            rm -f "$sb/repo/$R8_REL" || return 2
            ln -s "$sb/benign.yaml" "$sb/repo/$R8_REL" || return 2 ;;
        absent)
            git -C "$sb/repo" update-index --skip-worktree "$R8_REL" || return 2
            rm -f "$sb/repo/$R8_REL" || return 2 ;;
        *) return 2 ;;
    esac
    # The premise of the whole class: git must report NOTHING. If a future git reports the swap,
    # the scenario is measuring a different refusal and must say so rather than pass quietly.
    if [ -n "$(git -C "$sb/repo" status --porcelain)" ]; then
        violation "harness premise broken: \`git status --porcelain\` is NOT empty after the" \
                  "$kind swap, so this scenario no longer reproduces the reviewer's setup."
        return 3
    fi
    return 0
}

r8_case() {   # r8_case <tag> <kind> <label> <expected-codes-regex> [neuter]
    local tag="$1" kind="$2" label="$3" want="$4" neuter="${5:-}" sb rc
    sb="$WORK/r8-$tag"
    build_sb "$sb" || { echo "harness setup failed ($tag)" >&2; exit 2; }
    r8_sentinel "$sb" || { echo "harness setup failed ($tag/sentinel)" >&2; exit 2; }
    # The neuter COMMITS (that is what makes the sandbox a self-consistent old world), so it must
    # land BEFORE write_audit — otherwise HEAD moves under the audit line and every neutered case
    # fails with AUDIT_STALE_HEAD, i.e. for a reason that has nothing to do with the attack.
    if [ -n "$neuter" ]; then
        round8_neuter "$sb" "$neuter" || { echo "harness setup failed ($tag/neuter stale)" >&2; exit 2; }
    fi
    write_audit "$sb"
    # ROUND 9 / (d): SETUP FAILURE IS LOUD. This used to be `|| return 0`, which turned a broken
    # case — a failed `update-index`, a missing sentinel, an `ln -s` that could not run — into a
    # SILENT PASS: the scenario never ran, nothing was measured, and the harness still printed its
    # green summary. A proof harness that can skip its own cases proves nothing, so a setup
    # failure now aborts the whole run with the harness-error exit.
    r8_apply "$sb" "$kind"
    case $? in
        0) ;;
        3) return 1 ;;   # premise broken: r8_apply already called violation()
        *) echo "ax-prove-hermetic-runtime: harness setup failed ($tag/apply $kind)" >&2; exit 2 ;;
    esac
    run_gate "$sb" "$WORK/$tag.log"; rc=$?
    note "($tag) $label: exit=$rc"
    if [ -n "$want" ]; then
        if [ "$rc" -eq 0 ] || ! grep -qE "$want" "$WORK/$tag.log"; then
            violation "the $kind representation swap was not refused with $want (exit=$rc)." \
                      "\`git status\` is empty and the index still names the committed blob, so" \
                      "R25 verified something the push will not ship."
            head -5 "$WORK/$tag.log" >&2
        fi
    else
        if [ "$rc" -ne 0 ]; then
            violation "in a pre-round-8 sandbox the $kind swap did NOT reproduce (exit=$rc), so" \
                      "the round-8 refusals are not attributable — this harness has gone stale."
            head -5 "$WORK/$tag.log" >&2
        fi
    fi
}

# (P)/(Q) the two reproductions against the LIVE gate.
r8_case P symlink "index-regular path → SYMLINK (assume-unchanged)" \
    "GIT_INDEX_FLAGS_SET|GIT_WORKTREE_TYPE_MISMATCH"
r8_case Q absent  "tracked path DELETED (skip-worktree)          " \
    "GIT_INDEX_FLAGS_SET|GIT_TRACKED_PATH_ABSENT"
# (P')/(Q') the same attacks in a committed pre-round-8 world: they must land again.
r8_case P2 symlink "same swap, round-8 refusals removed          " "" all
r8_case Q2 absent  "same deletion, round-8 refusals removed      " "" all
# (R) LAYER INDEPENDENCE — only the index-bit refusal is gone; the representation backstop alone
# must still refuse. A sparse checkout sets skip-worktree, so without this the backstop would
# never be exercised and could rot into dead code behind the bit check.
r8_case R1 symlink "swap, ONLY the index-bit refusal removed     " "GIT_WORKTREE_TYPE_MISMATCH" bits
r8_case R2 absent  "deletion, ONLY the index-bit refusal removed " "GIT_TRACKED_PATH_ABSENT" bits

# (S) OVER-CORRECTION CONTROL: an UNINITIALIZED gitlink is the ordinary post-clone shape (all
# three in this catalog are empty fixture directories). Blocking it would refuse every fresh clone
# to close nothing, so the sandbox that carries one must still PASS.
SB_S="$WORK/r8-gitlink"; build_sb "$SB_S" || { echo "harness setup failed (S)" >&2; exit 2; }
S_HEAD="$(git -C "$SB_S/repo" rev-parse HEAD)"
mkdir -p "$SB_S/repo/vendor/sub"
( builtin cd "$SB_S/repo" && git update-index --add --cacheinfo "160000,$S_HEAD,vendor/sub" \
  && git "${GIT_ID[@]}" commit -q -m gitlink ) >/dev/null 2>&1 \
    || { echo "harness setup failed (S/gitlink)" >&2; exit 2; }
write_audit "$SB_S"
run_gate "$SB_S" "$WORK/s.log"; S_RC=$?
note "(S) control: UNINITIALIZED gitlink present : exit=$S_RC (want 0)"
if [ "$S_RC" -ne 0 ]; then
    violation "a tree carrying an uninitialized submodule was refused (exit=$S_RC). That is the" \
              "ordinary post-clone shape and it hides nothing — the round-8 gitlink binding must" \
              "fire only on an INITIALIZED submodule that is not at the recorded commit."
    head -5 "$WORK/s.log" >&2
fi

# ══ ROUND 9 (TD-2026-07-30-(P1-representation-parity)) ═══════════════════════════════
# Round 8 separated the SHAPES and then compared only BLOB BYTES. A tracked path's representation
# carries three more facts, none of which any digest here holds:
#   (T)/(U) the EXECUTABLE BIT, both directions. `core.fileMode=false` tells git to stop REPORTING
#       it, so status stays EMPTY and the fingerprint stays at the clean-tree constant while R25's
#       118 direct `./gradlew` invocations run a file the push records as 100644 — which a fresh
#       checkout cannot execute at all.          → GIT_EXEC_BIT_DIVERGENCE
#   (V) a gitlink with NO gitdir whose directory is nevertheless POPULATED. Round 8's exemption
#       returned success on the ABSENCE of `<gitlink>/.git` alone and never required the directory
#       to be EMPTY, so a committed step can `bash vendor/sub/check.sh` a file that the push does
#       not ship (a gitlink ships a sha; a fresh clone gets an empty directory).
#                                                → GIT_GITLINK_UNINITIALIZED_POPULATED
#   (W) two index entries differing only in CASE that a case-insensitive filesystem serves from ONE
#       file.                                    → GIT_CASEFOLD_ALIAS
# and ROUND 9 / (c) adds the two POSITIVE REGRESSIONS round 8 claimed but never exercised:
#   (X) the MIRROR of (P): an index-SYMLINK path that is a REGULAR FILE on disk.
#                                                → GIT_WORKTREE_TYPE_MISMATCH
#   (Y) an INITIALIZED submodule at a commit the superproject does not record.
#                                                → GIT_GITLINK_DIVERGENCE
# Each has a pre-round-9 (or pre-round-8) twin in which it lands again, so the refusal is
# attributable to the fix and not to the sandbox.

# ── ROUND 9/10 neuters. Same exactly-once anchor contract as every earlier neuter.
# `all` removes the three round-9 refusals from BOTH implementations; `bits` (reused from round 8)
# removes only the index-flag refusal, which is what makes (X) separately attributable.
#
# ROUND 10 / P2 (reviewer): THE SHIPPED ROUND-9 NEUTERS DISABLED BOTH IMPLEMENTATIONS TOGETHER, so
# a twin that lands again proved only "the pair is load-bearing" — not that EITHER of them is. The
# keys are therefore split per implementation:
#     all / r10all      both implementations   → the attack must LAND (exit 0)
#     guard / r10guard  only the 12c sweep     → the FINGERPRINT HELPER must still refuse; it is
#                                                the prior-release copy the recompute runs, so its
#                                                refusal surfaces as AUDIT_FINGERPRINT_UNVERIFIABLE
#     fp / r10fp        only the helper        → the 12c sweep must still refuse, on its own code
# Only the neutered files are staged: `git add -A` would ALSO re-add the attack's own index state
# from disk, which on a case-insensitive filesystem HEALS a casefold premise (the two entries
# collapse onto the one file's bytes) and makes the twin pass for a reason that is not the neuter.
round9_neuter() {   # round9_neuter <sb> <what>
                    # what ∈ all|guard|fp|r10all|r10guard|r10fp|r11all|r11guard|r11fp
    "${AX_PY_BIN:-python3}" - "$1/repo" "$2" <<'PY'
import sys, pathlib
repo, what = pathlib.Path(sys.argv[1]), sys.argv[2]
GUARD = repo / "practices/evals/completion_checklist_recency_guard.sh"
FP = repo / "practices/scripts/lib/tree_fingerprint.py"
# An entry is (path, anchor) — the anchor's `if` is killed — or (path, anchor, replacement) when
# the fix is not a report but a COMPUTATION. ROUND 11's fix is the latter: the round-9/10 refusals
# already existed and fired, they were merely keyed with `bytes.lower()`, so the honest neuter is
# to put `bytes.lower()` back. Killing the report instead would neuter round 10 as well and prove
# nothing about the fold.
R9_GUARD = [(GUARD, "        if _execbits:"), (GUARD, "        if _gldirt:"),
            (GUARD, "        if _aliased:")]
R9_FP = [(FP, "    if execbits:"), (FP, "    if gldirt:"), (FP, "    if aliased:")]
R10_GUARD = [(GUARD, "        if _diraliased:")]
R10_FP = [(FP, "    if diraliased:")]
R11_GUARD = [(GUARD,
              "                _casefold.setdefault(_ax_fold_path_key(_pfx, _foldcache), "
              "{}).setdefault(",
              "                _casefold.setdefault(_pfx.lower(), {}).setdefault(")]
R11_FP = [(FP,
           "        casefold.setdefault(_fold_path_key(prefix, foldcache), {}).setdefault(",
           "        casefold.setdefault(prefix.lower(), {}).setdefault(")]
# ROUND 12's fix is a COMPUTATION too, and the honest neuter is the SAME shape: put the round-11
# key back EXACTLY as it was — canonical caseless, but with ignorable format characters PRESERVED.
# It removes ONLY the strip, so anything that lands again is attributable to the strip and to
# nothing else (the round-10 report and the round-11 fold both stay live).
IGNORABLE_STRIP = '            s = "".join(ch for ch in s if unicodedata.category(ch) != "Cf")'
IGNORABLE_DEAD = "            s = s  # ROUND-12 NEUTER: the ignorable strip is removed"
R12_GUARD = [(GUARD, IGNORABLE_STRIP, IGNORABLE_DEAD)]
R12_FP = [(FP, IGNORABLE_STRIP, IGNORABLE_DEAD)]
pairs = {
    "all": R9_GUARD + R9_FP,
    "guard": R9_GUARD,
    "fp": R9_FP,
    "r10all": R10_GUARD + R10_FP,
    "r10guard": R10_GUARD,
    "r10fp": R10_FP,
    "r11all": R11_GUARD + R11_FP,
    "r11guard": R11_GUARD,
    "r11fp": R11_FP,
    "r12all": R12_GUARD + R12_FP,
    "r12guard": R12_GUARD,
    "r12fp": R12_FP,
}
if what not in pairs:
    print(f"unknown round9/10/11/12 neuter: {what}", file=sys.stderr)
    sys.exit(3)
for entry in pairs[what]:
    path, anchor = entry[0], entry[1]
    text = path.read_text(encoding="utf-8")
    n = text.count(anchor)
    if n != 1:
        print(f"round9 neuter anchor occurs {n}x (expected 1) in {path}: {anchor!r}",
              file=sys.stderr)
        sys.exit(3)
    if len(entry) > 2:
        dead = entry[2]
    else:
        dead = anchor[:len(anchor) - len(anchor.lstrip())] + "if False:"
    path.write_text(text.replace(anchor, dead, 1), encoding="utf-8")
PY
    local rc=$?
    [ "$rc" -ne 0 ] && return "$rc"
    ( builtin cd "$1/repo" \
      && git add -- practices/evals/completion_checklist_recency_guard.sh \
                    practices/scripts/lib/tree_fingerprint.py \
      && git "${GIT_ID[@]}" commit -q -m "pre-round-9/10" \
      && git update-ref refs/remotes/origin/main HEAD \
      && git push -q -f origin HEAD:refs/heads/main ) >/dev/null 2>&1
    return 0
}

# r11_plant <sb> <A-hex> <B-hex> — ROUND 11 / P1. Commit a two-spelling alias topology whose
# spellings are given as RAW BYTES: spelling A holds `check.sh` (which reads `A/helper`), spelling
# B holds `helper`. On a case-/normalization-INSENSITIVE filesystem A and B are one directory, so
# the check runs locally while the pushed tree records only `B/helper`.
# WHY THE TREE OBJECT IS WRITTEN BY HAND rather than with `git add`/`update-index --cacheinfo`/
# `--index-info`/`mktree`: MEASURED on Apple Git 2.50.1, every one of those PRECOMPOSES an NFD path
# to NFC — even with `core.precomposeunicode=false` — so none of them can express the NFD spelling.
# A repository created on a normalization-SENSITIVE filesystem hands exactly this tree to a macOS
# clone (verified: clone → index holds 65cc81, `git status` EMPTY, `bash é/check.sh` → PASS), so
# writing the object is reproducing an arriving state, not inventing an unreachable one.
# The ignore rule for the PHYSICAL spelling of `helper` goes in a PER-DIRECTORY .gitignore because
# write_audit rewrites the ROOT one; ignore rules never apply to tracked paths, so the index entry
# under spelling B is still checked.
r11_plant() {
    local sb="$1" ah="$2" bh="$3"
    "${AX_PY_BIN:-python3}" - "$sb/repo" "$ah" "$bh" <<'PY'
import os, subprocess, sys
repo, ah, bh = sys.argv[1], sys.argv[2], sys.argv[3]
A, B = bytes.fromhex(ah), bytes.fromhex(bh)
os.chdir(repo)
ENV = {**os.environ,
       "GIT_AUTHOR_NAME": "ax", "GIT_AUTHOR_EMAIL": "ax@example.invalid",
       "GIT_COMMITTER_NAME": "ax", "GIT_COMMITTER_EMAIL": "ax@example.invalid",
       "GIT_AUTHOR_DATE": "2026-08-01T00:00:00Z",
       "GIT_COMMITTER_DATE": "2026-08-01T00:00:00Z"}


def g(*a, **kw):
    return subprocess.run(["git", *a], stdout=subprocess.PIPE, check=True, env=ENV, **kw).stdout


def hobj(data, typ="blob"):
    return g("hash-object", "-t", typ, "-w", "--stdin", input=data).strip().decode()


os.makedirs(A, exist_ok=True)
check = b'cat "' + A + b'/helper"\n'
open(os.path.join(A, b"check.sh"), "wb").write(check)
open(os.path.join(A, b"helper"), "wb").write(b"PASS\n")
open(os.path.join(A, b".gitignore"), "wb").write(b"helper\n")
gi, chk, hlp = hobj(b"helper\n"), hobj(check), hobj(b"PASS\n")


def ent(mode, name, sha):
    return mode + b" " + name + b"\0" + bytes.fromhex(sha)


tA = hobj(ent(b"100644", b".gitignore", gi) + ent(b"100644", b"check.sh", chk), "tree")
tB = hobj(ent(b"100644", b"helper", hlp), "tree")
raw = g("cat-file", "tree", "HEAD^{tree}")
out, i = [], 0
while i < len(raw):
    sp = raw.index(b" ", i)
    nul = raw.index(b"\0", sp)
    mode, name, sha = raw[i:sp], raw[sp + 1:nul], raw[nul + 1:nul + 21]
    i = nul + 21
    if name not in (A, B):
        out.append((mode, name, sha))
out.append((b"40000", A, bytes.fromhex(tA)))
out.append((b"40000", B, bytes.fromhex(tB)))
out.sort(key=lambda e: e[1] + (b"/" if e[0] == b"40000" else b""))
root = hobj(b"".join(m + b" " + n + b"\0" + s for m, n, s in out), "tree")
head = g("rev-parse", "HEAD").strip().decode()
commit = g("commit-tree", root, "-p", head, "-m", "alias").strip().decode()
subprocess.run(["git", "update-ref", g("symbolic-ref", "HEAD").strip().decode(), commit],
               check=True, env=ENV)
subprocess.run(["git", "read-tree", root], check=True, env=ENV)
subprocess.run(["git", "update-index", "--refresh", "-q"], env=ENV,
               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
PY
    return $?
}

# r9_setup <sb> <kind> — runs BEFORE write_audit because it COMMITS (the index state IS the setup).
# Every path it leaves behind is CONSISTENT, so the audit line records an honest clean tree.
r9_setup() {
    local sb="$1" kind="$2" h b
    case "$kind" in
        execA)   # index 100644, disk non-executable → consistent; the attack adds the x bit
            git -C "$sb/repo" config core.fileMode false || return 2
            chmod -x "$sb/repo/backend/gradlew" || return 2
            git -C "$sb/repo" update-index --chmod=-x backend/gradlew || return 2
            ( builtin cd "$sb/repo" && git "${GIT_ID[@]}" commit -q -m "execA setup" ) \
                >/dev/null 2>&1 || return 2 ;;
        execB)   # index 100755, disk executable → consistent; the attack REMOVES the x bit
            git -C "$sb/repo" config core.fileMode false || return 2 ;;
        gldirt)  # an EMPTY uninitialized gitlink: exactly the shape (S) proves must still pass
            h="$(git -C "$sb/repo" rev-parse HEAD)" || return 2
            mkdir -p "$sb/repo/vendor/sub" || return 2
            ( builtin cd "$sb/repo" \
              && git update-index --add --cacheinfo "160000,$h,vendor/sub" \
              && git "${GIT_ID[@]}" commit -q -m gitlink ) >/dev/null 2>&1 || return 2 ;;
        casefold)
            # ROUND 10 / P2 — THE PREMISE, MADE EXPLICIT. This setup used to register the second
            # spelling with a DIVERGENT blob, and write_audit's `git add -A` then quietly healed it
            # back to the one file's bytes; nothing asserted either way. Measured with the healing
            # removed: divergent blobs make the alias show up as ` M alias.txt`, so the gate refuses
            # on AUDIT_TREE_DIRTY_NOW and the casefold code never runs — that shape cannot show the
            # refusal is load-bearing (and round 9 said as much in prose: the ordinary form is
            # already refused by the clean-tree precondition).
            # The shape that ISOLATES the casefold refusal is therefore the EQUAL-blob one: two
            # index entries, one file, a CLEAN tree, and nothing but the alias check able to see
            # it. It is also what the shipped round-9 case actually ran, unknowingly.
            printf 'REAL\n' > "$sb/repo/Alias.txt" || return 2
            ( builtin cd "$sb/repo" && git add Alias.txt \
              && git "${GIT_ID[@]}" commit -q -m alias1 ) >/dev/null 2>&1 || return 2
            b="$(git -C "$sb/repo" hash-object -w -- "$sb/repo/Alias.txt")" || return 2
            ( builtin cd "$sb/repo" \
              && git update-index --add --cacheinfo "100644,$b,alias.txt" \
              && git "${GIT_ID[@]}" commit -q -m alias2 ) >/dev/null 2>&1 || return 2 ;;
        dircase) # ROUND 10 / P1 — THE REVIEWER'S TOPOLOGY, verbatim. No LEAF collision at all:
                 #   index A/check.sh  (committed content: `cat A/helper`)
                 #   index a/helper    (committed content: PASS)
                 # On APFS `A` and `a` are ONE directory, so the local `cat A/helper` succeeds and
                 # every gate goes green; the PUSHED tree serves no `A/helper` on a case-sensitive
                 # receiver. Round 9 grouped COMPLETE folded paths, so the two never met.
                 mkdir -p "$sb/repo/A" || return 2
                 printf 'cat A/helper\n' > "$sb/repo/A/check.sh" || return 2
                 printf 'PASS\n' > "$sb/repo/A/helper" || return 2
                 b="$(printf 'PASS\n' | git -C "$sb/repo" hash-object -w --stdin)" || return 2
                 ( builtin cd "$sb/repo" && git add A/check.sh \
                   && git update-index --add --cacheinfo "100644,$b,a/helper" \
                   && git "${GIT_ID[@]}" commit -q -m dircase ) >/dev/null 2>&1 || return 2 ;;
        csdistinct) # ROUND 10 NEGATIVE CONTROL — the FALSE-POSITIVE test. Requires a genuinely
                 # case-sensitive filesystem: `A/` and `a/` as DISTINCT directories, plus the leaf
                 # pair `Alias.txt`/`alias.txt` as DISTINCT files. The refusal is a MEASUREMENT of
                 # (st_dev, st_ino), so distinct inodes must NOT be refused — otherwise every
                 # Linux fork-receiver whose tree happens to hold both spellings is bricked.
                 mkdir -p "$sb/repo/A" "$sb/repo/a" || return 2
                 printf 'cat A/helper\n' > "$sb/repo/A/check.sh" || return 2
                 printf 'ALSO\n' > "$sb/repo/A/helper" || return 2
                 printf 'PASS\n' > "$sb/repo/a/helper" || return 2
                 printf 'REAL\n' > "$sb/repo/Alias.txt" || return 2
                 printf 'EVIL\n' > "$sb/repo/alias.txt" || return 2
                 ( builtin cd "$sb/repo" && git add A a Alias.txt alias.txt \
                   && git "${GIT_ID[@]}" commit -q -m csdistinct ) >/dev/null 2>&1 || return 2 ;;
        nfcnfd)  # ROUND 11 / P1 — THE REVIEWER'S TOPOLOGY. No LEAF collision and no ASCII case
                 # difference either: the two DIRECTORY spellings differ only by UNICODE
                 # NORMALIZATION. `é` is NFC c3a9; `e`+U+0301 is NFD 65cc81. `bytes.lower()`
                 # leaves them distinct, so rounds 9-10 never compared their shared inode.
                 r11_plant "$sb" c3a9 65cc81 || return 2 ;;
        nacase)  # ROUND 11 / P1, the sibling through the same hole: NON-ASCII CASE. `É` is c389,
                 # `é` is c3a9; `bytes.lower()` maps A-Z and nothing else, so these folded apart
                 # while APFS served them from one directory.
                 r11_plant "$sb" c389 c3a9 || return 2 ;;
        ignzwnj) # ROUND 12 / P1 — THE IGNORABLE-FORMAT AXIS, independent of case AND of canonical
                 # normalization. Case-insensitive HFS+ folds designated formatting controls to
                 # ZERO and skips them (Apple TN1150, `FastUnicodeCompare`), so `SAFE/` and
                 # `SAFE<U+200C ZWNJ>/` are ONE directory. Round 11's canonical caseless key
                 # PRESERVED U+200C, so the two spellings keyed apart and the shared inode was
                 # never compared: the checkout is clean, `bash SAFE/check.sh` PASSES, and the
                 # PUSHED tree carries no literal `SAFE/helper` for a receiver that treats U+200C
                 # as significant. Committed path names only — no environment control.
                 r11_plant "$sb" 53414645 53414645e2808c || return 2 ;;
        ignrlo)  # the same hole with U+202E RIGHT-TO-LEFT OVERRIDE, which is a BIDI control rather
                 # than a zero-width joiner control — checked, not assumed: it is general category
                 # Cf, it IS Default_Ignorable_Code_Point in DerivedCoreProperties 17.0.0, and the
                 # live HFS+ volume folds it to zero exactly like U+200C.
                 r11_plant "$sb" 53414645 53414645e280ae || return 2 ;;
        csignorable) # ROUND 12 NEGATIVE CONTROL, and unlike the round-10/11 ones it needs NO
                 # special volume: MEASURED, BOTH APFS variants (case-insensitive and
                 # case-sensitive) serve `SAFE/` and `SAFE<U+200C>/` from DISTINCT inodes. The
                 # round-12 fold makes these ONE key, so without the (st_dev, st_ino)
                 # discriminator this tree would now be refused — which would brick every
                 # fork-receiver whose filesystem treats the character as significant, i.e. every
                 # filesystem except case-insensitive HFS+. Two inodes ⇒ two singleton groups.
                 mkdir -p "$sb/repo/SAFE" "$sb/repo/SAFE$(printf '\342\200\214')" || return 2
                 printf 'cat SAFE/helper\n' > "$sb/repo/SAFE/check.sh" || return 2
                 printf 'ALSO\n' > "$sb/repo/SAFE/helper" || return 2
                 printf 'PASS\n' > "$sb/repo/SAFE$(printf '\342\200\214')/helper" || return 2
                 ( builtin cd "$sb/repo" && git add SAFE "SAFE$(printf '\342\200\214')" \
                   && git "${GIT_ID[@]}" commit -q -m csignorable ) >/dev/null 2>&1 || return 2 ;;
        csnacase) # ROUND 11 NEGATIVE CONTROL on a REAL case-sensitive volume: `É/` and `é/` as
                 # GENUINELY DISTINCT directories. The round-11 fold makes these one key, so
                 # without the (st_dev, st_ino) discriminator this tree would now be refused —
                 # which would brick every fork-receiver on a case-sensitive filesystem whose tree
                 # happens to hold both spellings. Two inodes ⇒ two singleton groups ⇒ no refusal.
                 mkdir -p "$sb/repo/É" "$sb/repo/é" || return 2
                 printf 'cat "É/helper"\n' > "$sb/repo/É/check.sh" || return 2
                 printf 'ALSO\n' > "$sb/repo/É/helper" || return 2
                 printf 'PASS\n' > "$sb/repo/é/helper" || return 2
                 ( builtin cd "$sb/repo" && git add É é \
                   && git "${GIT_ID[@]}" commit -q -m csnacase ) >/dev/null 2>&1 || return 2 ;;
        shareprefix) # ROUND 10 NEGATIVE CONTROL, the arm a case-INSENSITIVE filesystem can build:
                 # two tracked entries under ONE spelling of a shared directory. Every tracked path
                 # in the live catalog shares prefixes with hundreds of others, so a prefix walk
                 # that grouped on the folded key alone (rather than on an OBSERVED inode alias)
                 # would refuse the whole tree.
                 mkdir -p "$sb/repo/A" || return 2
                 printf 'cat A/helper\n' > "$sb/repo/A/check.sh" || return 2
                 printf 'PASS\n' > "$sb/repo/A/helper" || return 2
                 ( builtin cd "$sb/repo" && git add A \
                   && git "${GIT_ID[@]}" commit -q -m shareprefix ) >/dev/null 2>&1 || return 2 ;;
        symreg)  # an index-SYMLINK path (mode 120000), consistent on disk
            ( builtin cd "$sb/repo" && ln -s benign-target.txt linkpath.txt \
              && git add linkpath.txt \
              && git "${GIT_ID[@]}" commit -q -m symlink ) >/dev/null 2>&1 || return 2 ;;
        glinit)  # a REAL, initialized submodule recorded at its FIRST commit
            mkdir -p "$sb/repo/vendor/mod" || return 2
            ( builtin cd "$sb/repo/vendor/mod" && git init -q . \
              && printf 'one\n' > f.txt && git add f.txt \
              && git "${GIT_ID[@]}" commit -q -m one ) >/dev/null 2>&1 || return 2
            h="$(git -C "$sb/repo/vendor/mod" rev-parse HEAD)" || return 2
            ( builtin cd "$sb/repo/vendor/mod" && printf 'two\n' > f.txt && git add f.txt \
              && git "${GIT_ID[@]}" commit -q -m two && git checkout -q "$h" ) \
                >/dev/null 2>&1 || return 2
            ( builtin cd "$sb/repo" \
              && git update-index --add --cacheinfo "160000,$h,vendor/mod" \
              && git "${GIT_ID[@]}" commit -q -m gitlink-init ) >/dev/null 2>&1 || return 2 ;;
        *) return 2 ;;
    esac
    return 0
}

# r9_attack <sb> <kind> <expect-status> — runs AFTER write_audit, NEVER commits. <expect-status> is
# `empty` when the class's whole premise is that `git status --porcelain` shows nothing; the premise
# is asserted, so a future git that DOES report the change makes this harness say so loudly instead
# of passing on a different refusal.
r9_attack() {
    local sb="$1" kind="$2" want_status="$3"
    case "$kind" in
        execA)    chmod +x "$sb/repo/backend/gradlew" || return 2 ;;
        execB)    chmod -x "$sb/repo/backend/gradlew" || return 2 ;;
        gldirt)   printf 'echo owned\n' > "$sb/repo/vendor/sub/check.sh" || return 2 ;;
        casefold|dircase|csdistinct|shareprefix|nfcnfd|nacase|csnacase|ignzwnj|ignrlo|csignorable) : ;;
                  # the alias (or its absence) IS the index state; nothing further to do on disk
        symreg)   git -C "$sb/repo" update-index --assume-unchanged linkpath.txt || return 2
                  rm -f "$sb/repo/linkpath.txt" || return 2
                  printf 'a regular file where the index says symlink\n' \
                      > "$sb/repo/linkpath.txt" || return 2 ;;
        glinit)   ( builtin cd "$sb/repo/vendor/mod" && git checkout -q master 2>/dev/null \
                    || git checkout -q main ) >/dev/null 2>&1 || return 2 ;;
        *) return 2 ;;
    esac
    if [ "$want_status" = "empty" ] && [ -n "$(git -C "$sb/repo" status --porcelain)" ]; then
        violation "harness premise broken: \`git status --porcelain\` is NOT empty after the" \
                  "$kind attack, so this scenario no longer reproduces the reviewer's setup."
        return 3
    fi
    return 0
}

# r9_premise <sb> <kind> — ASSERT the scenario's premise still holds at the moment the gate runs.
# ROUND 10 / P2 (reviewer): (W2) had NO post-neuter assertion, so its exit 0 was compatible with a
# sandbox in which the alias had quietly stopped existing — and the neuter's own `git add -A` was
# able to heal exactly that (it re-reads `alias.txt` from the ONE file on disk, equalising the two
# blobs). The neuter now stages only the toolchain, and every alias case asserts the premise here.
# Returns 1 after calling violation(); a case with no premise to assert returns 0.
r9_premise() {
    local sb="$1" kind="$2" n b ia ib ah bh
    case "$kind" in
        casefold)
            n="$(git -C "$sb/repo" ls-files -s | awk -F'\t' '{print tolower($2)}' \
                 | grep -cx 'alias.txt')"
            b="$(git -C "$sb/repo" ls-files -s | awk -F'\t' 'tolower($2)=="alias.txt"{print $1}' \
                 | awk '{print $2}' | sort -u | wc -l | tr -d ' ')"
            ia="$(stat -f '%d %i' "$sb/repo/Alias.txt" 2>/dev/null \
                  || stat -c '%d %i' "$sb/repo/Alias.txt")"
            ib="$(stat -f '%d %i' "$sb/repo/alias.txt" 2>/dev/null \
                  || stat -c '%d %i' "$sb/repo/alias.txt")"
            if [ "$n" -ne 2 ] || [ "$b" -ne 1 ] || [ -z "$ia" ] || [ "$ia" != "$ib" ]; then
                violation "premise broken (casefold): the index holds $n entries folding to" \
                          "alias.txt over $b blob(s), Alias.txt=[$ia] alias.txt=[$ib]. This class" \
                          "needs TWO entries, ONE inode and ONE blob — two blobs make the tree" \
                          "DIRTY, so the gate refuses on AUDIT_TREE_DIRTY_NOW and the casefold" \
                          "code never runs, which proves nothing about the casefold code."
                return 1
            fi ;;
        dircase)
            n="$(git -C "$sb/repo" ls-files | grep -cxE 'A/check\.sh|a/helper')"
            ia="$(stat -f '%d %i' "$sb/repo/A" 2>/dev/null || stat -c '%d %i' "$sb/repo/A")"
            ib="$(stat -f '%d %i' "$sb/repo/a" 2>/dev/null || stat -c '%d %i' "$sb/repo/a")"
            if [ "$n" -ne 2 ] || [ -z "$ia" ] || [ "$ia" != "$ib" ]; then
                violation "premise broken (dircase): the index holds $n of the two entries and" \
                          "A/=[$ia] a/=[$ib]. This class needs BOTH entries and ONE directory" \
                          "inode; on a case-SENSITIVE filesystem the topology cannot exist and" \
                          "this case must not be reported as a pass."
                return 1
            fi ;;
        csdistinct)
            ia="$(stat -f '%d %i' "$sb/repo/A" 2>/dev/null || stat -c '%d %i' "$sb/repo/A")"
            ib="$(stat -f '%d %i' "$sb/repo/a" 2>/dev/null || stat -c '%d %i' "$sb/repo/a")"
            if [ -z "$ia" ] || [ "$ia" = "$ib" ]; then
                violation "premise broken (csdistinct): A/=[$ia] a/=[$ib] are NOT distinct, so" \
                          "this sandbox is not on a case-sensitive filesystem and the" \
                          "false-positive control would be vacuous."
                return 1
            fi ;;
        nfcnfd|nacase|ignzwnj|ignrlo)
            # ROUND 11 / P1 (+ ROUND 12 for the two ignorable kinds). The premise is: BOTH
            # spellings are in the index AS DISTINCT BYTES, and the filesystem serves them from ONE
            # inode. If a future git precomposes the index entry (or the volume stops folding the
            # axis under test), the topology is gone and this case must say so rather than pass on
            # some other refusal.
            case "$kind" in
                nfcnfd)  ah=c3a9;    bh=65cc81 ;;
                nacase)  ah=c389;    bh=c3a9 ;;
                ignzwnj) ah=53414645; bh=53414645e2808c ;;
                ignrlo)  ah=53414645; bh=53414645e280ae ;;
            esac
            if ! "${AX_PY_BIN:-python3}" - "$sb/repo" "$ah" "$bh" <<'PY'
import os, subprocess, sys
repo, ah, bh = sys.argv[1], sys.argv[2], sys.argv[3]
A, B = bytes.fromhex(ah), bytes.fromhex(bh)
idx = subprocess.run(["git", "-C", repo, "ls-files", "-z"],
                     stdout=subprocess.PIPE, check=True).stdout.split(b"\0")
want = {A + b"/check.sh", B + b"/helper"}
have = want & set(idx)
sa = os.lstat(os.path.join(os.fsencode(repo), A))
sb_ = os.lstat(os.path.join(os.fsencode(repo), B))
ok = (have == want and A != B and (sa.st_dev, sa.st_ino) == (sb_.st_dev, sb_.st_ino))
if not ok:
    print(f"index holds {sorted(have)} of {sorted(want)}; "
          f"{A!r}={(sa.st_dev, sa.st_ino)} {B!r}={(sb_.st_dev, sb_.st_ino)}", file=sys.stderr)
sys.exit(0 if ok else 1)
PY
            then
                violation "premise broken ($kind): this class needs BOTH spellings in the index as" \
                          "DISTINCT BYTES and ONE directory inode on disk. Without that topology" \
                          "the refusal under test is not the one being measured."
                return 1
            fi ;;
        csignorable)
            # ROUND 12 negative control: the two spellings must be DISTINCT inodes here, or the
            # false-positive test is vacuous (it would be measuring a folding volume).
            ia="$(stat -f '%d %i' "$sb/repo/SAFE" 2>/dev/null || stat -c '%d %i' "$sb/repo/SAFE")"
            ib="$(stat -f '%d %i' "$sb/repo/SAFE$(printf '\342\200\214')" 2>/dev/null \
                  || stat -c '%d %i' "$sb/repo/SAFE$(printf '\342\200\214')")"
            if [ -z "$ia" ] || [ "$ia" = "$ib" ]; then
                violation "premise broken (csignorable): SAFE/=[$ia] SAFE<U+200C>/=[$ib] are NOT" \
                          "distinct, so this filesystem folds ignorable format characters and the" \
                          "round-12 false-positive control would be vacuous."
                return 1
            fi ;;
        csnacase)
            ia="$(stat -f '%d %i' "$sb/repo/É" 2>/dev/null || stat -c '%d %i' "$sb/repo/É")"
            ib="$(stat -f '%d %i' "$sb/repo/é" 2>/dev/null || stat -c '%d %i' "$sb/repo/é")"
            if [ -z "$ia" ] || [ "$ia" = "$ib" ]; then
                violation "premise broken (csnacase): É/=[$ia] é/=[$ib] are NOT distinct, so this" \
                          "sandbox is not on a case-sensitive filesystem and the round-11" \
                          "false-positive control would be vacuous."
                return 1
            fi ;;
    esac
    return 0
}

# r9_case <tag> <kind> <expect-status> <label> <want-regex> [neuter] [fp-override]
# fp-override exists for exactly one case, (W): a casefold alias is an INDEX state, so it must be
# committed in the SETUP, which means the post-fix fingerprint helper already refuses when
# write_audit calls it and the audit line would carry an empty fingerprint — the gate would then
# block on the RECORD SHAPE (check 11) instead of on the alias. A syntactically valid placeholder
# keeps checks 5/11 satisfied; the alias is refused in check 12c, which runs BEFORE the fingerprint
# recompute at 12a/12c-recompute, so what is measured is still the alias refusal and nothing else.
r9_case() {
    local tag="$1" kind="$2" want_status="$3" label="$4" want="$5" neuter="${6:-}" fpo="${7:-}" sb rc
    # R9_ROOT lets ONE case (the false-positive control) be built on a different filesystem; every
    # other case uses $WORK. Both are throwaway directories removed by the EXIT trap.
    sb="${R9_ROOT:-$WORK}/r9-$tag"
    build_sb "$sb" || { echo "harness setup failed ($tag)" >&2; exit 2; }
    printf 'benign\n' > "$sb/repo/benign-target.txt"
    ( builtin cd "$sb/repo" && git add benign-target.txt \
      && git "${GIT_ID[@]}" commit -q -m target ) >/dev/null 2>&1
    r9_setup "$sb" "$kind" || { echo "harness setup failed ($tag/setup $kind)" >&2; exit 2; }
    # The neuter COMMITS, so like round 8 it must land BEFORE write_audit or every neutered case
    # would fail with AUDIT_STALE_HEAD for a reason unrelated to the attack.
    if [ -n "$neuter" ]; then
        case "$neuter" in
            bits) round8_neuter "$sb" bits ;;
            *)    round9_neuter "$sb" "$neuter" ;;
        esac || { echo "harness setup failed ($tag/neuter stale)" >&2; exit 2; }
    fi
    write_audit "$sb" "$fpo"
    r9_attack "$sb" "$kind" "$want_status"
    case $? in
        0) ;;
        3) return 1 ;;                       # premise broken: r9_attack already called violation()
        *) echo "ax-prove-hermetic-runtime: harness setup failed ($tag/attack $kind)" >&2
           exit 2 ;;
    esac
    r9_premise "$sb" "$kind" || return 1     # ROUND 10 / P2: the premise, asserted at gate time
    run_gate "$sb" "$WORK/r9-$tag.log"; rc=$?
    note "($tag) $label: exit=$rc"
    if [ -n "$want" ]; then
        if [ "$rc" -eq 0 ] || ! grep -qE "$want" "$WORK/r9-$tag.log"; then
            violation "the $kind representation divergence was not refused with $want (exit=$rc)." \
                      "The index and the working tree disagree about a fact no digest carries, so" \
                      "R25 certified something the push will not ship."
            head -5 "$WORK/r9-$tag.log" >&2
        fi
    else
        if [ "$rc" -ne 0 ]; then
            violation "in a pre-fix sandbox the $kind divergence did NOT reproduce (exit=$rc), so" \
                      "the round-9 refusals are not attributable — this harness has gone stale."
            head -5 "$WORK/r9-$tag.log" >&2
        fi
    fi
}

# (T)/(U) the executable bit, BOTH directions, against the LIVE gate …
r9_case T  execA    empty "index 100644 + EXECUTABLE on disk           " \
    "GIT_EXEC_BIT_DIVERGENCE"
r9_case U  execB    empty "index 100755 + NOT executable on disk       " \
    "GIT_EXEC_BIT_DIVERGENCE"
# … and their pre-round-9 twins, in which the same attack lands again.
r9_case T2 execA    empty "same, round-9 refusals removed              " "" all
r9_case U2 execB    empty "same, round-9 refusals removed              " "" all
# (V) a populated but UNINITIALIZED gitlink, and its twin.
r9_case V  gldirt   empty "uninitialized gitlink POPULATED on disk     " \
    "GIT_GITLINK_UNINITIALIZED_POPULATED"
r9_case V2 gldirt   empty "same, round-9 refusals removed              " "" all
# (W) two index entries differing only in case, ONE file, and — ROUND 10 / P2 — status ASSERTED
# empty with the blobs ASSERTED equal. Round 9 ran this case with `any` and with divergent blobs
# that write_audit silently healed; measured with the healing removed, divergent blobs make the
# tree DIRTY and the gate refuses on AUDIT_TREE_DIRTY_NOW without ever reaching the casefold code.
# So the shape that isolates the refusal is the clean, equal-blob one (which is what round 9 in
# fact ran). The honest scope of the LEAF class is therefore: the divergent form is caught by the
# clean-tree precondition anyway, and this refusal names the fault in the form that precondition
# cannot see. The DIRECTORY form (Z) has no such backstop — that one was silently open.
r9_case W  casefold empty "two index entries differing only in CASE    " \
    "GIT_CASEFOLD_ALIAS" "" "0a815065ebf5ad6ce3828aba4cfc387f26a56a306e8616bb22aadce99dd11211"
r9_case W2 casefold empty "same, round-9 refusals removed              " "" all \
    "0a815065ebf5ad6ce3828aba4cfc387f26a56a306e8616bb22aadce99dd11211"
# (X) ROUND 9 / (c): the MIRROR of round 8's (P) — index SYMLINK, regular file on disk. Round 8
# implemented this direction and never exercised it. Run with the index-bit refusal neutered so the
# code under test is the REPRESENTATION backstop and not the assume-unchanged bit.
r9_case X  symreg   empty "index SYMLINK → regular file (bits neutered)" \
    "GIT_WORKTREE_TYPE_MISMATCH" bits
# (Y) ROUND 9 / (c): an INITIALIZED submodule moved off the recorded commit — round 8's other
# implemented-but-unexercised branch. Status is not asserted empty (a superproject reports "new
# commits"); the gitlink check runs before the clean-tree check, and the point is the CODE.
r9_case Y  glinit   any   "INITIALIZED gitlink at the wrong commit     " \
    "GIT_GITLINK_DIVERGENCE"

# ══ ROUND 10 (TD-2026-07-31-(P1-casefold-prefix)) ════════════════════════════════════
# (Z) THE REVIEWER'S TOPOLOGY. Round 9's (e) grouped COMPLETE folded paths, so an alias living in a
# shared DIRECTORY component was invisible to both implementations: `A/check.sh` and `a/helper`
# have different folded keys, and on APFS `A` and `a` are one directory inode. MEASURED against the
# d567c37 implementations: status EMPTY, fingerprint = the clean-tree constant 0a815065…, 12c's
# violation set EMPTY — while the pushed tree serves no `A/helper` to a case-sensitive receiver.
# The fingerprint is overridden with the clean-tree constant for the same reason as (W): the alias
# is an INDEX state, so it exists before write_audit, and the post-fix helper would refuse there —
# the gate would then block on the RECORD SHAPE instead of on the alias. The constant is the honest
# value for this tree (status is empty), and 12c fires before the recompute.
CLEAN_FP="0a815065ebf5ad6ce3828aba4cfc387f26a56a306e8616bb22aadce99dd11211"
r9_case Z  dircase  empty "DIRECTORY component aliased (A/ ≡ a/)      " \
    "GIT_CASEFOLD_DIR_ALIAS" "" "$CLEAN_FP"
# (Z2) the pre-round-10 twin: with the refusal removed from BOTH implementations the attack lands.
r9_case Z2 dircase  empty "same, round-10 refusal removed (both)      " "" r10all "$CLEAN_FP"
# (Z3)/(Z4) ROUND 10 / P2 — EACH IMPLEMENTATION, ON ITS OWN. With only the 12c sweep neutered the
# FINGERPRINT HELPER must still refuse; it is the prior-release copy the recompute runs, so its
# refusal surfaces as AUDIT_FINGERPRINT_UNVERIFIABLE. With only the helper neutered the sweep must
# still refuse, on its own code. Neither implementation may be dead weight behind the other.
r9_case Z3 dircase  empty "same, only the 12c SWEEP neutered          " \
    "AUDIT_FINGERPRINT_UNVERIFIABLE" r10guard "$CLEAN_FP"
r9_case Z4 dircase  empty "same, only the FINGERPRINT HELPER neutered " \
    "GIT_CASEFOLD_DIR_ALIAS" r10fp "$CLEAN_FP"
# The same split, applied to the three round-9 refusals whose shipped twins disabled both
# implementations at once (reviewer P2). Each must be independently load-bearing.
r9_case T3 execA    empty "exec bit, only the 12c SWEEP neutered      " \
    "AUDIT_FINGERPRINT_UNVERIFIABLE" guard
r9_case T4 execA    empty "exec bit, only the HELPER neutered         " \
    "GIT_EXEC_BIT_DIVERGENCE" fp
r9_case V3 gldirt   empty "gitlink dirt, only the 12c SWEEP neutered  " \
    "AUDIT_FINGERPRINT_UNVERIFIABLE" guard
r9_case V4 gldirt   empty "gitlink dirt, only the HELPER neutered     " \
    "GIT_GITLINK_UNINITIALIZED_POPULATED" fp
r9_case W3 casefold any   "leaf alias, only the 12c SWEEP neutered    " \
    "AUDIT_FINGERPRINT_UNVERIFIABLE" guard "$CLEAN_FP"
r9_case W4 casefold any   "leaf alias, only the HELPER neutered       " \
    "GIT_CASEFOLD_ALIAS" fp "$CLEAN_FP"

# ══ ROUND 11 (TD-2026-08-01-(P1-unicode-prefix-fold)) ════════════════════════════════
# (AA) THE REVIEWER'S ROUND-11 TOPOLOGY: the two DIRECTORY spellings differ only by UNICODE
# NORMALIZATION (`é` NFC c3a9 ≡ `e`+U+0301 NFD 65cc81). Rounds 9-10 keyed the prefix map with
# `bytes.lower()`, which is ASCII-only and normalization-blind, so the shared inode was never
# compared. MEASURED at beee364: status EMPTY, `bash é/check.sh` → PASS locally, both
# implementations SILENT, fingerprint = the clean-tree constant — while the pushed tree records
# only `e◌́/helper`. (AB) is the same defect through the same hole with NON-ASCII CASE (`É` c389 ≡
# `é` c3a9). The fingerprint override is the clean-tree constant for the same reason as (W)/(Z).
r9_case AA nfcnfd  empty "DIRECTORY aliased by NORMALIZATION (é ≡ é)" \
    "GIT_CASEFOLD_DIR_ALIAS" "" "$CLEAN_FP"
r9_case AB nacase  empty "DIRECTORY aliased by NON-ASCII CASE (É ≡ é)" \
    "GIT_CASEFOLD_DIR_ALIAS" "" "$CLEAN_FP"
# (AA2)/(AB2) the pre-round-11 twins. The neuter puts `bytes.lower()` BACK as the map key in both
# implementations — it does NOT remove the round-10 report, so what lands again is attributable to
# the FOLD and to nothing else.
r9_case AA2 nfcnfd empty "same, round-11 fold reverted (both)       " "" r11all "$CLEAN_FP"
r9_case AB2 nacase empty "same, round-11 fold reverted (both)       " "" r11all "$CLEAN_FP"
# (AA3)/(AA4)/(AB3)/(AB4) — EACH IMPLEMENTATION ON ITS OWN, the round-10 split pattern. With only
# the 12c sweep's fold reverted the FINGERPRINT HELPER must still refuse (it is the prior-release
# copy the recompute runs, so its refusal surfaces as AUDIT_FINGERPRINT_UNVERIFIABLE); with only
# the helper's fold reverted the sweep must still refuse on its own code.
r9_case AA3 nfcnfd empty "normalization, only the SWEEP reverted    " \
    "AUDIT_FINGERPRINT_UNVERIFIABLE" r11guard "$CLEAN_FP"
r9_case AA4 nfcnfd empty "normalization, only the HELPER reverted   " \
    "GIT_CASEFOLD_DIR_ALIAS" r11fp "$CLEAN_FP"
r9_case AB3 nacase empty "non-ASCII case, only the SWEEP reverted   " \
    "AUDIT_FINGERPRINT_UNVERIFIABLE" r11guard "$CLEAN_FP"
r9_case AB4 nacase empty "non-ASCII case, only the HELPER reverted  " \
    "GIT_CASEFOLD_DIR_ALIAS" r11fp "$CLEAN_FP"
# (AC) NO REGRESSION: the round-10 ASCII topology must still be refused by the widened fold.
r9_case AC dircase empty "round-10 ASCII A/ ≡ a/ still refused       " \
    "GIT_CASEFOLD_DIR_ALIAS" "" "$CLEAN_FP"

# (Z5) THE FALSE-POSITIVE CONTROL, and it is the one that matters: the refusal is a MEASUREMENT of
# (st_dev, st_ino), so a tree whose `A/` and `a/` are GENUINELY DISTINCT directories must pass.
# That topology can only be BUILT on a case-sensitive filesystem. The harness measures the one it
# has and says which arm ran — it never skips:
#   · case-sensitive $WORK (Linux, or AX_PROVE_CS_DIR pointing at a case-sensitive volume) → the
#     real control: distinct `A/`+`a/` directories AND distinct `Alias.txt`+`alias.txt` files.
#   · case-insensitive $WORK → the strongest shape that filesystem admits: two tracked entries
#     under ONE spelling of a shared directory. Every path in the live catalog shares prefixes, so
#     a walk that grouped on the folded key alone (instead of on an observed inode alias) would
#     refuse the whole tree; this arm holds that line.
cs_probe() {   # cs_probe <dir> → 0 when <dir> is on a CASE-SENSITIVE filesystem
    local d="$1/.axcase.$$"
    mkdir -p "$d" || return 1
    : > "$d/CaseProbe" || { rm -rf "$d"; return 1; }
    if [ -e "$d/caseprobe" ]; then rm -rf "$d"; return 1; fi
    rm -rf "$d"; return 0
}
if [ -n "${AX_PROVE_CS_DIR:-}" ] && [ -d "${AX_PROVE_CS_DIR}" ] && cs_probe "$AX_PROVE_CS_DIR"; then
    CS_ROOT="$AX_PROVE_CS_DIR/axprove.$$"
    mkdir -p "$CS_ROOT" || CS_ROOT=""
elif cs_probe "$WORK"; then
    CS_ROOT="$WORK"
fi
if [ -n "$CS_ROOT" ]; then
    note "(Z5) filesystem arm: CASE-SENSITIVE ($CS_ROOT) — building the real distinct-inode control"
    R9_ROOT="$CS_ROOT"
    r9_case Z5 csdistinct empty "DISTINCT A/ and a/ must NOT be refused     " ""
    r9_case AA5 csnacase empty "DISTINCT É/ and é/ must NOT be refused      " ""
    R9_ROOT=""
else
    note "(Z5) filesystem arm: CASE-INSENSITIVE (\$WORK) — distinct A/ and a/ cannot be created" \
         "here; running the shared-prefix arm instead (export AX_PROVE_CS_DIR=<case-sensitive dir>" \
         "for the real one)"
    r9_case Z5 shareprefix empty "shared directory prefix must NOT be refused " ""
fi

# ══ ROUND 12 (TD-2026-08-01-(P1-ignorable-fold)) ═════════════════════════════════════
# (AF)/(AG) THE THIRD EQUIVALENCE AXIS: IGNORABLE FORMAT CHARACTERS. Case-insensitive HFS+ folds
# designated formatting controls to ZERO and skips them entirely (Apple TN1150, `FastUnicodeCompare`
# — "All ignorable characters are folded to the value zero"), so `SAFE/` and `SAFE<U+200C>/` are ONE
# directory. Round 11's canonical caseless key PRESERVED those code points, so the two spellings
# keyed apart, the shared inode was never compared, and 12c's violation buckets came back EMPTY on
# exactly the topology round 10 closed for case and round 11 closed for normalization.
#
# UNLIKE ROUND 11's NORMALIZATION ARM, BOTH SIDES OF THIS ONE ARE REAL:
#   · the RED arm needs a volume that FOLDS the character. `hdiutil create -fs HFS+` builds one,
#     and this harness builds it rather than declaring the arm unbuildable. MEASURED on it: the 16
#     code points TN1150's fold table maps to zero are ALL served from one inode (16/16) and the
#     other 154 general-category-Cf characters are all DISTINCT (0/154) — the live volume and the
#     published table agree exactly.
#   · the FALSE-POSITIVE arm needs a volume that does NOT fold it, which is every ordinary one:
#     MEASURED, both case-insensitive and case-sensitive APFS give the two spellings DISTINCT
#     inodes. So (AH) runs in the plain sandbox and is a live control, not a simulated one.
# If no folding volume can be attached (no hdiutil, a future macOS that refuses to create HFS+, a
# non-macOS host), the RED arm falls back to the SIMULATED grouping check in (AI) below and SAYS
# SO — it is never silently skipped.
ign_probe() {   # ign_probe <dir> → 0 when <dir> folds IGNORABLE format characters away
    "${AX_PY_BIN:-python3}" - "$1" <<'PY'
import os, shutil, sys
root = os.fsencode(sys.argv[1])
d = os.path.join(root, b".axignprobe")
shutil.rmtree(d, ignore_errors=True)
ok = False
try:
    os.makedirs(d)
    a, b = os.path.join(d, b"SAFE"), os.path.join(d, b"SAFE\xe2\x80\x8c")
    os.mkdir(a)
    try:
        os.mkdir(b)
    except FileExistsError:
        pass
    sa, sb = os.lstat(a), os.lstat(b)
    ok = (sa.st_dev, sa.st_ino) == (sb.st_dev, sb.st_ino)
except OSError:
    ok = False
finally:
    shutil.rmtree(d, ignore_errors=True)
sys.exit(0 if ok else 1)
PY
}
IGN_ROOT=""; WORK_FOLDS_IGN=0
if [ -n "${AX_PROVE_IGN_DIR:-}" ] && [ -d "${AX_PROVE_IGN_DIR}" ] && ign_probe "$AX_PROVE_IGN_DIR"; then
    IGN_ROOT="$AX_PROVE_IGN_DIR/axprove.$$"
    mkdir -p "$IGN_ROOT" || IGN_ROOT=""
elif ign_probe "$WORK"; then
    IGN_ROOT="$WORK"; WORK_FOLDS_IGN=1
elif command -v hdiutil >/dev/null 2>&1; then
    # 48 MiB is ample: a sandbox is a fresh `git init` holding ten copied files and a bare remote.
    if hdiutil create -quiet -size 48m -fs "HFS+" -volname AXHFSIGN -ov "$WORK/axhfs.dmg" \
           >/dev/null 2>&1 \
       && hdiutil attach -quiet -nobrowse -mountpoint "$WORK/axhfsmnt" "$WORK/axhfs.dmg" \
           >/dev/null 2>&1; then
        IGN_MNT="$WORK/axhfsmnt"                     # set even on probe failure, so teardown detaches
        ign_probe "$IGN_MNT" && IGN_ROOT="$IGN_MNT"
    fi
fi
if [ -n "$IGN_ROOT" ]; then
    note "(AF/AG) filesystem arm: REAL — $IGN_ROOT folds ignorable format characters (TN1150)"
    R9_ROOT="$IGN_ROOT"
    r9_case AF ignzwnj empty "DIRECTORY aliased by IGNORABLE U+200C ZWNJ " \
        "GIT_CASEFOLD_DIR_ALIAS" "" "$CLEAN_FP"
    r9_case AG ignrlo  empty "DIRECTORY aliased by IGNORABLE U+202E RLO  " \
        "GIT_CASEFOLD_DIR_ALIAS" "" "$CLEAN_FP"
    # (AF2)/(AG2) the pre-round-12 twins. The neuter removes ONLY the ignorable strip and leaves
    # the round-11 canonical caseless fold in place, so what lands again is attributable to the
    # STRIP and to nothing else.
    r9_case AF2 ignzwnj empty "same, round-12 strip reverted (both)      " "" r12all "$CLEAN_FP"
    r9_case AG2 ignrlo  empty "same, round-12 strip reverted (both)      " "" r12all "$CLEAN_FP"
    # (AF3)/(AF4)/(AG3)/(AG4) — EACH IMPLEMENTATION ON ITS OWN, the round-10/11 split pattern.
    r9_case AF3 ignzwnj empty "ZWNJ, only the SWEEP reverted             " \
        "AUDIT_FINGERPRINT_UNVERIFIABLE" r12guard "$CLEAN_FP"
    r9_case AF4 ignzwnj empty "ZWNJ, only the HELPER reverted            " \
        "GIT_CASEFOLD_DIR_ALIAS" r12fp "$CLEAN_FP"
    r9_case AG3 ignrlo  empty "RLO, only the SWEEP reverted              " \
        "AUDIT_FINGERPRINT_UNVERIFIABLE" r12guard "$CLEAN_FP"
    r9_case AG4 ignrlo  empty "RLO, only the HELPER reverted             " \
        "GIT_CASEFOLD_DIR_ALIAS" r12fp "$CLEAN_FP"
    R9_ROOT=""
else
    note "(AF/AG) filesystem arm: NO IGNORABLE-FOLDING VOLUME — could not attach a case-insensitive" \
         "HFS+ image (\`hdiutil create -fs HFS+\`); the RED arm runs SIMULATED in (AI) and says so." \
         "Export AX_PROVE_IGN_DIR=<dir on a folding volume> for the live one."
fi
# (AH) THE FALSE-POSITIVE CONTROL, live: on any filesystem that treats U+200C as significant — both
# APFS variants, measured — the two spellings are DISTINCT inodes and must NOT be refused, even
# though the round-12 fold puts them under ONE key. This is the arm that keeps the widened fold from
# bricking every fork-receiver, and it is the reason the strip set may safely be WIDER than TN1150's
# 16 characters: an extra character can only ever merge KEYS, and a verdict still requires an
# OBSERVED shared inode.
if [ "$WORK_FOLDS_IGN" -eq 0 ]; then
    r9_case AH csignorable empty "DISTINCT SAFE/ and SAFE<U+200C>/ NOT refused" ""
else
    note "(AH) skipped: \$WORK itself folds ignorable characters, so a distinct-inode pair cannot" \
         "be built there (this is the inverse of the (AF/AG) fallback and is reported, not hidden)"
fi

# (AD) ROUND 11 — THE TWO IMPLEMENTATIONS MUST AGREE ON THE FOLD, and (AE) the NORMALIZATION
# false-positive control, which is SIMULATED and says so.
# WHY SIMULATED: a distinct-inode `é/` + `e◌́/` pair needs a normalization-SENSITIVE filesystem, and
# this platform has none to offer — MEASURED with hdiutil on this machine: case-insensitive APFS,
# CASE-SENSITIVE APFS, case-sensitive HFS+, ExFAT and FAT32 are ALL normalization-INSENSITIVE (the
# FAT variants additionally rewrite the spelling to NFD on write). So (Z5)/(AA5) are the real
# distinct-inode controls the platform CAN build (ASCII case and non-ASCII case), and the
# normalization one is exercised here against synthetic (st_dev, st_ino) identities through the
# SHIPPED grouping code. It is labelled SIMULATED rather than claimed as a live control.
note "(AD/AE) fold parity + SIMULATED normalization false-positive control"
# `-B`, and it is NOT cosmetic: this is the ONLY probe in this file that IMPORTS a module out of the
# live tree, and without -B python writes practices/scripts/lib/__pycache__/ into it. Measured while
# adding this arm: the prover left that directory untracked, so the very next `git status` reported
# a DIRTY tree and the push-evidence chain this harness exists to defend would have been broken by
# the harness itself. A proof that dirties the thing it measures is not a proof.
"${AX_PY_BIN:-python3}" -B - "$REPO_ROOT" <<'PY' || violation "(AD/AE) round-11 fold parity or the simulated normalization false-positive control FAILED"
import importlib.util, pathlib, re, subprocess, sys, unicodedata
sys.dont_write_bytecode = True
root = pathlib.Path(sys.argv[1])
spec = importlib.util.spec_from_file_location("axtf", root / "practices/scripts/lib/tree_fingerprint.py")
tf = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tf)
guard_src = (root / "practices/evals/completion_checklist_recency_guard.sh").read_text()
m = re.search(r"^def _ax_fold_path_key.*?^(?=\S)", guard_src, re.M | re.S)
if not m:
    print("  (AD) the 12c sweep's _ax_fold_path_key could not be located", file=sys.stderr)
    sys.exit(1)
ns = {"unicodedata": unicodedata}
exec(m.group(0), ns)
gfold, ffold = ns["_ax_fold_path_key"], tf._fold_path_key
# (AD) parity over EVERY prefix of EVERY tracked path in the live catalog, plus the adversarial
# folding corpus. Two implementations of one rule are two chances to drift.
corpus = []
for p in subprocess.run(["git", "-C", str(root), "ls-files", "-z"],
                        stdout=subprocess.PIPE, check=True).stdout.split(b"\0"):
    if p:
        c = p.split(b"/")
        corpus += [b"/".join(c[:n]) for n in range(1, len(c) + 1)]
live = len(set(corpus))
corpus = sorted(set(corpus)) + [
    b'\xc3\xa9', b'e\xcc\x81', b'\xc3\x89', b'E\xcc\x81', b'A', b'a', b'\xc5\xbf', b's',
    b'\xe2\x84\xaa', b'k', b'\xc3\x9f', b'ss', b'\xe1\xba\x9b\xcc\xa3', b'\xe1\xb9\xa9',
    b'\xc4\xb0', b'i', b'\xff', b'A\xffB', b'a\xffb', b'\xed\xa0\x80', b'', b'a/\xff/B',
    # ROUND 12: the ignorable axis, plus the characters HFS+ measurably does NOT ignore.
    b'SAFE', b'SAFE\xe2\x80\x8c', b'SAFE\xe2\x80\xae', b'SAFE\xef\xbb\xbf', b'\xe2\x80\x8c',
    b'SAFE\xc2\xad', b'SAFE\xef\xb8\x8f', b'SAFE\xe2\x81\xa0', b'\xe2\x80\x8cA/\xe2\x80\x8eB']
drift = [x for x in corpus if gfold(x) != ffold(x)]
if drift:
    print(f"  (AD) the two folds DISAGREE on {len(drift)} input(s), e.g. {drift[:3]}",
          file=sys.stderr)
    sys.exit(1)
print(f"  (AD) fold parity: {len(corpus)} inputs ({live} live-tree prefixes) — 0 disagreements")
# The fold must actually MERGE the pairs, otherwise (AE) below is vacuous.
for a, b, lab in ((b'\xc3\xa9', b'e\xcc\x81', "NFC/NFD"), (b'\xc3\x89', b'\xc3\xa9', "É/é")):
    if ffold(a) != ffold(b):
        print(f"  (AE) vacuous: {lab} do not fold equal", file=sys.stderr)
        sys.exit(1)
# (AE) the DISCRIMINATOR, through the shipped grouping code. ONE key, TWO synthetic inodes ⇒ no
# report (a normalization-sensitive receiver is not refused); ONE key, ONE inode ⇒ reported.
NFC, NFD = b'\xc3\xa9', b'e\xcc\x81'
key = ffold(NFC)
full = {NFC + b"/check.sh", NFD + b"/helper"}
two = {key: {(1, 10): {NFC}, (1, 11): {NFD}}}
one = {key: {(1, 10): {NFC, NFD}}}
leaf_t, dir_t = tf._alias_verdicts(two, full)
leaf_o, dir_o = tf._alias_verdicts(one, full)
if leaf_t or dir_t:
    print(f"  (AE) FALSE POSITIVE: distinct inodes were reported {leaf_t} {dir_t}", file=sys.stderr)
    sys.exit(1)
if not dir_o:
    print("  (AE) vacuous: one inode under one folded key produced no report", file=sys.stderr)
    sys.exit(1)
print("  (AE) SIMULATED normalization control: distinct inodes → 0 reports; "
      f"one inode → {dir_o} (the discriminator, not the fold, decides)")

# ── (AI) ROUND 12: THE STRIP SET, ASSERTED RATHER THAN ASSERTED-IN-PROSE. The docstring's choice
# (general category Cf) rests on four measurable claims. If a future Unicode release, a future
# Python, or a future edit breaks any of them, this must fail here instead of in a fork-receiver's
# push evidence. TN1150's set is the 16 code points its fold table maps to zero; it is quoted here
# as a LITERAL only to be checked AGAINST the predicate — the shipped code holds no such list.
TN1150 = [0x200C, 0x200D, 0x200E, 0x200F, 0x202A, 0x202B, 0x202C, 0x202D, 0x202E,
          0x206A, 0x206B, 0x206C, 0x206D, 0x206E, 0x206F, 0xFEFF]
cf = {c for c in range(0x110000) if unicodedata.category(chr(c)) == "Cf"}
claims = []
claims.append(("TN1150's 16 ignorables are all category Cf (the strip is a SUPERSET of what the "
               "filesystem folds — a missing character is a silent false-green)",
               all(c in cf for c in TN1150)))
claims.append(("no ASCII scalar is Cf, so the ASCII fast path stays a TRUE equivalence",
               not any(c < 128 for c in cf)))
claims.append(("neither casefold(NFD(.)) nor NFC(.) ever INTRODUCES a Cf character, so ONE strip "
               "pass placed first is provably sufficient",
               not any(any(unicodedata.category(x) == "Cf"
                           for x in unicodedata.normalize("NFD", chr(c)).casefold()
                           + unicodedata.normalize("NFC", chr(c)))
                       for c in range(0x110000)
                       if unicodedata.category(chr(c)) not in ("Cs", "Cf"))))
claims.append(("the fold MERGES the ignorable pairs (otherwise (AF)/(AG) and the simulated arm "
               "below are vacuous)",
               ffold(b"SAFE") == ffold(b"SAFE\xe2\x80\x8c") == ffold(b"SAFE\xe2\x80\xae")
               == ffold(b"SAFE\xef\xbb\xbf")))
for text, ok in claims:
    if not ok:
        print(f"  (AI) STRIP-SET CLAIM BROKEN: {text}", file=sys.stderr)
        sys.exit(1)
print(f"  (AI) strip set = general category Cf ({len(cf)} code points, Unicode "
      f"{unicodedata.unidata_version}); all 4 claims hold, TN1150's 16 ⊆ Cf")
# The SIMULATED counterpart of (AF)/(AG), which runs unconditionally so the RED direction is never
# unmeasured even on a host with no ignorable-folding filesystem: the SHIPPED grouping code, with
# the two spellings assigned ONE synthetic inode, must report — and with TWO, must not.
Z, ZW = b"SAFE", b"SAFE\xe2\x80\x8c"
zkey, zfull = ffold(Z), {Z + b"/check.sh", ZW + b"/helper"}
zl_t, zd_t = tf._alias_verdicts({zkey: {(1, 20): {Z}, (1, 21): {ZW}}}, zfull)
zl_o, zd_o = tf._alias_verdicts({zkey: {(1, 20): {Z, ZW}}}, zfull)
if zl_t or zd_t:
    print(f"  (AI) FALSE POSITIVE: distinct inodes were reported {zl_t} {zd_t}", file=sys.stderr)
    sys.exit(1)
if not zd_o:
    print("  (AI) vacuous: one inode under one folded key produced no report", file=sys.stderr)
    sys.exit(1)
print(f"  (AI) SIMULATED ignorable control: distinct inodes → 0 reports; one inode → {zd_o}")
PY

echo ""
if [ "$FAIL" -ne 0 ]; then
    echo "ax-prove-hermetic-runtime: FAIL — an inherited-runtime path is open" >&2
    exit 1
fi
echo "ax-prove-hermetic-runtime: PASS — a redirected git context, an exported git/cd/pwd/python3,"
echo "  a tampered fingerprint helper (committed or not), an exported set/[ arriving before the"
echo "  scrub, a /usr/bin/true interpreter, a PYTHONPATH sitecustomize, a clean-filter byte mask,"
echo "  a COMMITTED push-only bypass, a SELF-ERASING \$BASH_ENV, a preset AX_PRIV_REEXEC, an"
echo "  exported export() ahead of a sourced lib's preflight, a filter.<n>.process content mask, an"
echo "  index-regular path swapped for a SYMLINK under --assume-unchanged, a --skip-worktree"
echo "  DELETION, an executable-bit divergence in both directions, a POPULATED uninitialized"
echo "  gitlink, a LEAF casefold alias, a DIRECTORY-component casefold alias, a directory aliased by"
echo "  UNICODE NORMALIZATION (é ≡ e◌́) and one aliased by NON-ASCII CASE (É ≡ é) are each refused by"
if [ -n "${IGN_ROOT:-}" ]; then
echo "  the live gates; a directory aliased by an IGNORABLE FORMAT CHARACTER (SAFE ≡ SAFE<U+200C>,"
echo "  and ≡ SAFE<U+202E>) is refused ON A REAL CASE-INSENSITIVE HFS+ VOLUME attached by this run,"
echo "  with sweep-only and helper-only twins for each;"
else
echo "  the live gates; the IGNORABLE-FORMAT arm found NO folding filesystem on this host, so its"
echo "  RED direction ran SIMULATED in (AI) against the shipped grouping code and the live (AF)/(AG)"
echo "  volume arms were NOT exercised on this run (export AX_PROVE_IGN_DIR, or run where"
echo "  \`hdiutil create -fs HFS+\` works, for the real one);"
fi
if [ "${WORK_FOLDS_IGN:-0}" -eq 0 ]; then
echo "  the ignorable false-positive control is LIVE — distinct-inode SAFE/ and SAFE<U+200C>/ are"
echo "  NOT refused on this filesystem;"
else
echo "  the ignorable false-positive control was SKIPPED — \$WORK itself folds the character;"
fi
echo "  the unattacked control passes, an uninitialized gitlink is NOT refused, the"
if [ -n "${CS_ROOT:-}" ]; then
echo "  false-positive controls pass ON A REAL CASE-SENSITIVE FILESYSTEM (distinct inodes for A/·a/"
echo "  AND for É/·é/, and a shared prefix, are not aliases), the two implementations' path folds"
else
# CORRECTED 2026-08-01 (independent verification lane): this paragraph used to assert the
# distinct-inode controls unconditionally, even on a run where (Z5) had SAID it fell back to the
# shared-prefix arm. The per-arm line was honest and the summary was not — a summary that
# overstates what its own run measured is the defect this harness exists to catch.
echo "  false-positive control that ran is the SHARED-PREFIX one only — this filesystem is"
echo "  case-INSENSITIVE, so the distinct-inode A/·a/ and É/·é/ controls were NOT exercised on this"
echo "  run (export AX_PROVE_CS_DIR=<case-sensitive dir> for those); the two implementations' folds"
fi
echo "  agree over every prefix of every"
echo "  tracked path, and every round-5/6/7/8/9/10/11/12 addition has a neutered twin in which its"
echo "  attack lands again — including a bits-only twin for the round-8 representation backstop"
echo "  and, for every round-9, round-10, round-11 and round-12 refusal, SEPARATE sweep-only and helper-only"
echo "  twins proving each implementation load-bearing on its own — so the refusals are"
echo "  attributable to the fixes, not the sandbox."
exit 0
