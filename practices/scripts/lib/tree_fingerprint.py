#!/usr/bin/env python3
"""practices/scripts/lib/tree_fingerprint.py — THE working-tree fingerprint, one definition.

(P1-4, cross-family reviewer ROUND 4, 2026-07-30; TD-2026-07-30-P1-anchor-runtime.)

WHY THIS FILE EXISTS
    verify-completion.sh WRITES tree_fingerprint into the audit line and
    completion_checklist_recency_guard.sh must RECOMPUTE it to check the writer was not simply
    asserting a value. Two copies of a hash algorithm are two chances to drift, and a drifted
    recompute is worse than none — it fails honest runs and teaches the operator to ignore it.
    So the algorithm lives here and both sides call this file.

WHAT IT HASHES, deterministically:
    · `git status --porcelain -z -uall`  — the set and state of dirty/untracked paths
    · `git diff HEAD --binary`           — the CONTENT of every tracked modification
    · the bytes of every untracked non-ignored file (invisible to `git diff HEAD`)
    Ignored paths are excluded by git itself, so build/, node_modules/ and .ax-verify/ churn does
    not invalidate a record.

    --no-replace-objects (P1-1): `git replace` swaps the OBJECT behind a stable sha, so a diff
    against HEAD could be computed against a fabricated commit. The fingerprint is evidence; it
    must be taken against the real object graph.

    ROUND 5 / P1-1 (TD-2026-07-30-P1-hermetic-runtime) — THE GIT CONTEXT IS NOT INHERITED HERE
    EITHER. `git -C <repo>` says where to start looking, not which repository to use: GIT_DIR,
    GIT_WORK_TREE, GIT_COMMON_DIR, GIT_OBJECT_DIRECTORY, GIT_INDEX_FILE, GIT_NAMESPACE,
    GIT_CEILING_DIRECTORIES and GIT_CONFIG* all override it from the environment. MEASURED: with
    the context aimed at a clean shadow checkout of the same commit, this file fingerprinted a
    DIRTY tree as `0a815065ebf5…` — the clean-tree constant — instead of `c764531c8f18…`. So the
    subprocess environment is SCRUBBED of the whole GIT_* family, the binary is an absolute path
    (an exported `git` shell function cannot reach a subprocess started this way, but a PATH shim
    can), and the work tree that answers is REQUIRED to be the repo we were asked about — anything
    else raises RedirectedGitContext, which is a BLOCK, never a "nogit".

    ROUND 8 / P1-A (TD-2026-07-31-(P1-worktree-representation)) — A REPRESENTATION THE SWEEP DID
    NOT EXPECT IS NOT A `continue`. Round 7 hashed raw bytes for every tracked path git called
    clean, but skipped the paths whose on-disk SHAPE was not the index's (a symlink where the
    index says regular, and the mirror) and the paths that were not on disk at all. Both are
    reachable with one `git update-index` and both leave `git status --porcelain` EMPTY:
    measured, a clean tree, a regular-index path swapped for a symlink, and a skip-worktree path
    deleted all produced the IDENTICAL digest 0a815065…. They BLOCK now, together with the index
    bits that produce them and a diverged initialized submodule.

    ROUND 9 / P1-1 + P1-2 + (e) (TD-2026-07-30-(P1-representation-parity)) — ROUND 8 SEPARATED THE
    SHAPES AND THEN COMPARED ONLY BLOB BYTES. A tracked path's representation carries THREE more
    facts that no digest here holds and that `git status` can be told (or is simply unable) to
    report: (1) the EXECUTABLE BIT — `core.fileMode=false` + `git update-index --chmod=-x` + a
    local `chmod +x` leaves status EMPTY and the digest at the clean-tree constant while R25's 118
    direct `./gradlew` invocations run a file the push records as 100644; (2) a gitlink with NO
    gitdir whose directory is nevertheless POPULATED — round 8 returned success on the absence of
    `<gitlink>/.git` without requiring the directory to be EMPTY, so a committed check can `bash
    vendor/sub/check.sh` a file that a fresh clone never receives; (3) two index entries differing
    only in CASE, which a case-insensitive filesystem serves from ONE file. All three BLOCK.

    ROUND 10 / P1 (TD-2026-07-31-(P1-casefold-prefix)) — ROUND 9's (e) WAS LEAF-ONLY, AND THE
    ENTRY THAT CLAIMED IT WAS OVERCLAIMED. The casefold group above was keyed on the COMPLETE
    folded path, so two entries whose LEAF names differ never landed in the same group even when a
    shared DIRECTORY component was a casefold alias. MEASURED against this file at d567c37, in a
    throwaway repo on APFS:
        index: A/check.sh  (contains: cat A/helper)
        index: a/helper    (contains: PASS)
        disk:  A and a are the SAME directory inode (16777229, 34423509)
    `git status --porcelain` is EMPTY, `cat A/helper` succeeds locally so every gate goes green,
    and this file returned the clean-tree constant 0a815065… at exit 0 — while the PUSHED tree
    holds only `a/helper`, so a case-sensitive receiver has no `A/helper` at all and the committed
    check is broken on arrival. The alias is now measured over EVERY PATH PREFIX (each directory
    component, folded, plus the full path), gitlinks included, and a group whose members lstat to
    the same (st_dev, st_ino) BLOCKS — a directory component under GIT_CASEFOLD_DIR_ALIAS, the
    leaf case unchanged under GIT_CASEFOLD_ALIAS.

    ROUND 11 / P1 (TD-2026-08-01-(P1-unicode-prefix-fold)) — THE PREFIX WAS RIGHT, THE FOLD WAS
    ASCII. Rounds 9 and 10 keyed the map with `bytes.lower()`, which lowercases A-Z and nothing
    else and is blind to Unicode normalization. Two aliases the filesystem serves from ONE inode
    therefore never met, and no environment control is needed to reach either — both are committed
    content. MEASURED against this file at beee364, in a throwaway repo on APFS:
        index: é/check.sh    (NFC, c3a9;   contains: cat "é/helper")
        index: e◌́/helper     (NFD, 65cc81; contains: PASS)
        disk:  é and e◌́ are the SAME directory inode (16777229, 34664959)
    `git status --porcelain -uall` is EMPTY, `bash é/check.sh` prints PASS locally so every gate
    goes green, and this file returned the clean-tree constant 0a815065… at exit 0 — while the
    PUSHED tree records only `e◌́/helper`, which a normalization-SENSITIVE receiver does not serve
    as `é/helper`. `É/` vs `é/` is the same defect through the same hole (`bytes.lower()` does not
    touch c389). The key is now UNICODE CANONICAL CASELESS — NFC(casefold(NFD(s))), UAX #21 §1.3 —
    computed by `_fold_path_key`, which also states the non-UTF-8 disposition. The (st_dev, st_ino)
    discriminator and the leaf/directory split are UNCHANGED, so a case- or normalization-sensitive
    fork-receiver is unaffected and the two codes keep their meanings.

    ROUND 13 / P1 (TD-2026-08-01-(P1-symlink-target-alias)) — FOUR ROUNDS OF ALIAS CENSUS, AND NOT
    ONE OF THEM LOOKED AT WHAT A SYMLINK POINTS AT. Rounds 9-12 registered INDEX PATHS and widened
    the key three times. A symlink's TARGET is not an index path: it is BLOB CONTENT, read here as
    bytes and hashed and never resolved, so the census did not apply to it on ANY of the four axes.
    MEASURED — the reviewer's reproduction is committed content, no environment control:
        git mv backend/gradlew backend/gradlew-real
        ln -s GRADLEW-REAL backend/gradlew          # the CASE is the whole attack
    On case-insensitive APFS this resolves, R25 EXECUTES and goes green, `git status --porcelain
    -uall` is EMPTY and this file returns the clean-tree constant 0a815065…; on a case-SENSITIVE
    checkout the committed link is DANGLING and the receiver has no working `backend/gradlew`.
    The fix RESOLVES the target lexically against the link's own (recorded) directory and requires
    its spelling to be the one the index records whenever it lands on a tracked inode by a
    FOLD-EQUAL alias — the same `_fold_path_key` and the same (st_dev, st_ino) discriminator, so
    case, normalization, non-ASCII case and ignorable Cf are all covered at once. New code
    GIT_SYMLINK_TARGET_ALIAS (exit 15); the full disposition table for absolute / `..` / chained /
    directory / dangling / untracked targets is in SymlinkTargetAlias.__doc__.

ROUND 14 / P1-A (TD-2026-08-01-(P1-posix-resolution-and-runtime-paths)) — ROUND 13 RESOLVED THE
    TARGET LEXICALLY, AND A LEXICAL `..` IS NOT A POSIX `..`. The kernel pops `..` AFTER following
    an intermediate symlink; round 13 popped it textually FIRST, so any target whose `..` sits
    behind a symlinked component resolved to a DIFFERENT path than the one the receiver's kernel
    reaches. MEASURED — the reviewer's topology, committed content only:
        backend/real/gradlew-real     (tracked)      backend/jump -> real/sub   (tracked symlink)
        backend/real/sub/.keep        (tracked)      backend/gradlew -> jump/../GRADLEW-REAL
    POSIX: `jump` → `backend/real/sub`, `..` → `backend/real`, so the target is
    `backend/real/GRADLEW-REAL` = the tracked `backend/real/gradlew-real` on case-insensitive
    APFS — R25 runs the wrapper and goes green, while a case-SENSITIVE receiver gets a DANGLING
    `backend/gradlew`. Lexical: `backend/GRADLEW-REAL`, absent, so BOTH implementations took the
    dangling exit and reported NOTHING. (The same divergence is visible with no filesystem writes
    at all through macOS's stock `/var` link: `/var/../TFTPBOOT` and `/private/tftpboot` are one
    inode while lexical `/TFTPBOOT` does not exist.) `_resolve_link_target` now walks COMPONENT BY
    COMPONENT — follows intermediate links, never the final one, pops `..` after the follow — and
    carries two budgets (40 follows / 4096 components) whose exhaustion BLOCKS under a new code
    GIT_SYMLINK_RESOLUTION_UNBOUNDED (exit 16) rather than going silent. This SUPERSEDES BACKLOG
    P3-133, whose own remedy ("go silent when lexical and realpath diverge") would have PRESERVED
    this P1; the round-13 over-inclusive counterexample is re-measured under the new resolver in
    DECISIONS.md and no longer over-blocks.

ROUND 14b / P1 (SAME TD ENTRY) — FOLLOWING AN INTERMEDIATE MADE ITS SPELLING UNJUDGED. Round 14
    resolved correctly and then asked the alias question ONCE, about the FINAL candidate, so any
    alias sitting in an INTERMEDIATE position was refused by neither implementation. MEASURED by
    independent verification on one tree (tracked `legit/sub/real.txt`, tracked symlink
    `legit/dirlink -> sub`), committed content only:
        ln -s DIRLINK  legit/x   → exit 15   (final component — the round-13/14 class)
        ln -s DIRLINK/ legit/x   → exit 0    (SAME alias, one keystroke, UNREFUSED)
        ln -s DIRLINK/real.txt   → exit 0    (the same hole without the keystroke)
    The trailing slash is honoured LEGITIMATELY — the kernel follows the final component when
    something follows it — and that legitimacy is exactly what moved the alias out of reach: the
    follow lands on the correctly-spelled `legit/sub`, which passes on an EXACT match, while
    `legit/DIRLINK` — the spelling that dangles at a case-sensitive receiver — was never asked
    about. `_resolve_link_target` now RETURNS every non-final component it resolved and
    `_symlink_target_verdicts` takes the SAME verdict on each, under the SAME code
    (GIT_SYMLINK_TARGET_ALIAS): the subject and the remedy are identical, only the sentence
    changes. An UNTRACKED intermediate has no recorded spelling to alias and is NOT refused,
    which is the same disposition the final candidate has always had.
    THE MIS-GRADING IS THE LESSON. This was registered as P3-134 "register-only" on the strength
    of an attribution-hygiene argument; the row was procedurally defensible and materially wrong,
    because it recorded the no-slash shape without the one-keystroke route that makes it a live
    bypass of a shipped refusal. A defect's GRADE follows its reachability, not the tidiness of
    the argument for deferring it.

HONEST LIMIT, and it matters for how much the recompute proves:
    ON A CLEAN TREE both inputs are EMPTY, so the digest is a CONSTANT — the same value for every
    clean tree of every commit. It is a DIRT fingerprint, not a tree identity. That is exactly
    what it was introduced for (binding a resume record to the working state that produced it),
    but it means a recompute at push time proves "the tree is as dirty/clean as the record says",
    NOT "this is the code that was verified". The commit identity is carried by head_sha, which
    the recency guard re-reads from git independently.

Usage:
    python3 practices/scripts/lib/tree_fingerprint.py <repo_root>   # prints the digest, or
                                                                    # "nogit" for a non-git tree
    from tree_fingerprint import fingerprint                        # → str
"""
import hashlib
import os
import shutil
import stat
import subprocess
import sys
import unicodedata


