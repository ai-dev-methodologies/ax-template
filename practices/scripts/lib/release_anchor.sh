#!/usr/bin/env bash
# practices/scripts/lib/release_anchor.sh — THE single release-anchor resolver + authenticator.
#
# (P1-X / P1-Y, cross-family reviewer ROUND 3, 2026-07-30; TD-2026-07-30-P1-anchor-authenticity.)
#
# WHY THIS FILE EXISTS
# --------------------
# Two ratcheting guards (evidence_quote_spotcheck_guard, manifest_snapshot_integrity_guard) and
# the R25 runner (practices/scripts/verify-completion.sh) all need to answer the SAME question:
# "which commit is the previous release?". Round 2 left three independent copies of that logic,
# and the reviewer's round-3 attacks turn on the answer being (a) attacker-mutable and (b) not
# recorded anywhere the push gate can check. A single helper makes the runner's recorded
# anchor_sha and the guards' resolved anchor the same object by construction, which is what
# layer (3) below authenticates.
#
# ── P1-X: THE LOCAL ANCHOR REF IS ATTACKER-MUTABLE ───────────────────────────────────
# `refs/remotes/origin/main` is an ORDINARY LOCAL REF. `git update-ref` writes it. So:
#     M     = the malicious downgrade commit (this is what will be pushed)
#     FAKE  = a synthetic commit whose TREE is M's tree MINUS the two anchor paths,
#             parented on the REAL origin/main
#     git update-ref refs/remotes/origin/main FAKE
#     run R25   → both ratchets resolve their anchor to FAKE, find their own file ABSENT
#                 there, take the "first-release bootstrap" skip, and pass
#     git update-ref refs/remotes/origin/main <REAL>   → push M
# Nothing in round 2 looked at WHICH commit the ref pointed to; the audit line fingerprinted
# the working tree, and a ref is not part of the working tree.
#
# THREE INDEPENDENT LAYERS, all of which must hold:
#
#   (1) ANCESTRY — ANCHOR_NOT_ANCESTOR (blocking).
#       The resolved anchor commit MUST be an ancestor of HEAD
#       (`git merge-base --is-ancestor <anchor> HEAD`). On a normal release HEAD descends from
#       origin/main, so this is free. The reviewer's FAKE is parented on the REAL origin/main
#       and is therefore NOT an ancestor of M — the forgery is refused before any comparison.
#       LEGITIMATE EXCEPTION SHAPE, and it BLOCKS rather than skips: a local branch that has
#       diverged from origin/main (origin/main moved on, this branch has not been rebased).
#       The remedy is stated in the failure text — `git fetch origin && git rebase origin/main`
#       (or merge) — because a release that does not descend from the release it claims to
#       ratchet against has not actually been ratcheted: the two trees' histories are disjoint
#       past the merge base, so "the floor may only rise" is a comparison between two branches,
#       not between two releases. Skipping here would be the bypass; blocking is the point.
#
#   (2) BOOTSTRAP IS NOT FILE-ABSENCE ALONE — ANCHOR_BOOTSTRAP_IMPLAUSIBLE (blocking).
#       PLAINLY: a file that HAS HISTORY IN THIS REPO CAN NEVER LEGITIMATELY BE "ABSENT IN THE
#       PREVIOUS RELEASE". Absence at the anchor is honored as a first-release bootstrap ONLY
#       when the anchor's own history never contained the path:
#         (2a) `git rev-list --max-count=1 <anchor> -- <path>` must be EMPTY. The reviewer's
#              FAKE is parented on the REAL origin/main, which carries both guards, so FAKE
#              itself is a commit that TOUCHED the path (it deleted it) and rev-list returns it.
#              This layer therefore kills the reproduction on its own, independently of (1).
#         (2b) redundant probe from the OTHER end, so that a git-level trick which fools one
#              walk still has to fool a second one starting elsewhere: let C be the last commit
#              reachable from HEAD that touched the path; if C exists and C is an ancestor of
#              the anchor, then the anchor's tree cannot honestly lack the path.
#       DELIBERATELY NOT IMPLEMENTED AS WRITTEN IN THE BRIEF: "`rev-list -1 HEAD~1 -- <path>`
#       is non-empty ⇒ manufactured" would BLOCK A LEGITIMATE CASE — a ratcheting file added
#       one or two commits ago, while origin/main still predates it, has real HEAD~1 history and
#       a real, honest absence at the anchor. The predicate above is the false-positive-free form
#       of the same idea: it asks whether the ANCHOR's history had the file, not whether ANY
#       ancestor of HEAD did.
#
#   (3) AUDIT-BOUND ANCHOR + PRE-PUSH AUTHENTICATION (implemented by the callers, see
#       verify-completion.sh's audit writer and completion_checklist_recency_guard.sh).
#       verify-completion.sh records `anchor_sha`/`anchor_kind` — resolved HERE, so the runner
#       and the guards cannot disagree — in the audit line. The pre-push hook is handed the
#       AUTHORITATIVE remote sha by git itself (it comes from the remote's advertisement, not
#       from any local ref) and passes it to the recency guard, which requires the recorded
#       anchor_sha to EQUAL it. A forged local ref therefore produces an audit line bound to a
#       commit the remote does not have, and the push is refused. This is the layer that makes
#       ref forgery UNPUSHABLE even if (1) and (2) were both bypassed.
#       Legitimate exception, disclosed: the remote moved between the R25 run and the push
#       (someone else pushed). The honest resolution is `git fetch` + re-run R25 — the ratchet
#       was measured against a release that is no longer the release being extended.
#
# ── P1-Y: SYMLINK REPRESENTATION LAUNDERING ──────────────────────────────────────────
# THE ASYMMETRY, stated once because it is the whole class: THE ANCHOR SIDE READS GIT OBJECTS
# (`git show`, `git ls-tree`) AND THE SELF SIDE READS THE FILESYSTEM (`open()` follows symlinks).
# Any place those two disagree is a laundering channel. A path replaced by a SYMLINK is read by
# the self side as the TARGET's content and by the anchor side as the LINK BLOB (which is just
# the target pathname) — so a guard can verify itself green while its successor's anchor parse
# sees whatever the attacker chose to spell in a pathname.
#   ax_anchor_worktree_paths_regular — lstat (`[ -L ]`), NOT stat, on the leaf AND on every path
#     component below the base, so a symlinked DIRECTORY cannot launder a regular leaf either.
#     Runs on EVERY root (live and fixture) because it inspects the tree in front of it and
#     needs no release history — which is what makes it the half a FIXTURE can prove.
#   ax_anchor_release_paths_regular — `git ls-tree <anchor> -- <path>` mode must be 100644 or
#     100755. Mode 120000 (symlink) is ANCHOR_PATH_NOT_REGULAR, blocking. Anchor-bound, so
#     live-root only and not fixture-coverable; its evidence is the RED reproductions recorded
#     in practices/DECISIONS.md.
#
# CONTRACT
#   Every function reads git + its arguments, prints diagnostics to STDERR, and returns a status.
#   NONE calls `exit` — each caller maps a violation onto ITS OWN exit code, because the two
#   guards have different, already-published code spaces.
#
# Usage:
#   source practices/scripts/lib/release_anchor.sh
#   ax_anchor_resolve "$REPO_ROOT"                    # → AX_ANCHOR_REF/_KIND/_SHA
#   ax_anchor_check_ancestry "$REPO_ROOT" "<label>"   # → 1 on ANCHOR_NOT_ANCESTOR
#   ax_anchor_release_paths_regular "$REPO_ROOT" "<label>" <rel>...
#   ax_anchor_worktree_paths_regular "$BASE" "<label>" <rel>...

