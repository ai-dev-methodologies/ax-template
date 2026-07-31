#!/usr/bin/env bash
# practices/evals/evidence_quote_spotcheck_guard.sh — BACKLOG P2-1 (74th hard guard, ADVISORY live).
#
# evidence_guard.sh verifies evidence STRUCTURE only (upstream_id resolves, section/quote
# non-empty) — it never checks that the quote TEXT actually appears in the snapshot, so a
# fabricated or mis-attributed quote passes every blocking gate (proven by the 2026-06-01
# live-fetch audit, which found 56 such defects in a one-off pass). This guard closes the
# offline half of that escape DETERMINISTICALLY (full sweep, not random sampling — R25
# demands same-input/same-output): for every rule evidence entry carrying an `upstream_id`,
# the quote must appear as a substring of the referenced snapshot body
# ({catalog}/upstream/{upstream_id}.snapshot.md) after HTML-stripping + whitespace/typography
# normalization. `source_type: external` entries (URL-only, no snapshot) are out of scope —
# only a live fetch can verify those (see the live-fetch audit methodology).
#
# Mode:
#   default      ADVISORY — prints every mismatch as WARN + a summary, always exits 0.
#                Rationale: the first full sweep surfaced 95 pre-existing quote↔snapshot
#                misalignments (mostly quotes verified against the LIVE page while the disk
#                snapshot is a partial digest of it) — a blocking gate would freeze the
#                catalog on day one. Burn the backlog down, then promote via --strict.
#   --strict     exit 1 on any {catalog}/rules/*.md mismatch (the promotion path; also how
#                fixtures prove non-vacuity in run-all-guards).
#   --allow-missing-snapshot
#                In --strict mode only: QUOTE_NOT_IN_SNAPSHOT stays fatal (exit 1), but
#                SNAPSHOT_FILE_MISSING findings are DOWNGRADED to an advisory WARN list
#                (printed, non-fatal). This flag exists for an honest reason: the practices/
#                (Java-side) upstream/ manifest records sha/bytes only — the {id}.snapshot.md
#                BODIES were never committed, so those quotes cannot be verified offline and
#                restoring the bodies requires a network fetch (registered as a backlog
#                residual). The flag lets the QUOTE half of the sweep promote to strict while
#                the network-bound MISSING half is tracked but not blocking. Without --strict
#                the flag is inert (everything is advisory anyway).
#   --root DIR   scan DIR instead of the repo root (fixtures).
#
#   --include-templates
#                BACKLOG P2-40. Additionally sweeps templates/**/*.{tsx,ts} for the same
#                comment-wrapped frontmatter shape used across the L1-L4 template tree
#                (`/*\n---\n<yaml>\n---\n*/` at the top of the file) and quote-checks any
#                `evidence[].upstream_id` entry against EITHER catalog's upstream/ (a
#                template can cite a Java-side or a React-side snapshot). Findings are
#                tagged TEMPLATE_QUOTE_NOT_IN_SNAPSHOT / TEMPLATE_SNAPSHOT_FILE_MISSING and
#                are ALWAYS printed, but only participate in the --strict fatal exit when
#                --strict-templates is ALSO passed (see below) — this is the same
#                advisory-first / promote-later posture already established for the rules/
#                sweep, applied to a brand-new scan surface. `templates/**/*.md` (the
#                DECISIONS.md ADR log) is explicitly OUT OF SCOPE: it embeds MULTIPLE
#                YAML-fenced blocks per file (one per ADR) rather than one leading
#                frontmatter block, and a small number of its `evidence` entries are a
#                single-mapping `source_type: internal` shape, not the upstream_id+quote
#                list shape this guard verifies — a structurally different doc type,
#                disclosed here rather than silently mis-scanned.
#   --strict-templates
#                Promotes templates/** findings (only) to fatal under --strict. Kept
#                SEPARATE from --strict on purpose: the first live --include-templates sweep
#                (2026-07-28) found the one PRD-named fabricated anchor
#                (templates/L1/components/currency-input.tsx, fixed) PLUS ~105 pre-existing
#                quote↔snapshot misalignments across templates/L1/components/**
#                (mostly the same "paraphrased against the live page, not the digest
#                snapshot" class already known from the rules/ sweep — e.g. every
#                shadcn-ui-2026-05 citation quotes a per-component description that
#                the committed snapshot, an overview-only digest, never actually contains).
#                Flipping the templates scan straight to --strict would freeze the live
#                gate on a ~105-item backlog that is out of scope for the guard-coverage
#                closure that added this scan surface — tracked as a new backlog candidate
#                instead of silently fixed or silently dropped. Without --strict-templates
#                the flag is inert (findings are advisory regardless of --strict).
#   --templates-only-protected
#                P2-40 follow-up (2026-07-28 reviewer finding). --include-templates alone,
#                as registered live, is ADVISORY — so restoring the fabricated
#                templates/L1/components/currency-input.tsx anchor only WARNed and the
#                registered live invocation still exited 0: the required RED-on-revert did
#                NOT hold and the "fabricated anchor is blocked" claim was unearned. This
#                flag restricts the templates/** sweep to exactly the anchors listed in the
#                committed ledger
#                    practices/evals/evidence_protected_template_anchors.txt
#                (implies --include-templates), so those anchors CAN be fatal under
#                --strict --strict-templates while the ~105 pre-existing misalignments in
#                the unlisted remainder stay out of scope. The ledger is anchor-scoped
#                (`<path>::<upstream_id>` per line), not file-scoped, because
#                currency-formatter.tsx carries TWO upstream anchors of which only the
#                stripe-billing one is disk-clean — protecting the file wholesale would
#                have meant either an unearned pass or rewriting an unrelated quote to
#                make the gate green. Honest under-claiming: the protected set is exactly
#                what is verified, and what is excluded is named in the ledger.
#
#                Fail-closed non-vacuity (each ⇒ exit 2, so the gate cannot be emptied
#                into a silent pass): ledger absent · ledger with no entries · ledger with
#                no `# min_entries:` directive · fewer UNIQUE entries than that directive · a
#                declared min_entries below the guard-pinned live floor
#                (LIVE_MIN_PROTECTED_ENTRIES, applies when scanning the real repo root,
#                i.e. no --root) · an entry whose file does not exist · an entry whose file
#                carries zero upstream_id evidence · an entry whose declared upstream_id is
#                not actually cited by that file · a matching entry whose `quote` is missing
#                or normalized-empty (codex round-2: "" is a substring of every snapshot, so
#                blanking/deleting the quote used to pass vacuously instead of failing) ·
#                a matching entry whose `section` is missing or blank (same-shape bypass) ·
#                zero anchors scanned overall.
#
#                IDENTITY PINNING + TYPE STRICTNESS (codex round-3 closure, 2026-07-28).
#                Rounds 1-3 each bypassed this gate with a different trick because every
#                previous fix validated the SHAPE of a ledger entry while leaving the
#                IDENTITY of what must be protected — and the TYPE of the scalars compared —
#                unpinned. Both holes are closed by construction here:
#
#                (a) TYPE-STRICT SCALARS. `quote`, `section` and `upstream_id` of a protected
#                    anchor must be genuine YAML strings. Nothing is `str()`-coerced into the
#                    comparison any more: an unquoted `quote: 0` used to become the string
#                    "0", which occurs verbatim in the Stripe snapshot body ⇒ substring match
#                    ⇒ exit 0; `section: null` used to become the string "None", which is not
#                    blank ⇒ slipped past the blank check. int / float / bool / null / list /
#                    dict / missing each now exit 2 with their own reason code
#                    (PROTECTED_LEDGER_{MISSING,NON_STRING}_{QUOTE,SECTION} /
#                    PROTECTED_LEDGER_NON_STRING_UPSTREAM_ID). A protected quote must also
#                    clear MIN_PROTECTED_QUOTE_CHARS after normalization — a legally-typed
#                    one-character quote ("a") is the same vacuous-substring attack wearing a
#                    string's clothes.
#
#                (b) IDENTITY PINNING. `min_entries` preserved a ROW COUNT, not identities:
#                    delete the currency-input row, duplicate the clean currency-formatter
#                    row, keep min_entries: 2 ⇒ the guard checked the formatter twice, exited
#                    0, and currency-input became free to fabricate or blank. The ledger is
#                    now consumed as a SET of (path, upstream_id) identities: duplicate
#                    tuples are rejected outright (PROTECTED_LEDGER_DUPLICATE_IDENTITY), the
#                    min_entries comparison counts UNIQUE tuples, and a required-identity set
#                    must be fully present — sourced from BOTH the guard-pinned
#                    LIVE_REQUIRED_PROTECTED_IDENTITIES (real repo tree only; may not be
#                    reduced) and any `# require: <path>::<upstream_id>` directives declared
#                    in the ledger itself (all roots, incl. fixtures). A missing required
#                    identity is PROTECTED_LEDGER_REQUIRED_IDENTITY_MISSING (exit 2)
#                    REGARDLESS of row count. Ledger paths are additionally constrained to
#                    safe repo-relative form (PROTECTED_LEDGER_UNSAFE_PATH) so an identity
#                    can neither escape the scanned root nor be spelled two ways.
#
#                (c) Same-pass closures found while re-reading the whole protected path: a
#                    protected anchor whose snapshot body does not exist on disk is now a
#                    structural failure (PROTECTED_LEDGER_SNAPSHOT_MISSING, exit 2) instead
#                    of a finding that only bit under --strict-templates — deleting the
#                    snapshot must not be cheaper than falsifying the quote; and the declared
#                    `section` must itself occur in the snapshot body
#                    (TEMPLATE_SECTION_NOT_IN_SNAPSHOT finding ⇒ exit 1 under
#                    --strict --strict-templates), so a fabricated section is no longer
#                    unverified free text.
#
# ANCHOR RATCHET — THE FLOOR NOW LIVES IN THE PREVIOUS RELEASE, NOT IN THE WORKING TREE
# ------------------------------------------------------------------------------------
# (P1-1, cross-family reviewer finding 2026-07-30; TD-2026-07-30-P1-anchor-ratchet.)
#
# The five-surface census above compares five CURRENT-TREE values to each other. That is a
# consistency check, not a ratchet: every one of the five is editable in the same commit, so
# lowering all five together (64 → 63 in LIVE_MIN_PROTECTED_ENTRIES, in the frozenset, in the
# ledger's `# min_entries:`, in one `# require:` directive, and by deleting the matching row)
# left the census EQUAL and the gate GREEN while an anchor silently left the fatal set. The
# reviewer reproduced exactly that. Duplicating a number in two in-tree places raises the cost
# of a downgrade from one edit to five; it never makes it impossible, because all five live in
# the same mutable tree.
#
# So the reference is moved OUT of the tree: the guard reads its OWN PREVIOUS-RELEASE COPY via
# `git show <ANCHOR>:practices/evals/evidence_quote_spotcheck_guard.sh` and enforces
#   · LIVE_MIN_PROTECTED_ENTRIES(current) >= LIVE_MIN_PROTECTED_ENTRIES(anchor)
#     — MONOTONIC_FLOOR_REGRESSION, exit 4;
#   · LIVE_REQUIRED_PROTECTED_IDENTITIES(anchor) ⊆ LIVE_REQUIRED_PROTECTED_IDENTITIES(current)
#     — PROTECTED_IDENTITY_REMOVED, exit 5. Superset growth stays legal: that IS the ratchet
#     direction. Subset comparison (not equality) is also what catches SUBSTITUTION across
#     releases — swap one identity for another and the count/census stay satisfied, but the
#     removed identity is missing from the current set.
# Because the census forces all five surfaces to carry the same number, ratcheting the two
# guard-side constants transitively ratchets the ledger's row count, its `# min_entries:` and
# its `# require:` directives as well.
#
# WHY THIS IS SOUND (gate ordering): R25 (verify-completion.sh) runs with the working tree at
# HEAD, and HEAD is AHEAD of origin/main — it is the commit being released. So origin/main is
# genuinely the prior released state and cannot be edited by the commit under verification. The
# pre-push recency guard then binds every push to an R25 run at that HEAD, so a commit cannot
# reach origin/main without having satisfied the ratchet against the origin/main before it.
#
# Anchor resolution and its honest weak spots:
#   · origin/main — the strong case, per the paragraph above.
#   · HEAD — FALLBACK ONLY (detached checkout, or a fresh fork with no origin remote). Weaker:
#     if the downgrade is already committed locally, HEAD contains it and the comparison is
#     against the downgraded value. It preserves the ratchet across the *uncommitted* edit and
#     nothing more.
#   · unavailable (no git, no commits) — the anchor checks print WARN and are SKIPPED. Stated
#     plainly: in a tarball export the ratchet is inert. It is also unpushable there (the
#     pre-push recency guard is a git hook), so the released-artifact path retains the gate.
#   · Parsing is AST-based over the anchor's embedded python block, so reformatting the
#     frozenset (line breaks, ordering, comments, set-literal vs frozenset() call) does not
#     evade it; only removing a tuple does.
#
# TWO-RELEASE REPRESENTATION LAUNDERING — WHY THE PARSER IS NOW FAIL-CLOSED IN BOTH DIRECTIONS
# -------------------------------------------------------------------------------------------
# (P1-A, cross-family reviewer ROUND 2, 2026-07-30; TD-2026-07-30-P1-anchor-ratchet-r2.)
#
# The ratchet above reads the ANCHOR through a parser, and the parser only recognizes a literal
# int assignment and literal 2-string tuples. Everything it fails to recognize used to WARN and
# SKIP. That turned the parser itself into the bypass, spread over TWO RELEASES:
#
#   release N   — change REPRESENTATION ONLY, runtime values untouched (still 64/64, so the
#                 in-tree census is EQUAL and every finding-level check passes):
#                   · `LIVE_MIN_PROTECTED_ENTRIES = int("64")`   → not an ast.Constant int
#                   · `LIVE_REQUIRED_PROTECTED_IDENTITIES = _PREVIOUS_PINS` (an alias)
#                                                              → no tuple literals under it
#                   · or merely reflow the here-document opener so the embedded python block is
#                     no longer located at all → ast.parse sees shell text
#                 Release N therefore SHIPS GREEN while becoming unparseable to its successor.
#   release N+1 — do the real downgrade (64 → 63, delete the accordion row, drop the tuple).
#                 The anchor is release N, whose constants no longer parse ⇒ (None, None) ⇒
#                 WARN ANCHOR_UNPARSEABLE / ANCHOR_NO_FLOOR / ANCHOR_NO_PIN_SET ⇒ exit 0.
#
# Two layers close it, and the FIRST one is what actually kills the attack:
#
#   (1) SELF-PARSE CHECK (generation-N kill, exit 6 SELF_UNPARSEABLE). On every live-root run the
#       guard feeds ITS OWN CURRENT FILE to the SAME parser the anchor comparison uses, and
#       demands that the parse yield BOTH constants AND that the parsed values EQUAL the runtime
#       ones. So a representation that a future release could not read can never ship in the
#       first place: release N dies at its own gate. Value equality (not merely
#       "something parsed") is deliberate — it also catches a decoy assignment, a shadowing
#       re-assignment, and any tuple added to/removed from the literal that the runtime object
#       does not carry.
#
#   (2) FAIL-CLOSED ANCHOR SEMANTICS (belt-and-braces for histories that PREDATE (1)). Three
#       anchor states are now DISTINGUISHED instead of collapsed into one skip:
#         (i)   anchor ref / git unavailable          → WARN + skip STAYS. Nothing to compare;
#               such a tree is also unpushable (the pre-push recency guard is a git hook).
#         (ii)  this file ABSENT in the anchor commit → WARN + skip STAYS (first-release
#               bootstrap). Not attacker-creatable: git history is immutable to the working tree,
#               so an edit under review cannot make its own file vanish from a released commit.
#         (iii) this file PRESENT in the anchor but its constants NOT extractable as literals
#               (block does not parse / no floor / no pin set) → BLOCKING, exit 7
#               ANCHOR_UNPARSEABLE. This is the state release N above manufactures, and it is
#               the one state of the three that a mutable tree CAN reach on purpose.
#       Consequence, disclosed: the "first-release bootstrap" leniency that (iii) used to grant
#       is gone. It was already spent — origin/main carries both constants — and keeping it
#       would have meant keeping the laundering path open for the sake of a state that can no
#       longer occur going forward.
#
# ANCHOR AUTHENTICITY — WHICH COMMIT THE ANCHOR IS, AND HOW ITS BYTES ARE READ (round 3)
# --------------------------------------------------------------------------------------
# (P1-X / P1-Y, cross-family reviewer ROUND 3, 2026-07-30; TD-2026-07-30-P1-anchor-authenticity.)
#
# Rounds 1-2 hardened WHAT the anchor says. Round 3 attacked WHICH COMMIT it is, and HOW ITS BYTES
# ARE READ — both still fully in the attacker's hands. Full rationale lives in the shared helper
# practices/scripts/lib/release_anchor.sh; the codes it can raise here, all EXIT 8:
#   ANCHOR_NOT_ANCESTOR          — `refs/remotes/origin/main` is an ORDINARY LOCAL REF. Aim it
#                                  (via `git update-ref`) at a synthetic commit whose tree merely
#                                  DROPS this file and state (ii) above — "absent at the anchor ⇒
#                                  bootstrap skip" — becomes reachable after all. The anchor must
#                                  now be an ancestor of HEAD.
#   ANCHOR_BOOTSTRAP_IMPLAUSIBLE — plainly: A FILE THAT HAS HISTORY IN THIS REPO CAN NEVER
#                                  LEGITIMATELY BE "ABSENT IN THE PREVIOUS RELEASE". State (ii) is
#                                  honored only when the anchor's own history never carried the path.
#   ANCHOR_PATH_NOT_REGULAR      — an anchor-critical path is a SYMLINK (git mode 120000) at the
#                                  anchor. The anchor side reads GIT OBJECTS and the self side
#                                  reads the FILESYSTEM, so a link makes them read different bytes
#                                  by construction — and the link's target PATHNAME can itself be
#                                  spelled as parseable-but-weakened python, which is exactly what
#                                  the self-parse check (exit 6) would then be reading past.
#   SELF_PATH_NOT_REGULAR        — the same check on the WORKING TREE (lstat, and on every path
#                                  component, so a symlinked DIRECTORY cannot launder a regular
#                                  leaf). EXIT 2, not 8: it is an ordinary structural defect of the
#                                  tree in front of us, it runs on EVERY root including fixtures,
#                                  and it is therefore the one half of this P1 a FIXTURE can prove
#                                  (fixtures/evidence-quote-spotcheck/fail_protected_ledger_symlink).
#
# Usage:
#   bash practices/evals/evidence_quote_spotcheck_guard.sh
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --strict
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --strict --allow-missing-snapshot
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --strict --root evals/fixtures/...
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --include-templates
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --include-templates --strict --strict-templates --root evals/fixtures/...
#   bash practices/evals/evidence_quote_spotcheck_guard.sh --strict --strict-templates --templates-only-protected
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
           1) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"evidence_quote_spotcheck_guard: HERMETIC_PRIVILEGED_UNREACHABLE — a re-exec into bash privileged mode was already attempted and this shell is STILL unprivileged. Either exec is shadowed by a function, or AX_PRIV_REEXEC was preset in the environment to skip the re-exec. Both are refused; nothing in this gate runs unprivileged. Start it from a clean shell."} ;;
           *) case "${BASH:-}" in
                  /*) exec /usr/bin/env AX_PRIV_REEXEC=1 "$BASH" -p "$0" "$@" ;;
                  *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"evidence_quote_spotcheck_guard: HERMETIC_PRIVILEGED_UNREACHABLE — the running interpreter (BASH) is not an absolute path, so the SAME interpreter cannot be named unambiguously for the privileged re-exec."} ;;
              esac ;;
       esac ;;
esac
case $- in
    *p*) ;;
    *) _AX_PV_NULL=; _AX_PV_DIE=${_AX_PV_NULL:?"evidence_quote_spotcheck_guard: HERMETIC_PRIVILEGED_UNREACHABLE — the re-exec returned instead of replacing this process, which means exec itself is shadowed. Unprivileged execution is refused."} ;;
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
_AX_PF_LABEL="evidence_quote_spotcheck_guard"
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

# ROUND 6 / P1-1(c): ARGUMENT PARSING MOVED BELOW THE BOOTSTRAP. It used to live here, ahead of
# the hermetic scrub, and every line of it (`[`, `echo`, `exit`, `shift`) is an ordinary command
# lookup — i.e. attacker code running inside the gate before the gate has looked at its runtime.
# Nothing but the pure-keyword preflight may precede the scrub.

# ── ROUND 5 / P1-1+P1-2: HERMETIC RUNTIME BOOTSTRAP (A) — DELIBERATELY DUPLICATED ────
# (TD-2026-07-30-P1-hermetic-runtime; the full argument lives in the header of
#  practices/scripts/lib/release_anchor.sh.) THE RATCHET MAY NOT INHERIT ITS OWN RUNTIME.
# Measured: an exported `git` function rewrote the anchor pin; an exported `pwd` moved the pin's
# root to a foreign repo; an exported `python3` turned a FAILING guard into exit 0; GIT_DIR/
# GIT_WORK_TREE made every read describe a CLEAN shadow checkout of a DIRTY tree. All of it
# arrives through the environment, so it must die BEFORE the first git call, BEFORE any `source`,
# and BEFORE this script computes its own directory with `cd`/`pwd` — which is exactly why these
# lines cannot live in a sourced file. The duplication IS the bootstrap.
_AX_HRM_LABEL="evidence_quote_spotcheck_guard"; _AX_HRM_EXIT=2; _AX_HRM_NEED_PY=1
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
# ── ROUND 6 / P1-2: A PATH IS NOT AN IDENTITY ───────────────────────────────────────
# INVARIANT: a tool this gate runs must SAY WHAT IT IS. `-f`/`-x` FOLLOW SYMLINKS and assert
# nothing about the program: MEASURED — a symlink named python3 pointing at /usr/bin/true passed
# every lexical/`-x` test and turned the recency guard's entire python body into exit 0 (honest
# baseline 1). So each tool is (a) canonicalised through its real directory, (b) refused if it
# lives inside the tree under audit, and (c) made to IDENTIFY ITSELF by being RUN — `git --version`
# must produce a git version banner, python3 must print a python self-report under `-I -S`. A
# /usr/bin/true symlink prints nothing and is refused.
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
    # (c) identity by SELF-REPORT — the only statement about a program that a path cannot forge.
    if [ "${_ax_hn%%=*}" = "git" ]; then
        _ax_hver="$("$_ax_hb" --version 2>/dev/null)" || _ax_hver=""
        case "$_ax_hver" in
            "git version "[0-9]*) AX_GIT_BIN="$_ax_hb" ;;
            *)  { echo "$_AX_HRM_LABEL: HERMETIC_TOOL_UNAUTHENTIC — '$_ax_hb' is executable but does"
                  echo "  not identify itself as git (\`git --version\` said '${_ax_hver:-<nothing>}')."
                  echo "  Lexical absoluteness and -f/-x FOLLOW SYMLINKS and say nothing about the"
                  echo "  program: a symlink named python3 → /usr/bin/true satisfied all of them and"
                  echo "  turned an entire guard into exit 0. A tool this gate runs must say what it is."; } >&2
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
                  echo "  (exit 0 where the honest answer was 1). Identity is what the program says."; } >&2
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
unset _ax_hn _ax_hb _ax_hdir _ax_hver _AX_HRM_BAD _AX_HRM_PATH

SCRIPT_DIR="$(builtin cd "$(dirname "${BASH_SOURCE[0]}")" && builtin pwd)"
SELF_REPO_ROOT="$(builtin cd "$SCRIPT_DIR/../.." && builtin pwd -P)"
# ── HERMETIC RUNTIME BOOTSTRAP (B): bind the git identity to the trusted root ────────
# Part A removed the inherited git context; this derives the real one and REQUIRES it to be the
# root this entry resolved for itself. `git -C <root>` WALKS UP when <root> is not itself a work
# tree, so without this a gate can authenticate a repository that merely CONTAINS the directory it
# is scanning. The derived gitdir/worktree are then passed EXPLICITLY on every call for this root
# (see ax_git), so nothing downstream depends on discovery at all.
# NOT exported, and unset first: a binding that could arrive from the environment would be a
# NEW redirection channel of exactly the kind part A just closed. Every entry derives its own.
unset AX_GIT_BOUND_ROOT AX_GIT_BOUND_DIR
if "$AX_GIT_BIN" -C "$SELF_REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    _ax_hcan="$(builtin cd "$SELF_REPO_ROOT" 2>/dev/null && builtin pwd -P)"
    _ax_htop="$("$AX_GIT_BIN" -C "$SELF_REPO_ROOT" rev-parse --show-toplevel 2>/dev/null)"
    _ax_htop="$(builtin cd "${_ax_htop:-/nonexistent}" 2>/dev/null && builtin pwd -P)"
    if [ -z "$_ax_hcan" ] || [ "$_ax_htop" != "$_ax_hcan" ]; then
        { echo "$_AX_HRM_LABEL: GIT_CONTEXT_REDIRECTED — the git work tree answering this gate's"
          echo "  reads is '${_ax_htop:-<unresolvable>}', not the root it was invoked for"
          echo "  ('${_ax_hcan:-$SELF_REPO_ROOT}'). Every head / status / fingerprint / anchor answer would"
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

STRICT=0
ALLOW_MISSING=0
ROOT_OVERRIDE=""
INCLUDE_TEMPLATES=0
STRICT_TEMPLATES=0
TEMPLATES_ONLY_PROTECTED=0
while [ $# -gt 0 ]; do
    case "$1" in
        --strict) STRICT=1; shift ;;
        --allow-missing-snapshot) ALLOW_MISSING=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        --include-templates) INCLUDE_TEMPLATES=1; shift ;;
        --strict-templates) STRICT_TEMPLATES=1; shift ;;
        # implies --include-templates so the flag can never be a silent no-op
        --templates-only-protected) TEMPLATES_ONLY_PROTECTED=1; INCLUDE_TEMPLATES=1; shift ;;
        *) echo "evidence_quote_spotcheck_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

REPO_ROOT="${ROOT_OVERRIDE:-$SELF_REPO_ROOT}"
# LIVE_ROOT=1 ⇔ the RESOLVED scan root IS this repository, however it was supplied. The
# guard-pinned protected-identity floor applies only there; fixture roots declare their own
# min_entries. Reviewer finding (round 4): keying this on "was --root passed?" let an explicit
# `--root <the actual repo>` resolve to the identical physical tree while silently DROPPING the
# pinned identities — a protected anchor could then be substituted away and its fabricated quote
# would land only in the advisory pool. Compare canonicalized physical paths (pwd -P, so symlink
# and `.`/`..` spellings cannot alias past the check) instead of arg presence.
RESOLVED_ROOT="$(cd "$REPO_ROOT" 2>/dev/null && pwd -P)" || RESOLVED_ROOT=""
LIVE_ROOT=0; [ -n "$RESOLVED_ROOT" ] && [ "$RESOLVED_ROOT" = "$SELF_REPO_ROOT" ] && LIVE_ROOT=1

# ── RELEASE ANCHOR (P1 anchor-ratchet, TD-2026-07-30-P1-anchor-ratchet) ───────────────
# See the ANCHOR RATCHET header block for the why. Resolution order, and it is deliberately
# short: origin/main (the previous release — the strong case, because R25 runs at a tree that
# is AHEAD of it) → HEAD (WEAKER: a detached/fork-fresh clone with no origin/main; HEAD is the
# commit being amended rather than a released state, so a ratchet edit that is already
# committed locally is invisible to it) → unavailable (no git / no commits), in which case the
# anchor checks print a loud WARN and are skipped. FIXTURE ROOTS NEVER ANCHOR: the whole block
# is gated on LIVE_ROOT, exactly like the LIVE_MIN_* floors, because a fixture exists to
# isolate one failure mode and has no release history of its own.
#
# ROUND 3 (P1-X/P1-Y, TD-2026-07-30-P1-anchor-authenticity): resolution moved into the SINGLE
# helper practices/scripts/lib/release_anchor.sh, shared with manifest_snapshot_integrity_guard
# and with verify-completion.sh's audit writer, so the sha the runner RECORDS and the sha this
# guard RATCHETS AGAINST are the same object by construction (that identity is what the pre-push
# authentication in layer (3) checks). Three new blocking codes, all documented in the helper:
#   · ANCHOR_NOT_ANCESTOR          — the anchor must be an ancestor of HEAD. refs/remotes/... is
#                                    an ordinary local ref; `git update-ref` can aim it at a
#                                    synthetic commit that merely LACKS this file, turning the
#                                    bootstrap skip below into a bypass.
#   · ANCHOR_BOOTSTRAP_IMPLAUSIBLE — a file that HAS HISTORY IN THIS REPO can never legitimately
#                                    be "absent in the previous release". Absence is honored only
#                                    when the anchor's own history never contained the path.
#   · ANCHOR_PATH_NOT_REGULAR / SELF_PATH_NOT_REGULAR — the anchor side reads GIT OBJECTS and the
#                                    self side reads the FILESYSTEM; a symlink makes them read
#                                    different bytes on purpose. Both sides must be regular files.
# The self-side (SELF_PATH_NOT_REGULAR) check runs on EVERY root, live or fixture, because it
# inspects the tree in front of it rather than a release history — so it is the one half of P1-Y
# a fixture can prove. Exit 8 for anchor-side authenticity, distinct from 4/5/6/7; exit 2 for the
# self-side, which is an ordinary structural defect of the tree being scanned.
ANCHOR_AUTH_EXIT=8
#
# ROUND 4 (P1-1/P1-2/P1-3, TD-2026-07-30-P1-anchor-runtime): rounds 1-3 authenticated WHAT the
# anchor says, WHICH commit it is and HOW its bytes are represented. Round 4 found the ratchet's
# own RUNTIME unauthenticated. Three more blocking codes here, all EXIT 8 except where noted:
#   · ANCHOR_REPLACE_REFS_PRESENT — `git replace` keeps the sha and swaps the OBJECT, so every
#     rev-list/ls-tree/merge-base above can be answered out of a fabricated history while the
#     audit records the authentic sha. Every git call now runs --no-replace-objects (+
#     GIT_NO_REPLACE_OBJECTS=1 exported by the helper, which is what reaches the python below),
#     and a tree carrying replacement refs at all is refused.
#   · ANCHOR_REF_MOVED_MIDRUN — the R25 runner resolves the anchor ONCE and exports it; this
#     guard re-reads the ref at its own moment of use and must get the same commit, and re-reads
#     it again after its work (so a standalone run without a runner pin still observes it twice).
#   · HELPER_PATH_NOT_REGULAR (EXIT 2) — the inline preflight immediately below. Structural, like
#     SELF_PATH_NOT_REGULAR, and for the same reason: it is a property of the tree in front of us.
#
# ── P1-3(b): INLINE HELPER PREFLIGHT — DELIBERATELY DUPLICATED IN EVERY CONSUMER ──────
# `[ -f ]` FOLLOWS SYMLINKS, so the old loader would happily source a link whose target is
# anything at all, and it did so BEFORE any mode check existed. The check therefore has to run
# before the source — which means it CANNOT live in the helper, because that is the object under
# test certifying itself. These ~15 lines are duplicated in evidence_quote_spotcheck_guard,
# manifest_snapshot_integrity_guard, verify-completion.sh and .githooks/pre-push ON PURPOSE; the
# duplication is the bootstrap, and factoring it out would undo the fix.
_ax_pf_rel="practices/scripts/lib/release_anchor.sh"
_ax_pf_cur="$SELF_REPO_ROOT"
for _ax_pf_part in practices scripts lib release_anchor.sh; do
    _ax_pf_cur="$_ax_pf_cur/$_ax_pf_part"
    if [ -L "$_ax_pf_cur" ]; then
        echo "evidence_quote_spotcheck_guard: HELPER_PATH_NOT_REGULAR — ${_ax_pf_rel} resolves" >&2
        echo "  through a SYMLINK at ${_ax_pf_cur}. The release-anchor helper decides the entire" >&2
        echo "  ratchet policy; a link makes the bytes that are SOURCED differ from the bytes git" >&2
        echo "  records for that path, which is the same anchor-side/self-side asymmetry the" >&2
        echo "  P1-Y checks close for the ledger. Replace the link with the file itself." >&2
        exit 2
    fi
done
# ROUND 6 / P2: the validated ABSOLUTE binary, not the bare word (which resolves through PATH).
if [ -n "${AX_GIT_BIN:-}" ] \
   && "$AX_GIT_BIN" --no-replace-objects -C "$SELF_REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    _ax_pf_mode="$("$AX_GIT_BIN" --no-replace-objects -C "$SELF_REPO_ROOT" ls-tree HEAD -- "$_ax_pf_rel" 2>/dev/null | head -1)"
    _ax_pf_mode="${_ax_pf_mode%% *}"
    case "${_ax_pf_mode:-}" in
        100644|100755|"") ;;   # "" = untracked: no committed mode to check, lstat walk above stands
        *)
            echo "evidence_quote_spotcheck_guard: HELPER_PATH_NOT_REGULAR — ${_ax_pf_rel} has git" >&2
            echo "  mode ${_ax_pf_mode} at HEAD, which is not a regular file blob (100644/100755)." >&2
            exit 2 ;;
    esac
fi
# shellcheck source=practices/scripts/lib/release_anchor.sh
# RELOCATED-COPY AFFORDANCE, now gated (P1-3(v)): fixture_kill_proof_guard [87] proves fixture
# non-vacuity by running a MUTATED COPY of this file from a bare temp path, where the
# repo-relative helper does not exist. AX_RELEASE_ANCHOR_LIB names it for THAT case only, and
# the gate is now explicit: the override is consulted ONLY when the committed path is absent AND
# this root is not a git work tree — i.e. exactly the relocated sandbox. On any live tree the
# missing helper is a BLOCK, never an invitation to load the policy from somewhere the attacker
# chose; deleting the committed helper is separately blocking, because it is now on the
# anchor-critical path list below (deletion at the anchor = ANCHOR_BOOTSTRAP_IMPLAUSIBLE).
AX_ANCHOR_LIB="$SELF_REPO_ROOT/practices/scripts/lib/release_anchor.sh"
if [ ! -f "$AX_ANCHOR_LIB" ] \
   && ! "$AX_GIT_BIN" --no-replace-objects -C "$SELF_REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    AX_ANCHOR_LIB="${AX_RELEASE_ANCHOR_LIB:-$AX_ANCHOR_LIB}"
fi
if [ ! -f "$AX_ANCHOR_LIB" ]; then
    echo "evidence_quote_spotcheck_guard: RELEASE_ANCHOR_LIB_MISSING — practices/scripts/lib/release_anchor.sh not" >&2
    echo "  found under $SELF_REPO_ROOT. It carries the anchor resolution, the symlink (P1-Y)" >&2
    echo "  checks and the ancestry/bootstrap authentication (P1-X); without it this guard cannot" >&2
    echo "  run its ratchet at all, so it BLOCKS rather than degrading silently." >&2
    echo "  On a git work tree the AX_RELEASE_ANCHOR_LIB override is NOT consulted: a live root" >&2
    echo "  that has lost its committed helper is a tampered tree, not a sandbox." >&2
    exit 2
fi
. "$AX_ANCHOR_LIB"

# Anchor-critical paths. GUARD SELF is resolved against the REAL repo (it is this file), the
# protected-anchor ledger against the SCANNED root (a fixture ships its own).
ANCHOR_SELF_REL="practices/evals/evidence_quote_spotcheck_guard.sh"
ANCHOR_LEDGER_REL="practices/evals/evidence_protected_template_anchors.txt"
# P1-3(iv): the ratchet's own TOOLCHAIN is anchor-critical too. Until round 4 none of these was
# on any list, so deleting the helper activated the env fallback above and symlinking the hook or
# the runner was invisible to every gate. They live in the REAL repo (never in a fixture root).
ANCHOR_TOOLCHAIN_RELS=(
    "practices/scripts/lib/release_anchor.sh"
    "practices/scripts/lib/tree_fingerprint.py"
    "practices/scripts/verify-completion.sh"
    "practices/evals/completion_checklist_recency_guard.sh"
    ".githooks/pre-push"
    ".githooks/pre-push-lib.sh"
)

ax_anchor_worktree_paths_regular "$SELF_REPO_ROOT" "evidence_quote_spotcheck_guard" \
    "$ANCHOR_SELF_REL" "${ANCHOR_TOOLCHAIN_RELS[@]}" || exit 2
ax_anchor_worktree_paths_regular "${RESOLVED_ROOT:-$REPO_ROOT}" "evidence_quote_spotcheck_guard" \
    "$ANCHOR_LEDGER_REL" || exit 2

GIT_ANCHOR=""
GIT_ANCHOR_KIND="unavailable"
if [ "$LIVE_ROOT" = "1" ]; then
    ax_anchor_check_replace_refs "$SELF_REPO_ROOT" "evidence_quote_spotcheck_guard" \
        || exit "$ANCHOR_AUTH_EXIT"
    ax_anchor_resolve "$SELF_REPO_ROOT"
    ax_anchor_check_pin "evidence_quote_spotcheck_guard" || exit "$ANCHOR_AUTH_EXIT"
    GIT_ANCHOR="$AX_ANCHOR_REF"
    GIT_ANCHOR_KIND="$AX_ANCHOR_KIND"
    if [ -n "$GIT_ANCHOR" ]; then
        ax_anchor_check_ancestry "$SELF_REPO_ROOT" "evidence_quote_spotcheck_guard" \
            || exit "$ANCHOR_AUTH_EXIT"
        ax_anchor_release_paths_regular "$SELF_REPO_ROOT" "evidence_quote_spotcheck_guard" \
            "$ANCHOR_SELF_REL" "$ANCHOR_LEDGER_REL" "${ANCHOR_TOOLCHAIN_RELS[@]}" \
            || exit "$ANCHOR_AUTH_EXIT"
    # ROUND 5 / P1-3 (TD-2026-07-30-P1-hermetic-runtime): the list above checks that these paths
    # are REGULAR FILES, on both sides. It says nothing about what they CONTAIN — and the measured
    # attack is a regular-file tree_fingerprint.py rewritten to print a constant, after which the
    # runner writing the evidence and the gate recomputing it are the same compromised
    # implementation. So the toolchain's working-tree bytes must equal what git records at HEAD.
    ax_ratchet_toolchain_authentic "$SELF_REPO_ROOT" "evidence_quote_spotcheck_guard" "HEAD" \
        $(ax_ratchet_toolchain_paths) || exit "$ANCHOR_AUTH_EXIT"
    fi
fi

# -E, not -I -S: this body imports PyYAML from site-packages (measured: under -I -S the guard
# reported "PyYAML unavailable — cannot run"). -E still refuses PYTHONPATH / PYTHONHOME /
# PYTHONSTARTUP, which is the injection vector; see DECISIONS.md
# TD-2026-07-30-P1-preflight-and-raw-bytes for the honest limit this leaves.
# (The comment lives ABOVE the command on purpose: a comment placed between backslash
# continuations swallows the env-assignment prefix and the body then runs WITHOUT it.)
STRICT="$STRICT" ALLOW_MISSING="$ALLOW_MISSING" INCLUDE_TEMPLATES="$INCLUDE_TEMPLATES" \
STRICT_TEMPLATES="$STRICT_TEMPLATES" TEMPLATES_ONLY_PROTECTED="$TEMPLATES_ONLY_PROTECTED" \
LIVE_ROOT="$LIVE_ROOT" GIT_ANCHOR="$GIT_ANCHOR" GIT_ANCHOR_KIND="$GIT_ANCHOR_KIND" \
GIT_REPO_ROOT="$SELF_REPO_ROOT" "$AX_PY_BIN" -E - "$REPO_ROOT" << 'PY'
import ast, glob, html, os, re, subprocess, sys

root = sys.argv[1]
strict = os.environ.get("STRICT") == "1"
allow_missing = os.environ.get("ALLOW_MISSING") == "1"
include_templates = os.environ.get("INCLUDE_TEMPLATES") == "1"
strict_templates = os.environ.get("STRICT_TEMPLATES") == "1"
protected_only = os.environ.get("TEMPLATES_ONLY_PROTECTED") == "1"
live_root = os.environ.get("LIVE_ROOT") == "1"
anchor = os.environ.get("GIT_ANCHOR") or ""
anchor_kind = os.environ.get("GIT_ANCHOR_KIND") or "unavailable"
git_root = os.environ.get("GIT_REPO_ROOT") or root

# Committed protected-anchor ledger + the floor its declared min_entries may never drop
# below on the real repo tree. BOTH numbers are deliberately duplicated (ledger directive +
# guard constant) so emptying the gate requires two coordinated edits instead of one silent
# deletion. LIVE_MIN_PROTECTED_ENTRIES MAY NOT BE REDUCED.
PROTECTED_LEDGER_REL = os.path.join("practices", "evals",
                                    "evidence_protected_template_anchors.txt")
# 2026-07-29: 2 → 18. Sixteen further anchors were verified disk-clean and promoted to
# FATAL in the ledger. Raising this constant is the OTHER HALF of that ratchet: without it
# the ledger directive could be lowered back to 2 in a single edit and sixteen anchors would
# silently leave the fatal set while the gate still reported green — exactly the shrink the
# two-number duplication exists to prevent. Live protected sweep with all 18: 18 file(s),
# 22 anchor(s), 0 finding(s), exit 0.
#
# BACKLOG P3-93 / PRD-final-4 W1 (2026-07-30): 18 → 64. The wave-start census of the
# advisory templates sweep found 53 findings over 52 unique (path, upstream_id) identities;
# 46 of those (every identity except the 6 recharts-2026-05 ones, whose vendor URLs are
# genuinely dead by static fetch — 5 canonical URLs all HTTP 404, recorded per-URL in
# practices/upstream/_FETCH-RECEIPTS.yaml) were re-verified VERBATIM against freshly
# curl-fetched, deterministically-extracted snapshot bodies and are promoted to FATAL here.
# 18 + 46 = 64, and the 46 are disjoint from the existing 18 (verified by set comparison, not
# assumed: the overlapping FILES — checkbox.tsx, skeleton.tsx, sonner.tsx — carry a DIFFERENT
# upstream_id in each set, and identity is the (path, upstream_id) pair).
LIVE_MIN_PROTECTED_ENTRIES = 64

# Identity pinning (codex round-3). A COUNT is not an identity: min_entries=2 was satisfied
# by duplicating one clean row after deleting the row that actually matters. These exact
# (path, upstream_id) tuples MUST appear in the ledger whenever the real repo tree is
# scanned. Mirrored by `# require:` directives in the ledger itself, so dropping a protected
# identity takes two coordinated edits — the same posture as min_entries.
# LIVE_REQUIRED_PROTECTED_IDENTITIES MAY NOT BE REDUCED.
#
# BACKLOG P2-51 (2026-07-30): 2 → 18, i.e. EVERY protected row is now exact-required. Round-3
# pinned only the two anchors the gate was born for, so the other sixteen (the 2026-07-29
# ratchet) were protected by CARDINALITY ALONE: substitute any one of them for a different
# file's anchor, leave the row count at 18, and both min_entries and the guard-pinned floor
# were still satisfied — the displaced anchor left the fatal set silently while the gate
# reported green, which is the round-3 bypass with a different starting row. Cardinality can
# only detect shrinkage; identity detects SUBSTITUTION, and a ledger whose whole purpose is
# "these specific anchors are verified" has no row for which substitution is legitimate.
LIVE_REQUIRED_PROTECTED_IDENTITIES = frozenset({
    # The anchor P2-40 found FABRICATED — the RED-on-revert subject of this whole gate.
    ("templates/L1/components/currency-input.tsx", "stripe-billing-2026-05"),
    # Positive control — an already-correct anchor, so the gate is proven to pass for an
    # honest citation and not merely to fail for a dishonest one.
    ("templates/L1/components/currency-formatter.tsx", "stripe-billing-2026-05"),
    # ── P2-51: the 2026-07-29 ratchet's sixteen, promoted from cardinality-only to exact ──
    ("templates/L1/components/checkbox.tsx", "wcag-2-2"),
    ("templates/L1/components/currency-formatter.tsx", "next-intl-2026-05"),
    ("templates/L1/components/locale-switcher.tsx", "next-intl-2026-05"),
    ("templates/L1/components/markdown-renderer.tsx", "remark-2026-05"),
    ("templates/L1/components/relative-time.tsx", "next-intl-2026-05"),
    ("templates/L1/components/rich-text-editor.tsx", "tiptap-2026-05"),
    ("templates/L1/components/skeleton.tsx", "wcag-22-techniques-2026-05"),
    ("templates/L1/components/sonner.tsx", "wcag-22-techniques-2026-05"),
    ("templates/L1/lib/utils.ts", "shadcn-ui-2026-05"),
    ("templates/L2/blocks/error-boundary.tsx", "react-19-error-boundary"),
    ("templates/L2/blocks/locale-provider.tsx", "next-intl-2026-05"),
    ("templates/L2/blocks/notification-list.tsx", "tanstack-virtual-2026-05"),
    ("templates/L2/blocks/offline-banner.tsx", "mdn-navigator-online-2026-05"),
    ("templates/L2/blocks/translation-boundary.tsx", "next-intl-2026-05"),
    ("templates/L2/blocks/translation-boundary.tsx", "react-19-error-boundary"),
    ("templates/L2/blocks/virtualized-table.tsx", "tanstack-virtual-2026-05"),
    # ── P3-93 / PRD-final-4 W1 (2026-07-30): the 46 identities the full-refresh made
    # verbatim-clean, promoted from ADVISORY to FATAL. Each one is a (path, upstream_id)
    # whose quote was re-verified as a literal substring of a snapshot body assembled by
    # practices/scripts/snapshot-extract.sh from a live curl fetch (per-URL receipts in
    # practices/upstream/_FETCH-RECEIPTS.yaml). Nineteen of the 46 citations were RE-ANCHORED
    # rather than confirmed — the snapshot was never doctored to fit a citation.
    ("templates/L1/components/accordion.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/address-search.tsx", "kakao-postcode-2026-05"),
    ("templates/L1/components/alert-dialog.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/alert.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/aspect-ratio.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/avatar.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/badge.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/button.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/calendar.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/card.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/checkbox.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/collapsible.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/combobox.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/command.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/date-picker.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/date-range-picker.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/dialog.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/dropdown-menu.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/file-dropzone.tsx", "react-dropzone-2026-05"),
    ("templates/L1/components/form.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/hover-card.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/input.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/label.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/otp-input.tsx", "input-otp-2026-05"),
    ("templates/L1/components/otp-input.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/popover.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/progress.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/radio-group.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/resizable.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/scroll-area.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/select.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/separator.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/sheet.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/skeleton.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/slider.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/sonner.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/switch.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/tabs.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/textarea.tsx", "shadcn-ui-2026-05"),
    ("templates/L1/components/tooltip.tsx", "shadcn-ui-2026-05"),
    ("templates/L2/blocks/billing-history.tsx", "stripe-billing-2026-05"),
    ("templates/L2/blocks/invoice-list.tsx", "stripe-billing-2026-05"),
    ("templates/L2/blocks/pricing-table.tsx", "stripe-billing-2026-05"),
    ("templates/L2/blocks/theme-switcher.tsx", "next-themes-2026-05"),
    ("templates/L2/blocks/toast-queue.tsx", "shadcn-registry-2026-05"),
    ("templates/L2/blocks/toast.tsx", "shadcn-registry-2026-05"),
})
# FIVE-SURFACE EQUALITY CENSUS (PRD-final-4 C3, 2026-07-30). Until now this was a
# ONE-DIRECTIONAL `<=`: the pinned set merely had to not EXCEED the floor. That permitted the
# "upward half-move" — raise LIVE_MIN_PROTECTED_ENTRIES and the ledger's `# min_entries:` to
# 64 while adding only some of the tuples to the frozenset and only some of the `# require:`
# directives — and every check still passed, because a floor is satisfied by any larger row
# count and `<=` is satisfied by any smaller pin set. The ratchet is supposed to move as FIVE
# HALVES AT ONCE; a gate that accepts four of the five moving is a gate that lets the fifth
# stay behind silently.
#
# So the five surfaces are now census-compared for EQUALITY, and the comparison EXECUTES on
# every live run (see load_protected_ledger) rather than being asserted in prose:
#     1. ledger rows                        (unique (path, upstream_id) identities on disk)
#     2. distinct `# require:` directives    (the ledger's own identity pins)
#     3. `# min_entries:` directive          (the ledger's declared floor)
#     4. len(LIVE_REQUIRED_PROTECTED_IDENTITIES)  (the guard's identity pins)
#     5. LIVE_MIN_PROTECTED_ENTRIES          (the guard's declared floor)
# Any inequality is PROTECTED_LEDGER_CENSUS_UNEQUAL, exit 3 — a code DISTINCT from findings
# (1) and from the other structural ledger defects (2), so "the ratchet halves disagree" can
# never be read as "a quote is fabricated" or vice versa.
#
# The two guard-side surfaces (4, 5) are constants, so their equality is checked here at
# import time for EVERY root, live or fixture: a fixture root declares its own min_entries but
# cannot make the guard's own two numbers disagree with each other.
CENSUS_EXIT = 3   # distinct from 1 (findings) and 2 (other structural ledger defects)
if len(LIVE_REQUIRED_PROTECTED_IDENTITIES) != LIVE_MIN_PROTECTED_ENTRIES:
    print("evidence_quote_spotcheck_guard: PROTECTED_LEDGER_CENSUS_UNEQUAL — the guard's own "
          "two halves disagree: len(LIVE_REQUIRED_PROTECTED_IDENTITIES)="
          f"{len(LIVE_REQUIRED_PROTECTED_IDENTITIES)} != LIVE_MIN_PROTECTED_ENTRIES="
          f"{LIVE_MIN_PROTECTED_ENTRIES}. Every protected row is exact-required (P2-51), so "
          "the pin set and the floor are the same number by construction; a difference means "
          "one half of a ratchet was moved without the other.", file=sys.stderr)
    sys.exit(CENSUS_EXIT)

# ── ANCHOR RATCHET, EXECUTED (P1-1) ──────────────────────────────────────────────────
# See the ANCHOR RATCHET header block. Two exit codes, distinct from findings (1), other
# structural ledger defects (2) and the in-tree census (3), so "the ratchet was rolled back
# across releases" is never readable as any of those.
FLOOR_REGRESSION_EXIT = 4     # MONOTONIC_FLOOR_REGRESSION
IDENTITY_REMOVED_EXIT = 5     # PROTECTED_IDENTITY_REMOVED
SELF_UNPARSEABLE_EXIT = 6     # SELF_UNPARSEABLE — generation-N representation laundering (P1-A)
ANCHOR_UNPARSEABLE_EXIT = 7   # ANCHOR_UNPARSEABLE — anchor state (iii): present but unreadable
GUARD_SELF_REL = "practices/evals/evidence_quote_spotcheck_guard.sh"


def anchor_blob(rel):
    """Bytes of `rel` as of the release anchor, or None when that path did not exist there."""
    proc = subprocess.run(["git", "-C", git_root, "show", f"{anchor}:{rel}"],
                          stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    return proc.stdout if proc.returncode == 0 else None


def _string_pair_tuples(node):
    """Every 2-element all-string-constant tuple anywhere under `node`. Deliberately shape-
    tolerant: frozenset({...}) / frozenset([...]) / a bare set literal / reordered / reflowed /
    comment-interleaved all yield the same identity SET, so only DELETING a tuple registers."""
    out = set()
    for sub in ast.walk(node):
        if not isinstance(sub, ast.Tuple) or len(sub.elts) != 2:
            continue
        vals = [e.value for e in sub.elts
                if isinstance(e, ast.Constant) and isinstance(e.value, str)]
        if len(vals) == 2:
            out.add((vals[0], vals[1]))
    return out


# The here-document opener is matched by PATTERN, not by one literal spelling, so the several
# equivalent shell spellings of it are all located. This is deliberately RELAXING: it can only
# make extraction succeed where it used to fail, and any spelling it still cannot locate is
# caught fail-closed by the self-parse check (exit 6) rather than skipped.
PY_HEREDOC_RE = re.compile(r"<<-?[ \t]*(['\"]?)PY\1[ \t]*\n")


def _embedded_python(src_text):
    """The embedded python block of a copy of this guard (or the whole text when the opener
    cannot be located — which then fails ast.parse, which is the fail-closed outcome)."""
    m = PY_HEREDOC_RE.search(src_text)
    body = src_text[m.end():] if m else src_text
    end = body.rfind("\nPY\n")
    if end != -1:
        body = body[:end]
    return body


def parse_pins(src_text):
    """(floor, identities, error) as DECLARED BY a copy of this guard — the ANCHOR copy for the
    cross-release ratchet, and the CURRENT copy for the self-parse check (same parser on purpose:
    a representation this cannot read must not be shippable). A None element means that constant
    is not extractable as literals from that copy."""
    body = _embedded_python(src_text)
    try:
        tree = ast.parse(body)
    except SyntaxError as exc:
        return None, None, f"python block does not parse ({exc})"
    floor = ids = None
    for node in ast.walk(tree):
        if not isinstance(node, ast.Assign):
            continue
        names = [t.id for t in node.targets if isinstance(t, ast.Name)]
        if "LIVE_MIN_PROTECTED_ENTRIES" in names and isinstance(node.value, ast.Constant) \
                and isinstance(node.value.value, int):
            floor = node.value.value
        if "LIVE_REQUIRED_PROTECTED_IDENTITIES" in names:
            found = _string_pair_tuples(node.value)
            if found:
                ids = found
    return floor, ids, None


def self_parse_check():
    """P1-A layer (1): the CURRENT file must be readable by the SAME parser the anchor comparison
    uses, and the parse must reproduce the RUNTIME values. This is the generation-N kill for the
    two-release representation laundering: a copy of this guard that a future release could not
    read is refused HERE, before it can become anybody's anchor. Runs on every live-root run,
    with or without a resolvable git anchor — a tree that cannot be ratcheted against is still a
    tree that must not ship an unreadable representation."""
    self_path = os.path.join(git_root, GUARD_SELF_REL)
    try:
        self_src = open(self_path, encoding="utf-8", errors="replace").read()
    except OSError as exc:
        print("evidence_quote_spotcheck_guard: SELF_UNPARSEABLE — cannot read this guard's own "
              f"source at {GUARD_SELF_REL} ({exc}); the self-parse check is the generation-N kill "
              "for representation laundering and may not be skipped on the live tree.",
              file=sys.stderr)
        sys.exit(SELF_UNPARSEABLE_EXIT)
    s_floor, s_ids, s_err = parse_pins(self_src)
    problems = []
    if s_err:
        problems.append(f"the embedded python block of the CURRENT file {s_err}")
    if s_floor is None:
        problems.append("LIVE_MIN_PROTECTED_ENTRIES is not extractable as a literal int "
                        "assignment (e.g. it is a call, a name, or an expression)")
    elif s_floor != LIVE_MIN_PROTECTED_ENTRIES:
        problems.append(f"the parsed LIVE_MIN_PROTECTED_ENTRIES literal ({s_floor}) is not the "
                        f"RUNTIME value ({LIVE_MIN_PROTECTED_ENTRIES}) — a decoy or shadowing "
                        "assignment makes the parser and the program disagree")
    if s_ids is None:
        problems.append("LIVE_REQUIRED_PROTECTED_IDENTITIES yields no literal (path, upstream_id) "
                        "tuples (e.g. it is aliased to another name or built at runtime)")
    elif s_ids != set(LIVE_REQUIRED_PROTECTED_IDENTITIES):
        only_parsed = sorted(s_ids - set(LIVE_REQUIRED_PROTECTED_IDENTITIES))
        only_runtime = sorted(set(LIVE_REQUIRED_PROTECTED_IDENTITIES) - s_ids)
        problems.append("the parsed LIVE_REQUIRED_PROTECTED_IDENTITIES tuples are not the RUNTIME "
                        f"set ({len(only_parsed)} parsed-only, {len(only_runtime)} runtime-only; "
                        f"first parsed-only {only_parsed[:1]}, first runtime-only {only_runtime[:1]})")
    if problems:
        print("evidence_quote_spotcheck_guard: SELF_UNPARSEABLE — this file cannot be read by its "
              "OWN anchor parser, or reads differently than it runs:", file=sys.stderr)
        for p in problems:
            print(f"    · {p}", file=sys.stderr)
        print("  A release that ships in this shape becomes the NEXT release's anchor, and the "
              "cross-release ratchet would then find (None, None) and skip — which is the "
              "two-release representation-laundering bypass (change representation only while the "
              "runtime numbers stay 64/64, ship green, THEN downgrade). Keep both constants as "
              "literals the parser recognizes: `LIVE_MIN_PROTECTED_ENTRIES = <int>` and a "
              "frozenset/set of literal 2-string tuples.", file=sys.stderr)
        sys.exit(SELF_UNPARSEABLE_EXIT)


if live_root:
    self_parse_check()
    if not anchor:
        print("evidence_quote_spotcheck_guard: WARN ANCHOR_UNAVAILABLE — no git anchor "
              "(origin/main or HEAD) could be resolved, so the cross-release ratchet checks "
              "(MONOTONIC_FLOOR_REGRESSION / PROTECTED_IDENTITY_REMOVED) are SKIPPED. The "
              "in-tree five-surface census still ran. A tree with no git history cannot be "
              "pushed either (the pre-push recency guard is a git hook), so the released path "
              "keeps the ratchet.", file=sys.stderr)
    else:
        if anchor_kind != "origin/main":
            print(f"evidence_quote_spotcheck_guard: WARN ANCHOR_FALLBACK — ratcheting against "
                  f"{anchor_kind}, not origin/main. Weaker by construction: a downgrade that is "
                  "already committed locally is present in the anchor itself.", file=sys.stderr)
        prior_src = anchor_blob(GUARD_SELF_REL)
        if prior_src is None:
            print(f"evidence_quote_spotcheck_guard: WARN ANCHOR_GUARD_ABSENT — {GUARD_SELF_REL} "
                  f"does not exist at {anchor_kind}; nothing to ratchet against (first-release "
                  "bootstrap). Skipped. This skip is REACHABLE ONLY for a genuinely new path: "
                  "the shell preamble has already required (ANCHOR_BOOTSTRAP_IMPLAUSIBLE, exit 8) "
                  "that the anchor's own history never contained it, and (ANCHOR_NOT_ANCESTOR) "
                  "that the anchor is an ancestor of HEAD — so a forged refs/remotes/origin/main "
                  "aimed at a tree that merely DROPS this file cannot land here.",
                  file=sys.stderr)
        else:
            prior_floor, prior_ids, perr = parse_pins(
                prior_src.decode("utf-8", errors="replace"))
            # ── ANCHOR STATE (iii): PRESENT BUT UNREADABLE ⇒ BLOCKING (P1-A layer 2) ──
            # States (i) anchor/git unavailable and (ii) file absent at the anchor are handled
            # ABOVE and keep their WARN+skip: (i) has nothing to compare and is unpushable, (ii)
            # is git history the working tree cannot author. State (iii) is different in kind —
            # it is the state a release can DELIBERATELY manufacture by shipping an unparseable
            # representation of its own constants while leaving the runtime numbers untouched,
            # so that its successor's ratchet degrades to a skip. Layer (1) (self_parse_check)
            # prevents that shape from shipping at all; this is the belt-and-braces for anchors
            # committed BEFORE layer (1) existed, and it is why "no floor"/"no pin set" are
            # fatal here rather than treated as a bootstrap.
            unreadable = []
            if perr:
                unreadable.append(f"ANCHOR_UNPARSEABLE: {perr}")
            else:
                if prior_floor is None:
                    unreadable.append("ANCHOR_NO_FLOOR: the anchor copy declares no "
                                      "LIVE_MIN_PROTECTED_ENTRIES extractable as a literal int")
                if prior_ids is None:
                    unreadable.append("ANCHOR_NO_PIN_SET: the anchor copy yields no literal "
                                      "(path, upstream_id) tuples for "
                                      "LIVE_REQUIRED_PROTECTED_IDENTITIES")
            if unreadable:
                print("evidence_quote_spotcheck_guard: ANCHOR_UNPARSEABLE — "
                      f"{anchor_kind}:{GUARD_SELF_REL} EXISTS but its ratchet constants cannot be "
                      "read:", file=sys.stderr)
                for u in unreadable:
                    print(f"    · {u}", file=sys.stderr)
                print("  This is fail-closed on purpose. A skip here is the second half of the "
                      "two-release representation-laundering bypass: release N changes only the "
                      "REPRESENTATION of these constants (runtime values unchanged, so it ships "
                      "green), and release N+1 then does the real downgrade against an anchor its "
                      "own parser cannot read. Restore the anchor-side constants to literals, or "
                      "re-anchor onto a release that carries them.", file=sys.stderr)
                sys.exit(ANCHOR_UNPARSEABLE_EXIT)
            else:
                if LIVE_MIN_PROTECTED_ENTRIES < prior_floor:
                    print("evidence_quote_spotcheck_guard: MONOTONIC_FLOOR_REGRESSION — "
                          f"LIVE_MIN_PROTECTED_ENTRIES={LIVE_MIN_PROTECTED_ENTRIES} is BELOW the "
                          f"previous release's {prior_floor} (anchor {anchor_kind}:"
                          f"{GUARD_SELF_REL}). The floor is a RATCHET: it may rise, never fall. "
                          "Lowering the in-tree census surfaces coherently no longer helps — the "
                          "reference is the released copy, which the commit under review cannot "
                          "edit.", file=sys.stderr)
                    sys.exit(FLOOR_REGRESSION_EXIT)
                # Both constants are non-None from here on: the unreadable branch above is fatal,
                # so there is no longer a "skip that half" path for either of them (that skip WAS
                # the second half of the two-release laundering bypass).
                removed = sorted(prior_ids - set(LIVE_REQUIRED_PROTECTED_IDENTITIES))
                if removed:
                    print("evidence_quote_spotcheck_guard: PROTECTED_IDENTITY_REMOVED — "
                          f"{len(removed)} identity(ies) pinned by the previous release are "
                          "absent from LIVE_REQUIRED_PROTECTED_IDENTITIES:", file=sys.stderr)
                    for r, u in removed:
                        print(f"    {r}::{u}", file=sys.stderr)
                    print("  The pin set may only GROW. A removal (or a swap that keeps the "
                          "count) drops an anchor out of the fatal set, which is the whole "
                          "bypass this ratchet exists to make impossible.", file=sys.stderr)
                    sys.exit(IDENTITY_REMOVED_EXIT)
                print("evidence_quote_spotcheck_guard: anchor ratchet OK — floor "
                      f"{prior_floor} → {LIVE_MIN_PROTECTED_ENTRIES}, pin set "
                      f"{len(prior_ids)} → {len(LIVE_REQUIRED_PROTECTED_IDENTITIES)} "
                      f"(superset) vs {anchor_kind}; self-parse OK")

# A protected quote must carry substance. `quote: 0` is closed by the isinstance check, but
# `quote: "a"` is a genuine non-empty string that is still a substring of essentially every
# snapshot body — the same vacuous-substring attack with a valid type. Normalized length
# floor; the two live protected quotes are ~110 chars.
MIN_PROTECTED_QUOTE_CHARS = 24

def die_structural(msg):
    print(f"evidence_quote_spotcheck_guard: {msg}", file=sys.stderr)
    sys.exit(2)

def normalize(s):
    s = html.unescape(s)
    s = (s.replace('‘', "'").replace('’', "'")
          .replace('“', '"').replace('”', '"')
          .replace('—', '-').replace('–', '-')
          .replace('…', '...').replace(' ', ' '))
    # MARKDOWN CARRIAGE (P3-69 normalizer fix, 2026-07-29). An evidence `quote` is PROSE;
    # the snapshot stores that same prose inside MARKDOWN. Two markdown constructs inject
    # characters that belong to the container, not to the sentence, and a verbatim citation
    # therefore cannot reproduce them:
    #   · blockquote continuation — a multi-line `> ` block repeats the marker on EVERY
    #     line, so after whitespace collapse the body reads "... fallback UI instead > of
    #     the component tree ...". Demonstrated true-positive-that-should-pass:
    #     templates/L2/blocks/translation-boundary.tsx cites react-19-error-boundary.
    #     snapshot.md:18-20 verbatim and was reported as a mismatch purely because of the
    #     two `> ` continuations inside the quoted block.
    #   · inline code spans — the snapshot writes `getVirtualItems()` with backticks; the
    #     citing prose writes the same token bare.
    # Both are applied to BOTH SIDES of the comparison (the function is the single
    # normalizer for quote, section and snapshot body), and both are strictly RELAXING:
    # they can only turn a mismatch into a match, never the reverse, so no previously
    # detected fabrication can be hidden by this change (the rules/ sweep stays at 0
    # findings across 185 quotes, verified). The blockquote strip is line-anchored and MUST
    # run before the `\s+` collapse below, or the markers are already mid-line by then.
    s = re.sub(r'(?m)^[ \t]*>[ \t]?', '', s)
    s = s.replace('`', '')
    return re.sub(r'\s+', ' ', s).strip()

def strip_html(s):
    s = re.sub(r'<script\b.*?</script>', ' ', s, flags=re.S | re.I)
    s = re.sub(r'<style\b.*?</style>', ' ', s, flags=re.S | re.I)
    return re.sub(r'<[^>]*>', ' ', s)

try:
    import yaml
except ImportError:
    print("evidence_quote_spotcheck_guard: PyYAML unavailable — cannot run", file=sys.stderr)
    sys.exit(2)

snap_cache = {}

def load_snapshot(catalog, uid):
    """Returns (snap_path or None, normalized_text or None)."""
    snap = os.path.join(root, catalog, "upstream", uid + ".snapshot.md")
    if not os.path.isfile(snap):
        return None, None
    if snap not in snap_cache:
        snap_cache[snap] = normalize(strip_html(open(snap, errors="replace").read()))
    return snap, snap_cache[snap]

def resolve_snapshot_any_catalog(uid):
    """A templates/** citation isn't scoped to one catalog — try practices, then
    practices-react. Returns (catalog_or_None, snap_text_or_None)."""
    for catalog in ("practices", "practices-react"):
        snap, text = load_snapshot(catalog, uid)
        if snap is not None:
            return catalog, text
    return None, None

# ── PROSE-PRESENCE PASS (PRD-final-4 C1, 2026-07-30) ─────────────────────────────────
# A snapshot body is authored, and its SECTION MARKERS are markdown ATX headings that the
# author writes to carry the exact `section:` names the citing templates declare (the guard
# checks `section` fatally against the body, so the headings must contain those names — that
# is by design, "headings MAY be authored to carry cited section names").
#
# That creates a hole the section check itself cannot close: an author under pressure to make
# a `quote` resolve can put the quote's TEXT INTO A HEADING. The whole-body substring check
# then passes while nothing in the actual fetched PROSE says it — the citation is verified
# against the citer's own table of contents. PROSE MAY NOT BE AUTHORED; only headings may.
#
# So for a PROTECTED anchor the quote must additionally occur inside at least one NON-HEADING
# region. Implementation: split the raw body on heading lines (`^#{1,6}<space>`, the shape the
# authored snapshots use — see practices-react/upstream/shadcn-ui-2026-05.snapshot.md's
# `### <slug>` per-component sections), DROP the heading lines, and normalize each remaining
# run of lines as its own block. Quote must be a substring of >= 1 block.
#
# Why blocks and not "body with heading lines deleted, joined": a wrapped sentence legitimately
# spans several consecutive prose lines, so per-LINE matching would produce false findings; but
# joining ACROSS a dropped heading would let a quote match by straddling two unrelated
# paragraphs. Splitting at headings and matching within a block accepts the first and rejects
# the second.
HEADING_LINE_RE = re.compile(r'^[ \t]*#{1,6}[ \t]')
block_cache = {}

def snapshot_prose_blocks(catalog, uid):
    """Normalized non-heading blocks of {catalog}/upstream/{uid}.snapshot.md."""
    snap = os.path.join(root, catalog, "upstream", uid + ".snapshot.md")
    if snap not in block_cache:
        blocks, current = [], []
        for line in open(snap, errors="replace").read().splitlines():
            if HEADING_LINE_RE.match(line):
                if current:
                    blocks.append("\n".join(current))
                    current = []
            else:
                current.append(line)
        if current:
            blocks.append("\n".join(current))
        block_cache[snap] = [normalize(strip_html(b)) for b in blocks]
    return block_cache[snap]

quotes = 0
scanned_rules = 0
misses = []
for catalog in ("practices", "practices-react"):
    rules_glob = os.path.join(root, catalog, "rules", "*.md")
    for rule_path in sorted(glob.glob(rules_glob)):
        if os.path.basename(rule_path) == "_template.md":
            continue
        text = open(rule_path, errors="replace").read()
        m = re.match(r'^---\n(.*?)\n---\n', text, re.S)
        if not m:
            continue
        try:
            fm = yaml.safe_load(m.group(1))
        except Exception:
            continue
        if not isinstance(fm, dict):
            continue
        scanned_rules += 1
        for entry in (fm.get("evidence") or []):
            if not isinstance(entry, dict) or "upstream_id" not in entry:
                continue
            quotes += 1
            rel = os.path.relpath(rule_path, root)
            # Same coercion class the protected sweep closes structurally (codex round-3),
            # applied to the fatal live rules sweep as FINDINGS: `str()` used to manufacture
            # a passing value out of a non-string (`0` → "0", a real substring) or out of an
            # absent key ("" → a substring of every snapshot). Neither is coerced now. The
            # protected-set length floor is deliberately NOT applied here — 48 legitimate
            # short single-token rule quotes exist and re-anchoring them is a separate,
            # disclosed backlog item, not something to smuggle in under this gate.
            uid_raw = entry["upstream_id"]
            quote_raw = entry.get("quote", None)
            if not isinstance(uid_raw, str):
                misses.append((rel, repr(uid_raw), "UPSTREAM_ID_NOT_A_STRING", ""))
                continue
            uid = uid_raw
            if not isinstance(quote_raw, str):
                misses.append((rel, uid, "QUOTE_MISSING_OR_NOT_A_STRING",
                               type(quote_raw).__name__))
                continue
            quote = quote_raw
            if not normalize(quote):
                misses.append((rel, uid, "QUOTE_BLANK", ""))
                continue
            snap, snap_text = load_snapshot(catalog, uid)
            if snap is None:
                misses.append((rel, uid, "SNAPSHOT_FILE_MISSING", ""))
                continue
            if normalize(quote) not in snap_text:
                misses.append((rel, uid, "QUOTE_NOT_IN_SNAPSHOT", quote[:90]))

# ── templates/**/*.{tsx,ts} sweep (BACKLOG P2-40) ────────────────────────────
# Separate bucket — see --strict-templates header comment for why these do NOT
# join `misses` (the rules-sweep fatal-exit pool) unconditionally.
template_quotes = 0
template_scanned = 0
template_misses = []

