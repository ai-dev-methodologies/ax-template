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
# Why the round-13 addition to 12c (cross-family review ROUND 13, P1; TD-2026-08-01-(P1-symlink-
# target-alias)). New BLOCKING code: GIT_SYMLINK_TARGET_ALIAS.
#   Rounds 9-12 built an alias census and widened its key three times (case → normalization →
#   non-ASCII case → ignorable Cf). Every one of those rounds registered only INDEX PATHS. A
#   symlink's TARGET is not an index path — it is BLOB CONTENT, read as bytes and hashed and never
#   resolved — so the whole four-round census did not apply to it on ANY axis.
#   MEASURED (reviewer round 13; commit only, no environment control):
#       git mv backend/gradlew backend/gradlew-real
#       ln -s GRADLEW-REAL backend/gradlew          # the CASE is the whole attack
#   On ordinary case-insensitive APFS `backend/gradlew` resolves `GRADLEW-REAL` to the tracked
#   `backend/gradlew-real`, so R25 EXECUTES successfully, `git status --porcelain -uall` is EMPTY
#   and the fingerprint returns the clean-tree constant. On a case-SENSITIVE checkout the committed
#   symlink is DANGLING: the receiver gets a broken gradlew while our evidence says the tree is
#   clean and the build passed. (Reviewer confirmed live: `backend/gradlew` and `backend/GRADLEW`
#   are the same APFS inode, `backend/GRADLEW` is not an indexed spelling, and the shipped detector
#   produced helper_verdict ([], []).)
#   THE RULE: resolve the target's bytes against the link's own (recorded) directory; if the
#   result stays inside the repository, EXISTS, and its (st_dev, st_ino) is that of a REGISTERED
#   prefix, then its spelling must BE a registered spelling — a FOLD-EQUAL but different spelling is
#   an alias and BLOCKS. Precision comes from gating on fold-equality (`_ax_fold_path_key`, shared
#   with the fingerprint helper) rather than on bare inequality: `..` traversal, chains through an
#   intermediate symlinked directory, and absolute targets all differ from the record textually and
#   none of them is an alias, so none of them is refused. See the 12c comment for the full
#   disposition table (absolute / escaping / untracked / dangling / chained / directory / exact).
#
# Why the round-14 replacement of that resolution (cross-family review ROUND 14, P1-A;
# TD-2026-08-01-(P1-posix-resolution-and-runtime-paths)). New BLOCKING code:
# GIT_SYMLINK_RESOLUTION_UNBOUNDED.
#   Round 13 resolved the target LEXICALLY — it popped `..` textually, BEFORE following anything.
#   The kernel pops `..` AFTER following an intermediate symlink, so any target whose `..` sits
#   behind a symlinked component resolved to a path the receiver's kernel never visits. MEASURED,
#   committed content only: `backend/jump -> real/sub` + `backend/gradlew -> jump/../GRADLEW-REAL`
#   over a tracked `backend/real/gradlew-real` — POSIX reaches `backend/real/GRADLEW-REAL`, which
#   case-insensitive APFS serves as the TRACKED file, so R25 executes the wrapper and goes green,
#   while the lexical candidate `backend/GRADLEW-REAL` is ABSENT and BOTH implementations took the
#   dangling exit and said nothing. The walk is now component by component, and two budgets
#   (40 follows — the LARGER of Linux MAXSYMLINKS 40 and macOS SYMLOOP_MAX 32 — and 4096
#   components) BLOCK on exhaustion instead of going silent: an unfinished walk has not ANSWERED
#   the alias question, and a committed cycle is ELOOP at the receiver.
#
# Why the round-14b widening of the SUBJECT (same TD entry; found by INDEPENDENT VERIFICATION).
# No new code — GIT_SYMLINK_TARGET_ALIAS, applied to more subjects.
#   Round 14 followed intermediates correctly and then took the alias verdict ONCE, on the FINAL
#   candidate, so the follow DISCARDED the intermediate's spelling. MEASURED on one tree with a
#   tracked `mid/dirlink -> real`, committed content only:
#       ln -s DIRLINK  mid/x   → exit 15   (final component: the round-13/14 class)
#       ln -s DIRLINK/ mid/x   → exit 0    (SAME alias, one keystroke, UNREFUSED)
#       ln -s DIRLINK/real.txt → exit 0    (the same hole without the keystroke)
#   The trailing slash is honoured LEGITIMATELY — the kernel follows the final component when
#   something follows it — and that legitimacy is what moved the alias out of reach: the follow
#   lands on the correctly-spelled `mid/real`, which passes on an EXACT match, while `mid/DIRLINK`,
#   the spelling that dangles at a case-sensitive receiver, was never asked about. The verdict is
#   now taken on EVERY component the walk resolves. An UNTRACKED intermediate is NOT refused —
#   there is no recorded spelling for it to be an alias OF, the same exit an untracked final
#   candidate has always taken.
#
# Why the round-10 addition to 12c (cross-family review ROUND 10, P1; TD-2026-07-31-(P1-casefold-
# prefix)). New BLOCKING code: GIT_CASEFOLD_DIR_ALIAS.
#   Round 9's casefold check was LEAF-ONLY, and the entry that shipped it overclaimed. Both
#   implementations grouped COMPLETE folded paths, so two entries whose LEAF names differ never met
#   even when a shared DIRECTORY component was a casefold alias. MEASURED against the d567c37
#   implementations, throwaway repo on APFS:
#       index: A/check.sh   (committed content: `cat A/helper`)
#       index: a/helper     (committed content: PASS)
#       disk : A and a are the SAME directory inode (16777229, 34423509)
#   `git status --porcelain` EMPTY · `cat A/helper` succeeds locally so every gate goes green ·
#   tree_fingerprint returned the clean-tree constant 0a815065… at exit 0 · 12c's violation set was
#   EMPTY. The PUSHED tree holds only `a/helper`, so a case-sensitive receiver has no `A/helper` and
#   the committed check is broken on arrival.
#   The fix measures EVERY PATH PREFIX (each directory component, folded, plus the full path),
#   gitlinks included, and refuses a folded group whose DISTINCT spellings lstat to the same
#   (st_dev, st_ino). Still a MEASUREMENT: on a case-sensitive volume genuinely distinct `A/` and
#   `a/` yield distinct inodes and are NOT refused (verified on a real case-sensitive APFS image).
#   Measured cost on this catalog: 5,745 entries, ~1.1k extra directory lstats, +~0.02 s.
#
# Why the round-9 additions to 12c (cross-family review ROUND 9, P1-1/P1-2/(e); TD-2026-07-30-
# (P1-representation-parity)). New BLOCKING codes: GIT_EXEC_BIT_DIVERGENCE,
# GIT_GITLINK_UNINITIALIZED_POPULATED, GIT_CASEFOLD_ALIAS.
#   Round 8 separated the SHAPES (regular / symlink / gitlink / absent) and then compared only BLOB
#   BYTES. A tracked path's representation carries three more facts, and no digest here holds any:
#     · THE EXECUTABLE BIT. `git config core.fileMode false` + `git update-index --chmod=-x
#       backend/gradlew` + `chmod +x` on disk → `git status --porcelain` EMPTY, blob bytes
#       identical, fingerprint = the clean-tree constant, and R25's 118 direct `./gradlew`
#       invocations run against a locally-executable file that the push records as 100644 and a
#       fresh checkout cannot execute. The index mode is read from `git ls-files -s`, so the
#       comparison is INDEPENDENT of core.fileMode — that setting suppresses the REPORT, not the
#       RECORD. Both directions block, because the mirror (index 100755, non-executable on disk)
#       is how an operator "fixes" a locally failing script with a chmod the index never learns.
#     · A POPULATED BUT UNINITIALIZED GITLINK. Round 8's exemption returned success on the ABSENCE
#       of `<gitlink>/.git` and never required the directory to be EMPTY. Measured: `git
#       update-index --add --cacheinfo 160000,<sha>,vendor/sub` + a mandatory step that runs
#       `bash vendor/sub/check.sh` + that file created on disk with no `.git` → status EMPTY, R25
#       executes it, and the push ships a gitlink whose fresh clone is an EMPTY directory. The
#       fresh-clone shape (absent, or an actually-empty directory) stays ACCEPTED — that is the
#       state of all three gitlinks here and (S) in the prover holds it.
#     · A CASEFOLD ALIAS — LEAF ONLY, as round 10 had to correct. Two index entries differing only
#       in case are ONE file on APFS/NTFS.
#       Flagged only when they lstat to the same (st_dev, st_ino), so a case-sensitive
#       fork-receiver is unaffected. HONEST SCOPE: with divergent blobs the alias ALSO shows up as
#       a modification, so the gate's clean-tree precondition already refuses the ordinary form —
#       this refusal is defense in depth that names the fault (one file, two blobs, one of them
#       never on disk to be verified) instead of the symptom, and it fires in 12c before 12a.
#   Measured on this catalog: 5,745 tracked paths, ZERO of all three.
#
# Why the round-8 additions to 12c (cross-family review ROUND 8, P1-A; TD-2026-07-31-(P1-worktree-
# representation)). New BLOCKING codes: GIT_WORKTREE_TYPE_MISMATCH, GIT_TRACKED_PATH_ABSENT,
# GIT_INDEX_FLAGS_SET, GIT_GITLINK_DIVERGENCE.
#   Round 7 hashed the raw bytes of every tracked path git calls clean — but only for the paths
#   whose ON-DISK SHAPE it expected. Two representations were `continue`d, and each hides bytes
#   that a push ships. MEASURED, three trees, ONE digest (0a815065…, the clean-tree constant):
#     (i)  a clean tree;
#     (ii) HEAD carries a malicious practices/verification-checklist.yaml; `git update-index
#          --assume-unchanged` it, `rm` it, replace it with a SYMLINK to a benign file outside
#          the repository. `git status --porcelain` is EMPTY, the index still says mode 100644,
#          both sweeps hit islink() and skip → R25 verifies the benign target, the push ships the
#          malicious blob;
#     (iii) a malicious tracked source marked `--skip-worktree` and deleted. Status empty, the
#          build omits the file, both sweeps ignore the absent path.
#   A representation the sweep did not expect is not a `continue`; it is an answer the gate cannot
#   give, and unknown never passes. The index BITS behind (ii)/(iii) are also read directly
#   (`git ls-files -v`: lowercase tag = assume-unchanged, `S` = skip-worktree) and refused, and
#   gitlinks — round 7's other stated exemption — are BOUND: an initialized submodule must be at
#   the recorded commit. Applied symmetrically here and in practices/scripts/lib/
#   tree_fingerprint.py, because the writer and the verifier must not disagree about what a tree
#   is. Residue, registered rather than hidden: an UNINITIALIZED submodule is not blocked (all
#   three gitlinks in this catalog are empty post-clone fixture directories), and dirt inside an
#   initialized submodule's own work tree is that repository's fingerprint — docs/BACKLOG.md P3-119.
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