# Idempotent source guard (the two guards may both be sourced in one shell by a test harness).
[ -n "${_AX_RELEASE_ANCHOR_LIB:-}" ] && return 0
_AX_RELEASE_ANCHOR_LIB=1

# The remote-tracking ref the ratchet anchors to, and the REMOTE-SIDE ref name whose sha the
# pre-push hook receives from git. They are two spellings of the same branch and are kept
# together here so layer (3) cannot drift apart from layers (1)/(2).
AX_ANCHOR_TRACKING_REF="origin/main"
AX_ANCHOR_REMOTE_REF="refs/heads/main"
AX_ANCHOR_ZERO_SHA="0000000000000000000000000000000000000000"

# ax_anchor_resolve <repo_root>
#   Sets AX_ANCHOR_REF (the rev to pass to git), AX_ANCHOR_KIND (origin/main | HEAD |
#   unavailable) and AX_ANCHOR_SHA (the resolved commit sha, or "unavailable").
#   Resolution order, unchanged from round 1-2: origin/main (strong — R25 runs at a tree AHEAD
#   of it) → HEAD (weaker: a detached/fork-fresh clone; a downgrade already committed locally
#   is present in the anchor itself) → unavailable. Always returns 0; the caller decides what an
#   unavailable anchor means for it.
ax_anchor_resolve() {
    local repo="$1"
    AX_ANCHOR_REF=""
    AX_ANCHOR_KIND="unavailable"
    AX_ANCHOR_SHA="unavailable"
    command -v git >/dev/null 2>&1 || return 0
    git -C "$repo" rev-parse --git-dir >/dev/null 2>&1 || return 0
    if git -C "$repo" rev-parse --verify --quiet "$AX_ANCHOR_TRACKING_REF" >/dev/null 2>&1; then
        AX_ANCHOR_REF="$AX_ANCHOR_TRACKING_REF"
        AX_ANCHOR_KIND="origin/main"
    elif git -C "$repo" rev-parse --verify --quiet HEAD >/dev/null 2>&1; then
        AX_ANCHOR_REF="HEAD"
        AX_ANCHOR_KIND="HEAD"
    else
        return 0
    fi
    AX_ANCHOR_SHA="$(git -C "$repo" rev-parse --verify --quiet "${AX_ANCHOR_REF}^{commit}" 2>/dev/null)"
    if [ -z "$AX_ANCHOR_SHA" ]; then
        AX_ANCHOR_REF=""
        AX_ANCHOR_KIND="unavailable"
        AX_ANCHOR_SHA="unavailable"
    fi
    return 0
}

