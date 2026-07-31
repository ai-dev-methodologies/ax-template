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
import subprocess
import sys


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


def _raw_index_divergence(repo, gbin, genv, dirty):
    """Tracked paths git calls CLEAN whose raw on-disk bytes are not the blob it has in the index.

    ROUND 7 / P1-3(b). WHAT IS COVERED: every path in `git ls-files -s` that git's status did NOT
    report, i.e. the whole tracked tree minus what is already being hashed raw below. Measured on
    this catalog: 5,745 tracked paths / 5.4 MB / 0.18 s, so whole-tree coverage is affordable and
    is what runs — nothing is scoped out for cost.
    WHAT IS NOT COVERED, stated: (i) gitlinks (submodule commits — no bytes of ours on disk),
    (ii) tracked paths ABSENT from disk (sparse checkout / skip-worktree — an absent file cannot
    carry benign bytes, and absence is not a bytes question), (iii) untracked and ignored files,
    which have no index blob to disagree with and are hashed raw below anyway when non-ignored.
    A mismatch is NOT automatically an attack: eol conversion (core.autocrlf=true, core.eol=crlf,
    a `text` attribute) rewrites bytes on checkout and lands here too. Both causes make every byte
    claim about that path a claim about something other than the file, so both BLOCK, and the
    message names both so an honest operator is not left guessing.
    """
    out = subprocess.run([gbin, "--no-replace-objects", "-C", repo, "ls-files", "-s", "-z"],
                         stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, env=genv)
    if out.returncode != 0:
        raise RuntimeError("the index of %s could not be listed" % repo)
    algo = _object_algo(repo, gbin, genv)
    rootb = os.fsencode(repo)
    bad = []
    for rec in out.stdout.split(b"\0"):
        if not rec:
            continue
        try:
            meta, path = rec.split(b"\t", 1)
            mode, blob, _stage = meta.split(b" ")
        except ValueError:
            continue
        if mode == b"160000" or path in dirty:
            continue
        full = os.path.join(rootb, path)
        try:
            if mode == b"120000":
                data = os.readlink(full)
                data = data if isinstance(data, bytes) else os.fsencode(data)
            else:
                if os.path.islink(full):
                    continue
                with open(full, "rb") as fh:
                    data = fh.read()
        except OSError:
            continue
        if _blob_id(data, algo) != blob:
            bad.append(path.decode(errors="replace"))
            if len(bad) >= 8:
                break
    return bad


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
    diverged = _raw_index_divergence(repo, gbin, genv, set(modified) | set(untracked))
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
    # clean does not match its index blob on raw bytes. Callers BLOCK on 3/4/5/6 — printing
    # nothing and exiting 0 is exactly the fail-open this round closed.
    try:
        print(fingerprint(sys.argv[1]))
    except RedirectedGitContext as exc:
        print(f"tree_fingerprint: GIT_CONTEXT_REDIRECTED — {exc}", file=sys.stderr)
        sys.exit(3)
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