class RedirectedGitContext(RuntimeError):
    """The git context answering our reads is not the repository we were asked about."""


class GitFiltersPresent(RuntimeError):
    """The repository declares content filters, so no git content answer is about raw bytes.

    ROUND 6 / P1-4 (invariant beta: a claim about BYTES must be made on RAW BYTES). A
    `filter.<name>.clean` sits between the working file and every content answer git gives —
    `git diff HEAD --binary` reports the FILTER'S OUTPUT. Measured: with a clean filter echoing
    the committed copy, a tampered file produced an EMPTY `git diff HEAD --binary` while its
    on-disk bytes differed. The dirty accumulator below now hashes raw file bytes, but a
    repository that declares filters at all is refused: this file is evidence, and evidence taken
    through a lens the subject installed is not evidence.

    ROUND 7 / P1-3 — THE KEYWORD SUBSET WAS THE HOLE. This refusal matched only
    `filter.<n>.clean` and `filter.<n>.smudge`. git ALSO performs the same conversion through the
    LONG-RUNNING FILTER PROTOCOL, declared as `filter.<n>.process` — one key, no clean/smudge in
    sight. Measured (reviewer round 7): commit a malicious file plus a .gitattributes entry,
    configure ONLY filter.<n>.process, then replace the file on disk with benign bytes →
    `git status --porcelain` is EMPTY, so the path never enters `modified`, so the raw open()
    loop below never reads it: R25 tests the benign bytes while the push ships the malicious blob.
    The declaration test is now `^filter\\.` — ANY key under any filter driver, not a keyword
    subset — because the next protocol key is not something this file can be asked to predict.
    """


class WorktreeTypeMismatch(GitFiltersPresent):
    """A tracked path whose WORKTREE REPRESENTATION is not the one the index records.

    ROUND 8 / P1-A — THE SWEEP SKIPPED THE TWO REPRESENTATIONS THAT HIDE PUSHED BYTES. Round 7
    measured BYTES, but only for paths whose on-disk shape it expected: an index-REGULAR path that
    is a SYMLINK on disk was `continue`d (the raw loop cannot read a link as a file), and so was an
    index-SYMLINK path that is a regular file. MEASURED (reviewer round 8, reproduced here):
    commit a malicious `practices/verification-checklist.yaml`, `git update-index
    --assume-unchanged` it, `rm` it, and put a SYMLINK to a benign file outside the repo in its
    place → `git status --porcelain` is EMPTY, the index still says mode 100644, the sweep hits
    `islink()` and skips, and the fingerprint is the CLEAN-TREE CONSTANT 0a815065… — so R25 reads
    the benign target while the push ships the malicious blob.
    A representation the index does not record is not a byte question this file can answer, and an
    answer it cannot give may not be spelled `continue`. It BLOCKS.
    """


class TrackedPathUnreadable(GitFiltersPresent):
    """A tracked path git reports as unmodified that is ABSENT from disk, or cannot be read.

    ROUND 8 / P1-A, the other half. Round 7 named this exemption out loud ("(ii) tracked paths
    ABSENT from disk (sparse checkout / skip-worktree — an absent file cannot carry benign bytes,
    and absence is not a bytes question)") and the reasoning was WRONG IN THE DIRECTION THAT
    MATTERS: absence does not carry benign bytes, it carries NO bytes — so every check that reads
    the tree (the build, the lint, this fingerprint) is answered about a file that is not there,
    while the push ships the blob the index still holds. MEASURED: commit a malicious tracked
    file, `git update-index --skip-worktree`, `rm` it → status EMPTY, fingerprint = the clean-tree
    constant, R25 green, malicious blob pushed.
    The legitimate-looking ways to arrive here are `--assume-unchanged`, `--skip-worktree` and a
    sparse checkout; the message names all three, because an honest operator who has one of them
    on deserves to be told which, and none of them describes a tree that may serve as push
    evidence.
    """


class IndexFlagsSet(GitFiltersPresent):
    """The index carries assume-unchanged / skip-worktree bits — the flags that make status lie.

    ROUND 8 / P1-A. The two reproductions above both begin with `git update-index`. The bits are
    readable DIRECTLY (`git ls-files -v`: a lowercase tag is assume-unchanged, `S` is
    skip-worktree), so this file reads them rather than inferring their consequences one symptom
    at a time. Measured on this catalog: 5,745 tracked paths, ZERO non-`H` tags — so the refusal
    costs an honest tree nothing, and a tree that has one is telling git to stop reporting the
    truth about a path a release is about to ship.
    NOT part of this check, stated: the other `ls-files -v` tags (`M` unmerged, and the tags that
    only appear with --deleted/--modified/--others). Verified on a deliberately dirty tree — one
    modified, one deleted, one staged, one untracked path — that every tag stayed `H`, so this
    refusal is exactly the two bits and not a proxy for dirtiness.
    """


class ExecBitDivergence(GitFiltersPresent):
    """A tracked regular path whose EXECUTABLE-BIT CLASS on disk is not the one the index records.

    ROUND 9 / P1-1 (TD-2026-07-30-(P1-representation-parity)). Round 8 separated regular from
    symlink and then compared only BLOB BYTES. git's regular-file modes carry one more bit of
    representation — 100644 vs 100755 — and NOTHING compared it. MEASURED (reviewer round 9,
    reproduced in a throwaway repo): `git config core.fileMode false`, `git update-index --chmod=-x
    backend/gradlew`, commit, then `chmod +x backend/gradlew` on disk → `git status --porcelain` is
    EMPTY (core.fileMode=false tells git to stop reporting the bit), the blob bytes are identical
    so the round-8 sweep is satisfied, and this fingerprint returns the clean-tree constant. R25's
    118 direct `./gradlew` invocations then run happily against the locally-executable file while
    the push records mode 100644 and a fresh checkout cannot execute it at all.
    THE INDEX MODE IS READ FROM `git ls-files -s`, NOT FROM GIT'S FILTERED VIEW. That is the whole
    point: `core.fileMode=false` suppresses the *report*, not the *record*, so the record is what
    is compared, and the refusal is INDEPENDENT of core.fileMode by construction.
    BOTH DIRECTIONS BLOCK. index 100755 + non-executable on disk is the mirror: every gate that
    runs the file (a build script, a hook, a guard) fails locally, so an operator "fixes" it with a
    chmod that the index never learns about — and the push ships the mode nobody tested.
    WHAT IS COMPARED IS THE CLASS, not the bits: git records exactly two regular modes, so any-x
    ⇔ 100755 and no-x ⇔ 100644. A 0o700 vs 0o755 difference is not a divergence and is not flagged.
    Measured on this catalog: 5,745 tracked paths, ZERO divergences — the refusal costs an honest
    tree nothing.
    """


class GitlinkUninitializedDirt(GitFiltersPresent):
    """A gitlink whose worktree path holds FILES but no gitdir — "uninitialized" and yet populated.

    ROUND 9 / P1-2 (TD-2026-07-30-(P1-representation-parity)). Round 8 exempted an UNINITIALIZED
    submodule on the reasoning that "nothing on disk, nothing was tested". The exemption tested the
    wrong thing: it returned success on the ABSENCE OF `<gitlink>/.git` alone, without ever
    requiring the directory to be EMPTY. MEASURED (reviewer round 9): add a gitlink with
    `git update-index --add --cacheinfo 160000,<sha>,vendor/sub`, commit a mandatory check that
    runs `bash vendor/sub/check.sh`, then create `vendor/sub/check.sh` on disk with NO `.git`
    anywhere inside → `git status --porcelain` is EMPTY (git does not descend into a gitlink),
    every sweep returns clean, and R25 executes a file that the push does not ship: a gitlink
    commits only the recorded sha, and a fresh clone of it gets an EMPTY directory.
    THE CORRECTED RULE: an uninitialized gitlink is acceptable ONLY when its worktree path is
    ABSENT or an ACTUALLY-EMPTY directory. The fresh-clone case — which is why round 8 declined to
    block at all, and which is still right — is exactly the empty-directory case, and all three
    gitlinks in this catalog are in it. Anything else BLOCKS.
    DISTINCT FROM docs/BACKLOG.md P3-119, which is dirt inside an INITIALIZED submodule's own work
    tree (that repository's fingerprint to compute). This is content under a gitlink that has no
    repository at all, so there is nothing else that could ever account for it.
    """


class CasefoldAlias(GitFiltersPresent):
    """Two index entries differing only in CASE that are ONE file on a case-insensitive filesystem.

    ROUND 9 / (e) (TD-2026-07-30-(P1-representation-parity)). On APFS (and NTFS, and a
    case-insensitive HFS+) `A.sh` and `a.sh` are the same file. git's index can hold BOTH, with
    different blobs; the filesystem can hold only one. Every read this file makes — and every read
    the build makes — then answers about that one file for both entries, so one of the two blobs is
    a byte claim about a file that is not on disk, and the push ships both.
    THE TEST IS A MEASUREMENT, NOT AN ASSUMPTION ABOUT THE FILESYSTEM: two index paths whose
    casefolded forms are equal are flagged only when they lstat to the SAME (st_dev, st_ino), i.e.
    when the aliasing is observed. On a case-sensitive filesystem the same repository yields two
    distinct inodes and nothing is refused, so this costs a Linux fork-receiver nothing.
    Measured on this catalog: ZERO casefold collisions and ZERO inode aliases among 5,745 tracked
    paths, so the refusal costs this tree nothing either.
    ROUND 10 SCOPE CORRECTION: this class is now the LEAF half of the alias family — it fires when
    every aliased spelling is a full tracked path. A shared DIRECTORY component is the other half
    and raises CasefoldDirectoryAlias; round 9 could not see it at all.
    ROUND 11 SCOPE CORRECTION: "differing only in CASE" was ALSO too narrow, in two directions at
    once, because the key was `bytes.lower()`. It is now canonical caseless (`_fold_path_key`), so
    this class covers a leaf pair that differs by UNICODE NORMALIZATION (`é` ≡ `e◌́`) or by
    NON-ASCII case (`É` ≡ `é`) exactly as it covers `A.sh` ≡ `a.sh`. The inode discriminator is
    unchanged, so the cost to a case- or normalization-SENSITIVE fork-receiver is still zero.
    ROUND 12 SCOPE CORRECTION: a THIRD axis, and the same shape of miss. Case-insensitive HFS+
    folds designated formatting controls to ZERO (Apple TN1150), so `safe.sh` ≡ `safe<U+200C>.sh`
    is ONE file — MEASURED on a real HFS+ volume — while round 11's key preserved them. The fold
    now strips general category Cf first, so this class covers that pair too; the discriminator is
    STILL unchanged, so a filesystem that distinguishes them (measured: both APFS variants) yields
    two inodes and is not refused.
    """