# ax_anchor_check_ancestry <repo_root> <label>
#   Layer (1). 0 = anchor is an ancestor of HEAD (or there is no anchor / no HEAD to compare);
#   1 = ANCHOR_NOT_ANCESTOR, blocking for the caller.
ax_anchor_check_ancestry() {
    local repo="$1" label="$2" head
    [ -n "${AX_ANCHOR_REF:-}" ] || return 0
    head="$(git -C "$repo" rev-parse --verify --quiet HEAD 2>/dev/null)" || head=""
    [ -n "$head" ] || return 0
    if git -C "$repo" merge-base --is-ancestor "$AX_ANCHOR_SHA" "$head" 2>/dev/null; then
        return 0
    fi
    {
        echo "${label}: ANCHOR_NOT_ANCESTOR — the resolved release anchor"
        echo "    ${AX_ANCHOR_KIND} = ${AX_ANCHOR_SHA}"
        echo "  is NOT an ancestor of HEAD (${head}). A release ratchets against the release it"
        echo "  EXTENDS, so on any honest release HEAD descends from the anchor and this check is"
        echo "  free. It fails in exactly two situations, and both must block:"
        echo "    · refs/remotes/origin/main is an ORDINARY LOCAL REF that \`git update-ref\` can"
        echo "      point anywhere. Aiming it at a synthetic commit that merely LACKS the"
        echo "      ratcheting files makes every ratchet take its first-release bootstrap skip."
        echo "      That commit is parented on the real release, so it is not an ancestor of the"
        echo "      commit being shipped — which is what this check sees."
        echo "    · your branch has genuinely diverged: origin/main moved on and this branch was"
        echo "      never rebased onto it. Then the comparison is between two BRANCHES, not two"
        echo "      releases, and 'the floor may only rise' does not mean what it says. Fix it:"
        echo "        git fetch origin && git rebase origin/main   (or merge origin/main)"
        echo "      then re-run. This is a BLOCK, never a skip: an unratchetable tree must not ship."
    } >&2
    return 1
}

# _ax_anchor_bootstrap_implausible <repo_root> <label> <rel>
#   Layer (2). Called ONLY when <rel> is absent at the anchor. 0 = the absence is a plausible
#   first-release bootstrap; 1 = ANCHOR_BOOTSTRAP_IMPLAUSIBLE (blocking).
_ax_anchor_bootstrap_implausible() {
    local repo="$1" label="$2" rel="$3" hist last_touch reason=""
    hist="$(git -C "$repo" rev-list --max-count=1 "$AX_ANCHOR_SHA" -- "$rel" 2>/dev/null)"
    if [ -n "$hist" ]; then
        reason="the anchor's OWN history contains ${rel} (last touched by ${hist}), so its"$'\n'"    absence at ${AX_ANCHOR_SHA} is a DELETION, not a never-existed"
    else
        last_touch="$(git -C "$repo" rev-list --max-count=1 HEAD -- "$rel" 2>/dev/null)"
        if [ -n "$last_touch" ] \
           && git -C "$repo" merge-base --is-ancestor "$last_touch" "$AX_ANCHOR_SHA" 2>/dev/null; then
            reason="the commit that last touched ${rel} on HEAD's history (${last_touch}) is an"$'\n'"    ANCESTOR of the anchor, so the anchor's tree cannot honestly lack it"
        fi
    fi
    [ -n "$reason" ] || return 0
    {
        echo "${label}: ANCHOR_BOOTSTRAP_IMPLAUSIBLE — ${rel} is absent at the release anchor"
        echo "  (${AX_ANCHOR_KIND} = ${AX_ANCHOR_SHA}), but that absence is MANUFACTURED:"
        echo "    ${reason}."
        echo "  THE RULE, plainly: A FILE THAT HAS HISTORY IN THIS REPO CAN NEVER LEGITIMATELY BE"
        echo "  'ABSENT IN THE PREVIOUS RELEASE'. Absence is honored as a first-release bootstrap"
        echo "  skip ONLY for a path the anchor's history never contained. Otherwise the skip is"
        echo "  the payload: point refs/remotes/origin/main at a commit whose tree simply DROPS the"
        echo "  ratcheting files, and every ratchet politely bootstraps itself out of existence."
        echo "  If you are deliberately deleting this path, the ratchet it carries is being"
        echo "  retired — do that in a commit that says so, not through an anchor that pretends"
        echo "  the file was never here."
    } >&2
    return 1
}