def parse_template_frontmatter(path):
    """Leading `/*\\n---\\n<yaml>\\n---\\n*/` block as a dict, or None. Shared by the full
    sweep and the protected-ledger sweep so the two can never drift apart."""
    text = open(path, errors="replace").read()
    m = re.match(r'/\*\n---\n(.*?)\n---\n\*/', text, re.S)
    if not m:
        return None
    try:
        fm = yaml.safe_load(m.group(1))
    except Exception:
        return None
    return fm if isinstance(fm, dict) else None

def upstream_evidence_entries(fm):
    return [e for e in (fm.get("evidence") or [])
            if isinstance(e, dict) and "upstream_id" in e]

def check_template_anchor(rel, uid, quote, section=None, protected=False):
    """Records a finding for one (file, upstream_id) citation. Returns nothing.

    protected=True (ledger sweep) tightens two things over the advisory full sweep:
      · a snapshot body that does not exist is a STRUCTURAL failure (exit 2), not a finding
        that only bites under --strict-templates — otherwise deleting the snapshot is a
        cheaper bypass than falsifying the quote;
      · the declared `section` must itself occur in the snapshot body, so a fabricated
        section is verified text rather than unchecked prose;
      · the quote must occur in the body's PROSE, not only inside a heading line — see the
        snapshot_prose_blocks() block comment (QUOTE_ONLY_IN_HEADING)."""
    found_catalog, snap_text = resolve_snapshot_any_catalog(uid)
    if found_catalog is None:
        if protected:
            die_structural(f"PROTECTED_LEDGER_SNAPSHOT_MISSING — {rel}::{uid} resolves to no "
                           "snapshot body under practices/upstream or practices-react/upstream; "
                           "a protected anchor with no snapshot verifies nothing")
        template_misses.append((rel, uid, "TEMPLATE_SNAPSHOT_FILE_MISSING", ""))
        return
    quote_in_body = normalize(quote) in snap_text
    if not quote_in_body:
        template_misses.append((rel, uid, "TEMPLATE_QUOTE_NOT_IN_SNAPSHOT", quote[:90]))
    elif protected and not any(normalize(quote) in block
                               for block in snapshot_prose_blocks(found_catalog, uid)):
        # Reported only when the quote DOES resolve against the whole body: a quote absent
        # everywhere is already TEMPLATE_QUOTE_NOT_IN_SNAPSHOT, and emitting both for one
        # defect would double-count the finding total the acceptance matrix reads.
        template_misses.append((rel, uid, "TEMPLATE_QUOTE_ONLY_IN_HEADING", quote[:90]))
    if section is not None and normalize(section) not in snap_text:
        template_misses.append((rel, uid, "TEMPLATE_SECTION_NOT_IN_SNAPSHOT", section[:90]))