class CasefoldDirectoryAlias(GitFiltersPresent):
    """A DIRECTORY component that two index entries spell differently and the filesystem serves once.

    ROUND 10 / P1 (TD-2026-07-31-(P1-casefold-prefix)). Round 9 grouped COMPLETE folded paths, so
    the alias it could see was the leaf one (`Alias.txt` ≡ `alias.txt`). The reviewer's topology
    needs no leaf collision at all:
        A/check.sh   — committed, runs `cat A/helper`
        a/helper     — committed, contains PASS
    The two full folded keys differ, so round 9 grouped them apart and returned the clean-tree
    constant; on APFS `A` and `a` are ONE directory, so `cat A/helper` succeeds locally, R25 goes
    green, and the pushed tree records `a/helper` — which a case-sensitive receiver does not serve
    as `A/helper`. The committed check is broken on arrival, and nothing measured it.
    THE MEASUREMENT IS THE SAME ONE: every prefix of every tracked path is folded, and a folded
    group is refused only when two DISTINCT spellings lstat to the SAME (st_dev, st_ino). A
    case-sensitive tree with genuinely distinct `A/` and `a/` directories yields two inodes and is
    NOT refused.
    ROUND 11 CORRECTION TO THIS PARAGRAPH: it used to end "verified with a simulated distinct-inode
    pair, because this machine has no case-sensitive volume to build the twin on". That is no
    longer the state of the harness and was already avoidable — ax-prove-hermetic-runtime.sh (Z5)
    builds the control on a REAL case-sensitive volume, created with
    `hdiutil create -fs "Case-sensitive APFS"`, and MEASURED there: distinct `A/`+`a/` directories
    and distinct `Alias.txt`+`alias.txt` files are NOT refused. Round 11 adds the same real-volume
    control for the non-ASCII case pair (`É/` vs `é/`, distinct inodes → not refused).
    WHAT IS STILL SIMULATED, AND WHY — the NORMALIZATION control. A distinct-inode `é/` + `e◌́/`
    pair requires a normalization-SENSITIVE filesystem, and this machine has none to offer:
    MEASURED, case-insensitive APFS, case-SENSITIVE APFS, case-sensitive HFS+, ExFAT and FAT32 are
    all normalization-INSENSITIVE (the FAT variants additionally rewrite the spelling to NFD on
    write). So that arm runs against synthetic (st_dev, st_ino) identities and says so; it is the
    same discriminator the real arms exercise, and it is labelled SIMULATED in the harness output
    rather than claimed as a live control.
    ROUND 12 — THE IGNORABLE AXIS, AND BOTH ITS ARMS ARE REAL. `SAFE/check.sh` (running
    `cat SAFE/helper`) plus `SAFE<U+200C>/helper` is the same topology with the two directory
    spellings differing only by an invisible formatting control. It is refused on a REAL
    case-insensitive HFS+ volume created with `hdiutil create -fs HFS+` — MEASURED there:
    `SAFE` and `SAFE<U+200C>` (and `<U+202E>`, and `<U+FEFF>`) are ONE inode, exactly the 16 code
    points TN1150's fold table maps to zero and no others (154 further Cf characters measured
    DISTINCT on the same volume). The false-positive control needs no special volume at all: both
    APFS variants serve those two spellings from DISTINCT inodes, so the ordinary sandbox is
    itself the live control, and it is NOT refused.
    HONEST RESIDUE: two spellings that fold equal AND are hardlinks of one inode on a case- or
    normalization-SENSITIVE filesystem would be flagged. That requires deliberately hardlinking
    `A.txt` to `a.txt`; the same property was already true of the leaf check in round 9.
    """


class SymlinkResolutionUnbounded(GitFiltersPresent):
    """A tracked symlink whose target cannot be resolved within the budgets any kernel imposes.

    ROUND 14 / P1-A (TD-2026-08-01-(P1-posix-resolution-and-runtime-paths)). Resolving a target
    the way the kernel does means FOLLOWING intermediate symlinks, and following can cycle
    (`a -> b/x`, `b -> a`) or expand without bound. Round 13's lexical collapse could not cycle,
    so it needed no budget; a correct resolver does.

    THE BUDGET BLOCKS RATHER THAN GOING SILENT, deliberately. When the walk stops early the
    alias question is UNANSWERED, and converting "unanswered" into "nothing found" is the exact
    defect every round of this family since round 8 has been closing. It is also not merely
    conservative: Linux resolves at most 40 link traversals (MAXSYMLINKS) and macOS 32
    (SYMLOOP_MAX), so a chain that exhausts the larger of the two is one NO receiver's kernel
    will resolve — the receiver gets ELOOP, which is a defect in its own right.
    The budget is the LARGER of the two kernel limits precisely so that a chain some receiver
    would resolve is never refused here.
    """


class SymlinkTargetAlias(GitFiltersPresent):
    """A tracked SYMLINK whose TARGET spells a tracked path with an ALIAS of its recorded spelling.

    ROUND 13 / P1 (TD-2026-08-01-(P1-symlink-target-alias)). Rounds 9-12 built an alias census over
    every prefix of every tracked path and widened its key three times (case → normalization →
    non-ASCII case → ignorable Cf). Every one of those rounds registered only INDEX PATHS. A
    symlink's TARGET is not an index path — it is BLOB CONTENT, read here as bytes and hashed
    (line ~735), never resolved and never registered — so the ENTIRE census simply did not apply
    to it, on any of the four axes.
    THE REVIEWER'S REPRODUCTION, commit-only, no environment control:
        git mv backend/gradlew backend/gradlew-real
        ln -s GRADLEW-REAL backend/gradlew          # note the CASE
        git add backend/gradlew backend/gradlew-real && git commit
    On ordinary case-insensitive APFS `backend/gradlew` resolves `GRADLEW-REAL` to the tracked
    `backend/gradlew-real`, so R25 EXECUTES successfully, `git status --porcelain -uall` is EMPTY
    and this file returns the clean-tree constant 0a815065…. On a case-SENSITIVE checkout the
    committed symlink is DANGLING: the receiver gets a broken `backend/gradlew` while our evidence
    says the tree is clean and the build passed. Confirmed live by the reviewer — `backend/gradlew`
    and `backend/GRADLEW` resolve to the same APFS inode, `backend/GRADLEW` is not an indexed
    spelling, the shipped detector produced helper_verdict ([], []), and the target folds equal to
    `backend/gradlew-real` while `target_is_registered_prefix` is FALSE.

    THE RULE THAT SHIPS, stated exactly — it is the alias census EXTENDED TO TARGETS, and nothing
    more. For every tracked index entry of mode 120000 that is a symlink on disk:
      1. resolve the target's BYTES LEXICALLY, relative to the link's own directory (which is the
         index's recorded spelling, so the base is authentic);
      2. if the result is ABSOLUTE, escapes the repository root through `..`, or is the root
         itself → NOT THIS CLASS (see the disposition table below);
      3. lstat the candidate. If it does not exist → NOT THIS CLASS (dangling, below);
      4. look the candidate's (st_dev, st_ino) up in the REGISTERED PREFIX SET — the same
         tracked-paths-and-their-directory-components map the prefix walk already builds. Not
         found → NOT THIS CLASS (the target is untracked);
      5. if the candidate's spelling IS one of the registered spellings for that inode → PASS;
      6. otherwise, if it FOLDS EQUAL (`_fold_path_key`, the SHARED key — so this covers case,
         normalization, non-ASCII case and ignorable Cf in one step, and gains every future axis
         the fold gains) to a registered spelling → BLOCK;
      7. otherwise → NOT THIS CLASS (the candidate reached a tracked inode by some route that is
         not a spelling alias — a hardlink, or an intermediate symlink component that is itself
         committed and resolves at the receiver).
    Step 6 is where precision lives. "The spelling must EQUAL the record" as a bare rule would
    refuse `..` traversal, absolute targets and legitimate chains; gating the refusal on
    FOLD-EQUALITY refuses EXACTLY the aliases and cannot fire on any of those.
    ROUND 14b ADDS NO STEP; IT ADDS SUBJECTS. Steps 3-6 are applied to EVERY component the walk
    resolves — each intermediate it follows and each directory component it traverses — and not
    to the final candidate alone. A component the receiver's kernel must resolve is a spelling
    that can dangle there, so it is a subject of the same census; step 4's untracked exit is what
    keeps this precise, because an intermediate with no recorded spelling has nothing to alias.

    DISPOSITION OF EVERY EDGE CASE, and why:
      · `..` TRAVERSAL that stays inside the repo — resolved, then compared. The two live tracked
        symlinks in this catalog are exactly this shape (`../../.ledger-target.txt` and
        `../../.receipts-target.yaml`), both land on the EXACT recorded spelling, and both pass.
        ROUND 14 replaced the LEXICAL `..` with the kernel's: intermediate components are followed
        FIRST and `..` pops the RESOLVED stack. Round 13's text here conceded only an "under-
        inclusive" divergence and then a "fail-closed, pathological" over-inclusive one; both
        concessions were describing a resolver that answered a different question than the
        receiver's kernel asks, and BOTH directions are now gone by construction rather than by
        argument. The round-13 P1 was found through the under-inclusive direction — see
        `_resolve_link_target.__doc__` for the reviewer's `jump/../GRADLEW-REAL` topology, where
        the lexical candidate was ABSENT and both implementations reported nothing while the
        kernel reached a tracked file. The round-13 over-inclusive counterexample
        (`ln -s outdirlink/../SECRET.txt`, `outdirlink -> ../outside`) now takes the "escapes"
        exit on its first `..`, because the follow has already returned the stack to the root.
        A budget bounds the follow (40, the larger of the two kernel limits) and its exhaustion
        BLOCKS under GIT_SYMLINK_RESOLUTION_UNBOUNDED — never silence.
      · ABSOLUTE target — NOT BLOCKED. It names a location on the receiver's root filesystem; the
        index cannot record it, so there is no recorded spelling to be an alias OF. (An absolute
        target that happens to point back into this checkout is unportable for reasons that have
        nothing to do with aliasing; registered as docs/BACKLOG.md P3-131.)
      · TARGET OUTSIDE THE REPOSITORY (`..` above the root) — NOT BLOCKED, same reason.
      · CHAINED SYMLINK (a target that names another TRACKED symlink) — COMPARED, and correctly:
        `lstat` does not follow the FINAL component, so the candidate's inode is the second link's
        own inode, which the prefix walk registered. An exact spelling passes; an aliased spelling
        of the second link BLOCKS, which is the same defect one level up.
      · TARGET THROUGH AN INTERMEDIATE SYMLINK DIRECTORY, SPELLED AS THE INDEX RECORDS IT — NOT
        BLOCKED, and ROUND 14 changed WHY. Round 13 kept the link's own name in the candidate, so
        the spelling failed to fold-equal the record and step 6 declined — the right verdict for
        the wrong reason. Step 1 now FOLLOWS the intermediate, so the candidate is the real file's
        own path and step 5 passes it on an EXACT spelling match. The verdict is unchanged
        (exit 0) and it is now reached by agreeing with the record instead of by failing to
        recognise it. The same holds through SEVERAL correctly-spelled intermediates and through a
        correctly-spelled tracked DIRECTORY component: each is judged by steps 3-6 and each passes
        at step 5.
      · TARGET THROUGH AN INTERMEDIATE THAT IS AN ALIAS — BLOCKED, ROUND 14b, and this is the
        entry round 14 did not have. Steps 3-6 are applied to EVERY component the walk resolves,
        not to the final candidate alone. `-> DIRLINK/` and `-> DIRLINK/real.txt` over a tracked
        `dirlink` now block exactly as `-> DIRLINK` already did; before 14b the first two exited
        0 while the third exited 15, which is a one-character bypass of this very gate.
      · UNTRACKED INTERMEDIATE — NOT BLOCKED, deliberately, and for the same reason an untracked
        TARGET is not: there is no recorded spelling for it to be an alias OF, so no comparison
        exists to make. A link through a gitignored build directory (or through an untracked
        symlink inside one) is ordinary. The receiver's outcome is governed by whether the
        intermediate is shipped at all, which is the dangling class (P3-132), not this one.
      · TARGET RESOLVING TO A DIRECTORY THAT IS A TRACKED PREFIX — COMPARED, because the registry
        is the PREFIX map, not the leaf set. `link -> BACKEND` where the index records `backend`
        BLOCKS; `link -> backend` passes.
      · UNTRACKED TARGET — NOT BLOCKED. There is no recorded spelling to alias. A link to a build
        output or a gitignored path is ordinary and portable-by-intent.
      · DANGLING TARGET — NOT BLOCKED, deliberately, and this is the one worth arguing. It is a
        real defect, but it is a DIFFERENT defect class and it is not one this gate exists to
        catch: the whole doctrine of the round-8..12 family is "the tree R25 verified is not the
        tree the push ships". A dangling symlink is IDENTICALLY broken here and at the receiver, so
        R25's evidence does not LIE about it — whatever step would have read it failed here first.
        Blocking it would additionally refuse the legitimate shapes above (a link to an ignored
        build output that has not been built yet is dangling in a fresh clone). Registered as
        docs/BACKLOG.md P3-132 with the honest grade.
      · DIRTY symlinks are not examined: their on-disk target is not the committed one, and the
        recency guard's own clean-tree precondition (check 12a) refuses a dirty tree outright.

    THE DISCRIMINATOR IS THE SAME MEASUREMENT the rest of the family uses — an OBSERVED
    (st_dev, st_ino) identity plus a fold equality, never an assumption about the filesystem. On a
    case-sensitive receiver `ln -s GRADLEW-REAL` simply does not resolve, so there is no inode to
    match and nothing is refused; the refusal exists precisely where the local resolution is a lie
    about the pushed tree. Measured on this catalog: 5,745 tracked entries, 2 tracked symlinks,
    ZERO refusals — it costs an honest tree nothing.
    """


