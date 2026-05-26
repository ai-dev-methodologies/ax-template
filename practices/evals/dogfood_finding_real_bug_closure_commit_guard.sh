#!/usr/bin/env bash
# practices/evals/dogfood_finding_real_bug_closure_commit_guard.sh
# R86b (46th hard guard) — mechanises practices/rules/dogfood-finding-real-bug-must-reference-closure-commit.md
# (R86). For every entry in docs/dogfood-ledger/*.yaml whose
# `classification` is `real_bug`, the entry MUST carry a
# `closure_commit_sha` field whose value is:
#
#   (a) non-empty,
#   (b) matches ^[0-9a-f]{7,40}$ (valid short or full git SHA), AND
#   (c) resolves to an existing commit in the local repository
#       (`git cat-file -e <sha>^{commit}`).
#
# scope_deferral entries are exempt (those carry expiry triggers per R85)
# and methodology_gap entries are also exempt (those drive methodology
# change, not a single closure commit). Only `real_bug` carries the SHA
# requirement.
#
# Exit codes:
#   0 — every real_bug entry has a valid, resolvable closure_commit_sha.
#   1 — at least one violation.
#   2 — usage / environment error (yaml missing, python3 missing, not a
#       git repo).
#
# Usage:
#   bash practices/evals/dogfood_finding_real_bug_closure_commit_guard.sh
#   bash practices/evals/dogfood_finding_real_bug_closure_commit_guard.sh --root DIR
#
# Bash 3.2 compatible.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "dogfood_finding_real_bug_closure_commit_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "cannot cd to $REPO_ROOT" >&2; exit 2; }

LEDGER_DIR="docs/dogfood-ledger"
[ ! -d "$LEDGER_DIR" ] && exit 0

if ! command -v python3 >/dev/null 2>&1; then
    echo "dogfood_finding_real_bug_closure_commit_guard: python3 not in PATH" >&2
    exit 2
fi

# Pre-check: must be inside a git repo (we need git cat-file).
if ! git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    echo "dogfood_finding_real_bug_closure_commit_guard: not a git repo (run from inside a working tree)" >&2
    exit 2
fi

# Phase 1 — shape validation. python writes any violations to stderr and
# exits 1 on failure; on success it exits 0 and writes nothing. The
# tuple emission for phase 2 happens in a separate invocation below.
if ! python3 - "$LEDGER_DIR" <<'PY'
import pathlib
import re
import sys

import yaml


SHA_RE = re.compile(r"^[0-9a-f]{7,40}$")

ledger_dir = pathlib.Path(sys.argv[1])
violations = []

for ledger_yaml in sorted(ledger_dir.glob("*.yaml")):
    try:
        doc = yaml.safe_load(ledger_yaml.read_text()) or {}
    except yaml.YAMLError as e:
        print(f"YAML parse error: {ledger_yaml}: {e}", file=sys.stderr)
        sys.exit(2)
    for idx, f in enumerate(doc.get("findings") or []):
        if not isinstance(f, dict):
            continue
        if f.get("classification", "") != "real_bug":
            continue
        sha = f.get("closure_commit_sha", None)
        persona = f.get("persona", "??")
        preview = (f.get("finding", "") or "").split(":", 1)[0][:60]
        if sha is None:
            violations.append(
                f"{ledger_yaml}: finding[{idx}] persona={persona} "
                f"({preview}…) — closure_commit_sha field missing"
            )
            continue
        sha = str(sha).strip()
        if not sha:
            violations.append(
                f"{ledger_yaml}: finding[{idx}] persona={persona} "
                f"({preview}…) — closure_commit_sha is empty"
            )
            continue
        if not SHA_RE.match(sha):
            violations.append(
                f"{ledger_yaml}: finding[{idx}] persona={persona} "
                f"({preview}…) — closure_commit_sha {sha!r} does not match "
                f"^[0-9a-f]{{7,40}}$"
            )

if violations:
    print(
        "VIOLATION: dogfood-ledger real_bug entries missing or malformed "
        "closure_commit_sha (R86):",
        file=sys.stderr,
    )
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        f"dogfood_finding_real_bug_closure_commit_guard: {len(violations)} "
        f"shape violation(s) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)
PY
then
    exit 1
fi

# Phase 1 passed. Now emit the (sha, source) tuples to stdout for phase 2.
phase1_output=$(python3 - "$LEDGER_DIR" <<'PY'
import pathlib, re, sys, yaml
SHA_RE = re.compile(r"^[0-9a-f]{7,40}$")
ledger_dir = pathlib.Path(sys.argv[1])
for ledger_yaml in sorted(ledger_dir.glob("*.yaml")):
    doc = yaml.safe_load(ledger_yaml.read_text()) or {}
    for idx, f in enumerate(doc.get("findings") or []):
        if not isinstance(f, dict) or f.get("classification") != "real_bug":
            continue
        sha = f.get("closure_commit_sha", None)
        if sha is None:
            continue
        sha = str(sha).strip()
        if not sha or not SHA_RE.match(sha):
            continue
        persona = f.get("persona", "??")
        preview = (f.get("finding", "") or "").split(":", 1)[0][:60]
        print(f"{sha}\t{ledger_yaml}\tfinding[{idx}]\t{persona}\t{preview}")
PY
)

# Phase 2 — verify each SHA actually resolves to a commit in the local repo.
phase2_violations=0
phase2_lines=""
while IFS=$'\t' read -r sha file_path locator persona preview; do
    [ -z "$sha" ] && continue
    if ! git -C "$REPO_ROOT" cat-file -e "${sha}^{commit}" 2>/dev/null; then
        phase2_violations=$((phase2_violations + 1))
        phase2_lines="${phase2_lines}
$file_path: $locator persona=$persona ($preview…) — closure_commit_sha '$sha' does not resolve to a local commit (git cat-file -e ${sha}^{commit} failed)"
    fi
done <<EOF
$phase1_output
EOF

if [ "$phase2_violations" -gt 0 ]; then
    echo "VIOLATION: dogfood-ledger real_bug closure_commit_sha values do not resolve to local commits (R86):" >&2
    echo "$phase2_lines" >&2
    echo "" >&2
    echo "Fix shape: confirm the sha is correct and the commit is present in the local clone (a freshly-cloned fork-receiver checkout MUST contain the closure commit, else the catalog is shipping orphaned ledger entries)." >&2
    echo "dogfood_finding_real_bug_closure_commit_guard: $phase2_violations resolution violation(s) — merge BLOCKED" >&2
    exit 1
fi

echo "dogfood_finding_real_bug_closure_commit_guard: every real_bug has a valid, resolvable closure_commit_sha"
exit 0
