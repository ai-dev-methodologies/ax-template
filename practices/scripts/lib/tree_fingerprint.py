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
    HONEST RESIDUE: two spellings that fold equal AND are hardlinks of one inode on a case- or
    normalization-SENSITIVE filesystem would be flagged. That requires deliberately hardlinking
    `A.txt` to `a.txt`; the same property was already true of the leaf check in round 9.
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
    return ("%s (uninitialized gitlink — no gitdir — yet its directory holds %d entr%s: %s)"
            % (show, len(kids), "y" if len(kids) == 1 else "ies", names))


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

    THE KEY IS UNICODE CANONICAL CASELESS MATCHING: NFC(casefold(NFD(s))).
      · The INNER NFD is UAX #21 §1.3 ("Default Caseless Matching" is defined over NFD forms).
        It is load-bearing, not ceremony — MEASURED here: `casefold()` with no normalization at
        all separates every pair above, and the outer step alone is not enough either.
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
    The ASCII fast path is not an approximation: for pure-ASCII input NFD and NFC are the
    identity and full casefold coincides exactly with `lower()`, so it returns the same bytes the
    slow path would. It exists so the live catalog's ~6.8k distinct prefixes — all ASCII — cost
    what they cost before. Note it does NOT create a seam: a non-ASCII prefix that folds INTO
    ASCII (`ſ` → `s`) still meets the ASCII spelling, because the slow path produces the same key.

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
    return (diverged[:8], mistyped[:8], unreadable[:8], flagged[:8], gitlinks[:8],
            execbits[:8], gldirt[:8], aliased[:8], diraliased[:8])


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
    (diverged, mistyped, unreadable, flagged, gitlinks,
     execbits, gldirt, aliased, diraliased) = _raw_index_sweep(
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