class GitlinkDivergence(GitFiltersPresent):
    """An INITIALIZED submodule whose work tree is not at the commit the index records.

    ROUND 8 / P1-A. Round 7 exempted gitlinks with "no bytes of ours on disk", which is true of
    the SUPERPROJECT's blobs and false of what a run actually tests: if the submodule work tree
    sits at a different commit than the gitlink records, R25 tested one submodule and the push
    ships the other. That IS bindable and is bound here — the recorded sha must equal the
    submodule's HEAD.
    WHAT IS DELIBERATELY NOT BLOCKED: an UNINITIALIZED submodule (no `.git` inside it). All three
    gitlinks in this catalog are in that state (empty fixture directories), it is the ordinary
    post-clone shape, and nothing was tested from it either — blocking it would refuse every fresh
    clone to close nothing. Also not covered: dirt INSIDE an initialized submodule's own work
    tree, which is that repository's fingerprint to compute, not this one's. Registered as
    docs/BACKLOG.md P3-119.
    """


class RawIndexDivergence(GitFiltersPresent):
    """A tracked path git reports as unmodified whose RAW on-disk bytes are not its index blob.

    ROUND 7 / P1-3(b) — THE ROBUST HALF, and the reason the broadened declaration test is not the
    whole answer. Refusing declarations is a claim about the config keys we thought to enumerate;
    THIS is a measurement. For every tracked path git's (filter-aware) status calls clean, the
    bytes on disk are hashed as a git blob HERE, in python, with no git process in the loop, and
    compared to the object id git recorded in the index. A `.process` driver (or a `.clean`, or
    any future protocol) that makes a divergent file look clean lands on a mismatch it cannot
    reach — the comparison never asks git what the file contains.

    Subclasses GitFiltersPresent on purpose: every existing caller already BLOCKS on that type,
    so a new failure mode cannot slip through an `except` that predates it.
    """


def _git_bin():
    """The absolute git binary, validated. AX_GIT_BIN is published by the hermetic bootstrap of
    whatever entry started us; it is re-validated here rather than trusted, because this file is
    also run directly."""
    cand = os.environ.get("AX_GIT_BIN") or ""
    if not (cand.startswith("/") and os.path.isfile(cand) and os.access(cand, os.X_OK)):
        cand = shutil.which("git", path=os.defpath + os.pathsep + os.environ.get("PATH", "")) or ""
    if not cand:
        raise RuntimeError("git is not available")
    return cand


def _git_env():
    """A scrubbed environment: no inherited GIT_* of any kind, replacement refs off."""
    env = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}
    env["GIT_NO_REPLACE_OBJECTS"] = "1"
    return env


def _git(repo, *args, _bin=None, _env=None):
    p = subprocess.run([_bin or _git_bin(), "--no-replace-objects", "-C", repo, *args],
                       stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                       env=_env if _env is not None else _git_env())
    if p.returncode != 0:
        raise RuntimeError("git " + " ".join(args))
    return p.stdout


def _filters_declared(repo, gbin, genv):
    """Return a human description of any content-filter machinery in <repo>, else None."""
    # ROUND 7 / P1-3: ANY key under any filter driver. `filter.<n>.process` performs the same
    # conversion through the long-running protocol and matched neither `clean` nor `smudge`.
    p = subprocess.run([gbin, "--no-replace-objects", "-C", repo, "config", "--get-regexp",
                        r"^filter\."],
                       stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=genv)
    if p.returncode == 0 and p.stdout.strip():
        return ("git config declares content filters: "
                + p.stdout.decode(errors="replace").strip().splitlines()[0])
    p = subprocess.run([gbin, "--no-replace-objects", "-C", repo, "rev-parse",
                        "--absolute-git-dir"],
                       stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=genv)
    if p.returncode == 0 and p.stdout.strip():
        attrs = os.path.join(p.stdout.decode().strip(), "info", "attributes")
        try:
            if os.path.isfile(attrs) and os.path.getsize(attrs) > 0:
                return f"{attrs} is non-empty (untracked, unreviewed filter attachment point)"
        except OSError:
            return f"{attrs} could not be inspected"
    return None


def _object_algo(repo, gbin, genv):
    """The repository's object-id hash. sha1 unless the repo was created with sha256."""
    p = subprocess.run([gbin, "--no-replace-objects", "-C", repo, "rev-parse",
                        "--show-object-format"],
                       stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=genv)
    name = p.stdout.decode(errors="replace").strip() if p.returncode == 0 else ""
    return "sha256" if name == "sha256" else "sha1"


def _blob_id(data, algo):
    """git's object id for <data> as a blob, computed here — no git process in the loop."""
    h = hashlib.new(algo)
    h.update(b"blob %d\0" % len(data))
    h.update(data)
    return h.hexdigest().encode()


def _gitlink_uninitialized_dirt(full, show):
    """ROUND 9 / P1-2: an uninitialized gitlink may be ABSENT or EMPTY — never populated.

    Returns a description when the worktree path holds anything, else None. `os.listdir` is the
    whole test: a gitlink ships only its recorded sha, so a fresh clone of this commit produces an
    empty directory, and anything a run read from a populated one is code the receiver never gets.
    """
    if not os.path.exists(full):
        return None                                     # absent: the ordinary pre-init shape
    if not os.path.isdir(full):
        return ("%s (a gitlink whose worktree path is not a directory at all)" % show)
    try:
        kids = os.listdir(full)
    except OSError as exc:
        return ("%s (uninitialized gitlink whose directory could not be listed: %s)"
                % (show, exc.strerror or exc))
    if not kids:
        return None                                     # empty: the ordinary post-clone shape
    names = ", ".join(sorted(os.fsdecode(k) for k in kids)[:4])
    # BACKLOG P3-123 — CLOSED BY DECISION: KEEP THE REFUSAL, IMPROVE THE MESSAGE.
    # The overwhelmingly common way to reach this on macOS is a Finder-created `.DS_Store` inside
    # an otherwise empty uninitialized gitlink. A NAME ALLOWLIST was considered and REJECTED:
    # `.DS_Store` is a real file with arbitrary, attacker-controllable bytes, and the invariant
    # here is about ANY bytes the receiver will not get — so exempting a NAME cannot discharge
    # "this directory cannot hide real content". (Gitignoring it does not help either: the test is
    # `os.listdir`, not `git status`.) The cost of keeping the refusal is one confusing message,
    # so the message now names the likely cause and the one-line fix instead of leaving an
    # operator to guess.
    hint = ""
    if any(os.fsdecode(k) == ".DS_Store" for k in kids):
        hint = (" — this looks like macOS Finder noise: `.DS_Store` is present. It is still REAL "
                "CONTENT with arbitrary bytes, so it is not exempted by name; remove it with "
                "`find %s -name .DS_Store -delete` and re-run" % show)
    return ("%s (uninitialized gitlink — no gitdir — yet its directory holds %d entr%s: %s)%s"
            % (show, len(kids), "y" if len(kids) == 1 else "ies", names, hint))


def _lstat_cached(cache, rootb, rel):
    """lstat <rootb>/<rel> once per DISTINCT relative path. Returns the stat result, or None.

    ROUND 10 / P1: the prefix walk below visits every directory component of every tracked path,
    so `practices/` is reached ~700 times on this catalog. The cache makes the walk cost the number
    of DISTINCT prefixes (files + directories) rather than the number of (entry, component) pairs,
    and the full path is itself the last prefix — so the exec-bit check reads the same cached stat
    instead of taking a second one. Measured net effect on 5,745 entries: +~1.1k lstat calls for
    the directories, -5,745 for the shared full-path stat.
    """
    try:
        return cache[rel]
    except KeyError:
        try:
            st = os.lstat(os.path.join(rootb, rel))
        except OSError:
            st = None
        cache[rel] = st
        return st