# ── ROUND 7 / P1-1: PRIVILEGED-MODE RE-EXEC — THIS IS THE FIRST EXECUTABLE TEXT ─────
# (TD-2026-07-30-P1-privileged-startup.) The round-6 preflight below is IN-SCRIPT, and bash
# sources $BASH_ENV BEFORE a script's first line — so ANY in-script sensor is structurally too
# late. MEASURED (cross-family reviewer, round 7): a SELF-ERASING payload
#     builtin unset BASH_ENV ENV; function set(){ builtin exit 0; }
# leaves NOTHING for the preflight to find — BASH_ENV is gone from the environ it reads, and the
# function is NOT exported so no BASH_FUNC_* entry exists — and the first `set -uo pipefail` this
# file runs exits 0. `shopt -s expand_aliases` + an alias is the same class.
# THE FIX IS AT THE INVOCATION BOUNDARY, not in the script: bash PRIVILEGED MODE (-p) does not
# process $BASH_ENV/$ENV and does not import functions from the environment. This entry therefore
# re-executes ITSELF under -p before doing anything else. Measured, bash 3.2.57(1) / Apple:
#     BASH_ENV=<payload> bash  <entry>   → exit 0          (pre-fix; the gate never ran)
#     BASH_ENV=<payload> bash -p <entry> → honest verdict   (payload inert)
# CONSTRUCTS USED, and why each is unhijackable: `case` is a shell KEYWORD (never resolved through
# the function table); $-, ${x-} and ${x:?} are PARAMETER EXPANSIONS, not commands; /usr/bin/env is
# invoked by ABSOLUTE PATH (bash never looks a word containing a slash up as a function).
# `exec` IS shadowable, so the SECOND case re-asserts privileged mode AFTER it: a neutered exec
# falls through to a non-zero abort instead of quietly continuing unprivileged (measured: a
# BASH_ENV defining exec(){ :; } exits 1 here, it does not proceed).
# ── BACKLOG P2-70: THE RE-EXEC USES AN ENVIRONMENT ALLOWLIST (`env -i`) ─────────────
# The re-exec below hands the child a MEASURED allowlist instead of the parent's whole
# environment. Round 8 refused this for three reasons and round 9 MEASURED all three to be wrong:
# (i) the allowlist preserves the AX_RELEASE_ANCHOR_* pins (every consumer tests `[ -n "${V:-}" ]`,
# so empty == unset and forwarding them by prefix is exact); (ii) STRICT / LIVE_ROOT /
# GIT_ANCHOR are set INSIDE the guards AFTER the re-exec, so they were never inherited to begin
# with; (iii) BASH_ENV/ENV are forwarded ON PURPOSE — dropping them would SILENCE the
# HERMETIC_PREFLIGHT_HOSTILE refusal that is the point of seeing them, and forwarding them keeps
# that loud refusal firing exactly as before.
# WHAT IT BUYS: everything NOT on the list stops reaching this process — LD_PRELOAD /
# DYLD_INSERT_LIBRARIES, LC_*/LANG, and every variable a future dependency might read. The
# bootstrap below scrubs the GIT_* and PYTHON* families by name; a denylist can only remove what
# somebody thought of, and this is the same argument that made the GIT_* scrub a family sweep.
# WHY THE MARKER IS APPENDED LAST: `env` applies assignments left to right, so a hostile
# AX_PRIV_REEXEC=0 arriving through the AX_ prefix must not be able to overwrite the marker and
# turn the re-exec into an infinite loop.
# CONSTRUCTS: array assignment / append, `for`, `case`, and the indirect expansions
# ${!AX_@} · ${!v+s} · ${!v} — none of them is a command lookup, so invariant (α) (NOTHING
# OVERRIDABLE MAY EXECUTE BEFORE THE SCRUB THAT DETECTS OVERRIDES) still holds: the only command
# on this path is still /usr/bin/env, by absolute path.
# SCOPE, and why it is not all six: this form is applied ONLY to the entries whose process
# subtree execs NO FOREIGN TOOLCHAIN. `.githooks/pre-push` execs `./gradlew` directly,
# `run-all-guards.sh` does so transitively (vacuity_class_proof_guard's live PIT run), and
# `verify-completion.sh` execs BOTH gradle and npm. Those three keep the inheriting form with the
# reason recorded inline, because the environment surface of a foreign toolchain
# (GRADLE_*/JDK_*/NODE_*/npm_config_*) is not enumerable from this repository, and shipping an
# allowlist for a toolchain nobody ran under it would be an unmeasured change to the release gate.
# THE LOOP MARKER IS NOT TRUSTED. AX_PRIV_REEXEC means only "a re-exec was already attempted".
# An attacker who PRESETS it does not skip the re-exec — that branch ABORTS (measured, exit 1).
# It is unset the moment privileged mode holds, so it never reaches a child entry and cannot turn
# into a one-shot disable for the guards this entry launches.
# RESIDUAL, stated rather than hidden: a $BASH_ENV whose first line is `exit 0` ends the shell
# before this line exists (measured, exit 0, nothing printed). No in-script construct can survive
# that. It is the environment-control boundary declared in practices/DECISIONS.md,
# TD-2026-07-30-(ratchet-threat-model) — the same adversary can simply not install the hooks.
case $- in
    *p*) ;;
    *) case "${AX_PRIV_REEXEC-}" in
           1) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"completion_checklist_recency_guard: HERMETIC_PRIVILEGED_UNREACHABLE — a re-exec into bash privileged mode was already attempted and this shell is STILL unprivileged. Either exec is shadowed by a function, or AX_PRIV_REEXEC was preset in the environment to skip the re-exec. Both are refused; nothing in this gate runs unprivileged. Start it from a clean shell."} ;;
           *) case "${BASH:-}" in
                  /*) _AX_PV_ENV=()
                      for _AX_PV_N in PATH HOME TMPDIR JAVA_HOME BASH_ENV ENV ${!AX_@}; do
                          case "${!_AX_PV_N+s}" in s) _AX_PV_ENV+=("$_AX_PV_N=${!_AX_PV_N}") ;; esac
                      done
                      _AX_PV_ENV+=(AX_PRIV_REEXEC=1)
                      exec /usr/bin/env -i "${_AX_PV_ENV[@]}" "$BASH" -p "$0" "$@" ;;
                  *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"completion_checklist_recency_guard: HERMETIC_PRIVILEGED_UNREACHABLE — the running interpreter (BASH) is not an absolute path, so the SAME interpreter cannot be named unambiguously for the privileged re-exec."} ;;
              esac ;;
       esac ;;
esac
case $- in
    *p*) ;;
    *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"completion_checklist_recency_guard: HERMETIC_PRIVILEGED_UNREACHABLE — the re-exec returned instead of replacing this process, which means exec itself is shadowed. Unprivileged execution is refused."} ;;
esac
unset AX_PRIV_REEXEC

# ── ROUND 6 / P1-1: PURE-KEYWORD PREFLIGHT — SECOND, AND BEFORE ANY OVERRIDABLE COMMAND ──
# CORRECTION (ROUND 7): round 6 claimed this block was "the first executable text of all 8
# entries". It was NOT. The two SOURCED libs (practices/scripts/lib/release_anchor.sh:251 and
# .githooks/pre-push-lib.sh:27) ran `export GIT_NO_REPLACE_OBJECTS=1` FIRST, and `export` is an
# ordinary command lookup: MEASURED, an exported export() plus `alias exit=:` produced
# SOURCE_RC=0 for both. Those two exports now sit BELOW this preflight, and the round-7
# privileged re-exec above is what actually executes first.
# (TD-2026-07-30-P1-preflight-and-raw-bytes.) INVARIANT (α): NOTHING OVERRIDABLE MAY EXECUTE
# BEFORE THE SCRUB THAT DETECTS OVERRIDES. Round 5 put `set -uo pipefail` and `[ -n … ]` ahead of
# its own hermetic bootstrap, and both are ordinary command lookups: MEASURED —
#   BASH_FUNC_set%%='() { exit 0; }'  → .githooks/pre-push exit 0   (honest baseline 1)
#   BASH_FUNC_[%%='() { exit 0; }'    → this guard      exit 0      (honest baseline 2)
# A dependency LIST cannot fix that, because the list is consulted by code that has already run.
# So the first thing every entry does is expressed ONLY in constructs an exported function cannot
# reach:
#   · shell KEYWORDS (`if`, `case`, `for`, `[[ ]]`) — never resolved through the function table;
#   · variable ASSIGNMENT — not a command at all;
#   · ONE command invoked by ABSOLUTE PATH (`/usr/bin/env`): bash never looks a word containing a
#     slash up as a function, so this call cannot be hijacked;
#   · abort via `${x:?msg}` — a PARAMETER EXPANSION, not a command. A non-interactive shell writes
#     msg to stderr and exits non-zero. `exit`, `echo`, `printf`, `[`, `test` and `set` are all
#     shadowable and are therefore unusable at this point.
# WHY /usr/bin/env AND NOT `${!BASH_FUNC_@}`: bash imports BASH_FUNC_* out of the environment and
# then DELETES the shell variable, so the prefix expansion is EMPTY in the child while the environ
# still carries the entry (measured, bash 3.2.57 / Apple). The environ is the only place the
# channel is visible, and /usr/bin/env is the only unhijackable way to read it.
_AX_PF_LABEL="completion_checklist_recency_guard"
_AX_PF_ENV="$(/usr/bin/env)"
case "$_AX_PF_ENV" in
    "") _AX_PF_NULL=; _AX_PF_DIE=${_AX_PF_NULL:?"$_AX_PF_LABEL: HERMETIC_PREFLIGHT_UNVERIFIABLE — /usr/bin/env produced no output, so this gate cannot tell whether the environment carries exported shell functions. It is the one read that cannot be hijacked; without it, unknown never passes."} ;;
esac
case "$_AX_PF_ENV" in
    *BASH_FUNC_*) _AX_PF_NULL=; _AX_PF_DIE=${_AX_PF_NULL:?"$_AX_PF_LABEL: HERMETIC_PREFLIGHT_HOSTILE — the environment carries an exported shell function (BASH_FUNC_*). bash imports it BEFORE this script's first line, so it replaces a command this gate runs: measured, an exported set/[ turned this gate's honest exit into exit 0. Unset it (unset -f <name>) and re-run."} ;;
esac
case "${BASH_ENV:-}${ENV:-}" in
    ?*) _AX_PF_NULL=; _AX_PF_DIE=${_AX_PF_NULL:?"$_AX_PF_LABEL: HERMETIC_PREFLIGHT_HOSTILE — BASH_ENV/ENV is set, so every non-interactive bash this gate starts would source that file before running the gate's own code. Unset it and re-run."} ;;
esac
unset _AX_PF_ENV _AX_PF_NULL _AX_PF_DIE _AX_PF_LABEL
set -uo pipefail

# ── ROUND 5 / P1-1+P1-2: HERMETIC RUNTIME BOOTSTRAP (A) — DELIBERATELY DUPLICATED ────
# (TD-2026-07-30-P1-hermetic-runtime; the full argument lives in the header of
#  practices/scripts/lib/release_anchor.sh.) THE RATCHET MAY NOT INHERIT ITS OWN RUNTIME.
# Measured: an exported `git` function rewrote the anchor pin; an exported `pwd` moved the pin's
# root to a foreign repo; an exported `python3` turned a FAILING guard into exit 0; GIT_DIR/
# GIT_WORK_TREE made every read describe a CLEAN shadow checkout of a DIRTY tree. All of it
# arrives through the environment, so it must die BEFORE the first git call, BEFORE any `source`,
# and BEFORE this script computes its own directory with `cd`/`pwd` — which is exactly why these
# lines cannot live in a sourced file. The duplication IS the bootstrap.
_AX_HRM_LABEL="completion_checklist_recency_guard"; _AX_HRM_EXIT=2; _AX_HRM_NEED_PY=1
# ROUND 6 / P1-1(b): the list is now EVERY name any entry invokes anywhere, enumerated by
# grepping this catalog's own gate files — including the shell keyword-lookalikes that round 5
# omitted and that the reviewer weaponised (`set`, `[`, `test`, `printf`, `exit`, `exec`, `read`,
# `trap`, `shift`, `local`, `eval`). The pure-keyword preflight above already refuses ANY exported
# function, so this list is belt-and-braces — but a shortfall here used to BE the hole, and a list
# that is merely "what we remembered" is what round 5 shipped.
_AX_HRM_DEPS="git bash sh python python3 env cd pwd command builtin printf echo eval exec read \
test declare unset export local source grep egrep fgrep sed awk cut tr sort uniq head tail wc \
find ls cat cp mv rm mkdir rmdir mktemp dirname basename date shasum sha256sum md5sum xargs tee \
true false set exit return trap shift getopts times umask ulimit wait kill jobs let shopt \
enable alias unalias type hash caller readonly typeset mapfile readarray realpath readlink stat \
chmod touch diff cmp od base64 seq expr sleep tar gzip curl jq yq printenv id whoami uname \
install"
# Glob-metacharacter names (`[`, `[[`) cannot ride the unquoted split above — they would be
# read as bracket expressions by pathname expansion — so they are appended QUOTED at each use.
_AX_HRM_DEPS_Q="[ [["
_AX_HRM_BAD=""
for _ax_hn in $_AX_HRM_DEPS "[" "[["; do
    declare -F "$_ax_hn" >/dev/null 2>&1 && _AX_HRM_BAD="$_ax_hn"
done
if [ -n "$_AX_HRM_BAD" ]; then
    { echo "$_AX_HRM_LABEL: HELPER_FUNCTION_INJECTED — this shell already defines a function named"
      echo "  '$_AX_HRM_BAD', and that is a COMMAND THIS GATE INVOKES. bash imports exported"
      echo "  functions across \`bash script.sh\`, so the definition silently replaces the program"
      echo "  every check below runs — measured: an exported \`git\` rewrote the release-anchor pin,"
      echo "  an exported \`python3\` turned a failing guard into exit 0, an exported \`pwd\` moved the"
      echo "  pin's root to a foreign repository. Unset it (\`unset -f $_AX_HRM_BAD\`) and re-run."; } >&2
    exit $_AX_HRM_EXIT
fi
for _ax_hn in $_AX_HRM_DEPS "[" "[["; do unset -f "$_ax_hn" 2>/dev/null || true; done
# Any exported shell function that SURVIVED the scrub above is refused too — the enumeration is a
# list of what we know we call, and an unknown runtime is not a safe one. Names in this catalog's
# own namespaces are reported as HELPER_FUNCTION_INJECTED instead, because that is what they are
# and because the round-4 policy checks own that vocabulary.
_AX_HRM_FUNCS="$(/usr/bin/env | sed -n 's/^BASH_FUNC_\([A-Za-z_][A-Za-z0-9_]*\).*/\1/p' | tr '\n' ' ')"
if [ -n "${BASH_ENV:-}${ENV:-}" ]; then
    { echo "$_AX_HRM_LABEL: HERMETIC_ENV_HOSTILE — the environment sets BASH_ENV/ENV, which EVERY"
      echo "  non-interactive bash this gate starts would source before running the gate's own"
      echo "  code. That is code the gate never read, executing inside the gate. Unset it and"
      echo "  re-run — fail-closed on purpose: an unknown runtime is not a safe one."; } >&2
    exit $_AX_HRM_EXIT
elif [ -n "$_AX_HRM_FUNCS" ]; then
    case " $_AX_HRM_FUNCS " in
        *" ax_"*|*" _ax_"*|*" pp_"*)
            { echo "$_AX_HRM_LABEL: HELPER_FUNCTION_INJECTED — the environment carries an exported"
              echo "  shell function in this catalog's own namespace: ${_AX_HRM_FUNCS}"
              echo "  Those names ARE the ratchet policy — which commit the anchor is, whether an"
              echo "  absence is an honest bootstrap, which refs ship work. A definition arriving"
              echo "  from outside replaces the policy with the caller's."; } >&2 ;;
        *)
            { echo "$_AX_HRM_LABEL: HERMETIC_ENV_HOSTILE — the environment carries exported shell"
              echo "  function(s): ${_AX_HRM_FUNCS}"
              echo "  An exported function replaces a command this gate calls, after every check it"
              echo "  performs. The enumerated dependency list above is what we know we invoke; a"
              echo "  runtime carrying definitions we did not enumerate is refused rather than"
              echo "  assumed harmless. Unset them and re-run."; } >&2 ;;
    esac
    exit $_AX_HRM_EXIT
fi
# The WHOLE GIT_* family, not a denylist of the ones we thought of: every one of GIT_DIR,
# GIT_WORK_TREE, GIT_COMMON_DIR, GIT_OBJECT_DIRECTORY, GIT_ALTERNATE_OBJECT_DIRECTORIES,
# GIT_INDEX_FILE, GIT_NAMESPACE, GIT_CEILING_DIRECTORIES, GIT_CONFIG*, GIT_EXEC_PATH redirects
# what `git -C <path>` actually reads. GIT_NO_REPLACE_OBJECTS is scrubbed too and re-set below:
# inherited as 0 it RE-ENABLES replacement refs.
for _ax_hn in ${!GIT_@}; do unset "$_ax_hn" 2>/dev/null || true; done
unset BASH_ENV ENV GIT_DIR GIT_WORK_TREE GIT_COMMON_DIR GIT_OBJECT_DIRECTORY GIT_INDEX_FILE \
    GIT_ALTERNATE_OBJECT_DIRECTORIES GIT_NAMESPACE GIT_CEILING_DIRECTORIES GIT_CONFIG \
    GIT_CONFIG_GLOBAL GIT_CONFIG_SYSTEM GIT_CONFIG_NOSYSTEM GIT_CONFIG_COUNT GIT_EXEC_PATH \
    GIT_DISCOVERY_ACROSS_FILESYSTEM 2>/dev/null || true