def require_protected_str(rel, uid, entry, field):
    """Type-strict scalar extraction for a PROTECTED anchor. Returns the value only when it
    is a genuine YAML string; every other shape (missing / null / int / float / bool / list /
    dict) is a structural ledger defect with its own reason code.

    This exists because `str(entry.get(field, ""))` silently manufactured a passing value out
    of a non-string: `quote: 0` → "0" (a literal substring of the Stripe snapshot body) and
    `section: null` → "None" (non-blank, so the blank check never fired). Coercion is the
    bypass; refusing to coerce is the fix. (YAML `true`/`0`/`1.5` parse to bool/int/float, and
    none of those is a `str`, so every unquoted scalar lands in the non-string branch.)"""
    upper = field.upper()
    if field not in entry:
        die_structural(f"PROTECTED_LEDGER_MISSING_{upper} — {rel}::{uid} declares no `{field}` "
                       f"key; a protected anchor without a {field} verifies nothing")
    value = entry[field]
    if not isinstance(value, str):
        die_structural(f"PROTECTED_LEDGER_NON_STRING_{upper} — {rel}::{uid} `{field}` is "
                       f"{type(value).__name__} ({value!r}), not a string. Protected scalars "
                       "are never str()-coerced: `0` would become \"0\" (a real substring of a "
                       "snapshot body) and `null` would become \"None\" (non-blank), both of "
                       "which pass a check they should fail.")
    return value