def _fold_path_key(pfx, cache=None):
    """ROUND 11 / P1: the CANONICAL CASELESS key of a path prefix (bytes in, bytes out).

    ROUNDS 9-10 KEYED WITH `bytes.lower()`, WHICH IS ASCII-ONLY AND NORMALIZATION-BLIND. Two
    aliases the filesystem serves from one inode therefore never met:
      · NORMALIZATION. UTF-8 NFC `é` is c3a9; NFD `e` + U+0301 is 65cc81. `bytes.lower()` leaves
        them distinct, so the shared inode was never compared. MEASURED on APFS (this machine,
        beee364): index `é/check.sh` (NFC, runs `cat é/helper`) + index `e◌́/helper` (NFD),
        `git status --porcelain -uall` EMPTY, one directory inode, `bash é/check.sh` → PASS,
        fingerprint = the clean-tree constant 0a815065… — while the PUSHED tree records only
        `e◌́/helper`, which a normalization-SENSITIVE receiver does not serve as `é/helper`.
      · NON-ASCII CASE. `bytes.lower()` maps A-Z and nothing else, so `É/` (c389) and `é/` (c3a9)
        folded apart even though APFS serves them from one directory. Same defect, same silence.

      · IGNORABLE FORMAT CHARACTERS — ADDED ROUND 12, a THIRD equivalence axis, independent of
        both of the above. Case-insensitive HFS+ folds designated formatting controls to ZERO and
        skips them entirely (Apple TN1150, `FastUnicodeCompare`: "All ignorable characters are
        folded to the value zero"), so `SAFE/` and `SAFE<U+200C>/` are ONE directory. Rounds 9-11
        preserved those code points, so the two spellings keyed apart and their shared inode was
        never compared. MEASURED on a real case-insensitive HFS+ volume built here with
        `hdiutil create -fs HFS+`: `SAFE` and `SAFE<U+200C ZWNJ>` are ONE inode, as are
        `SAFE<U+202E RLO>` and `SAFE<U+FEFF>` — while the pushed tree records only the spelling
        with the invisible character, which a receiver that treats it as significant does not
        serve as `SAFE/helper`.

    THE KEY IS UNICODE CANONICAL CASELESS MATCHING OVER IGNORABLE-STRIPPED INPUT:
    NFC(casefold(NFD(strip_Cf(s)))).
      · The INNER NFD is UAX #21 §1.3 ("Default Caseless Matching" is defined over NFD forms).
        It is load-bearing, not ceremony — MEASURED here: `casefold()` with no normalization at
        all separates the NORMALIZATION pair (`é` NFC vs `e`+U+0301 NFD). CORRECTED 2026-08-01:
        this sentence used to say it separates "every pair above", which is FALSE — unnormalized
        `casefold()` already equates `É`/`é` AND `ſ`/`s`; only the normalization pair needs the
        NFD. The correction that introduced the previous sentence fixed one overclaim and left
        this one standing three lines away, which is the recurring defect: a partial correction
        that leaves the same disproved claim alive elsewhere.
      · The OUTER normalization is NOT load-bearing — CORRECTED 2026-08-01 by an independent
        verification lane that measured it. The original sentence here claimed U+1E9B U+0323 vs
        U+1E69 folds EQUAL only WITH an outer normalization; that comparison was against
        `casefold()` with no normalization at all. Given the INNER NFD, `casefold(NFD(s))` alone
        already equalizes every cited pair, and over 14,284 inputs the with-outer and inner-only
        variants induce an IDENTICAL partition. The outer step is redundant-but-harmless key
        canonicalization, kept because this value is only ever compared for EQUALITY and a
        canonical spelling is cheaper to reason about. It is retained, not removed, so that the
        key never depends on which unnormalized spelling reached it first — but calling it
        load-bearing was an overclaim, and an overclaim about one's own evidence is the exact
        defect this catalog keeps finding in its guards.
      · The outer form is NFC rather than the standard's NFD purely because this value is only
        ever compared for EQUALITY, and two strings are canonically equivalent iff their NFC forms
        are equal iff their NFD forms are equal. NFC is the shorter key and the conventional
        canonical target on this platform (git spells it `core.precomposeunicode`).
      · `casefold()` and not `lower()`: full case folding. `lower()` is a locale/round-trip
        operation (U+017F ſ lowercases to itself but folds to `s`; U+212A K folds to `k`), and
        caseless MATCHING is what a filesystem does.
      · WHY GENERAL CATEGORY Cf AND NOT A HAND-LIST — decided by measurement, not by taste. The
        strip set must be a SUPERSET of what a target filesystem ignores: a MISSING character is a
        silent false-green, while an EXTRA one cannot produce a refusal on its own, because the
        (st_dev, st_ino) discriminator below still requires an OBSERVED shared inode. Measured:
          — HFS+'s ignorable set is EXACTLY 16 code points (U+200C-200F, U+202A-202E, U+206A-206F,
            U+FEFF), derived from the published fold table and then CONFIRMED 16/16 against the
            live volume. All 16 are general category Cf.
          — Cf is 170 code points (Unicode 16.0 runtime). The extra 154 are inert: the same live
            HFS+ volume gives all 154 DISTINCT inodes, i.e. no measured filesystem folds them.
          — Default_Ignorable_Code_Point was REJECTED. Python exposes no such property, so it
            would have to ship as a 4,174-code-point table pinned to a UCD release (3,769 of them
            UNASSIGNED), and it is not even a superset of Cf — it EXCLUDES 32 Cf characters (the
            prepended concatenation marks, U+FFF9-FFFB, U+13430-1343F). It would ADD variation
            selectors and Hangul fillers, which the live volume measurably does NOT ignore.
          — U+202E was CHECKED rather than assumed (it is a bidi control, and some derivations
            are said to omit it): it IS Cf, it IS Default_Ignorable in DerivedCoreProperties
            17.0.0, and the live volume DOES fold it. Covered under every candidate.
          — U+0000 is deliberately NOT ignorable in TN1150 (the algorithm maps NUL to a non-zero
            sentinel so it can serve as the end-of-string marker), and NUL cannot occur in a path.
        A category predicate also cannot go stale the way a literal list can.
      · WHY THE STRIP RUNS FIRST. Removing a combining-class-0 character can unblock canonical
        reordering of the marks around it, so stripping before NFD yields a strictly more
        canonical result than stripping after. One pass is provably enough: MEASURED over all
        1,114,112 scalars, neither `casefold(NFD(·))` nor `NFC(·)` ever INTRODUCES a Cf character
        (0 of them), so nothing downstream can put back what the strip removed.

    The ASCII fast path is not an approximation: for pure-ASCII input NFD and NFC are the
    identity, full casefold coincides exactly with `lower()`, and — MEASURED — no ASCII scalar is
    category Cf, so an ASCII-only prefix can contain nothing the strip would remove. It therefore
    returns the same bytes the slow path would, on all three axes. It exists so the live catalog's
    ~6.8k distinct prefixes — all ASCII — cost what they cost before. Note it does NOT create a
    seam: a non-ASCII prefix that folds INTO ASCII (`ſ` → `s`, or `SAFE<U+200C>` → `safe`) still
    meets the ASCII spelling, because the slow path produces the same key.

    NON-UTF-8 PATHS ARE NOT A CRASH AND NOT A BLOCK. Paths are bytes; Linux permits any non-NUL
    byte, and a fork-receiver whose tree holds a latin-1 filename is not doing anything wrong.
    The decode is `surrogateescape`, so undecodable bytes survive as lone surrogates, pass through
    normalization and casefold untouched (they are unassigned, combining class 0), and re-encode
    to the ORIGINAL bytes — `A/\\xff/b` still folds its ASCII half to `a/\\xff/b`. The bare
    `except` is a belt only (surrogateescape does not raise); it falls back to the round-10
    `lower()` key, which can only ever group a byte-identical spelling with itself, i.e. it
    reports nothing and blocks nothing. An unreadable spelling is not evidence of an alias.
    """
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
            key = pfx.lower()
    if cache is not None:
        cache[pfx] = key
    return key


def _register_prefixes(casefold, statcache, rootb, path, foldcache=None):
    """ROUND 10 / P1: fold EVERY PATH PREFIX, not just the full path.

    casefold maps a folded prefix to {(st_dev, st_ino): {spellings}}. A folded key holding two
    DISTINCT spellings under ONE inode is an observed alias; the same key under two inodes is a
    case-sensitive filesystem doing exactly what it should, and is not an alias.
    Called for EVERY index entry INCLUDING gitlinks — round 9 registered the map after the 160000
    `continue`, so a submodule path could alias a directory and never be looked at (the reviewer
    registered that omission separately; it closes here because it is the same walk).
    ROUND 11 / P1: the key is `_fold_path_key`, i.e. canonical caseless, not `bytes.lower()`. The
    (st_dev, st_ino) DISCRIMINATOR IS UNCHANGED and is what keeps this measurement free of false
    positives — a case-sensitive or normalization-sensitive fork-receiver whose tree genuinely
    holds two such directories yields two inodes and two singleton groups, and is not refused.
    """
    comps = path.split(b"/")
    for n in range(1, len(comps) + 1):
        prefix = b"/".join(comps[:n])
        st = _lstat_cached(statcache, rootb, prefix)
        if st is None:
            continue
        casefold.setdefault(_fold_path_key(prefix, foldcache), {}).setdefault(
            (st.st_dev, st.st_ino), set()).add(prefix)


# ROUND 14 / P1-A. Two budgets, both stated as the RECEIVER'S kernel would state them.
# Linux caps a single resolution at 40 link traversals (MAXSYMLINKS, `man 7 path_resolution`);
# macOS/BSD caps it at 32 (SYMLOOP_MAX). The LARGER of the two is used on purpose: a chain that
# some receiver's kernel would happily resolve must never be refused here, so the budget refuses
# only what EVERY kernel refuses. The component budget is a second, independent stop for a target
# that grows without ever re-following the same link.
_SYMLINK_FOLLOW_BUDGET = 40
_SYMLINK_STEP_BUDGET = 4096


def _resolve_link_target(rootb, linkpath, target):
    """ROUND 14 / P1-A: resolve <target> THE WAY THE KERNEL DOES — component by component.

    <linkpath> is an INDEX path, so the base directory is the spelling the repository records —
    the resolution starts from an authentic base rather than from anything on disk. (git cannot
    record a path THROUGH a symlink, so every directory component of an index path is a real
    directory; the base therefore needs no resolution of its own.)
    Returns (kind, candidate, walked):
      ("absolute", None) · ("escapes", None) · ("root", None) · ("inside", b"a/b")
      ("unbounded", b"<reason>")  — the walk exceeded a budget; the CALLER BLOCKS, see below.
    <walked> is the ROUND 14b addition: [(spelling, lstat)] for EVERY NON-FINAL component the walk
    resolved — every intermediate the receiver's kernel must itself resolve before it can reach
    the candidate. The caller takes the SAME alias verdict on each of them; see
    `_symlink_target_verdicts` and SymlinkTargetAlias.__doc__ for why the final component alone
    was a one-character bypass.

    WHY THIS IS NOT LEXICAL ANY MORE. Round 13 collapsed `..` TEXTUALLY, before following
    anything. The kernel pops `..` AFTER following an intermediate symlink, and the two answers
    are different paths whenever a component before the `..` is a link. Reviewer's topology
    (round 14), all four paths committed:
        backend/real/gradlew-real     (tracked regular file)
        backend/real/sub/.keep        (tracked)
        backend/jump -> real/sub      (tracked symlink)
        backend/gradlew -> jump/../GRADLEW-REAL
    POSIX follows `jump` first, so `jump/..` is `backend/real` and the target is
    `backend/real/GRADLEW-REAL` — which on case-insensitive APFS IS the tracked
    `backend/real/gradlew-real`, so R25 executes the wrapper and goes green, while a
    case-SENSITIVE receiver gets a DANGLING `backend/gradlew`. The lexical resolver produced
    `backend/GRADLEW-REAL`, which does not exist, so BOTH implementations took their dangling
    exit and reported nothing. The same divergence is observable with no filesystem writes at all
    on macOS via the stock `/var` symlink: `/var/../TFTPBOOT` and `/private/tftpboot` are one
    inode while the lexical `/TFTPBOOT` is absent.
    The rules below are the kernel's, and only the kernel's:
      · a component is FOLLOWED iff something remains after it — `lstat` does not follow the
        final component, which is what keeps the chained-symlink disposition correct (and what
        a trailing slash legitimately overrides, because then something DOES remain);
      · following replaces the component with the link's target, so `..` afterwards pops from the
        RESOLVED stack;
      · an intermediate link with an ABSOLUTE target leaves for the receiver's root filesystem —
        "absolute", not this class;
      · a missing or unreadable intermediate is left alone: the final lstat then fails and the
        caller takes the dangling exit, exactly as before.

    THE BUDGET BLOCKS, IT DOES NOT GO SILENT. Exceeding either budget means this walk cannot
    answer whether the target aliases a tracked spelling, and "cannot answer" is the one state
    this family never converts into silence (that conversion is the defect every round since 8
    has been closing). A committed link chain that no kernel will resolve is also a defect in its
    own right — the receiver gets ELOOP — so the refusal is not merely conservative.

    ROUND 14b / P1 — AND THAT TRAILING SLASH IS WHY <walked> EXISTS. Following an intermediate is
    correct, but round 14 then took the ALIAS VERDICT on the FINAL candidate only, so following an
    intermediate DISCARDED its spelling. One keystroke moved a blocked alias into the unrefused
    position (measured, same tree, tracked `legit/dirlink -> sub` over tracked `legit/sub`):
        ln -s DIRLINK  legit/x   → exit 15   (final component: the round-13/14 class)
        ln -s DIRLINK/ legit/x   → exit 0    (SAME alias, now an INTERMEDIATE, unrefused)
    The trailing slash is honoured legitimately — the kernel does follow the final component when
    one is present — and the follow lands on the correctly-spelled `legit/sub`, which passes on an
    EXACT match while `legit/DIRLINK`, the spelling that actually dangles at a case-sensitive
    receiver, was never asked about. `-> DIRLINK/real.txt` is the same hole without the keystroke.
    Every component this walk resolves is now returned and judged.

    P3-133 IS SUPERSEDED BY THIS FUNCTION, not by its proposed remedy. That row asked for
    "cross-check the lexical candidate against realpath and go SILENT on divergence"; silence on
    divergence is precisely what the round-14 topology exploits, so adopting it would have
    PRESERVED the P1. Resolving correctly removes the divergence instead of tolerating it, and
    the round-13 over-inclusive counterexample (`ln -s outdirlink/../SECRET.txt` with
    `outdirlink -> ../outside`) now takes the "escapes" exit on the FIRST `..`, because the
    follow has already returned the stack to the repository root.
    """
    walked = []
    if target.startswith(b"/"):
        return ("absolute", None, walked)
    stack = linkpath.split(b"/")[:-1]
    queue = list(target.split(b"/"))
    follows = 0
    steps = 0
    while queue:
        comp = queue.pop(0)
        steps += 1
        if steps > _SYMLINK_STEP_BUDGET:
            return ("unbounded",
                    b"resolution consumed more than %d path components"
                    % _SYMLINK_STEP_BUDGET, walked)
        if comp in (b"", b"."):
            continue
        if comp == b"..":
            if not stack:
                return ("escapes", None, walked)
            stack.pop()
            continue
        stack.append(comp)
        if not queue:
            break                               # FINAL component: lstat does not follow it
        cur = b"/".join(stack)
        try:
            st = os.lstat(os.path.join(rootb, cur))
        except OSError:
            continue                            # missing intermediate: the final lstat decides
        # ROUND 14b / P1: this component is NOT the final one, so the receiver's kernel must
        # resolve THIS SPELLING to get anywhere. Record it before the follow (the follow pops it
        # off the stack, which is exactly how round 14 lost it) — no extra syscall, the lstat is
        # the one the walk already needed.
        walked.append((cur, st))
        if not stat.S_ISLNK(st.st_mode):
            continue
        follows += 1
        if follows > _SYMLINK_FOLLOW_BUDGET:
            return ("unbounded",
                    b"resolution followed more than %d symlinks (no kernel resolves this: "
                    b"Linux MAXSYMLINKS is 40, macOS SYMLOOP_MAX is 32)"
                    % _SYMLINK_FOLLOW_BUDGET, walked)
        try:
            nxt = os.readlink(os.path.join(rootb, cur))
        except OSError:
            continue                            # unreadable intermediate: as above
        if not isinstance(nxt, bytes):
            nxt = os.fsencode(nxt)
        if nxt.startswith(b"/"):
            return ("absolute", None, walked)   # leaves for the receiver's root filesystem
        stack.pop()                             # step back into the LINK'S OWN directory
        queue = nxt.split(b"/") + queue
    if not stack:
        return ("root", None, walked)
    return ("inside", b"/".join(stack), walked)