# ax_anchor_release_paths_regular <repo_root> <label> <rel>...
#   Layer P1-Y (anchor side) + layer (2). For each path, as of the resolved anchor:
#     · present with mode 100644/100755 → OK
#     · present with any other mode (120000 symlink, 160000 gitlink, 040000 tree) →
#       ANCHOR_PATH_NOT_REGULAR, blocking
#     · absent → the bootstrap-plausibility test above
#   No-op when no anchor resolved (the caller has already decided what that means).
#   0 = all clean, 1 = at least one violation (every violation is printed).
ax_anchor_release_paths_regular() {
    local repo="$1" label="$2"; shift 2
    local rel entry mode bad=0
    [ -n "${AX_ANCHOR_REF:-}" ] || return 0
    for rel in "$@"; do
        entry="$(git -C "$repo" ls-tree "$AX_ANCHOR_SHA" -- "$rel" 2>/dev/null | head -1)"
        if [ -z "$entry" ]; then
            _ax_anchor_bootstrap_implausible "$repo" "$label" "$rel" || bad=1
            continue
        fi
        mode="${entry%% *}"
        case "$mode" in
            100644|100755) ;;
            *)
                {
                    echo "${label}: ANCHOR_PATH_NOT_REGULAR — ${rel} exists at the release anchor"
                    echo "  (${AX_ANCHOR_KIND} = ${AX_ANCHOR_SHA}) with git mode ${mode}, which is not a"
                    echo "  regular file blob (100644/100755). Mode 120000 is a SYMLINK, and a symlink is"
                    echo "  the representation-laundering channel this check exists to close: the anchor"
                    echo "  side reads GIT OBJECTS, so \`git show\` hands back the LINK BLOB — the target"
                    echo "  PATHNAME — while the self side reads the FILESYSTEM and follows the link to"
                    echo "  the real content. Choose a pathname that is also parseable-but-weakened"
                    echo "  source and the two sides can be made to disagree by construction. An"
                    echo "  anchor-critical path must be a regular file on BOTH sides."
                } >&2
                bad=1
                ;;
        esac
    done
    return "$bad"
}

# ax_anchor_worktree_paths_regular <base_dir> <label> <rel>...
#   Layer P1-Y (self side). lstat-based, so it sees the LINK rather than its target, and it
#   walks EVERY path component below <base_dir> — a symlinked directory launders a regular leaf
#   just as well as a symlinked leaf does. A path that does not exist is not a violation here
#   (absence is the callers' own business); a path that exists but is not a regular file is.
#   Runs on every root, live or fixture — this is the fixture-provable half of P1-Y.
#   0 = all clean, 1 = at least one violation.
ax_anchor_worktree_paths_regular() {
    local base="$1" label="$2"; shift 2
    local rel rest part cur bad=0 hit
    for rel in "$@"; do
        cur="$base"
        rest="$rel"
        hit=0
        while [ -n "$rest" ]; do
            part="${rest%%/*}"
            if [ "$part" = "$rest" ]; then rest=""; else rest="${rest#*/}"; fi
            [ -n "$part" ] || continue
            cur="$cur/$part"
            if [ -L "$cur" ]; then
                {
                    echo "${label}: SELF_PATH_NOT_REGULAR — ${rel} resolves through a SYMLINK at"
                    echo "  ${cur#"$base"/} (under ${base})."
                    echo "  Every check in this guard that reads the filesystem follows that link, while"
                    echo "  every check that reads the release anchor gets the LINK BLOB — the target"
                    echo "  PATHNAME — from git. The two sides then disagree by construction, which is"
                    echo "  precisely the laundering channel: make the TARGET PATHNAME itself parse as"
                    echo "  weakened source, and this generation verifies the real file green while the"
                    echo "  next generation's anchor parse reads the pathname instead."
                    echo "  An anchor-critical path (and every directory on the way to it) must be a"
                    echo "  regular file / real directory. Replace the symlink with the file itself."
                } >&2
                bad=1
                hit=1
                break
            fi
        done
        if [ "$hit" -eq 0 ] && [ -e "$cur" ] && [ ! -f "$cur" ]; then
            {
                echo "${label}: SELF_PATH_NOT_REGULAR — ${rel} exists under ${base} but is not a"
                echo "  regular file. Anchor-critical paths are compared against git blobs; anything"
                echo "  that is not a regular file cannot be compared and must not be trusted."
            } >&2
            bad=1
        fi
    done
    return "$bad"
}