def safe_ledger_path(rel, line):
    """A ledger path is an IDENTITY component, so it must have exactly one spelling and it
    must stay inside the scanned root. Absolute paths, `..` traversal, backslashes and `.`
    segments are rejected rather than normalized — normalizing would let one identity be
    written two ways and defeat the required-identity set comparison."""
    if os.path.isabs(rel) or rel.startswith("/") or "\\" in rel:
        die_structural(f"PROTECTED_LEDGER_UNSAFE_PATH — {line!r} (path must be repo-relative)")
    parts = rel.split("/")
    if any(p in ("", ".", "..") for p in parts):
        die_structural(f"PROTECTED_LEDGER_UNSAFE_PATH — {line!r} "
                       "(no empty, '.' or '..' path segments; an identity has one spelling)")
    return rel

def load_protected_ledger():
    """Parses the committed protected-anchor ledger into an ORDERED SET of required
    identities. EVERY degenerate shape exits 2 rather than yielding an empty (vacuously
    passing) protected set — see the --templates-only-protected header block for the
    enumeration. Identity semantics (round-3): duplicates rejected, min_entries compared
    against the UNIQUE count, and the required-identity set (guard-pinned on the live tree +
    `# require:` directives anywhere) must be fully present."""
    path = os.path.join(root, PROTECTED_LEDGER_REL)
    if not os.path.isfile(path):
        die_structural(f"PROTECTED_LEDGER_MISSING — {PROTECTED_LEDGER_REL} not found under {root}")
    declared_min = None
    entries = []          # unique, in file order
    seen = set()
    declared_required = set()
    for raw in open(path, errors="replace").read().splitlines():
        line = raw.strip()
        if not line:
            continue
        if line.startswith("#"):
            m = re.match(r'#\s*min_entries:\s*(\d+)\s*$', line)
            if m:
                if declared_min is not None:
                    die_structural("PROTECTED_LEDGER_DUPLICATE_MIN_ENTRIES — exactly one "
                                   "`# min_entries: N` directive is allowed")
                declared_min = int(m.group(1))
                continue
            m = re.match(r'#\s*require:\s*(\S.*?)\s*$', line)
            if m:
                spec = m.group(1)
                if "::" not in spec:
                    die_structural(f"PROTECTED_LEDGER_MALFORMED_REQUIRE — {line!r} "
                                   "(expected `# require: <path>::<upstream_id>`)")
                rrel, ruid = (part.strip() for part in spec.split("::", 1))
                if not rrel or not ruid:
                    die_structural(f"PROTECTED_LEDGER_MALFORMED_REQUIRE — {line!r} "
                                   "(expected `# require: <path>::<upstream_id>`)")
                declared_required.add((safe_ledger_path(rrel, line), ruid))
            continue
        if "::" not in line:
            die_structural(f"PROTECTED_LEDGER_MALFORMED_ENTRY — {line!r} "
                           "(expected <path>::<upstream_id>)")
        rel, uid = (part.strip() for part in line.split("::", 1))
        if not rel or not uid:
            die_structural(f"PROTECTED_LEDGER_MALFORMED_ENTRY — {line!r} "
                           "(expected <path>::<upstream_id>)")
        identity = (safe_ledger_path(rel, line), uid)
        if identity in seen:
            # Round-3 bypass: delete the row that matters, duplicate a clean row, keep the
            # count. A repeated identity inflates min_entries while shrinking the protected
            # SET — it is a ledger defect, never a legitimate shape.
            die_structural(f"PROTECTED_LEDGER_DUPLICATE_IDENTITY — {rel}::{uid} is listed more "
                           "than once; min_entries counts unique identities, and duplicating a "
                           "clean anchor is how a protected anchor gets silently dropped")
        seen.add(identity)
        entries.append(identity)
    if declared_min is None:
        die_structural(f"PROTECTED_LEDGER_NO_MIN_ENTRIES — {PROTECTED_LEDGER_REL} must declare "
                       "`# min_entries: N` (the count that may not be reduced)")
    if not entries:
        die_structural(f"PROTECTED_LEDGER_EMPTY — {PROTECTED_LEDGER_REL} declares no anchors; "
                       "an emptied ledger is a silenced gate, not a clean tree")
    if len(entries) < declared_min:
        die_structural(f"PROTECTED_LEDGER_SHRUNK — {len(entries)} unique anchor(s) < declared "
                       f"min_entries={declared_min}")
    if live_root and declared_min < LIVE_MIN_PROTECTED_ENTRIES:
        die_structural(f"PROTECTED_LEDGER_FLOOR — declared min_entries={declared_min} is below the "
                       f"guard-pinned live floor {LIVE_MIN_PROTECTED_ENTRIES} "
                       "(LIVE_MIN_PROTECTED_ENTRIES may not be reduced)")

    # ── FIVE-SURFACE EQUALITY CENSUS, EXECUTED (PRD-final-4 C3) ──────────────────────
    # The two checks immediately above are the ORIGINAL one-directional pair: rows >= min,
    # min >= guard floor. Both are satisfied by a ledger that is LARGER than the ratchet
    # declares, and neither says anything about whether the two identity-pin surfaces
    # (`# require:` directives and the guard frozenset) moved with the numbers. So on the live
    # tree all five surfaces are now compared for EQUALITY, here, on every run — not asserted
    # in a comment and not verified by a count grep in an acceptance matrix that a future
    # editor may not run. Exit 3 (CENSUS_EXIT), distinct from findings (1) and from the other
    # structural ledger defects (2).
    #
    # FIXTURE ROOTS ARE DELIBERATELY EXEMPT and keep exactly today's gating (rows >= declared
    # min, no guard-pinned floor, no census): a fixture exists to isolate ONE failure mode, so
    # a 1-row fixture ledger must stay legal. The census is a property of the live ratchet, and
    # the live tree is where the ratchet can be half-moved.
    if live_root:
        census = {
            "ledger rows (unique identities)": len(entries),
            "distinct `# require:` directives": len(declared_required),
            "ledger `# min_entries:` directive": declared_min,
            "len(LIVE_REQUIRED_PROTECTED_IDENTITIES)": len(LIVE_REQUIRED_PROTECTED_IDENTITIES),
            "LIVE_MIN_PROTECTED_ENTRIES": LIVE_MIN_PROTECTED_ENTRIES,
        }
        if len(set(census.values())) != 1:
            print("evidence_quote_spotcheck_guard: PROTECTED_LEDGER_CENSUS_UNEQUAL — the five "
                  "ratchet surfaces must all carry the SAME number and do not:",
                  file=sys.stderr)
            for label, value in census.items():
                print(f"    {value:>6}  {label}", file=sys.stderr)
            print("  All five move together or none does. A larger row count than the declared "
                  "floor, or a floor raised without the matching identity pins, is a HALF-MOVED "
                  "ratchet: the surfaces that lag stop protecting anything while the gate still "
                  "reports green.", file=sys.stderr)
            sys.exit(CENSUS_EXIT)
        print("evidence_quote_spotcheck_guard: protected ratchet census OK — all five surfaces "
              f"== {len(entries)} (rows / require directives / min_entries / guard pin set / "
              "guard floor)")

    required = set(declared_required)
    if live_root:
        required |= set(LIVE_REQUIRED_PROTECTED_IDENTITIES)
    absent = sorted(required - seen)
    if absent:
        die_structural("PROTECTED_LEDGER_REQUIRED_IDENTITY_MISSING — "
                       + "; ".join(f"{r}::{u}" for r, u in absent)
                       + f" absent from {PROTECTED_LEDGER_REL} (row count is not identity: the "
                         "required anchors must each be present, however many rows the ledger has)")
    return entries