def _symlink_target_verdicts(rootb, symlinks, inodes, foldcache):
    """ROUND 13 / P1 + ROUND 14 / P1-A: the alias census, applied to what a symlink POINTS AT.

    <inodes> maps (st_dev, st_ino) -> {registered spellings}; it is built from the SAME prefix
    walk that feeds the casefold map, so "registered" means "a tracked path or a directory
    component of one". The full disposition table — and why each non-blocking case is not blocked
    — is in SymlinkTargetAlias.__doc__; this function is the seven steps stated there.
    Returns (alias_reports, unbounded_reports): the second list is the round-14 budget refusal,
    reported under its OWN code because its remedy is different (unbreak the chain, not respell
    the target).
    """
    out = []
    unbounded = []
    for path, target in symlinks:
        kind, cand, walked = _resolve_link_target(rootb, path, target)
        # ROUND 14b / P1: the SAME verdict on EVERY component the walk resolved, not on the final
        # candidate alone. Round 14 asked the alias question once, at the end, so an alias that
        # sat in an INTERMEDIATE position was refused by neither resolver — `-> DIRLINK` blocked
        # (exit 15) while `-> DIRLINK/` and `-> DIRLINK/real.txt` passed (exit 0) over the very
        # same tracked `dirlink`. The remedy is identical (respell the target), so the CODE is
        # identical; only the sentence changes, to name the component that dangles. This runs for
        # EVERY kind, including "escapes"/"root"/"unbounded": an intermediate the receiver cannot
        # resolve is broken there no matter where the walk ended up afterwards.
        if walked:
            seen = set()
            for wsp, wst in walked:
                if wsp in seen:
                    continue                    # a cycle re-visits a spelling; report it once
                seen.add(wsp)
                wnames = inodes.get((wst.st_dev, wst.st_ino))
                if not wnames or wsp in wnames:
                    continue                    # untracked intermediate, or the recorded spelling
                wkey = _fold_path_key(wsp, foldcache)
                walias = sorted(n for n in wnames
                                if _fold_path_key(n, foldcache) == wkey)
                if not walias:
                    continue                    # a tracked inode reached by a non-alias route
                out.append("%s -> %s (resolves HERE THROUGH the intermediate component %s, which "
                           "this repository records as %s)"
                           % (path.decode(errors="replace"), target.decode(errors="replace"),
                              wsp.decode(errors="replace"),
                              " / ".join(n.decode(errors="replace") for n in walias)))
        if kind == "unbounded":
            unbounded.append("%s -> %s (%s)"
                             % (path.decode(errors="replace"),
                                target.decode(errors="replace"),
                                cand.decode(errors="replace")))
            continue
        if kind != "inside":
            continue                            # absolute / escapes the repo / the root itself
        try:
            st = os.lstat(os.path.join(rootb, cand))
        except OSError:
            continue                            # dangling — a different defect class (P3-132)
        names = inodes.get((st.st_dev, st.st_ino))
        if not names:
            continue                            # the target is not a tracked path at all
        if cand in names:
            continue                            # the spelling the index records: exactly right
        key = _fold_path_key(cand, foldcache)
        alias = sorted(n for n in names if _fold_path_key(n, foldcache) == key)
        if not alias:
            continue                            # a tracked inode reached by a non-alias route
        out.append("%s -> %s (resolves HERE to %s, which this repository records as %s)"
                   % (path.decode(errors="replace"), target.decode(errors="replace"),
                      cand.decode(errors="replace"),
                      " / ".join(n.decode(errors="replace") for n in alias)))
    return sorted(out), sorted(unbounded)


def _alias_verdicts(casefold, fullpaths):
    """Split the observed aliases into (leaf, directory). ROUND 10 / P1.

    A group whose spellings are ALL full tracked paths is the round-9 leaf case
    (GIT_CASEFOLD_ALIAS). A group in which any spelling is a directory COMPONENT is the round-10
    case (GIT_CASEFOLD_DIR_ALIAS) — it is reported apart because the remedy is different (rename a
    directory, not a file) and because the leaf demo and the directory demo must be distinguishable
    in the falsification harness.
    The spellings are a SET, so a path listed once per stage during a merge conflict cannot produce
    the "path ≡ path" self-report round 9 would have emitted (registered by the reviewer as a P3;
    it closes here because the set is what the prefix walk needed anyway).
    """
    leaf, directory = [], []
    for byident in casefold.values():
        for names in byident.values():
            if len(names) < 2:
                continue
            shown = " ≡ ".join(sorted(n.decode(errors="replace") for n in names))
            (leaf if all(n in fullpaths for n in names) else directory).append(shown)
    return sorted(leaf), sorted(directory)


def _gitlink_divergence(repo, gbin, genv, path, want):
    """ROUND 8 / P1-A + ROUND 9 / P1-2. Returns (bucket, message) or None.

    bucket is "dirt" for an UNINITIALIZED gitlink that is nonetheless populated (round 9) and
    "diverged" for an INITIALIZED submodule that is not at the recorded commit (round 8). The two
    are reported under different codes because they are different lies: one ships an empty
    directory where a run read files, the other ships a different commit than the run tested.
    """
    full = os.path.join(os.fsencode(repo), path)
    show = path.decode(errors="replace")
    if not os.path.exists(os.path.join(full, b".git")):
        # ROUND 9 / P1-2: "uninitialized" is not by itself a pass. It is a pass only when there is
        # nothing there — the round-8 exemption tested for the absence of a gitdir and forgot to
        # test for the absence of FILES, which is the state a populated fake submodule occupies.
        dirt = _gitlink_uninitialized_dirt(full, show)
        return ("dirt", dirt) if dirt else None
    p = subprocess.run([gbin, "--no-replace-objects", "-C", os.fsdecode(full),
                        "rev-parse", "HEAD"],
                       stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=genv)
    have = p.stdout.strip() if p.returncode == 0 else b""
    if have and have != want:
        return ("diverged", "%s (the superproject records %s, the submodule work tree is at %s)"
                % (show, want[:12].decode(), have[:12].decode()))
    if not have:
        return ("diverged", "%s (initialized but its HEAD could not be read, so the commit this "
                "run tested cannot be compared with the one the push ships)" % show)
    # ── BACKLOG P3-119 (escalated to P1: a CONFIG-CONTROLLED FAIL-OPEN) ──────────────
    # The commit comparison above says WHICH commit the submodule is on. It says nothing about
    # whether that submodule's own work tree has been edited, and until now that residue was
    # merely "registered rather than hidden". It is worse than registered — it is REACHABLE WITH
    # COMMITTED CONTENT. MEASURED in an isolated repo:
    #   · dirt inside an initialized submodule moves the superproject digest OFF the clean-tree
    #     constant, but EVERY KIND of dirt yields the SAME digest (d965e058…), because the
    #     gitlink path hits open() → OSError → "<absent>". So the digest is not a faithful
    #     witness of what changed — it is a single "something" marker;
    #   · and with `diff.ignoreSubmodules=all` OR `submodule.<name>.ignore=all` the superproject
    #     status goes EMPTY and the digest returns to EXACTLY the clean-tree constant
    #     (0a815065…), while `git -C <sub> status --porcelain` still reports ` M a.txt`.
    #     `submodule.<name>.ignore` can be set in `.gitmodules`, which is COMMITTED CONTENT.
    # That is the same class as GIT_CONTEXT_REDIRECTED, which this file already refuses: a
    # configuration the tree itself carries decides what the verifier is allowed to see, and the
    # answer it produces is false-green push evidence.
    # THE FIX: ask the SUBMODULE, whose own status does not consult the superproject's ignore
    # config. Non-empty is blocking — the receiver of this push gets the recorded commit, not the
    # edits, so any output here is bytes a run may have read that the push does not ship.
    ps = subprocess.run([gbin, "--no-replace-objects", "-C", os.fsdecode(full),
                         "status", "--porcelain"],
                        stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=genv)
    if ps.returncode != 0:
        return ("diverged", "%s (initialized, at the recorded commit, but its own `git status` "
                "could not be read — so whether its work tree carries edits this push does not "
                "ship has no answer, and unknown never passes)" % show)
    if ps.stdout.strip():
        first = ps.stdout.decode(errors="replace").strip().splitlines()
        return ("diverged", "%s (initialized and at the recorded commit %s, but ITS OWN work tree "
                "is dirty: %s%s). A gitlink ships only the commit it records, so those edits are "
                "bytes this run may have read and the push does not carry. NOTE: the "
                "superproject's status can be made to hide this entirely with "
                "diff.ignoreSubmodules=all or submodule.<name>.ignore=all — and the latter lives "
                "in .gitmodules, i.e. in COMMITTED CONTENT — which is why the question is put to "
                "the submodule instead of to the superproject."
                % (show, want[:12].decode(), "; ".join(first[:4]),
                   "" if len(first) <= 4 else " (+%d more)" % (len(first) - 4)))
    return None