export GIT_NO_REPLACE_OBJECTS=1
# git and python3 are resolved ONCE, to absolute paths, from a PATH stripped of relative entries
# (a `.` on PATH is a shim in whatever directory the gate happens to run from). The inherited
# order is otherwise preserved, because forcing system directories here would silently swap the
# interpreter that carries PyYAML and make whole guards skip — a fail-open dressed as hardening.
unset AX_GIT_BIN AX_PY_BIN
_AX_HRM_PATH=""; _ax_hifs="$IFS"; IFS=:
for _ax_hd in $PATH; do
    case "$_ax_hd" in /*) ;; *) continue ;; esac
    [ -d "$_ax_hd" ] || continue
    _AX_HRM_PATH="${_AX_HRM_PATH:+$_AX_HRM_PATH:}$_ax_hd"
done
IFS="$_ax_hifs"; unset _ax_hifs _ax_hd
AX_GIT_BIN="$(PATH="$_AX_HRM_PATH" command -v git 2>/dev/null || true)"
AX_PY_BIN="$(PATH="$_AX_HRM_PATH" command -v python3 2>/dev/null || true)"
# ── ROUND 6 / P1-2: A PATH IS NOT EVEN A SMOKE TEST ─────────────────────────────────
# INVARIANT: a tool this gate runs must at least ANSWER LIKE THE PROGRAM IT IS SUPPOSED TO BE.
# `-f`/`-x` FOLLOW SYMLINKS and assert nothing at all: MEASURED — a symlink named python3
# pointing at /usr/bin/true passed every lexical/`-x` test and turned the recency guard's entire
# python body into exit 0 (honest baseline 1). So each tool is (a) canonicalised through its real
# directory, (b) refused if it lives inside the tree under audit, and (c) RUN once against a
# fixed challenge — `git --version` must produce a git version banner, python3 must print a
# self-report under `-I -S`. A /usr/bin/true symlink prints nothing and is refused.
# WHAT THAT IS AND IS NOT (corrected, reviewer ROUND 8 / P1-B — the earlier prose called this
# 'identity' and the tool 'authenticated', and it is NEITHER). The challenge is PUBLIC and FIXED,
# so a hostile wrapper forwards it to the real binary, answers correctly, and does what it likes
# with everything else — measured by the reviewer in round 7, which flipped fail_audit_log_missing
# from exit 1 to exit 0. This is therefore a SMOKE TEST for a MIS-RESOLVED tool (a stub, a
# /usr/bin/true symlink, a shim on an inherited PATH), not authentication and not identity. PATH
# executables are DECLARED TRUSTED — see practices/DECISIONS.md TD-2026-07-30-(ratchet-threat-
# model) and docs/BACKLOG.md P2-68 for the external-trust-root work that would change that. The
# code name HERMETIC_TOOL_UNAUTHENTIC is kept because guards, fixtures and DECISIONS entries
# reference it; it means 'did not answer the smoke test', never 'failed authentication'.
# The PYTHON* family is scrubbed for the same reason the GIT_* family is: PYTHONPATH /
# PYTHONHOME / PYTHONEXECUTABLE / PYTHONSTARTUP redirect what the interpreter IS before a single
# line of ours runs — measured, a PYTHONPATH sitecustomize.py doing `os._exit(0)` skipped the whole
# python guard (honest baseline 1). Scrubbing is belt; `-I -S` at every ratchet call site is braces.
for _ax_hn in ${!PYTHON@}; do unset "$_ax_hn" 2>/dev/null || true; done
unset PYTHONPATH PYTHONHOME PYTHONSTARTUP PYTHONEXECUTABLE PYTHONUSERBASE PYTHONWARNINGS \
    PYTHONIOENCODING PYTHONMALLOC PYTHONBREAKPOINT PYTHONDEVMODE PYTHONPYCACHEPREFIX \
    PYTHONOPTIMIZE PYTHONVERBOSE PYTHONINSPECT PYTHONCASEOK PYTHONNOUSERSITE 2>/dev/null || true
export PYTHONDONTWRITEBYTECODE=1
for _ax_hn in "git=$AX_GIT_BIN" "python3=$AX_PY_BIN"; do
    if [ "${_ax_hn%%=*}" = "python3" ] && [ "$_AX_HRM_NEED_PY" != "1" ]; then continue; fi
    _ax_hb="${_ax_hn#*=}"
    if [ -z "$_ax_hb" ] || [ "${_ax_hb#/}" = "$_ax_hb" ] || [ ! -f "$_ax_hb" ] || [ ! -x "$_ax_hb" ]; then
        { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNUSABLE — ${_ax_hn%%=*} did not resolve to an"
          echo "  executable regular file on an absolute path (got '${_ax_hb:-<nothing>}')."
          echo "  This gate runs that program to decide whether a release may ship; it will not"
          echo "  guess, and it will not fall back to whatever the inherited PATH offers."; } >&2
        exit $_AX_HRM_EXIT
    fi
    # (a) canonicalise: the DIRECTORY is resolved with `builtin pwd -P`, so a symlinked directory
    #     on the way to the tool cannot disguise where it lives.
    _ax_hdir="$(builtin cd "$(dirname "$_ax_hb")" 2>/dev/null && builtin pwd -P)" || _ax_hdir=""
    if [ -z "$_ax_hdir" ]; then
        { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNUSABLE — the directory of ${_ax_hn%%=*} ('$_ax_hb')"
          echo "  could not be canonicalised, so this gate cannot say where the program it is about"
          echo "  to run actually lives."; } >&2
        exit $_AX_HRM_EXIT
    fi
    _ax_hb="$_ax_hdir/$(basename "$_ax_hb")"
    # (c) the SMOKE TEST: run it once and read what it says. A path cannot make this statement,
    #     and a hostile wrapper can (it forwards the fixed public challenge) — so this catches a
    #     mis-resolved tool, which is the in-scope class, and nothing beyond it.
    if [ "${_ax_hn%%=*}" = "git" ]; then
        _ax_hver="$("$_ax_hb" --version 2>/dev/null)" || _ax_hver=""
        case "$_ax_hver" in
            "git version "[0-9]*) AX_GIT_BIN="$_ax_hb" ;;
            *)  { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNAUTHENTIC — '$_ax_hb' is executable but does"
                  echo "  not identify itself as git (\`git --version\` said '${_ax_hver:-<nothing>}')."
                  echo "  Lexical absoluteness and -f/-x FOLLOW SYMLINKS and say nothing about the"
                  echo "  program: a symlink named python3 → /usr/bin/true satisfied all of them and"
                  echo "  turned an entire guard into exit 0. A mis-resolved tool is refused here; a"
                  echo "  hostile PATH wrapper is NOT caught by this and is declared out of scope."; } >&2
                exit $_AX_HRM_EXIT ;;
        esac
    else
        _ax_hver="$("$_ax_hb" -I -S -c 'import sys;sys.stdout.write("AXPY %d %d %s %s" % (sys.version_info[0], sys.version_info[1], sys.implementation.name, sys.executable or "-"))' 2>/dev/null)" || _ax_hver=""
        case "$_ax_hver" in
            "AXPY 3 "*) AX_PY_BIN="$_ax_hb" ;;
            *)  { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNAUTHENTIC — '$_ax_hb' is executable but does"
                  echo "  not identify itself as a python3 interpreter under \`-I -S\` (it said"
                  echo "  '${_ax_hver:-<nothing>}'). MEASURED: a symlink named python3 → /usr/bin/true"
                  echo "  passed every path test and silently skipped this gate's whole python body"
                  echo "  (exit 0 where the honest answer was 1). This is a smoke test for a"
                  echo "  mis-resolved interpreter, not authentication — see DECISIONS."; } >&2
                exit $_AX_HRM_EXIT ;;
        esac
    fi
done
export AX_GIT_BIN AX_PY_BIN
# Ratchet-internal python ALWAYS runs isolated: -I ignores PYTHON* env + user site + cwd on
# sys.path, -S skips site entirely, which is what kills a sitecustomize.py payload. NOT usable for
# the three call sites that need PyYAML out of site-packages (the checklist parser and the two
# ratcheting guards' bodies) — those use -E, which still refuses PYTHONPATH/PYTHONHOME/
# PYTHONSTARTUP, and are preceded by an `import yaml` capability probe that BLOCKS (never skips).
# See DECISIONS.md TD-2026-07-30-P1-preflight-and-raw-bytes for the honest limit that leaves.
# PUBLISHED, NOT CONSUMED IN-TREE: every in-tree call site spells the flags LITERALLY so that
# `grep -n ' -I -S '` finds all of them; this export exists for fork-receiver call sites.
export AX_PY_ISO="-I -S"
# ── BACKLOG P2-67: mktemp IS A TOOL THIS GATE RUNS, AND ITS ANSWER IS A TRUST BOUNDARY ──
# Every entry and every proof harness called `mktemp -d` as a BARE WORD, and then used the
# directory it handed back to extract PREVIOUS-RELEASE BLOBS out of git and to build the
# sandboxes whose verdicts decide whether a release ships. Two unexamined assumptions sat in
# that one word:
#   · WHICH mktemp. A bare word resolves through PATH — the same channel the round-6 smoke test
#     exists to refuse for git and python3. A PATH-earlier shim can return a directory the
#     attacker already owns (this catalog SHIPS such a shim, deliberately, in
#     resume_provenance_guard.sh, which is proof the channel is not hypothetical).
#   · WHAT IT RETURNED. Nothing checked the returned path at all. `mktemp -d` is documented to
#     create mode-0700, but the gate never verified it — and a shim, an inherited umask quirk or
#     a pre-existing path is exactly the case where the documentation stops applying. A
#     group/other-writable extraction directory means another process can replace the extracted
#     previous-release implementation between the extraction and the run.
# So mktemp is resolved ONCE, absolutely, from the same relative-entry-stripped PATH as git and
# python3, and `_ax_mktemp_d` VERIFIES what came back: a real directory (not a symlink), owned by
# THIS euid, with no group/other write bit. Anything else BLOCKS — this is the directory the gate
# is about to trust, and an unverifiable one is not a safe one. The verification doubles as the
# functional smoke test the other two tools get: a /usr/bin/true shim returns nothing and dies at
# the first branch.
AX_MKTEMP_BIN="$(PATH="$_AX_HRM_PATH" command -v mktemp 2>/dev/null || true)"
if [ -z "$AX_MKTEMP_BIN" ] || [ "${AX_MKTEMP_BIN#/}" = "$AX_MKTEMP_BIN" ] \
   || [ ! -f "$AX_MKTEMP_BIN" ] || [ ! -x "$AX_MKTEMP_BIN" ]; then
    { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNUSABLE — mktemp did not resolve to an executable"
      echo "  regular file on an absolute path (got '${AX_MKTEMP_BIN:-<nothing>}'). This gate"
      echo "  extracts previous-release blobs and builds sandboxes inside the directory that"
      echo "  program returns; it will not take that directory from whatever PATH offers."; } >&2
    exit $_AX_HRM_EXIT
fi
_ax_hdir="$(builtin cd "$(dirname "$AX_MKTEMP_BIN")" 2>/dev/null && builtin pwd -P)" || _ax_hdir=""
if [ -z "$_ax_hdir" ]; then
    { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNUSABLE — the directory of mktemp ('$AX_MKTEMP_BIN')"
      echo "  could not be canonicalised, so this gate cannot say where the program it is about"
      echo "  to run actually lives."; } >&2
    exit $_AX_HRM_EXIT
fi
AX_MKTEMP_BIN="$_ax_hdir/$(basename "$AX_MKTEMP_BIN")"
export AX_MKTEMP_BIN
# _ax_mktemp_d <label> — echo a VERIFIED private temp directory, or return non-zero having said
# why on stderr. Callers treat non-zero as blocking; nothing here falls back to an unverified
# directory. `stat` is tried in BSD then GNU spelling; if neither answers, the mode is UNKNOWN
# and unknown never passes.
_ax_mktemp_d() {
    local _ax_md_lbl="$1" _ax_md_d _ax_md_st _ax_md_uid _ax_md_mode
    _ax_md_d="$("$AX_MKTEMP_BIN" -d "${TMPDIR:-/tmp}/ax-priv.XXXXXXXX" 2>/dev/null)" || _ax_md_d=""
    if [ -z "$_ax_md_d" ] || [ -L "$_ax_md_d" ] || [ ! -d "$_ax_md_d" ]; then
        { echo "${_ax_md_lbl}: HERMETIC_TEMPDIR_UNUSABLE — the resolved mktemp did not return a"
          echo "  real directory (got '${_ax_md_d:-<nothing>}'). A symlink or a non-directory is"
          echo "  refused outright: this gate is about to extract release blobs into it."; } >&2
        return 1
    fi
    _ax_md_st="$(stat -f '%u %Lp' "$_ax_md_d" 2>/dev/null)" || _ax_md_st=""
    [ -n "$_ax_md_st" ] || _ax_md_st="$(stat -c '%u %a' "$_ax_md_d" 2>/dev/null)" || _ax_md_st=""
    case "$_ax_md_st" in
        [0-9]*" "[0-7][0-7][0-7]|[0-9]*" "[0-7][0-7][0-7][0-7]) ;;
        *)  { echo "${_ax_md_lbl}: HERMETIC_TEMPDIR_UNVERIFIABLE — the owner/mode of ${_ax_md_d}"
              echo "  could not be read (stat said '${_ax_md_st:-<nothing>}'), so this gate cannot"
              echo "  tell whether another user can write into the directory it is about to trust."; } >&2
            rm -rf "$_ax_md_d" 2>/dev/null || true
            return 1 ;;
    esac
    _ax_md_uid="${_ax_md_st%% *}"
    _ax_md_mode="${_ax_md_st##* }"
    if [ "$_ax_md_uid" != "${EUID:-$(id -u)}" ] || [ $(( 8#$_ax_md_mode & 8#22 )) -ne 0 ]; then
        { echo "${_ax_md_lbl}: HERMETIC_TEMPDIR_HOSTILE — ${_ax_md_d} is owned by uid"
          echo "  ${_ax_md_uid} (this process is ${EUID:-?}) and/or is group/other-writable"
          echo "  (mode ${_ax_md_mode}). The previous release's implementation is extracted into"
          echo "  this directory and then RUN; a directory somebody else can write is a directory"
          echo "  somebody else chooses the implementation in."; } >&2
        rm -rf "$_ax_md_d" 2>/dev/null || true
        return 1
    fi
    echo "$_ax_md_d"
    return 0
}
unset _ax_hn _ax_hb _ax_hdir _ax_hver _AX_HRM_BAD _AX_HRM_PATH

SCRIPT_DIR="$(builtin cd "$(dirname "${BASH_SOURCE[0]}")" && builtin pwd)"
REPO_ROOT="$(builtin cd "$SCRIPT_DIR/../.." && builtin pwd)"
# ── HERMETIC RUNTIME BOOTSTRAP (B): bind the git identity to the trusted root ────────
# Part A removed the inherited git context; this derives the real one and REQUIRES it to be the
# root this entry resolved for itself. `git -C <root>` WALKS UP when <root> is not itself a work
# tree, so without this a gate can authenticate a repository that merely CONTAINS the directory it
# is scanning. The derived gitdir/worktree are then passed EXPLICITLY on every call for this root
# (see ax_git), so nothing downstream depends on discovery at all.
# NOT exported, and unset first: a binding that could arrive from the environment would be a
# NEW redirection channel of exactly the kind part A just closed. Every entry derives its own.
unset AX_GIT_BOUND_ROOT AX_GIT_BOUND_DIR
if "$AX_GIT_BIN" -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    _ax_hcan="$(builtin cd "$REPO_ROOT" 2>/dev/null && builtin pwd -P)"
    _ax_htop="$("$AX_GIT_BIN" -C "$REPO_ROOT" rev-parse --show-toplevel 2>/dev/null)"
    _ax_htop="$(builtin cd "${_ax_htop:-/nonexistent}" 2>/dev/null && builtin pwd -P)"
    if [ -z "$_ax_hcan" ] || [ "$_ax_htop" != "$_ax_hcan" ]; then
        { echo "$_AX_HRM_LABEL: GIT_CONTEXT_REDIRECTED — the git work tree answering this gate's"
          echo "  reads is '${_ax_htop:-<unresolvable>}', not the root it was invoked for"
          echo "  ('${_ax_hcan:-$REPO_ROOT}'). Every head / status / fingerprint / anchor answer would"
          echo "  then describe a different tree than the one being verified — measured: with the"
          echo "  context aimed at a clean shadow checkout, a DIRTY tree reported the clean-tree"
          echo "  fingerprint constant and a clean status."; } >&2
        exit $_AX_HRM_EXIT
    fi
    AX_GIT_BOUND_ROOT="$_ax_hcan"
    AX_GIT_BOUND_DIR="$("$AX_GIT_BIN" -C "$AX_GIT_BOUND_ROOT" rev-parse --absolute-git-dir 2>/dev/null)"
    if [ -z "$AX_GIT_BOUND_DIR" ]; then
        echo "$_AX_HRM_LABEL: GIT_CONTEXT_REDIRECTED — the gitdir of '$AX_GIT_BOUND_ROOT' could not" >&2
        echo "  be derived, so no explicit git context can be pinned. Blocking rather than falling" >&2
        echo "  back to discovery, which is the thing being pinned." >&2
        exit $_AX_HRM_EXIT
    fi
    case "$AX_GIT_BIN" in "$AX_GIT_BOUND_ROOT"/*) _ax_htop=repo ;; *) _ax_htop="" ;; esac
    case "$AX_PY_BIN" in "$AX_GIT_BOUND_ROOT"/*) _ax_htop=repo ;; esac
    if [ -n "$_ax_htop" ]; then
        echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNUSABLE — git/python3 resolved to a binary INSIDE the" >&2
        echo "  repository being verified ($AX_GIT_BIN / $AX_PY_BIN). The tree under audit does not" >&2
        echo "  get to supply the programs that audit it." >&2
        exit $_AX_HRM_EXIT
    fi
    unset _ax_hcan _ax_htop
fi


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
# ROUND 5: the whole GIT_* family is scrubbed by the hermetic bootstrap at the top of this file
# (GIT_DIR/GIT_WORK_TREE made every read below describe a clean shadow checkout), and the python
# block receives the VALIDATED ABSOLUTE git binary rather than the word `git`.
export GIT_NO_REPLACE_OBJECTS=1

# ROUND 6 / P1-2: -I -S — ISOLATED, NO site. -I ignores PYTHON* env vars, the user site
# directory and the script directory on sys.path; -S skips site.py entirely, which is what a
# `sitecustomize.py` payload rides in on. MEASURED: PYTHONPATH pointing at a sitecustomize.py
# that calls os._exit(0) turned this guard from exit 1 into exit 0. This body imports stdlib
# only (json/os/re/subprocess/pathlib/datetime), so isolation costs it nothing.
"$AX_PY_BIN" -I -S - "$SCAN_ROOT" "$EXPECT_SHA" "$EXPECT_ANCHOR_SHA" "$REPO_ROOT" "$AX_GIT_BIN" <<'PYEOF'
import sys
import pathlib
import json
import os
import re
import stat
import hashlib
import subprocess
import datetime
import unicodedata
import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

root = pathlib.Path(sys.argv[1])
expect_sha_arg = sys.argv[2] if len(sys.argv) > 2 else ""
expect_anchor_arg = sys.argv[3] if len(sys.argv) > 3 else ""
# The repo THIS GUARD lives in (never the scanned root). Used only to locate the committed
# writer + fingerprint helper for the schema cross-check and the recompute, so that a fixture
# root cannot supply its own definition of what a genuine audit line looks like.
guard_repo = pathlib.Path(sys.argv[4]) if len(sys.argv) > 4 else None
# ROUND 5 / P1-1+P1-2: the absolute, validated git binary published by the hermetic bootstrap.
# The bare word "git" resolves through PATH and, worse, through an inherited exported FUNCTION;
# and every git call below runs with a SCRUBBED environment, because GIT_DIR/GIT_WORK_TREE
# silently answer these questions out of a different repository.
GIT_BIN = sys.argv[5] if len(sys.argv) > 5 and sys.argv[5] else "git"
GIT_ENV = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}
GIT_ENV["GIT_NO_REPLACE_OBJECTS"] = "1"
DIGEST_RE = re.compile(r"\A[0-9a-f]{64}\Z")


def git_out(*args, root=None, check=True):
    """Run git hermetically. Returns (rc, stdout). `check=False` lets the caller decide what a
    failure means — but no caller may treat it as 'nothing found' (ROUND 5 fail-closed sweep)."""
    cmd = [GIT_BIN, "--no-replace-objects"]
    if root is not None:
        cmd += ["-C", str(root)]
    cmd += list(args)
    p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=GIT_ENV)
    return p.returncode, p.stdout.decode(errors="replace").strip()


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


# ROUND 11 / P1 (TD-2026-08-01-(P1-unicode-prefix-fold)) + ROUND 12 / P1 (TD-2026-08-01-(P1-
# ignorable-fold)) — THE CANONICAL CASELESS PATH KEY OVER IGNORABLE-STRIPPED INPUT, and its body is
# BYTE-FOR-BYTE the same as practices/scripts/lib/tree_fingerprint.py `_fold_path_key`.
# The two implementations must reach the SAME verdict on the same input; the full rationale (why
# the Cf strip runs FIRST, why general category Cf rather than TN1150's 16-character hand-list or
# Default_Ignorable_Code_Point, why NFD inside, why NFC outside, why casefold and not lower, why
# the ASCII fast path is not an approximation, and the non-UTF-8 disposition) lives in that
# docstring and is not duplicated here.
# In one line: rounds 9-10 keyed with `bytes.lower()`, which is ASCII-only and normalization-blind,
# so `é`(c3a9) ≡ `e◌́`(65cc81) and `É`(c389) ≡ `é`(c3a9) — both ONE inode on APFS — were never
# compared, and 12c's nine violation buckets came back EMPTY on the reviewer's topology; round 11
# closed those two axes and still PRESERVED ignorable format characters, so `SAFE/` ≡
# `SAFE<U+200C>/` — ONE inode on case-insensitive HFS+, measured on a real volume — went the same
# way, with the same empty buckets.
def _ax_fold_path_key(pfx, cache=None):
    if cache is not None:
        try:
            return cache[pfx]
        except KeyError:
            pass
    if pfx.isascii():
        key = pfx.lower()
    else:
        try:
            s = pfx.decode("utf-8", "surrogateescape")
            s = "".join(ch for ch in s if unicodedata.category(ch) != "Cf")
            s = unicodedata.normalize("NFC", unicodedata.normalize("NFD", s).casefold())
            key = s.encode("utf-8", "surrogateescape")
        except (UnicodeError, ValueError):
            # Not reachable via surrogateescape; a belt. The round-10 key can only ever group a
            # byte-identical spelling with itself, so it reports nothing and blocks nothing.
            key = pfx.lower()
    if cache is not None:
        cache[pfx] = key
    return key


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
#    ROUND 5 / P1-1: "is this root a live git work tree" is answered by requiring the toplevel git
#    reports to BE this root. `git -C <dir>` walks UP, so a fixture directory that merely SITS
#    INSIDE this repository used to answer with the enclosing repository's HEAD — and a redirected
#    GIT_DIR answered with somebody else's. Neither is this root.
rc_top, top_out = git_out("rev-parse", "--show-toplevel", root=root, check=False)
live_git_root = False
if rc_top == 0 and top_out:
    live_git_root = os.path.realpath(top_out) == os.path.realpath(str(root))
inside_git = rc_top == 0

expected_head = None
if expected_head_file.is_file():
    expected_head = expected_head_file.read_text().strip()
elif expect_sha_arg:
    expected_head = expect_sha_arg
elif live_git_root:
    rc_head, head_out = git_out("rev-parse", "HEAD", root=root, check=False)
    if rc_head != 0 or not head_out:
        # ROUND 5 fail-closed sweep: this used to fall through to "not a git repo ⇒ PASS". A git
        # work tree whose HEAD cannot be read is not a tarball — it is a repository whose state we
        # could not establish, and the whole point of this guard is to compare a record to it.
        emit_fail(
            "GIT_CONTEXT_UNUSABLE",
            f'{root} is a git work tree but its HEAD could not be read, so there is nothing to '
            f'check the audit record against. The pre-round-5 shape treated that as "no git, no '
            f'fixture ⇒ PASS", which turns a broken (or deliberately broken) git context into a '
            f'green push gate. Unknown does not pass.'
        )
    expected_head = head_out

if expected_head is None:
    if inside_git and not live_git_root and not expected_head_file.is_file() and not expect_sha_arg:
        # Inside SOME repository but not the top of it, and carrying no fixture marker: every
        # answer we could give would be about a different tree.
        emit_fail(
            "GIT_CONTEXT_REDIRECTED",
            f'{root} is not the top of the git work tree that answers reads for it '
            f'({top_out or "<unresolvable>"}), and it declares no expected_head.txt. A record '
            f'audited against a different repository\'s HEAD is not evidence about this one.'
        )
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
#    ROUND 5: the recorded value must BE a fingerprint — 64 lowercase hex characters — not merely
#    a non-empty string that is neither "nogit" nor "unverifiable-*". "x" satisfied the old test,
#    and on a fixture root (where the recompute below does not run) nothing else ever looked at it.
#    This is a property of the LINE, so it is checked on every root, live or fixture.
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

#    ROUND 5: and the value must BE a fingerprint — 64 lowercase hex characters. The test above
#    asks only that it is not one of two KNOWN degraded spellings, so "x" passed it, and on a
#    fixture root nothing downstream ever looked at the value again. Kept as its own rejection
#    rather than folded into the predicate above so that each half is separately falsifiable —
#    and guarded by tree_fp_usable so it REFINES that verdict instead of duplicating it: a line
#    with no fingerprint at all is check 6's finding, not this one's. (Without the guard this
#    check would also fire on the absent-fingerprint fixture, making [87]'s kill proof for THAT
#    item report a vacuity that is really just two layers overlapping — and it would call
#    DIGEST_RE.match(None).)
if tree_fp_usable and not DIGEST_RE.match(tree_fp):
    emit_fail(
        "AUDIT_TREE_UNIDENTIFIED",
        f'the recorded tree_fingerprint ({str(tree_fp)[:40]!r}) is not a fingerprint at all: it '
        f'must be 64 lowercase hex characters. Until round 5 the test was "non-empty, not nogit, '
        f'not unverifiable-*", so ANY string identified a tree — and the recompute that would '
        f'have caught it could itself be switched off by replacing the helper. Re-run '
        f'`bash practices/scripts/verify-completion.sh` at the commit you are pushing.'
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
    rc_st, st_out = git_out("status", "--porcelain", "-uall", root=root, check=False)
    if rc_st == 0 and st_out:
        dirty = st_out.splitlines()
        shown = "\n    ".join(dirty[:10])
        more = f"\n    … and {len(dirty) - 10} more" if len(dirty) > 10 else ""
        hint = f"\n  Currently uncommitted/untracked here:\n    {shown}{more}"
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
# 11b. THE INTROSPECTION ITSELF MUST BE DETERMINISTIC (BACKLOG P3-114).
#     Until now this cross-check was three nested "if it worked" branches: no guard_repo → skip,
#     writer not a file → skip, regex did not match → skip. A pin whose corroboration is
#     SKIPPABLE is not a pin — the shape that makes it skip (delete the writer from the tree the
#     guard resolves, or respell the printf so the pattern misses) is cheaper to produce than the
#     shape it refuses. And `re.search` takes the FIRST regex-shaped hit, so an ADDED earlier
#     `printf '{"ts"…\n'` anywhere in the writer becomes "the schema" — the writer's own comment
#     already records that this happened once by accident (the round-7 toolpaths sidecar had to
#     be moved BELOW the audit printf and renamed to a leading "kind" key precisely because it
#     was being read as the schema). An accident that changes which line is authoritative is a
#     decoy channel whether or not anyone aimed it.
#     So: guard_repo is always supplied by this file's own shell half (argv[4] = the repo THIS
#     GUARD lives in, never the scanned root), an unreadable writer BLOCKS, and the extraction
#     must be SINGLE-AUTHORITATIVE — exactly one `printf '{"ts"…\n'` in the file. Zero means the
#     writer no longer writes what this guard verifies; two or more means the guard cannot say
#     WHICH of them is the schema, and unknown never passes.
if guard_repo is None:
    emit_fail(
        "AUDIT_WRITER_UNREADABLE",
        'this guard was invoked without the path of the repository it lives in, so it cannot '
        'cross-check its pinned audit-line schema against the committed writer '
        '(practices/scripts/verify-completion.sh). The pin is what makes a hand-authored line '
        'reproduce the writer or fail; a pin nobody corroborated is a comment. This argument is '
        'supplied by this file\'s own shell half, so reaching this message means the python body '
        'was invoked directly by something else.'
    )
writer = guard_repo / "practices" / "scripts" / "verify-completion.sh"
try:
    wtext = writer.read_text(errors="replace")
except OSError as _exc:
    emit_fail(
        "AUDIT_WRITER_UNREADABLE",
        f'the committed audit writer could not be read at {writer} ({_exc.__class__.__name__}). '
        f'This guard pins the exact field set a genuine audit line carries and corroborates that '
        f'pin against the code that actually writes it. With the writer unreadable the pin stands '
        f'alone and unverified, which is the state an attacker who wants the pin unenforced would '
        f'engineer. Restore practices/scripts/verify-completion.sh and re-run.'
    )
# The audit printf is the schema. Extract the key names from the FORMAT STRING of the line that
# appends to $AUDIT_LOG, so the pin above is checked against the code that actually writes, not
# against a comment.
#     THE EXTRACTION POINT, spelled exactly (all three clauses are load-bearing):
#       (i)  a LINE-INITIAL `printf '{"ts"…` — the writer's audit printf is a top-level statement
#            at column 0. Anchoring there is also what distinguishes CODE from PROSE: the writer's
#            own explanatory comment quotes the pattern, and an unanchored `re.findall` counts
#            that comment as a second schema (measured: 2 hits, and the pre-fix `re.search` was
#            simply taking whichever came first in the file);
#       (ii) EXACTLY ONE such line — zero means the writer stopped emitting the record this guard
#            verifies; two or more means the guard cannot say which is authoritative;
#       (iii) that one statement must be the one that APPENDS TO THE AUDIT LOG. The schema is not
#            "a line shaped like the schema", it is "the format string of the write this guard
#            reads back", and only the redirection says which write that is.
_writer_lines = wtext.split("\n")
_cands = [i for i, _l in enumerate(_writer_lines) if re.match(r"printf\s+'\{\"ts\"", _l)]
if len(_cands) != 1:
    emit_fail(
        "AUDIT_WRITER_SCHEMA_UNRESOLVED",
        f'the committed writer (practices/scripts/verify-completion.sh) contains '
        f'{len(_cands)} line-initial printf statements of the audit-line shape, and this guard '
        f'requires EXACTLY ONE. Zero means the writer no longer emits the record this guard '
        f'verifies — the schema pin would then be corroborated by nothing and the check would '
        f'silently stand down. Two or more means the guard cannot say WHICH one is the schema, '
        f'and taking "the first regex-shaped hit" is a decoy channel: an added earlier line of '
        f'that shape silently becomes the authority. Keep exactly one audit printf in the writer '
        f'(the transparency sidecar deliberately leads with a "kind" key for this reason).'
    )
_i = _cands[0]
_stmt = [_writer_lines[_i]]
while _stmt[-1].endswith("\\") and _i + 1 < len(_writer_lines):
    _i += 1
    _stmt.append(_writer_lines[_i])
_stmt = "\n".join(_stmt)
if '>> "$AUDIT_LOG"' not in _stmt:
    emit_fail(
        "AUDIT_WRITER_SCHEMA_UNRESOLVED",
        'the single audit-shaped printf in practices/scripts/verify-completion.sh does not '
        'append to "$AUDIT_LOG". This guard reads .ax-verify/runs.jsonl and pins the field set '
        'of the record it finds there; the pin is only meaningful if it was taken from the '
        'statement that WRITES that file. A format string that is no longer wired to the audit '
        'log is a schema for nothing, and the guard will not adopt it by resemblance.'
    )
_m_schema = re.search(r"printf\s+'(\{\"ts\".*?)\\n'", _stmt, re.S)
if _m_schema is None:
    emit_fail(
        "AUDIT_WRITER_SCHEMA_UNRESOLVED",
        'the audit printf statement in practices/scripts/verify-completion.sh was located but '
        'its format string could not be parsed, so this guard cannot corroborate its pinned '
        'field set against the writer. Unknown never passes.'
    )
emitted = tuple(re.findall(r'"([a-z_]+)":', _m_schema.group(1)))
if tuple(AUDIT_SCHEMA_KEYS) != emitted:
    emit_fail(
        "AUDIT_WRITER_SCHEMA_DRIFT",
        f'this guard pins the audit-line shape as {list(AUDIT_SCHEMA_KEYS)} but the '
        f'committed writer (practices/scripts/verify-completion.sh) emits {list(emitted)}. '
        f'The pin exists so a hand-authored line cannot pass as genuine; a pin that no '
        f'longer matches the writer either rejects every honest run or accepts a shape '
        f'nobody reviewed. Update BOTH in the same commit.'
    )

# 11c. THE ANCHOR'S *KIND* IS ALSO A CLAIM (BACKLOG P3-113).
#     The record carries anchor_kind ∈ {origin/main, HEAD, unavailable} and until now it was
#     printed in failure text and NEVER VERIFIED. The two kinds are not equivalent: "origin/main"
#     is the release the remote actually has, while "HEAD" is this repository's own tip — an
#     anchor that ALREADY CONTAINS whatever was committed locally, so every ratchet measured
#     against it compares the downgrade to itself. The helper's header says so ("weaker"), the
#     spotcheck guard's ledger says so, and nothing enforced it: a HEAD-fallback record was
#     consumed exactly like an origin/main record.
#     Verified, not merely recorded: on a LIVE git root this re-resolves the anchor the same way
#     practices/scripts/lib/release_anchor.sh does (origin/main^{commit} → HEAD^{commit} →
#     unavailable) and requires the recorded kind to be the one this tree actually yields. That
#     refuses BOTH directions — a line claiming the strong kind in a tree that has no origin/main
#     (the hand-authored upgrade), and a genuine weak record being consumed where the strong one
#     was available (the silent downgrade). Fixture roots are not git trees and are skipped, as
#     everywhere else in this guard.
if live_git_root:
    _rc_om, _ = git_out("rev-parse", "--verify", "--quiet", "origin/main^{commit}",
                        root=root, check=False)
    if _rc_om == 0:
        _expected_kind = "origin/main"
    else:
        _rc_hd, _ = git_out("rev-parse", "--verify", "--quiet", "HEAD^{commit}",
                            root=root, check=False)
        _expected_kind = "HEAD" if _rc_hd == 0 else "unavailable"
    _recorded_kind = latest.get("anchor_kind")
    if _recorded_kind != _expected_kind:
        emit_fail(
            "AUDIT_ANCHOR_KIND_MISMATCH",
            f'the audit line records anchor_kind={_recorded_kind!r}, but this repository resolves '
            f'the release anchor as {_expected_kind!r} (same order as '
            f'practices/scripts/lib/release_anchor.sh: origin/main → HEAD → unavailable).\n'
            f'  · {_recorded_kind!r} where {_expected_kind!r} was available is a WEAKER anchor '
            f'consumed as if it were the strong one: "HEAD" anchors the ratchet to this '
            f'repository\'s own tip, which already contains anything committed locally, so a '
            f'downgrade is compared against itself. It is a documented fallback for a '
            f'fork-fresh/detached clone, not a substitute for the release the remote has.\n'
            f'  · The reverse — claiming {_recorded_kind!r} in a tree that cannot resolve it — is '
            f'a hand-authored upgrade of the record.\n'
            f'  Re-run `bash practices/scripts/verify-completion.sh` in this repository '
            f'(`git fetch origin` first if origin/main is simply missing).'
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
#     ROUND 5 / P1-3 — THE VERIFIER MAY NOT SHARE THE RUNNER'S IMPLEMENTATION, AND UNKNOWN MAY
#     NOT PASS. Two defects, both measured:
#       (a) FAIL-OPEN: the comparison was `if recomputed and recomputed != "nogit" and …` — so an
#           EMPTY or "nogit" result DISABLED the check. With a hand-authored line carrying a bogus
#           fingerprint this guard exits 1 (AUDIT_FINGERPRINT_MISMATCH); replace the helper with
#           `print("nogit")` and it exits 0. The recorded value is now required to BE a digest and
#           the recomputed one likewise, with every other outcome blocking.
#       (b) SELF-AUTHENTICATION: the helper was checked for MODE, never for CONTENT, so the runner
#           that wrote the record and this verifier ran the SAME mutable file — a helper edited to
#           print the recorded constant passed. The recompute is therefore performed with the
#           PREVIOUS RELEASE'S copy of the helper, read out of git (`<anchor>:<path>`) into a
#           temporary file OUTSIDE the tree, and the toolchain's working-tree bytes must equal what
#           git records at the sha being pushed.
#     WHAT THAT BINDS, honestly: the anchor is the sha the REMOTE advertises when the hook supplies
#     it (unforgeable locally), else origin/main, else HEAD — and with HEAD as the anchor the
#     implementation is the same one being audited, so the binding degrades to (a) alone. It is
#     stated rather than hidden; the push path, which is the one that matters, always supplies the
#     remote's sha.
#     MIGRATION, because this binds to the PREVIOUS release: a refactor of tree_fingerprint.py is
#     free (the prior copy computes the same digest); a change to what the algorithm OUTPUTS cannot
#     pass this gate and is a deliberate human decision — see practices/DECISIONS.md
#     (TD-2026-07-30-P1-hermetic-runtime).
#     A MEASUREMENT MUST NOT DISTURB WHAT IT MEASURES: the helper is executed as a SUBPROCESS,
#     never imported. `import tree_fingerprint` writes practices/scripts/lib/__pycache__/*.pyc
#     into the very tree being fingerprinted, so the recompute would report a mismatch it caused
#     itself — observed the first time this check ran. Running a file as a script leaves no cache.
# ROUND 6 / P1-3(b): NINE, not six. The two RATCHETING GUARDS and the guard RUNNER were absent
# from this list while being three of the programs that decide whether a release ships. Kept in
# lockstep with ax_ratchet_toolchain_paths() in practices/scripts/lib/release_anchor.sh.
TOOLCHAIN_RELS = (
    "practices/scripts/lib/release_anchor.sh",
    "practices/scripts/lib/tree_fingerprint.py",
    "practices/scripts/verify-completion.sh",
    "practices/evals/completion_checklist_recency_guard.sh",
    "practices/evals/evidence_quote_spotcheck_guard.sh",
    "practices/evals/manifest_snapshot_integrity_guard.sh",
    "practices/evals/run-all-guards.sh",
    ".githooks/pre-push",
    ".githooks/pre-push-lib.sh",
)

if live_git_root and not expected_head_file.is_file() and guard_repo is not None:
    # 12b0. NO CONTENT FILTERS (ROUND 6 / P1-4, invariant β: a byte claim must be made on RAW
    #       BYTES). `filter.<n>.clean` sits between the file on disk and every content answer git
    #       gives — measured, a clean filter that echoes the committed copy made a TAMPERED
    #       tree_fingerprint.py compare byte-identical to its blob (`git diff --quiet` rc 0). The
    #       comparison below is fixed to use raw hashes; this refusal exists because "we passed
    #       --no-filters everywhere" is a claim about every present and future call site.
    #       ROUND 7 / P1-3: the pattern was a KEYWORD SUBSET and `filter.<n>.process` — git's
    #       long-running filter protocol — performs the same conversion under a key that matches
    #       neither word. MEASURED: with only .process configured, a file whose disk bytes differ
    #       from its committed blob makes `git status --porcelain` EMPTY and the tree fingerprint
    #       report the CLEAN-TREE CONSTANT 0a815065ebf5…, so R25 tests benign bytes while the push
    #       ships the malicious blob. ANY key under ANY filter driver is refused now.
    rc_f, filt = git_out("config", "--get-regexp", r"^filter\.",
                         root=root, check=False)
    if rc_f == 0 and filt:
        emit_fail(
            "GIT_FILTERS_PRESENT",
            f'this repository declares git content filters:\n    '
            + "\n    ".join(filt.splitlines()[:5])
            + '\n  A clean filter makes git report the FILTER\'S OUTPUT for a working file, not the '
              'file. A released catalog has no reason to carry one; remove it and re-run.'
        )
    rc_g, gitdir = git_out("rev-parse", "--absolute-git-dir", root=root, check=False)
    if rc_g == 0 and gitdir:
        attrs = pathlib.Path(gitdir) / "info" / "attributes"
        try:
            attr_txt = attrs.read_text() if attrs.is_file() else ""
        except OSError:
            attr_txt = "<unreadable>"
        if attr_txt.strip():
            emit_fail(
                "GIT_FILTERS_PRESENT",
                f'{attrs} is non-empty. That file is neither tracked nor reviewed, and it is where '
                f'a filter is attached to a path without touching a single committed byte.'
            )
    for rel in TOOLCHAIN_RELS:
        rc_a, attr_out = git_out("check-attr", "filter", "--", rel, root=root, check=False)
        if rc_a == 0 and attr_out and not attr_out.endswith(": filter: unspecified"):
            emit_fail(
                "GIT_FILTERS_PRESENT",
                f'a filter attribute is attached to a ratchet-critical path: {attr_out}. Every byte '
                f'claim about that path would be a claim about the filter\'s output.'
            )

    # 12b. THE RATCHET'S OWN CODE MUST BE THE COMMITTED CODE (at the sha being pushed) — ON RAW
    #      BYTES. This used to be `git diff --quiet`, which honours clean filters and therefore
    #      answered "identical" for a file whose on-disk bytes were not. `hash-object --no-filters`
    #      hashes the file as it is; the blob id at <rev> is what git recorded. Two raw ids.
    for rel in TOOLCHAIN_RELS:
        rc_w, want = git_out("rev-parse", "--verify", "--quiet", f"{expected_head}:{rel}",
                             root=root, check=False)
        on_disk = (root / rel).is_file()
        rc_h, have = (0, "")
        if on_disk:
            rc_h, have = git_out("hash-object", "--no-filters", "-t", "blob", "--", rel,
                                 root=root, check=False)
        # ABSENT ON BOTH SIDES IS NOT A VIOLATION (a tree that never carried this part of the
        # toolchain); absent on exactly ONE side is a deletion or an unrecorded addition of a file
        # that decides whether a release ships, and blocks.
        if rc_w != 0 and not on_disk:
            continue
        if rc_w != 0 or not want or rc_h != 0 or not have:
            emit_fail(
                "RATCHET_TOOLCHAIN_UNVERIFIABLE",
                f'{rel} could not be compared on raw bytes against {expected_head[:12]} '
                f'(blob at rev: {want or "<absent>"}; on disk: {have or "<unreadable>"}). These '
                f'nine files ARE the gate; unknown never passes.'
            )
        if want != have:
            emit_fail(
                "RATCHET_TOOLCHAIN_MODIFIED",
                f'{rel} in this working tree is not what git records at {expected_head[:12]} '
                f'(raw {have[:12]} vs {want[:12]}). These nine files ARE the gate: the anchor '
                f'policy, the fingerprint, the runner, both ratcheting guards, the guard runner, '
                f'this guard and the hook. Every other check on them asks whether they are regular '
                f'files; a regular-file tree_fingerprint.py rewritten to print a constant passed '
                f'all of those and then served as BOTH the writer and the verifier of the evidence. '
                f'Commit or restore the file and re-run.'
            )

    # 12c. THE WHOLE TRACKED TREE, ON RAW BYTES (ROUND 7 / P1-3b). 12b0 refuses a DECLARATION and
    #      12b compares the nine toolchain paths; neither is a measurement of the rest of the tree,
    #      and the attack does not need the toolchain: commit a malicious file, declare a filter
    #      driver on it, put benign bytes on disk. git's status then reports nothing, so every
    #      check that starts from `git status` — including the fingerprint's raw-byte loop, which
    #      only opens the paths status named — is answered about a file that is not there.
    #      So: for every tracked path git calls CLEAN, hash the bytes on disk AS A GIT BLOB, here,
    #      and compare with the object id in the index. No git process participates, so no filter
    #      protocol (present or future) can answer it.
    #      COVERAGE, stated exactly: all of `git ls-files -s -v` MINUS paths git already reports
    #      dirty (their raw bytes are hashed into the fingerprint anyway). Measured cost on this
    #      catalog: 5,745 index entries (5,740 regular / 2 symlinks / 3 gitlinks) / 31.6 MB of
    #      regular-file bytes / ~0.2 s. (This read 5.4 MB until ROUND 9; re-measured then — the sum
    #      of st_size over tracked regular files is 31,648,540 bytes.)
    #      A mismatch also fires for eol conversion (core.autocrlf/core.eol/`text`), which is not
    #      an attack but is equally fatal to a byte claim; the message names both causes.
    #      ROUND 8 / P1-A — THE TWO EXEMPTIONS ABOVE WERE THE HOLE, IN BOTH IMPLEMENTATIONS.
    #      Round 7 wrote "(i) gitlinks, (iii) paths absent from disk (sparse/skip-worktree;
    #      absence carries no bytes)" and silently `continue`d past an index-REGULAR path that is
    #      a SYMLINK on disk as well (the raw loop cannot read a link as a file). MEASURED
    #      (reviewer round 8, reproduced in a throwaway repo): HEAD carries a malicious
    #      practices/verification-checklist.yaml; `git update-index --assume-unchanged` it, `rm`
    #      it, put a symlink to a benign file OUTSIDE the repo in its place → `git status
    #      --porcelain` EMPTY, index mode still 100644, and both sweeps skip. Same with a
    #      malicious tracked file marked `--skip-worktree` and deleted. All three states (clean
    #      tree / symlink swap / tracked-but-absent) produced the IDENTICAL fingerprint
    #      0a815065…, i.e. R25 verifies bytes that are not the ones the push ships.
    #      So: an unexpected worktree REPRESENTATION blocks (GIT_WORKTREE_TYPE_MISMATCH, both
    #      directions), an absent/unreadable tracked path blocks (GIT_TRACKED_PATH_ABSENT), the
    #      index BITS that produce them are read directly and refused (GIT_INDEX_FLAGS_SET — `git
    #      ls-files -v` spells assume-unchanged as a lowercase tag and skip-worktree as `S`;
    #      measured, ZERO non-`H` tags on this catalog, so the refusal costs an honest tree
    #      nothing), and gitlinks are BOUND rather than exempt: an INITIALIZED submodule must be
    #      at the commit the superproject records (GIT_GITLINK_DIVERGENCE). An UNINITIALIZED one
    #      is deliberately not blocked — all three gitlinks here are empty post-clone fixture
    #      directories and nothing was tested from them; the residue is docs/BACKLOG.md P3-119.
    #      ROUND 9 / P1-1 + P1-2 + (e) — SEPARATING THE SHAPES AND THEN COMPARING ONLY BYTES LEFT
    #      THREE REPRESENTATION FACTS ACCEPTED, and no digest here carries any of them:
    #        · GIT_EXEC_BIT_DIVERGENCE — the index mode's executable bit vs the filesystem's, BOTH
    #          directions, read from `ls-files -s` so `core.fileMode=false` cannot suppress it.
    #        · GIT_GITLINK_UNINITIALIZED_POPULATED — round 8's uninitialized exemption returned
    #          success on the absence of `<gitlink>/.git` alone; it now additionally requires the
    #          directory to be ABSENT or EMPTY (the fresh-clone shape, still accepted).
    #        · GIT_CASEFOLD_ALIAS — two index entries differing only in case that lstat to the same
    #          (st_dev, st_ino), i.e. one file serving two blobs on a case-insensitive filesystem.
    #      Measured on this catalog: 5,745 tracked paths, ZERO of all three, so none costs an
    #      honest tree anything.
    #      ROUND 10 / P1 — THE CASEFOLD CHECK WAS LEAF-ONLY. It grouped COMPLETE folded paths, so
    #      `A/check.sh` and `a/helper` (different leaves, ONE directory inode on APFS) never met.
    #      The map is now keyed on EVERY PATH PREFIX, gitlinks included, and a directory-component
    #      alias blocks with GIT_CASEFOLD_DIR_ALIAS.
    #      ROUND 11 / P1 (TD-2026-08-01-(P1-unicode-prefix-fold)) — THE PREFIX WAS RIGHT, THE FOLD
    #      WAS ASCII. The key was `bytes.lower()`: it lowercases A-Z and nothing else and is blind
    #      to Unicode normalization, so `é`(c3a9) vs `e◌́`(65cc81) and `É`(c389) vs `é`(c3a9) —
    #      each ONE inode on APFS, each reachable with committed content and no environment
    #      control — were never compared. MEASURED at beee364 by driving THIS sweep over synthetic
    #      index/stat responses for one inode: the ASCII pair fires GIT_CASEFOLD_DIR_ALIAS, the
    #      NFC/NFD pair leaves all nine violation buckets EMPTY, and so does the non-ASCII case
    #      pair. The key is now UNICODE CANONICAL CASELESS (`_ax_fold_path_key` above, the same
    #      function as the fingerprint helper's), the (dev, ino) discriminator is unchanged, and
    #      the leaf/directory code split is unchanged.
    #      ROUND 13 / P1 (TD-2026-08-01-(P1-symlink-target-alias)) — FOUR ROUNDS OF ALIAS CENSUS,
    #      AND NONE OF THEM LOOKED AT WHAT A SYMLINK POINTS AT. Rounds 9-12 registered INDEX PATHS
    #      only; a symlink's TARGET is BLOB CONTENT, read as bytes and hashed below and never
    #      RESOLVED, so the census did not apply to it on any of the four axes. MEASURED (reviewer
    #      round 13; committed content, no environment control): `git mv backend/gradlew
    #      backend/gradlew-real` + `ln -s GRADLEW-REAL backend/gradlew` → on case-insensitive APFS
    #      the link resolves, R25 EXECUTES and goes green, `git status --porcelain -uall` is EMPTY
    #      and the fingerprint is the clean-tree constant, while a case-SENSITIVE receiver gets a
    #      DANGLING gradlew. The target is now resolved against the link's own recorded directory
    #      (ROUND 14: component by component, the kernel's way — not lexically) and must spell the
    #      record whenever it lands on a tracked inode by a FOLD-EQUAL
    #      alias (`_ax_fold_path_key`, so case + normalization + non-ASCII case + ignorable Cf in
    #      one step, and every future axis the fold gains)  → GIT_SYMLINK_TARGET_ALIAS.
    #      ROUND 14b: that verdict is taken on EVERY component the walk resolves, not on the final
    #      candidate alone — round 14's follow discarded the intermediate's spelling, so
    #      `-> DIRLINK` blocked while `-> DIRLINK/` and `-> DIRLINK/real.txt` passed over the very
    #      same tracked `dirlink`.
    #      NOT REFUSED, by construction and on purpose: an ABSOLUTE target, one that ESCAPES the
    #      repository through `..`, one that names an UNTRACKED path, a DANGLING one (a different
    #      defect class — identically broken here and at the receiver, so the evidence does not lie
    #      about it; docs/BACKLOG.md P3-132), one reached through an intermediate symlinked
    #      DIRECTORY SPELLED AS THE INDEX RECORDS IT (round 14 follows it, so the candidate is the
    #      real file's own path and step 5 passes it on an EXACT match; the intermediate itself is
    #      judged by the same steps and passes them), one reached through an UNTRACKED intermediate
    #      (no recorded spelling to alias), and one
    #      that spells the record EXACTLY — which is what both live tracked symlinks in this
    #      catalog do, through `..` traversal. Measured here: 2 tracked symlinks, ZERO refusals.
    rc_of, objfmt = git_out("rev-parse", "--show-object-format", root=root, check=False)
    _algo = "sha256" if (rc_of == 0 and objfmt.strip() == "sha256") else "sha1"
    _p_idx = subprocess.run([GIT_BIN, "--no-replace-objects", "-C", str(root),
                             "ls-files", "-s", "-v", "-z"],
                            stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=GIT_ENV)
    _p_st = subprocess.run([GIT_BIN, "--no-replace-objects", "-C", str(root),
                            "status", "--porcelain", "-z", "-uall"],
                           stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=GIT_ENV)
    if _p_idx.returncode != 0 or _p_st.returncode != 0:
        emit_fail(
            "RAW_TREE_SWEEP_UNVERIFIABLE",
            "the index or the status of this tree could not be read, so the raw-byte sweep that "
            "backstops the filter refusal could not run. Unknown never passes."
        )
    else:
        _dirty = set()
        _ents = _p_st.stdout.split(b"\0")
        _i = 0
        while _i < len(_ents):
            _e = _ents[_i]; _i += 1
            if len(_e) < 4:
                continue
            if _e[:1] in (b"R", b"C"):
                _i += 1
            _dirty.add(_e[3:])
        _bad, _mistyped, _absent, _flagged, _glbad = [], [], [], [], []
        # ROUND 9 / P1-1 + P1-2 + (e) (TD-2026-07-30-(P1-representation-parity)). Round 8 separated
        # regular from symlink and then compared only BLOB BYTES — so three representation facts
        # that no digest here carries were still accepted:
        #   · the EXECUTABLE BIT. `git config core.fileMode false` + `git update-index --chmod=-x
        #     backend/gradlew` + `chmod +x backend/gradlew` on disk → `git status --porcelain`
        #     EMPTY, blob bytes identical, fingerprint = the clean-tree constant, and R25's 118
        #     direct `./gradlew` invocations run against a file the push records as 100644, which
        #     a fresh checkout cannot execute. The index mode is read from `ls-files -s`, so the
        #     comparison is INDEPENDENT of core.fileMode (that setting suppresses the report, not
        #     the record). Both directions block.
        #   · a POPULATED but UNINITIALIZED gitlink. Round 8 returned success on the ABSENCE of
        #     `<gitlink>/.git` without requiring the directory to be EMPTY, so `vendor/sub` can
        #     hold a check.sh that a mandatory step executes while the push ships only the recorded
        #     sha and a fresh clone gets an empty directory. Distinct from P3-119 (dirt inside an
        #     INITIALIZED submodule).
        #   · two index entries differing only in CASE, which APFS/NTFS serve from ONE file. Flagged
        #     only when they lstat to the same (st_dev, st_ino) — a measurement, so a case-sensitive
        #     fork-receiver is unaffected.
        # ROUND 10 / P1 (TD-2026-07-31-(P1-casefold-prefix)): the casefold map is keyed on every
        # PATH PREFIX, not on the complete path. Round 9 grouped complete folded paths, so two
        # entries whose LEAF names differ never met even when a shared DIRECTORY component was an
        # alias — measured at d567c37: index `A/check.sh` (running `cat A/helper`) + index
        # `a/helper`, one directory inode on APFS, `git status` EMPTY, both implementations silent,
        # and the pushed tree serves no `A/helper` on a case-sensitive receiver.
        _execbits, _gldirt = [], []
        _symlinks = []          # ROUND 13: (index path, on-disk target bytes), resolved after the
                                # walk — the registry is only complete once every entry has
                                # contributed its prefixes
        _casefold = {}          # folded prefix -> {(dev, ino): {spellings}}
        _statcache = {}         # relative path -> lstat result | None, one call per distinct prefix
        _foldcache = {}         # relative path -> canonical caseless key, one fold per prefix
        _fullpaths = set()      # the tracked paths themselves: leaf alias vs directory alias
        _rootb = os.fsencode(str(root))
        for _rec in _p_idx.stdout.split(b"\0"):
            if not _rec:
                continue
            try:
                _meta, _path = _rec.split(b"\t", 1)
                _tag, _mode, _blob, _stage = _meta.split(b" ")
            except ValueError:
                continue
            _show = _path.decode(errors="replace")
            # ROUND 10 / P1: the prefix walk runs FIRST, for EVERY entry — before the gitlink
            # `continue` (round 9 registered the map after it, so a 160000 path could alias a
            # directory unseen) and before the dirty skip. One lstat per DISTINCT prefix; the full
            # path is itself the last prefix, so the exec-bit check below reads the cached stat
            # instead of taking a second one.
            _fullpaths.add(_path)
            _comps = _path.split(b"/")
            for _n in range(1, len(_comps) + 1):
                _pfx = b"/".join(_comps[:_n])
                if _pfx in _statcache:
                    _pst = _statcache[_pfx]
                else:
                    try:
                        _pst = os.lstat(os.path.join(_rootb, _pfx))
                    except OSError:
                        _pst = None
                    _statcache[_pfx] = _pst
                if _pst is None:
                    continue
                # ROUND 11 / P1: canonical caseless, NOT `bytes.lower()`. Same function as the
                # fingerprint helper's `_fold_path_key`; the (dev, ino) discriminator below is
                # UNCHANGED, so a case- or normalization-SENSITIVE tree still yields two inodes,
                # two singleton groups and no refusal.
                _casefold.setdefault(_ax_fold_path_key(_pfx, _foldcache), {}).setdefault(
                    (_pst.st_dev, _pst.st_ino), set()).add(_pfx)
            # ROUND 8 / P1-A (0): the BITS. Both reproductions begin with `git update-index`, and
            # `ls-files -v` reports them directly — a lowercase tag is assume-unchanged, `S` is
            # skip-worktree. Reading the cause is cheaper and more honest than inferring its
            # symptoms one at a time. Verified on a deliberately dirty tree (modified/deleted/
            # staged/untracked) that every tag stays `H`, so this is the two bits, not dirtiness.
            if _tag == b"S":
                _flagged.append("%s (skip-worktree)" % _show)
            elif _tag.isalpha() and _tag.islower():
                _flagged.append("%s (assume-unchanged)" % _show)
            _full = os.path.join(os.fsencode(str(root)), _path)
            if _mode == b"160000":
                # ROUND 8 / P1-A: gitlinks are BOUND, not exempt. An INITIALIZED submodule whose
                # HEAD is not the recorded commit means this run tested one submodule while the
                # push ships another. An uninitialized one carries nothing and is left alone.
                if os.path.islink(_full):
                    _mistyped.append("%s (index: gitlink/submodule, on disk: SYMLINK)" % _show)
                elif os.path.exists(os.path.join(_full, b".git")):
                    _pg = subprocess.run([GIT_BIN, "--no-replace-objects", "-C",
                                          os.fsdecode(_full), "rev-parse", "HEAD"],
                                         stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                                         env=GIT_ENV)
                    _have = _pg.stdout.strip() if _pg.returncode == 0 else b""
                    if not _have:
                        _glbad.append("%s (initialized, but its HEAD could not be read)" % _show)
                    elif _have != _blob:
                        _glbad.append("%s (superproject records %s, work tree is at %s)"
                                      % (_show, _blob[:12].decode(), _have[:12].decode()))
                    else:
                        # BACKLOG P3-119 (escalated to P1 — a CONFIG-CONTROLLED FAIL-OPEN), the
                        # symmetric half of the same addition in practices/scripts/lib/
                        # tree_fingerprint.py. Being AT the recorded commit says nothing about
                        # whether the submodule's own work tree was edited, and the superproject
                        # cannot be asked: MEASURED, `diff.ignoreSubmodules=all` OR
                        # `submodule.<name>.ignore=all` — the latter settable in .gitmodules,
                        # i.e. COMMITTED CONTENT — empties the superproject status and returns
                        # the digest to EXACTLY the clean-tree constant while
                        # `git -C <sub> status --porcelain` still reports ` M a.txt`. Same class
                        # as GIT_CONTEXT_REDIRECTED: the tree decides what the verifier may see.
                        # The submodule's own status does not consult that config, so it is asked
                        # instead; a failed read is unknown, and unknown never passes.
                        _pst = subprocess.run([GIT_BIN, "--no-replace-objects", "-C",
                                               os.fsdecode(_full), "status", "--porcelain"],
                                              stdout=subprocess.PIPE,
                                              stderr=subprocess.DEVNULL, env=GIT_ENV)
                        if _pst.returncode != 0:
                            _glbad.append("%s (initialized and at the recorded commit, but its "
                                          "own `git status` could not be read)" % _show)
                        elif _pst.stdout.strip():
                            _lines = _pst.stdout.decode(errors="replace").strip().splitlines()
                            _glbad.append(
                                "%s (at the recorded commit %s, but ITS OWN work tree is dirty: "
                                "%s%s — the push ships only the recorded commit, so those edits "
                                "are bytes this run may have read and the receiver does not get)"
                                % (_show, _blob[:12].decode(), "; ".join(_lines[:4]),
                                   "" if len(_lines) <= 4 else " (+%d more)" % (len(_lines) - 4)))
                elif os.path.exists(_full):
                    # ROUND 9 / P1-2: no gitdir is a pass ONLY when there is nothing there.
                    if not os.path.isdir(_full):
                        _gldirt.append("%s (a gitlink whose worktree path is not a directory)"
                                       % _show)
                    else:
                        try:
                            _kids = os.listdir(_full)
                        except OSError as _e:
                            _kids = None
                            _gldirt.append("%s (uninitialized gitlink whose directory could not "
                                           "be listed: %s)" % (_show, _e.strerror or _e))
                        if _kids:
                            # BACKLOG P3-123 — CLOSED BY DECISION: KEEP THE REFUSAL, IMPROVE THE
                            # MESSAGE (symmetric with practices/scripts/lib/tree_fingerprint.py).
                            # A NAME ALLOWLIST for `.DS_Store` was considered and REJECTED: it is
                            # a real file with arbitrary, attacker-controllable bytes, and the
                            # invariant is about ANY bytes the receiver will not get, so a
                            # name-based exception cannot discharge "cannot hide real content".
                            # (Gitignoring it does not help: the test is os.listdir.)
                            _hint = ""
                            if any(os.fsdecode(_k) == ".DS_Store" for _k in _kids):
                                _hint = (" — this looks like macOS Finder noise: `.DS_Store` is "
                                         "present. It is still REAL CONTENT with arbitrary bytes, "
                                         "so it is not exempted by name; remove it with `find %s "
                                         "-name .DS_Store -delete` and re-run" % _show)
                            _gldirt.append(
                                "%s (uninitialized gitlink — no gitdir — yet its directory holds "
                                "%d entr%s: %s)%s"
                                % (_show, len(_kids), "y" if len(_kids) == 1 else "ies",
                                   ", ".join(sorted(os.fsdecode(_k) for _k in _kids)[:4]), _hint))
                continue
            # ROUND 9 / P1-1 + (e): read BEFORE the `dirty` continue — neither fact is carried by
            # the digest, and `git status` reports neither once core.fileMode is false.
            _st = _statcache.get(_path)      # already taken by the prefix walk above
            if _st is not None and stat.S_ISREG(_st.st_mode) and _mode in (b"100644", b"100755"):
                if bool(_st.st_mode & 0o111) != (_mode == b"100755"):
                    _execbits.append(
                        "%s (index: %s, on disk: %s)"
                        % (_show, "100755 (executable)" if _mode == b"100755" else "100644 (plain)",
                           "executable" if _st.st_mode & 0o111 else "NOT executable"))
            if _path in _dirty:
                continue
            _is_link = os.path.islink(_full)
            if _mode == b"120000":
                if not _is_link:
                    # THE MIRROR CASE, checked because the pair is the invariant: nothing here
                    # reads an index-symlink path that is a regular file, so round 7 skipped it.
                    if os.path.exists(_full):
                        _mistyped.append("%s (index: symlink, on disk: regular file or directory)"
                                         % _show)
                    else:
                        _absent.append("%s (index: symlink, absent from disk)" % _show)
                    continue
                try:
                    _d = os.readlink(_full)
                    _d = _d if isinstance(_d, bytes) else os.fsencode(_d)
                except OSError as _e:
                    _absent.append("%s (%s)" % (_show, _e.strerror or _e))
                    continue
                # ROUND 13 / P1: the target is BLOB CONTENT — hashed below, and until now never
                # RESOLVED. The bytes are already in hand and the blob comparison binds them to
                # the committed ones, so collect here and compare after the walk.
                _symlinks.append((_path, _d))
            else:
                if _is_link:
                    try:
                        _t = os.readlink(_full)
                        _t = _t.decode(errors="replace") if isinstance(_t, bytes) else _t
                    except OSError:
                        _t = "<unreadable>"
                    _mistyped.append("%s (index: regular file mode %s, on disk: SYMLINK -> %s)"
                                     % (_show, _mode.decode(errors="replace"), _t))
                    continue
                try:
                    with open(_full, "rb") as _fh:
                        _d = _fh.read()
                except OSError as _e:
                    _absent.append("%s (%s)" % (_show, _e.strerror or _e))
                    continue
            _h = hashlib.new(_algo)
            _h.update(b"blob %d\0" % len(_d))
            _h.update(_d)
            if _h.hexdigest().encode() != _blob:
                _bad.append(_show)
        # ROUND 9 / (e): NO early break above. The casefold check is a property of a PAIR, so a
        # walk that stops at the eighth byte mismatch could miss the second half of an alias.
        # ROUND 10 / P1: a group whose spellings are ALL full tracked paths is the leaf case
        # (GIT_CASEFOLD_ALIAS); a group in which any spelling is a directory COMPONENT is the
        # round-10 case (GIT_CASEFOLD_DIR_ALIAS) — different remedy, so a different code. The
        # spellings are a SET, so a path listed once per stage during a merge conflict cannot
        # produce the "path ≡ path" self-report round 9 would have emitted.
        _aliased, _diraliased = [], []
        for _byident in _casefold.values():
            for _names in _byident.values():
                if len(_names) < 2:
                    continue
                _shown = " ≡ ".join(sorted(_n.decode(errors="replace") for _n in _names))
                if all(_n in _fullpaths for _n in _names):
                    _aliased.append(_shown)
                else:
                    _diraliased.append(_shown)
        _aliased.sort()
        _diraliased.sort()
        # ROUND 13 / P1 (TD-2026-08-01-(P1-symlink-target-alias)): the alias census, applied to
        # what a tracked SYMLINK POINTS AT. `_inodes` is derived from the SAME `_statcache` the
        # prefix walk filled, so "registered" means exactly "a tracked path or a directory
        # component of one" — one pass over the distinct prefixes, no new lstat. The seven steps
        # and the full disposition table are in the 12c comment above and in the fingerprint
        # helper's SymlinkTargetAlias docstring; this is the same rule, written twice on purpose.
        _inodes = {}
        for _rel, _pst in _statcache.items():
            if _pst is not None:
                _inodes.setdefault((_pst.st_dev, _pst.st_ino), set()).add(_rel)
        # ROUND 14 / P1-A (TD-2026-08-01-(P1-posix-resolution-and-runtime-paths)): the resolution
        # above was LEXICAL — it collapsed `..` textually, BEFORE following anything, while the
        # kernel pops `..` AFTER following an intermediate symlink. Measured with the reviewer's
        # committed topology (backend/jump -> real/sub, backend/gradlew -> jump/../GRADLEW-REAL
        # over a tracked backend/real/gradlew-real): POSIX reaches backend/real/GRADLEW-REAL,
        # which case-insensitive APFS serves as the TRACKED file, so R25 executes the wrapper and
        # goes green — while the lexical candidate backend/GRADLEW-REAL is ABSENT, so this sweep
        # and the fingerprint helper BOTH took the dangling exit and reported nothing, and a
        # case-SENSITIVE receiver gets a dangling backend/gradlew. The walk below is the kernel's:
        # a component is followed iff something remains after it (lstat never follows the FINAL
        # one), following replaces the component so a later `..` pops the RESOLVED stack, an
        # absolute intermediate leaves for the receiver's root filesystem, and a missing or
        # unreadable intermediate is left alone so the final lstat still decides "dangling".
        # The two budgets BLOCK on exhaustion (GIT_SYMLINK_RESOLUTION_UNBOUNDED) instead of going
        # silent: an unfinished walk has not answered the alias question, and 40 follows is the
        # LARGER of the two kernel limits (Linux MAXSYMLINKS 40 / macOS SYMLOOP_MAX 32), so a
        # chain any receiver's kernel would resolve is never refused. Written twice on purpose —
        # the fingerprint helper's `_resolve_link_target` is the same rule.
        # ROUND 14b / P1 (same TD entry): round 14 followed intermediates CORRECTLY and then took
        # the alias verdict on the FINAL candidate only, so the follow DISCARDED the intermediate's
        # spelling and one keystroke moved an alias out of reach. Measured on one tree with a
        # tracked `legit/dirlink -> sub`: `ln -s DIRLINK legit/x` → exit 15, `ln -s DIRLINK/` →
        # exit 0, and `-> DIRLINK/real.txt` → exit 0. The trailing slash is honoured LEGITIMATELY
        # (the kernel follows the final component when one follows it), which is precisely what
        # turned a refused final-component alias into an unrefused intermediate one. `_walked`
        # below carries every non-final component the walk resolved and each gets the SAME verdict
        # under the SAME code — the subject (a committed spelling that dangles at the receiver)
        # and the remedy (respell it) are identical, so a second code would only fragment it.
        _FOLLOW_BUDGET = 40
        _STEP_BUDGET = 4096
        _symaliased = []
        _symunbounded = []
        for _lpath, _tgt in _symlinks:
            if _tgt.startswith(b"/"):
                continue                        # absolute: a location on the receiver's root fs
            _stack = _lpath.split(b"/")[:-1]
            _queue = list(_tgt.split(b"/"))
            _walked = []                        # ROUND 14b: every NON-FINAL component resolved
            _escaped = False
            _unbounded = ""
            _follows = 0
            _steps = 0
            while _queue:
                _comp = _queue.pop(0)
                _steps += 1
                if _steps > _STEP_BUDGET:
                    _unbounded = ("resolution consumed more than %d path components"
                                  % _STEP_BUDGET)
                    break
                if _comp in (b"", b"."):
                    continue
                if _comp == b"..":
                    if not _stack:
                        _escaped = True
                        break
                    _stack.pop()
                    continue
                _stack.append(_comp)
                if not _queue:
                    break                       # FINAL component: lstat does not follow it
                _cur = b"/".join(_stack)
                try:
                    _ist = os.lstat(os.path.join(_rootb, _cur))
                except OSError:
                    continue                    # missing intermediate: the final lstat decides
                # ROUND 14b: record BEFORE the follow — the follow pops this component off the
                # stack, which is exactly how round 14 lost the spelling. No extra syscall.
                _walked.append((_cur, _ist))
                if not stat.S_ISLNK(_ist.st_mode):
                    continue
                _follows += 1
                if _follows > _FOLLOW_BUDGET:
                    _unbounded = ("resolution followed more than %d symlinks (no kernel resolves "
                                  "this: Linux MAXSYMLINKS is 40, macOS SYMLOOP_MAX is 32)"
                                  % _FOLLOW_BUDGET)
                    break
                try:
                    _nxt = os.readlink(os.path.join(_rootb, _cur))
                except OSError:
                    continue                    # unreadable intermediate: as above
                if not isinstance(_nxt, bytes):
                    _nxt = os.fsencode(_nxt)
                if _nxt.startswith(b"/"):
                    _escaped = True             # leaves for the receiver's root filesystem
                    break
                _stack.pop()                    # step back into the LINK'S OWN directory
                _queue = _nxt.split(b"/") + _queue
            # ROUND 14b / P1: the SAME verdict on EVERY component the walk resolved. It runs for
            # every outcome — escaped, root, unbounded, inside — because an intermediate the
            # receiver's kernel cannot resolve is broken there regardless of where this walk ended.
            if _walked:
                _wseen = set()
                for _wsp, _wst in _walked:
                    if _wsp in _wseen:
                        continue                # a cycle re-visits a spelling; report it once
                    _wseen.add(_wsp)
                    _wnames = _inodes.get((_wst.st_dev, _wst.st_ino))
                    if not _wnames or _wsp in _wnames:
                        continue                # untracked intermediate, or the recorded spelling
                    _wkey = _ax_fold_path_key(_wsp, _foldcache)
                    _walias = sorted(_n for _n in _wnames
                                     if _ax_fold_path_key(_n, _foldcache) == _wkey)
                    if not _walias:
                        continue                # a tracked inode reached by a non-alias route
                    _symaliased.append(
                        "%s -> %s (resolves HERE THROUGH the intermediate component %s, which "
                        "this repository records as %s)"
                        % (_lpath.decode(errors="replace"), _tgt.decode(errors="replace"),
                           _wsp.decode(errors="replace"),
                           " / ".join(_n.decode(errors="replace") for _n in _walias)))
            if _unbounded:
                _symunbounded.append("%s -> %s (%s)"
                                     % (_lpath.decode(errors="replace"),
                                        _tgt.decode(errors="replace"), _unbounded))
                continue
            if _escaped or not _stack:
                continue                        # leaves the repository, or IS the repository root
            _cand = b"/".join(_stack)
            try:
                _cst = os.lstat(os.path.join(_rootb, _cand))
            except OSError:
                continue                        # dangling: a different defect class (P3-132)
            _names = _inodes.get((_cst.st_dev, _cst.st_ino))
            if not _names or _cand in _names:
                continue                        # untracked target, or the recorded spelling
            _ckey = _ax_fold_path_key(_cand, _foldcache)
            _alias = sorted(_n for _n in _names
                            if _ax_fold_path_key(_n, _foldcache) == _ckey)
            if not _alias:
                continue                        # a tracked inode reached by a non-alias route
            _symaliased.append(
                "%s -> %s (resolves HERE to %s, which this repository records as %s)"
                % (_lpath.decode(errors="replace"), _tgt.decode(errors="replace"),
                   _cand.decode(errors="replace"),
                   " / ".join(_n.decode(errors="replace") for _n in _alias)))
        _symaliased.sort()
        _symunbounded.sort()
        if _execbits:
            emit_fail(
                "GIT_EXEC_BIT_DIVERGENCE",
                "tracked paths whose EXECUTABLE BIT on disk is not the one the index records: "
                + ", ".join(_execbits[:8]) + ". git records exactly two regular modes (100644 / "
                "100755). `core.fileMode=false` tells git to stop REPORTING a difference; it does "
                "not change the RECORD, so this comparison reads the mode from `git ls-files -s` "
                "and is independent of that setting. Measured: a 100644 file made executable on "
                "disk left `git status` empty and the tree fingerprint at the clean-tree constant "
                "while R25's direct `./gradlew` invocations ran the locally-executable file that a "
                "fresh checkout of the pushed commit cannot execute. Fix the record "
                "(`git update-index --chmod=+x|-x <path>`) or the file, and re-run R25."
            )
        if _gldirt:
            emit_fail(
                "GIT_GITLINK_UNINITIALIZED_POPULATED",
                "gitlinks with no gitdir whose worktree path is nevertheless POPULATED: "
                + ", ".join(_gldirt[:8]) + ". A gitlink ships only the commit it records, so a "
                "fresh clone of the pushed commit gets an EMPTY directory there — anything R25 "
                "read or executed from a populated one is code the receiver never gets. An "
                "uninitialized gitlink is acceptable only when its path is absent or an "
                "actually-empty directory (that is the ordinary post-clone shape, and it stays "
                "accepted). Initialize the submodule or remove the untracked content, then re-run."
            )
        if _aliased:
            emit_fail(
                "GIT_CASEFOLD_ALIAS",
                "index entries that differ only in CASE and are ONE file on this filesystem: "
                + ", ".join(_aliased[:8]) + ". They lstat to the same device/inode, so every read "
                "— this sweep, the fingerprint, the build, the lint — answers about a single file "
                "for both entries while the push ships two distinct blobs; one of them was never "
                "on disk to be verified. Rename one entry so the index and the filesystem agree."
            )
        if _diraliased:
            emit_fail(
                "GIT_CASEFOLD_DIR_ALIAS",
                "index entries whose DIRECTORY components are spelled two ways and are ONE "
                "directory on this filesystem: " + ", ".join(_diraliased[:8]) + ". The spellings "
                "lstat to the same device/inode, so a path built with either one resolves here — "
                "measured at d567c37: a committed `A/check.sh` reading `A/helper` while the index "
                "records `a/helper` left `git status` EMPTY, both implementations silent and the "
                "tree fingerprint at the clean-tree constant, while the PUSHED tree serves no "
                "`A/helper` at all on a case-sensitive receiver. This is a MEASUREMENT, not an "
                "assumption about the filesystem: genuinely distinct directories yield distinct "
                "inodes and are NOT refused. Settle on one spelling (`git mv`) and re-run R25."
            )
        if _symaliased:
            emit_fail(
                "GIT_SYMLINK_TARGET_ALIAS",
                "tracked SYMLINKS whose TARGET spells a tracked path with an ALIAS of the "
                "spelling this repository records: " + ", ".join(_symaliased[:8]) + ". A "
                "symlink's target is BLOB CONTENT, not an index path, so four rounds of "
                "index-path alias census never looked at it. On this filesystem the alias "
                "RESOLVES, so the build, the lint and every sweep succeed and the tree reports "
                "CLEAN — while a receiver whose filesystem treats the difference as significant "
                "gets a DANGLING link. Measured: `git mv backend/gradlew backend/gradlew-real` + "
                "`ln -s GRADLEW-REAL backend/gradlew` left `git status --porcelain -uall` EMPTY, "
                "ran R25 to green and left the fingerprint at the clean-tree constant. This is a "
                "MEASUREMENT of an observed (st_dev, st_ino) identity plus a fold equality: a "
                "target that leaves the repository, names an untracked path, dangles, or spells "
                "the record EXACTLY is NOT refused. ROUND 14b: the verdict is taken on EVERY "
                "component the walk resolves, not on the final one alone — `-> DIRLINK` blocked "
                "while `-> DIRLINK/` and `-> DIRLINK/real.txt` passed over the very same tracked "
                "`dirlink`, a one-character bypass. Spell the target the way the index records "
                "the path (`ln -sf <recorded-spelling>`) and re-run R25."
            )
        if _symunbounded:
            emit_fail(
                "GIT_SYMLINK_RESOLUTION_UNBOUNDED",
                "tracked SYMLINKS whose target could not be resolved within the budget every "
                "kernel imposes: " + ", ".join(_symunbounded[:8]) + ". Resolving a target the way "
                "the kernel does means FOLLOWING intermediate symlinks, and a committed chain "
                "that cycles or expands without bound leaves the alias question UNANSWERED — "
                "which this family never converts into silence (that conversion is the defect "
                "every round since 8 has been closing). It is also a defect in its own right: "
                "the receiver's kernel returns ELOOP. The budget is the LARGER of the two kernel "
                "limits (Linux MAXSYMLINKS 40, macOS SYMLOOP_MAX 32), so a chain any receiver "
                "would resolve is never refused here. Unbreak the chain and re-run R25."
            )
        if _flagged:
            emit_fail(
                "GIT_INDEX_FLAGS_SET",
                "the index of the tree being pushed carries bits that tell git to stop reporting "
                "the truth about tracked paths: " + ", ".join(_flagged[:8]) + ". "
                "`git update-index --assume-unchanged` and `--skip-worktree` keep `git status` "
                "empty while the file on disk is replaced, deleted or swapped for a symlink — "
                "measured, either one makes the tree fingerprint report the clean-tree constant "
                "while the push ships the committed blob. Clear them (`git update-index "
                "--no-assume-unchanged` / `--no-skip-worktree <path>`) and re-run R25."
            )
        if _mistyped:
            emit_fail(
                "GIT_WORKTREE_TYPE_MISMATCH",
                "tracked paths whose WORKTREE REPRESENTATION is not the one the index records: "
                + ", ".join(_mistyped[:8]) + ". The index says one kind of object and the "
                "filesystem holds another, so every read of that path — this sweep, the "
                "fingerprint, the build, the lint — answers about something the push will not "
                "ship. Measured: an index-regular path replaced by a symlink to a benign file "
                "outside the repository left `git status` empty and the fingerprint at the "
                "clean-tree constant. A representation this gate cannot account for is refused, "
                "never skipped."
            )
        if _absent:
            emit_fail(
                "GIT_TRACKED_PATH_ABSENT",
                "tracked paths this repository reports as unmodified that are not readable on "
                "disk: " + ", ".join(_absent[:8]) + ". The legitimate-looking ways to arrive here "
                "are `git update-index --assume-unchanged`, `git update-index --skip-worktree` "
                "and a SPARSE CHECKOUT; under all three `git status` stays empty while the file "
                "the push ships is not on disk at all, so nothing R25 executed or hashed was "
                "about it. Restore the path (and clear the bit) and re-run R25."
            )
        if _glbad:
            emit_fail(
                "GIT_GITLINK_DIVERGENCE",
                "initialized submodules whose work tree is not at the commit the superproject "
                "records: " + ", ".join(_glbad[:8]) + ". The push ships the RECORDED commit, so "
                "a run performed against a different one certifies code the receiver will not "
                "get. `git submodule update --init` and re-run R25."
            )
        if _bad:
            emit_fail(
                "GIT_RAW_INDEX_DIVERGENCE",
                "tracked files this repository reports as unmodified do NOT match their index "
                "blob when their bytes are hashed directly: " + ", ".join(_bad) + ". Either a "
                "content filter (filter.<n>.clean/.smudge/.process) is interposing on git's "
                "answers, or an eol conversion (core.autocrlf/core.eol/a text attribute) rewrites "
                "them on checkout. Under both, the tree that was verified is not the tree being "
                "pushed."
            )

    # 12a. THE TREE MUST BE CLEAN NOW, read from git rather than from the record. Check 7 reads
    #      tree_clean/tree_clean_end OUT OF THE LINE — values supplied by whoever wrote it. This
    #      is the same fact, established independently, and it survives a compromised fingerprint
    #      helper (measured: with the helper stubbed, a hand-authored line certified a DIRTY tree).
    rc_st, st_out = git_out("status", "--porcelain", "-uall", root=root, check=False)
    if rc_st != 0:
        emit_fail(
            "GIT_CONTEXT_UNUSABLE",
            f'`git status` failed in {root}, so this gate cannot establish whether the tree being '
            f'pushed is the committed one. The record\'s own tree_clean field is written by the '
            f'party being audited; an independent read that cannot be taken is not a clean bill.'
        )
    if st_out:
        shown = "\n    ".join(st_out.splitlines()[:10])
        emit_fail(
            "AUDIT_TREE_DIRTY_NOW",
            f'the audit line claims a clean tree, but this working tree is dirty RIGHT NOW:\n'
            f'    {shown}\n'
            f'  A push ships the COMMIT, so evidence gathered from a tree that differs from it '
            f'certifies code the receiver will never get. Until this round the only independent '
            f'check of that was the fingerprint recompute, which a replaced helper disabled. '
            f'Commit, stash or ignore these paths and re-run verify-completion.sh.'
        )

    # 12c. RECOMPUTE — with the PREVIOUS RELEASE'S implementation, not this tree's.
    fp_rel = "practices/scripts/lib/tree_fingerprint.py"
    anchor_rev = None
    if expect_anchor_arg and expect_anchor_arg != ZERO_SHA:
        anchor_rev = expect_anchor_arg           # the remote's own advertisement — authoritative
    else:
        for cand in ("refs/remotes/origin/main", "HEAD"):
            rc_c, out_c = git_out("rev-parse", "--verify", "--quiet", cand + "^{commit}",
                                  root=root, check=False)
            if rc_c == 0 and out_c:
                anchor_rev = out_c
                break
    impl_path, impl_kind, tmpdir = None, None, None
    if anchor_rev:
        # ROUND 6 / P2 (registered by the reviewer, closed here): the extraction used to run the
        # blob through `git cat-file -p` CAPTURED AS TEXT, `.strip()` it and append a newline — so
        # the "previous release's implementation" was not the previous release's bytes. It is now
        # read as BYTES and written verbatim, and the written file is required to hash back to the
        # blob id it came from. The temp directory is created under a base that does NOT honour
        # TMPDIR (attacker-settable, and a TMPDIR inside a work tree gives the extracted helper a
        # git context), with mkdtemp's 0700 semantics.
        rc_w2, want_blob = git_out("rev-parse", "--verify", "--quiet",
                                   f"{anchor_rev}:{fp_rel}", root=root, check=False)
        if rc_w2 == 0 and want_blob:
            import tempfile
            import hashlib as _hl
            proc_b = subprocess.run([GIT_BIN, "--no-replace-objects", "-C", str(root),
                                     "cat-file", "blob", want_blob],
                                    stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=GIT_ENV)
            if proc_b.returncode == 0:
                raw = proc_b.stdout
                got = _hl.sha1(b"blob %d\0" % len(raw) + raw).hexdigest()
                if got != want_blob:
                    emit_fail(
                        "AUDIT_FINGERPRINT_UNVERIFIABLE",
                        f'the previous release\'s copy of {fp_rel} did not survive extraction '
                        f'intact ({got[:12]} vs {want_blob[:12]}), so the recompute would be '
                        f'performed with an implementation that is nobody\'s.'
                    )
                tmpdir = tempfile.mkdtemp(prefix="ax-fpimpl-", dir="/tmp")
                impl_path = os.path.join(tmpdir, "tree_fingerprint.py")
                with open(impl_path, "wb") as fh:
                    fh.write(raw)
                impl_kind = f"release {anchor_rev[:12]}"
    if impl_path is None:
        # No prior copy (first release carrying this file, or an anchor that predates it). Fall
        # back to the working-tree helper — which 12b has just proven identical to the pushed
        # commit's — and say so, rather than skipping the recompute altogether.
        fp_helper = guard_repo / "practices" / "scripts" / "lib" / "tree_fingerprint.py"
        if not fp_helper.is_file():
            emit_fail(
                "AUDIT_FINGERPRINT_UNVERIFIABLE",
                f'{fp_rel} is absent and no release copy of it could be read, so the recorded '
                f'tree_fingerprint cannot be recomputed at all. A gate that cannot verify its own '
                f'evidence blocks rather than skipping — deleting the checker is not a way to pass.'
            )
        impl_path, impl_kind = str(fp_helper), "the working tree (no release copy available)"
    try:
        # -I -S for the same reason the parent runs with them: this subprocess is the RECOMPUTE,
        # i.e. the only independent statement about the tree, and a site hook would own it.
        proc = subprocess.run([sys.executable, "-I", "-S", impl_path, str(root)],
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=GIT_ENV)
        recomputed = proc.stdout.decode(errors="replace").strip()
        rec_rc = proc.returncode
    except Exception as e:
        recomputed, rec_rc = "", -1
        proc = None
    finally:
        if tmpdir:
            import shutil as _sh
            _sh.rmtree(tmpdir, ignore_errors=True)
    if rec_rc != 0 or not DIGEST_RE.match(recomputed or ""):
        emit_fail(
            "AUDIT_FINGERPRINT_UNVERIFIABLE",
            f'recomputing the tree fingerprint with {impl_kind} produced '
            f'{recomputed[:40]!r} (exit {rec_rc}) instead of a digest. THIS IS THE CHECK THAT '
            f'FAILED OPEN before round 5: an empty or "nogit" result simply skipped the '
            f'comparison, so replacing the helper with `print("nogit")` turned a rejected forgery '
            f'(exit 1) into a passing push (exit 0). Unknown blocks.'
        )
    if recomputed != tree_fp:
        emit_fail(
            "AUDIT_FINGERPRINT_MISMATCH",
            f'the recorded tree_fingerprint ({str(tree_fp)[:12]}) is not the fingerprint of the '
            f'tree that is here now ({recomputed[:12]}), as computed by {impl_kind}. Either the '
            f'line was written by hand — .ax-verify/runs.jsonl is an ordinary text file — or the '
            f'tree has changed since the run that certified it, in which case the certificate is '
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
#     ROUND 5 fail-closed sweep: `except Exception: _replace = ""` made an ENUMERATION FAILURE
#     indistinguishable from "there are none" — the one state that can be manufactured read as
#     clean. On a live git root a failed enumeration now blocks.
_rc_rep, _replace = git_out("for-each-ref", "--format=%(refname)", "refs/replace/",
                            root=root, check=False)
if _rc_rep != 0:
    if live_git_root:
        emit_fail(
            "AUDIT_REPLACE_REFS_PRESENT",
            f'the replacement-ref enumeration itself failed (git exited {_rc_rep}) in {root}, so '
            f'this gate cannot tell whether refs/replace/* is empty. That question decides whether '
            f'every sha comparison above was answered out of a fabricated object graph, and an '
            f'unanswerable question fails closed.'
        )
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