if protected_only:
    # Restricted sweep: exactly the ledger's anchors, and they ARE fatal under
    # --strict --strict-templates (unlike the advisory full sweep below).
    for rel, uid in load_protected_ledger():
        path = os.path.join(root, rel)
        if not os.path.isfile(path):
            die_structural(f"PROTECTED_LEDGER_FILE_MISSING — {rel} is listed in "
                           f"{PROTECTED_LEDGER_REL} but does not exist")
        fm = parse_template_frontmatter(path)
        if fm is None:
            die_structural(f"PROTECTED_LEDGER_NO_FRONTMATTER — {rel} has no parsable leading "
                           "evidence frontmatter block")
        cited = upstream_evidence_entries(fm)
        if not cited:
            die_structural(f"PROTECTED_LEDGER_NO_EVIDENCE — {rel} carries zero upstream_id "
                           "evidence entries; protecting it would verify nothing")
        # Round-3 (a): `str(e["upstream_id"])` coerced non-string ids into the identity
        # comparison. A protected file's upstream anchors must be genuine strings, so an
        # `upstream_id: 0` can never be matched-by-coercion against a ledger id.
        for e in cited:
            if not isinstance(e["upstream_id"], str):
                die_structural(f"PROTECTED_LEDGER_NON_STRING_UPSTREAM_ID — {rel} cites "
                               f"upstream_id of type {type(e['upstream_id']).__name__} "
                               f"({e['upstream_id']!r}); protected anchors are never coerced")
        matching = [e for e in cited if e["upstream_id"] == uid]
        if not matching:
            die_structural(f"PROTECTED_LEDGER_ANCHOR_ABSENT — {rel} does not cite "
                           f"upstream_id={uid} (ledger is stale or the anchor was renamed)")
        template_scanned += 1
        for entry in matching:
            template_quotes += 1
            # Codex round-2: `quote` defaulted to "" when absent, and "" is a substring of
            # EVERY snapshot body, so blanking or deleting the protected quote made
            # check_template_anchor() vacuously pass (0 findings, exit 0) — the fabricated-
            # anchor defense was bypassed by REMOVING the quote instead of falsifying it.
            # Codex round-3: the round-2 fix still ran `str(...)` FIRST, so `quote: 0` became
            # "0" (a real substring of the Stripe snapshot ⇒ pass) and `section: null` became
            # "None" (not blank ⇒ pass). Type comes before value now: a protected scalar must
            # BE a string; nothing is coerced into the comparison. Every rejection is a
            # structural ledger defect (exit 2, same family as the PROTECTED_LEDGER_* checks
            # above) raised BEFORE check_template_anchor can see the value.
            quote = require_protected_str(rel, uid, entry, "quote")
            section = require_protected_str(rel, uid, entry, "section")
            if not normalize(quote):
                die_structural(f"PROTECTED_LEDGER_EMPTY_QUOTE — {rel}::{uid} carries a "
                               "missing or blank `quote` — an empty quote is vacuously a "
                               "substring of every snapshot body and would silently pass")
            if len(normalize(quote)) < MIN_PROTECTED_QUOTE_CHARS:
                die_structural(f"PROTECTED_LEDGER_QUOTE_TOO_SHORT — {rel}::{uid} quote "
                               f"normalizes to {len(normalize(quote))} char(s) "
                               f"(< {MIN_PROTECTED_QUOTE_CHARS}); a quote too short to be "
                               "distinctive is a substring of nearly every snapshot body — "
                               "the empty-quote bypass wearing a valid type")
            if not section.strip():
                die_structural(f"PROTECTED_LEDGER_EMPTY_SECTION — {rel}::{uid} carries a "
                               "missing or blank `section` — a protected anchor with no "
                               "declared section verifies nothing about which part of the "
                               "snapshot backs the quote")
            check_template_anchor(rel, uid, quote, section=section, protected=True)
    if template_quotes == 0:
        die_structural("ZERO_SCAN — protected ledger resolved but no upstream_id anchor was "
                       "actually checked")