def _raw_index_sweep(repo, gbin, genv, dirty):
    """Every disagreement between the INDEX and the WORKING TREE, for paths git calls CLEAN.

    ROUND 7 / P1-3(b) measured BYTES here. ROUND 8 / P1-A adds the three disagreements that are
    not byte disagreements and that round 7 spelled `continue` — an unexpected REPRESENTATION
    (symlink where the index says regular, and the mirror), ABSENCE, and a diverged submodule —
    plus a direct read of the index BITS that produce them. Returns five lists; the caller raises.

    WHAT IS COVERED: all of `git ls-files -s -v` minus paths git already reports dirty (their raw
    bytes are hashed into the digest below anyway). Measured on this catalog: 5,745 tracked index
    entries (5,740 regular / 2 symlinks / 3 gitlinks), 31.6 MB of regular-file bytes, ~0.2 s (the
    figure read 5.4 MB until ROUND 9; re-measured then — sum of st_size over tracked regular files
    is 31,648,540 bytes), so whole-tree coverage is affordable and is what runs — nothing is scoped
    out for cost. One `ls-files` invocation carries both the mode/blob (`-s`) and the
    assume-unchanged / skip-worktree tag (`-v`).
    WHAT IS STILL NOT COVERED, stated: untracked and ignored files (no index blob to disagree
    with; the non-ignored ones are hashed raw below), and dirt inside an initialized submodule's
    own work tree (docs/BACKLOG.md P3-119).
    A byte mismatch is NOT automatically an attack: eol conversion (core.autocrlf=true,
    core.eol=crlf, a `text` attribute) rewrites bytes on checkout and lands here too. Both causes
    make every byte claim about that path a claim about something other than the file, so both
    BLOCK, and the message names both so an honest operator is not left guessing.
    """
    out = subprocess.run([gbin, "--no-replace-objects", "-C", repo, "ls-files", "-s", "-v", "-z"],
                         stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=genv)
    if out.returncode != 0:
        raise RuntimeError("the index of %s could not be listed" % repo)
    algo = _object_algo(repo, gbin, genv)
    rootb = os.fsencode(repo)
    diverged, mistyped, unreadable, flagged, gitlinks = [], [], [], [], []
    execbits, gldirt = [], []
    # ROUND 10 / P1: folded PREFIX -> {(dev, ino): {spellings}}; round 9 keyed the COMPLETE folded
    # path, which cannot express an alias that lives in a shared DIRECTORY component.
    # ROUND 11 / P1: the fold is CANONICAL CASELESS (`_fold_path_key`), not `bytes.lower()`.
    casefold = {}
    statcache = {}         # relative path -> lstat result | None, one call per distinct prefix
    foldcache = {}         # prefix -> canonical caseless key, one fold per distinct prefix
    fullpaths = set()      # the tracked paths themselves, to tell a leaf alias from a directory one
    # ROUND 13 / P1: (index path, on-disk target bytes) for every tracked symlink, resolved AFTER
    # the walk — the registry a target is compared against is only complete once every entry has
    # contributed its prefixes.
    symlinks = []
    for rec in out.stdout.split(b"\0"):
        if not rec:
            continue
        try:
            meta, path = rec.split(b"\t", 1)
            tag, mode, blob, _stage = meta.split(b" ")
        except ValueError:
            continue
        show = path.decode(errors="replace")
        # ROUND 10 / P1: the prefix walk happens FIRST, for EVERY entry — before the gitlink
        # `continue` below and before the dirty skip. An alias is a property of a PAIR of
        # spellings anywhere in the tree, so an entry that this sweep has nothing else to say
        # about still has to contribute its components.
        fullpaths.add(path)
        _register_prefixes(casefold, statcache, rootb, path, foldcache)
        # (0) THE BITS THEMSELVES. `-v` spells assume-unchanged as a LOWERCASE tag and
        #     skip-worktree as `S`; both tell git to stop reporting the truth about a path that a
        #     release is about to ship, and both are the first move of the two reproductions.
        if tag == b"S":
            flagged.append("%s (skip-worktree)" % show)
        elif tag.isalpha() and tag.islower():
            flagged.append("%s (assume-unchanged)" % show)
        full = os.path.join(rootb, path)
        if mode == b"160000":
            if os.path.islink(full):
                mistyped.append("%s (index: gitlink/submodule, on disk: SYMLINK)" % show)
            else:
                gl = _gitlink_divergence(repo, gbin, genv, path, blob)
                if gl:
                    (gldirt if gl[0] == "dirt" else gitlinks).append(gl[1])
            continue
        # ROUND 9 / P1-1 + (e): the two representation facts that are NOT the blob and NOT the
        # dirty/clean split, so both are read BEFORE the `dirty` continue below. `git status` can
        # be told to stop reporting the executable bit (core.fileMode=false) and NEVER reports a
        # casefold alias, and the digest carries neither, so a path that is dirty for content can
        # still be lying about its mode.
        st = _lstat_cached(statcache, rootb, path)      # already taken by the prefix walk above
        if st is not None and stat.S_ISREG(st.st_mode) and mode in (b"100644", b"100755"):
            if bool(st.st_mode & 0o111) != (mode == b"100755"):
                execbits.append(
                    "%s (index: %s, on disk: %s)"
                    % (show, "100755 (executable)" if mode == b"100755" else "100644 (plain)",
                       "executable" if st.st_mode & 0o111 else "NOT executable"))
        if path in dirty:
            continue
        is_link = os.path.islink(full)
        if mode == b"120000":
            # THE MIRROR CASE. An index symlink that is a regular file on disk is read by nothing
            # here (readlink fails) — round 7 skipped it, so a laundering file could sit under a
            # path the index calls a link.
            if not is_link:
                if os.path.exists(full):
                    mistyped.append("%s (index: symlink, on disk: regular file or directory)" % show)
                else:
                    unreadable.append("%s (index: symlink, absent from disk)" % show)
                continue
            try:
                data = os.readlink(full)
                data = data if isinstance(data, bytes) else os.fsencode(data)
            except OSError as exc:
                unreadable.append("%s (%s)" % (show, exc.strerror or exc))
                continue
            # ROUND 13 / P1: the target is BLOB CONTENT — hashed below, and until now never
            # RESOLVED. Collect it here (the bytes are already in hand and the blob comparison two
            # lines down binds them to the committed ones) and compare after the walk.
            symlinks.append((path, data))
        else:
            if is_link:
                try:
                    tgt = os.readlink(full)
                    tgt = tgt.decode(errors="replace") if isinstance(tgt, bytes) else tgt
                except OSError:
                    tgt = "<unreadable>"
                mistyped.append("%s (index: regular file mode %s, on disk: SYMLINK -> %s)"
                                % (show, mode.decode(errors="replace"), tgt))
                continue
            try:
                with open(full, "rb") as fh:
                    data = fh.read()
            except OSError as exc:
                unreadable.append("%s (%s)" % (show, exc.strerror or exc))
                continue
        if _blob_id(data, algo) != blob:
            diverged.append(show)
        # NOTE: no early `break` here. Round 8 had one, and it was only ever reached when EVERY
        # bucket was already full; the casefold check below needs the WHOLE index (an alias is a
        # property of a pair, so a truncated walk can miss the second half of one).
    aliased, diraliased = _alias_verdicts(casefold, fullpaths)
    # ROUND 13 / P1: the registry the symlink targets are resolved against. It is derived from the
    # SAME statcache the prefix walk filled, so "registered" is exactly "a tracked path or a
    # directory component of one" — one pass over the distinct prefixes (~6.8k here), no new
    # lstat.
    inodes = {}
    for rel, st in statcache.items():
        if st is not None:
            inodes.setdefault((st.st_dev, st.st_ino), set()).add(rel)
    symaliased, symunbounded = _symlink_target_verdicts(rootb, symlinks, inodes, foldcache)
    return (diverged[:8], mistyped[:8], unreadable[:8], flagged[:8], gitlinks[:8],
            execbits[:8], gldirt[:8], aliased[:8], diraliased[:8], symaliased[:8],
            symunbounded[:8])


