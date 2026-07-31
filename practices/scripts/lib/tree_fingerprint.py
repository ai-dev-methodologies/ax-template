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
    p = subprocess.run([gbin, "--no-replace-objects", "-C", repo, "config", "--get-regexp",
                        r"^filter\..*\.(clean|smudge)$"],
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
    # 5 = the repository declares content filters. Callers BLOCK on 3/4/5 — printing nothing and exiting 0 is exactly the fail-open this round closed.
    try:
        print(fingerprint(sys.argv[1]))
    except RedirectedGitContext as exc:
        print(f"tree_fingerprint: GIT_CONTEXT_REDIRECTED — {exc}", file=sys.stderr)
        sys.exit(3)
    except GitFiltersPresent as exc:
        # Exit 5 (ROUND 6): a repository that declares content filters cannot be fingerprinted
        # honestly. Callers BLOCK — this is a tamper signal, not a degraded mode.
        print(f"tree_fingerprint: GIT_FILTERS_PRESENT — {exc}", file=sys.stderr)
        sys.exit(5)
    except Exception as exc:
        print(f"tree_fingerprint: FINGERPRINT_UNVERIFIABLE — {exc}", file=sys.stderr)
        sys.exit(4)