elif include_templates:
    templates_dir = os.path.join(root, "templates")
    tmpl_paths = []
    if os.path.isdir(templates_dir):
        for ext in ("*.tsx", "*.ts"):
            tmpl_paths += glob.glob(os.path.join(templates_dir, "**", ext), recursive=True)
    for path in sorted(set(tmpl_paths)):
        fm = parse_template_frontmatter(path)
        if fm is None:
            continue
        template_scanned += 1
        for entry in upstream_evidence_entries(fm):
            template_quotes += 1
            rel = os.path.relpath(path, root)
            # No str() coercion here either (round-3): a non-string upstream_id/quote, or an
            # absent quote, is reported as its own finding instead of being turned into a
            # value that happens to match.
            uid_raw = entry["upstream_id"]
            quote_raw = entry.get("quote", None)
            if not isinstance(uid_raw, str):
                template_misses.append((rel, repr(uid_raw), "TEMPLATE_UPSTREAM_ID_NOT_A_STRING", ""))
                continue
            if not isinstance(quote_raw, str):
                template_misses.append((rel, uid_raw, "TEMPLATE_QUOTE_MISSING_OR_NOT_A_STRING",
                                        type(quote_raw).__name__))
                continue
            if not normalize(quote_raw):
                template_misses.append((rel, uid_raw, "TEMPLATE_QUOTE_BLANK", ""))
                continue
            check_template_anchor(rel, uid_raw, quote_raw)

    # Zero-scan guard: template files with a frontmatter block were found but none carried
    # an upstream_id evidence entry — for the real repo that is a broken invocation; for a
    # deliberately evidence-free fixture this branch is simply never reached (template_scanned
    # stays 0 too, see check below), so it only fires on the "found frontmatter, found no
    # evidence at all" degenerate case.
    if template_scanned > 0 and template_quotes == 0:
        print("evidence_quote_spotcheck_guard: ZERO_SCAN — templates/**/*.{tsx,ts} frontmatter "
              "found but no upstream_id evidence scanned", file=sys.stderr)
        sys.exit(2)