def fingerprint(repo):
    """Return the working-tree fingerprint for <repo>, or "nogit" when it is not a git tree.

    Raises RedirectedGitContext when <repo> IS inside a git tree but the tree answering the reads
    is a different one — that is a tamper signal, not a degraded mode, so it must not collapse
    into the same "nogit" that an honest tarball produces.
    """
    gbin, genv = _git_bin(), _git_env()
    try:
        top = _git(repo, "rev-parse", "--show-toplevel", _bin=gbin, _env=genv).decode().strip()
    except Exception:
        # Not a git working tree (or git unavailable): no cheap tree identity. Constant value ⇒
        # head-only binding, and such a run can never serve as push evidence anyway.
        return "nogit"
    if not top or os.path.realpath(top) != os.path.realpath(repo):
        raise RedirectedGitContext(
            f"the git work tree answering reads for {repo} is {top or '<unresolvable>'}; "
            "a fingerprint taken through a redirected context describes a different tree "
            "(measured: a dirty tree reported the clean-tree constant)")
    filt = _filters_declared(repo, gbin, genv)
    if filt is not None:
        raise GitFiltersPresent(filt)
    try:
        status = _git(repo, "status", "--porcelain", "-z", "-uall", _bin=gbin, _env=genv)
    except Exception as exc:
        # A tree that IS git but cannot be read is unknown, not clean. Fail closed: the caller
        # blocks rather than recording a value it could not compute.
        raise RuntimeError(f"the working tree of {repo} could not be read: {exc}") from exc

    h = hashlib.sha256()
    h.update(b"status\0")
    h.update(status)
    h.update(b"\0diff\0")

    entries = status.split(b"\0")
    untracked = []
    modified = []
    i = 0
    while i < len(entries):
        entry = entries[i]
        i += 1
        if len(entry) < 4:
            continue
        xy, path = entry[:2], entry[3:]
        if xy[:1] in (b"R", b"C"):
            i += 1  # a rename/copy record is followed by its origin path as a separate entry
        if xy == b"??":
            untracked.append(path)
        else:
            modified.append(path)

    # ROUND 7 / P1-3(b): the SET of dirty paths above came from `git status`, which is
    # FILTER-AWARE — a `filter.<n>.process` driver makes a divergent file report clean, so it
    # never enters `modified` and the raw loop below never opens it. Measure the complement
    # instead of trusting it: every path git calls clean must hash, as raw bytes, to its index
    # blob. No git process participates in that comparison, so no filter protocol can answer it.
    # ROUND 8 / P1-A: and the sweep no longer `continue`s past a representation it did not
    # expect. An index-regular path that is a SYMLINK on disk, an index-symlink path that is a
    # regular file, a tracked path ABSENT from disk, an index carrying assume-unchanged /
    # skip-worktree bits, and an initialized submodule that is not at the recorded commit are all
    # states in which the tree this file describes is not the tree the push ships. Each BLOCKS.
    # ROUND 9 / P1-1, P1-2, (e): three more disagreements between the index and the working tree
    # that are neither bytes nor the shapes round 8 enumerated — the EXECUTABLE BIT (which
    # core.fileMode=false tells git to stop reporting, and which no digest here carries), a
    # populated but UNINITIALIZED gitlink (a directory the push ships EMPTY), and two index entries
    # that a case-insensitive filesystem serves from ONE file.
    # ROUND 10 / P1, (TD-2026-07-31-(P1-casefold-prefix)): the casefold measurement above is taken
    # over every PATH PREFIX, so an alias that lives in a shared DIRECTORY component — which round
    # 9's complete-path grouping could not express at all — is refused too.
    # ROUND 13 / P1, (TD-2026-08-01-(P1-symlink-target-alias)): every alias round so far registered
    # only INDEX PATHS, and a symlink's TARGET is not one — it is blob content, read as bytes and
    # never resolved, so the four-round census did not apply to it on ANY axis. It does now.
    (diverged, mistyped, unreadable, flagged, gitlinks,
     execbits, gldirt, aliased, diraliased, symaliased, symunbounded) = _raw_index_sweep(
        repo, gbin, genv, set(modified) | set(untracked))
    if flagged:
        raise IndexFlagsSet(
            "the index carries bits that tell git to stop reporting the truth about tracked "
            "paths: " + ", ".join(flagged)
            + ". `git update-index --assume-unchanged` and `--skip-worktree` make "
              "`git status` report a clean tree while the file on disk is replaced, removed or "
              "swapped for a symlink — measured, both turn this fingerprint into the clean-tree "
              "constant while the push ships the committed blob. Clear them with "
              "`git update-index --no-assume-unchanged / --no-skip-worktree <path>` and re-run.")
    if mistyped:
        raise WorktreeTypeMismatch(
            "tracked paths whose WORKTREE REPRESENTATION is not the one the index records: "
            + ", ".join(mistyped)
            + ". The index says one kind of object and the filesystem holds another, so every "
              "read of that path answers about something the push will not ship — measured, an "
              "index-regular path replaced by a symlink to a benign file outside the repository "
              "made this fingerprint report the clean-tree constant. A representation this file "
              "cannot account for is refused, never skipped.")
    if unreadable:
        raise TrackedPathUnreadable(
            "tracked paths git reports as unmodified that could not be read from disk: "
            + ", ".join(unreadable)
            + ". The legitimate-looking ways to arrive here are `git update-index "
              "--assume-unchanged`, `git update-index --skip-worktree` and a SPARSE CHECKOUT; "
              "under all three `git status` stays empty while the file the push ships is not on "
              "disk at all, so nothing this run executed or hashed was about it. Restore the "
              "path (and clear the bit) before producing push evidence.")
    if execbits:
        raise ExecBitDivergence(
            "tracked paths whose EXECUTABLE BIT on disk is not the one the index records: "
            + ", ".join(execbits)
            + ". git records exactly two regular modes (100644 / 100755) and `core.fileMode=false` "
              "tells git to stop REPORTING a difference — it does not change the RECORD, so this "
              "comparison is made against `git ls-files -s` and is independent of that setting. "
              "Measured: a 100644 file made executable on disk left `git status` empty and the "
              "fingerprint at the clean-tree constant while every `./gradlew` invocation ran the "
              "locally-executable file that a fresh checkout cannot execute. Fix the record "
              "(`git update-index --chmod=+x|-x <path>`) or the file, and re-run.")
    if gldirt:
        raise GitlinkUninitializedDirt(
            "gitlinks with no gitdir whose worktree path is nevertheless POPULATED: "
            + ", ".join(gldirt)
            + ". A gitlink ships only the commit it records, so a fresh clone of this commit gets "
              "an EMPTY directory there — anything read or executed from a populated one is code "
              "the receiver never gets. An uninitialized gitlink is acceptable only when its path "
              "is absent or an actually-empty directory. Either initialize the submodule "
              "(`git submodule update --init`) or remove the untracked content.")
    if aliased:
        raise CasefoldAlias(
            "index entries that differ only in CASE and are ONE file on this filesystem: "
            + ", ".join(aliased)
            + ". They lstat to the same device/inode, so every read — this sweep, the build, the "
              "lint — answers about a single file for both entries while the push ships two "
              "distinct blobs; one of them was never on disk to be verified. Rename one entry so "
              "the index and the filesystem agree.")
    if diraliased:
        raise CasefoldDirectoryAlias(
            "index entries whose DIRECTORY components are spelled two ways and are ONE directory "
            "on this filesystem: " + ", ".join(diraliased)
            + ". The spellings lstat to the same device/inode, so a path built with either one "
              "resolves here — measured, `A/check.sh` reading `A/helper` while the index records "
              "`a/helper` left `git status` empty and this fingerprint at the clean-tree constant, "
              "and the pushed tree serves no `A/helper` at all on a case-sensitive receiver. This "
              "is a MEASUREMENT: genuinely distinct directories yield distinct inodes and are not "
              "refused. Settle on one spelling (`git mv`) so the index and the filesystem agree.")
    if symaliased:
        raise SymlinkTargetAlias(
            "tracked SYMLINKS whose TARGET spells a tracked path with an ALIAS of the spelling "
            "this repository records: " + ", ".join(symaliased)
            + ". The target is blob content, so no amount of index-path auditing sees it: on this "
              "filesystem the alias resolves, so every read — the build, the lint, this sweep — "
              "succeeds and the tree reports CLEAN, while a receiver whose filesystem treats the "
              "difference as significant gets a DANGLING link. Measured: `ln -s GRADLEW-REAL "
              "backend/gradlew` over a tracked `backend/gradlew-real` left `git status "
              "--porcelain -uall` EMPTY, ran R25 to green and returned the clean-tree constant. "
              "This is a MEASUREMENT of an observed (st_dev, st_ino) identity plus a fold "
              "equality — a target that leaves the repository, names an untracked path, dangles, "
              "or spells the record EXACTLY is not refused. ROUND 14b: the verdict is taken on "
              "EVERY component the walk resolves, not on the final one alone — `-> DIRLINK` "
              "blocked while `-> DIRLINK/` and `-> DIRLINK/real.txt` passed over the very same "
              "tracked `dirlink`, a one-character bypass. Spell the target the way the index "
              "records the path (`ln -sf <recorded-spelling>`) and re-run.")
    if symunbounded:
        raise SymlinkResolutionUnbounded(
            "tracked SYMLINKS whose target could not be resolved within the budget every kernel "
            "imposes: " + ", ".join(symunbounded)
            + ". Resolving a target the way the kernel does means following intermediate "
              "symlinks, and a committed chain that cycles or expands without bound leaves the "
              "alias question UNANSWERED — which this family never converts into silence. The "
              "receiver gets ELOOP from such a chain anyway (Linux MAXSYMLINKS 40, macOS "
              "SYMLOOP_MAX 32; the larger is used here, so a chain any kernel would resolve is "
              "never refused). Unbreak the chain and re-run.")
    if gitlinks:
        raise GitlinkDivergence(
            "initialized submodules whose work tree is not at the commit the superproject "
            "records: " + ", ".join(gitlinks)
            + ". The push ships the RECORDED commit, so a run performed against a different one "
              "tested code the receiver will not get. `git submodule update --init` and re-run.")
    if diverged:
        raise RawIndexDivergence(
            "tracked files git reports as unmodified do NOT match their index blob when their "
            "bytes are hashed directly: " + ", ".join(diverged)
            + ". Either a content filter (filter.<n>.clean / .smudge / .process) is interposing "
              "on git's answers, or an eol conversion (core.autocrlf / core.eol / a text "
              "attribute) rewrites these files on checkout. Under both, a byte claim about these "
              "paths describes something other than the file on disk.")

    # ROUND 6 / P1-4 — THE CONTENT OF A MODIFICATION IS READ FROM THE FILE, NOT FROM `git diff`.
    # This slot used to hold `git diff HEAD --binary`, which honours clean filters: with one
    # installed, a tampered working file produced a ZERO-BYTE diff and the fingerprint reported
    # the tree as it is committed. The bytes are now read with open(), which no git configuration
    # can interpose on.
    # THE CLEAN-TREE CONSTANT IS PRESERVED BY CONSTRUCTION: on a clean tree `status` is empty, so
    # `modified` is empty and nothing is appended after the b"\0diff\0" separator — byte-identical
    # to the old digest. That matters because the recency guard recomputes with the PREVIOUS
    # RELEASE'S copy of this file; a changed clean-tree constant would brick every honest push.
    for path in sorted(modified):
        h.update(b"raw\0")
        h.update(path)
        h.update(b"\0")
        try:
            with open(os.path.join(os.fsencode(repo), path), "rb") as fh:
                for chunk in iter(lambda: fh.read(1 << 16), b""):
                    h.update(chunk)
        except OSError:
            h.update(b"<absent>")
        h.update(b"\0")

    # Untracked (non-ignored) files: no diff can see them, so hash their bytes.
    for path in sorted(untracked):
        h.update(b"untracked\0")
        h.update(path)
        h.update(b"\0")
        try:
            with open(os.path.join(os.fsencode(repo), path), "rb") as fh:
                for chunk in iter(lambda: fh.read(1 << 16), b""):
                    h.update(chunk)
        except OSError:
            h.update(b"<unreadable>")

    return h.hexdigest()


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: tree_fingerprint.py <repo_root>", file=sys.stderr)
        sys.exit(2)
    # Exit codes are part of the contract (ROUND 5/6): 0 = a digest (or the honest "nogit" of a
    # non-git tree), 3 = the git context was redirected, 4 = a git tree we could not read,
    # 5 = the repository declares content filters, 6 (ROUND 7 / P1-3b) = a tracked file git calls
    # clean does not match its index blob on raw bytes, and (ROUND 8 / P1-A) 7 = a tracked path's
    # worktree REPRESENTATION is not the index's, 8 = a tracked path is absent/unreadable,
    # 9 = the index carries assume-unchanged / skip-worktree bits, 10 = an initialized submodule
    # is not at the recorded commit, and (ROUND 9 / TD-2026-07-30-(P1-representation-parity))
    # 11 = a tracked path's EXECUTABLE BIT is not the index's, 12 = an uninitialized gitlink is
    # populated, 13 = two index entries differing only in case are one file on disk, and
    # (ROUND 10 / TD-2026-07-31-(P1-casefold-prefix)) 14 = a shared DIRECTORY component is spelled
    # two ways and is one directory on disk (13 is leaf-only and always was). ROUND 11 /
    # TD-2026-08-01-(P1-unicode-prefix-fold) widened WHAT COUNTS AS "spelled two ways" for BOTH 13
    # and 14 — the key is canonical caseless (NFC(casefold(NFD(s)))), so a NORMALIZATION alias
    # (`é` ≡ `e◌́`) and a NON-ASCII CASE alias (`É` ≡ `é`) now reach the same two codes that ASCII
    # case already did; no code was added, because the remedy (rename one spelling) is the same.
    # ROUND 12 / TD-2026-08-01-(P1-ignorable-fold) widened it again on a THIRD axis (ignorable Cf),
    # also without a new code. ROUND 13 / TD-2026-08-01-(P1-symlink-target-alias) DOES add one:
    # 15 = a tracked SYMLINK's TARGET spells a tracked path with an alias of the recorded spelling.
    # It is a new code and not a widening of 13/14 because the subject is different (blob content,
    # not an index path) and so is the remedy (`ln -sf <recorded-spelling>`, not `git mv`).
    # Callers BLOCK
    # on every non-zero code — printing nothing and exiting 0 is exactly the fail-open round 5
    # closed, `continue`ing past a representation is the same fail-open one level down (round 8),
    # and accepting a representation fact the digest does not carry is the same again (round 9).
    try:
        print(fingerprint(sys.argv[1]))
    except RedirectedGitContext as exc:
        print(f"tree_fingerprint: GIT_CONTEXT_REDIRECTED — {exc}", file=sys.stderr)
        sys.exit(3)
    except IndexFlagsSet as exc:
        print(f"tree_fingerprint: GIT_INDEX_FLAGS_SET — {exc}", file=sys.stderr)
        sys.exit(9)
    except WorktreeTypeMismatch as exc:
        print(f"tree_fingerprint: GIT_WORKTREE_TYPE_MISMATCH — {exc}", file=sys.stderr)
        sys.exit(7)
    except TrackedPathUnreadable as exc:
        print(f"tree_fingerprint: GIT_TRACKED_PATH_ABSENT — {exc}", file=sys.stderr)
        sys.exit(8)
    except GitlinkDivergence as exc:
        print(f"tree_fingerprint: GIT_GITLINK_DIVERGENCE — {exc}", file=sys.stderr)
        sys.exit(10)
    except ExecBitDivergence as exc:
        print(f"tree_fingerprint: GIT_EXEC_BIT_DIVERGENCE — {exc}", file=sys.stderr)
        sys.exit(11)
    except GitlinkUninitializedDirt as exc:
        print(f"tree_fingerprint: GIT_GITLINK_UNINITIALIZED_POPULATED — {exc}", file=sys.stderr)
        sys.exit(12)
    except CasefoldAlias as exc:
        print(f"tree_fingerprint: GIT_CASEFOLD_ALIAS — {exc}", file=sys.stderr)
        sys.exit(13)
    except CasefoldDirectoryAlias as exc:
        print(f"tree_fingerprint: GIT_CASEFOLD_DIR_ALIAS — {exc}", file=sys.stderr)
        sys.exit(14)
    except SymlinkTargetAlias as exc:
        print(f"tree_fingerprint: GIT_SYMLINK_TARGET_ALIAS — {exc}", file=sys.stderr)
        sys.exit(15)
    except SymlinkResolutionUnbounded as exc:
        print(f"tree_fingerprint: GIT_SYMLINK_RESOLUTION_UNBOUNDED — {exc}", file=sys.stderr)
        sys.exit(16)
    except RawIndexDivergence as exc:
        # Ordered BEFORE GitFiltersPresent: RawIndexDivergence subclasses it so that callers
        # written against the older type still block, but the code printed must be the specific
        # one — "we refused because you declared a filter" would be a false statement here.
        print(f"tree_fingerprint: GIT_RAW_INDEX_DIVERGENCE — {exc}", file=sys.stderr)
        sys.exit(6)
    except GitFiltersPresent as exc:
        # Exit 5 (ROUND 6): a repository that declares content filters cannot be fingerprinted
        # honestly. Callers BLOCK — this is a tamper signal, not a degraded mode.
        print(f"tree_fingerprint: GIT_FILTERS_PRESENT — {exc}", file=sys.stderr)
        sys.exit(5)
    except Exception as exc:
        print(f"tree_fingerprint: FINGERPRINT_UNVERIFIABLE — {exc}", file=sys.stderr)
        sys.exit(4)