# Zero-scan guard: a root with rule files but zero upstream quotes is a broken invocation,
# not a clean pass.
if scanned_rules > 0 and quotes == 0:
    print("evidence_quote_spotcheck_guard: ZERO_SCAN — rules found but no upstream_id evidence scanned", file=sys.stderr)
    sys.exit(2)

for rel, uid, kind, detail in misses:
    print(f"WARN [{rel}] upstream_id={uid}: {kind}" + (f" — quote starts: {detail!r}" if detail else ""))
for rel, uid, kind, detail in template_misses:
    print(f"WARN [{rel}] upstream_id={uid}: {kind}" + (f" — quote starts: {detail!r}" if detail else ""))

# Everything that is NOT the network-bound SNAPSHOT_FILE_MISSING class stays fatal under
# --allow-missing-snapshot. Defined as the complement on purpose: an allowlist of kinds
# would silently drop any kind added later (e.g. the round-3 type-strict findings) out of
# the fatal pool — exactly the "new shape slips through an old filter" bug this guard keeps
# being bypassed by.
missing_misses = [m for m in misses if m[2] == "SNAPSHOT_FILE_MISSING"]
quote_misses = [m for m in misses if m[2] != "SNAPSHOT_FILE_MISSING"]
verdict = f"{len(misses)} of {quotes} upstream quote(s) do not match their snapshot body"

if protected_only:
    print(f"evidence_quote_spotcheck_guard: PROTECTED templates anchors ({PROTECTED_LEDGER_REL}) — "
          f"{template_scanned} file(s), {template_quotes} anchor(s), {len(template_misses)} finding(s)"
          + ("" if strict_templates else " (advisory — pass --strict --strict-templates to promote)"))
elif include_templates:
    print(f"evidence_quote_spotcheck_guard: templates/**/*.{{tsx,ts}} — {template_scanned} file(s) with "
          f"evidence frontmatter, {template_quotes} upstream_id quote(s), {len(template_misses)} "
          f"finding(s)" + ("" if strict_templates else " (advisory — pass --strict-templates to promote)"))

def fatal_template_findings():
    return strict and strict_templates and template_misses

# --allow-missing-snapshot (strict only): QUOTE mismatches stay fatal; SNAPSHOT_FILE_MISSING
# is downgraded to a non-fatal advisory list. See the header for why (Java-side snapshot
# bodies are uncommitted / network-bound — a tracked backlog residual). Applies to the
# rules/ sweep only — templates/** findings are gated by --strict-templates, not this flag.
if strict and allow_missing:
    if missing_misses:
        print(f"evidence_quote_spotcheck_guard: ADVISORY — {len(missing_misses)} SNAPSHOT_FILE_MISSING "
              f"finding(s) downgraded (--allow-missing-snapshot; network-bound backlog residual)")
    if quote_misses or fatal_template_findings():
        print(f"evidence_quote_spotcheck_guard: {len(quote_misses)} of {quotes} upstream quote(s) do not "
              f"match their snapshot body — BLOCKED (--strict)", file=sys.stderr)
        sys.exit(1)
    print(f"evidence_quote_spotcheck_guard: all resolvable upstream quote(s) verified "
          f"({len(missing_misses)} missing-snapshot finding(s) advisory)")
    sys.exit(0)

if (misses and strict) or fatal_template_findings():
    if misses:
        print(f"evidence_quote_spotcheck_guard: {verdict} — BLOCKED (--strict)", file=sys.stderr)
    if fatal_template_findings():
        print(f"evidence_quote_spotcheck_guard: {len(template_misses)} of {template_quotes} "
              f"templates/**/*.{{tsx,ts}} upstream quote(s) do not match their snapshot body — "
              f"BLOCKED (--strict --strict-templates)", file=sys.stderr)
    sys.exit(1)
if misses:
    print(f"evidence_quote_spotcheck_guard: ADVISORY — {verdict} (exit 0; promote with --strict once burned down)")
else:
    print(f"evidence_quote_spotcheck_guard: all {quotes} upstream quote(s) verified against snapshot bodies")
sys.exit(0)
PY
GUARD_RC=$?

# ── P1-2, standalone half: observe the anchor ref a SECOND time ───────────────────────
# (ROUND 4, TD-2026-07-30-P1-anchor-runtime.) The pin check above compares this guard's read
# against the RUNNER's; that only exists when a runner started us. A guard invoked directly has
# no pin, so it authenticates the ref against ITSELF: re-read it now that the ratchet work is
# done. A ref that moved in between means every conclusion above was drawn about a commit that
# is no longer the release being extended — and refs/remotes/origin/main is an ordinary local
# ref, so this is writable by anything running concurrently.
# HONEST LIMIT, same shape as the runner's tree sampling: two observations bound a window, they
# do not eliminate it. A ref moved and restored entirely between these two reads is unobserved.
if [ "$LIVE_ROOT" = "1" ] && [ -n "$GIT_ANCHOR" ]; then
    ax_anchor_verify_unmoved "$SELF_REPO_ROOT" "evidence_quote_spotcheck_guard" \
        || exit "$ANCHOR_AUTH_EXIT"
fi
exit "$GUARD_RC"
