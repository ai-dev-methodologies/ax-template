#!/usr/bin/env bash
# practices/evals/dogfood_finding_real_bug_test_coverage_guard.sh
# R87b (47th hard guard) — mechanises practices/rules/dogfood-finding-real-bug-must-reference-test-coverage.md
# (R87). Every entry in docs/dogfood-ledger/*.yaml whose classification is
# real_bug MUST carry test-coverage evidence in EXACTLY ONE of four
# shapes:
#
#   (a) closure_test_method
#       — Java test method name; shape ^[A-Za-z_][A-Za-z0-9_]*$
#       (no further resolution required — discovery is advisory)
#   (b) closure_test_commit_sha
#       — distinct git SHA where the regression test landed;
#       ^[0-9a-f]{7,40}$, resolvable via `git cat-file -e <sha>^{commit}`
#   (c) closure_test_path
#       — relative path to a regression test file that exists on disk
#   (d) closure_verification_ref + closure_verification_reason
#       — escape hatch for non-executable closures (doc/comment/config/
#       external/fork-receiver-owned). closure_verification_reason MUST
#       be one of {doc-only, comment-contract, config-only,
#       external-system, fork-receiver-owned}.
#
# Executable shapes (a/b/c) take precedence — verification_ref is
# permitted ONLY when no executable regression anchor exists.
#
# Exit codes:
#   0 — every real_bug entry has at least one valid coverage shape.
#   1 — at least one violation.
#   2 — usage / environment error (yaml missing, python3 missing, not
#       a git repo for shape b).
#
# Usage:
#   bash practices/evals/dogfood_finding_real_bug_test_coverage_guard.sh
#   bash practices/evals/dogfood_finding_real_bug_test_coverage_guard.sh --root DIR
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
        *) echo "dogfood_finding_real_bug_test_coverage_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

[ -n "$ROOT_OVERRIDE" ] && REPO_ROOT="$ROOT_OVERRIDE"
cd "$REPO_ROOT" || { echo "cannot cd to $REPO_ROOT" >&2; exit 2; }

LEDGER_DIR="docs/dogfood-ledger"
[ ! -d "$LEDGER_DIR" ] && exit 0

if ! command -v python3 >/dev/null 2>&1; then
    echo "dogfood_finding_real_bug_test_coverage_guard: python3 not in PATH" >&2
    exit 2
fi

if ! git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
    echo "dogfood_finding_real_bug_test_coverage_guard: not a git repo" >&2
    exit 2
fi

# Phase 1 — shape + reason validation. Violations go to stderr; valid
# (shape, sha-or-path, source) tuples go to stdout for phase 2 verification.
if ! python3 - "$LEDGER_DIR" <<'PY'
import pathlib
import re
import sys

import yaml


METHOD_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(#[A-Za-z_][A-Za-z0-9_]*)?$")
SHA_RE = re.compile(r"^[0-9a-f]{7,40}$")
ALLOWED_REASONS = {
    "doc-only",
    "comment-contract",
    "config-only",
    "external-system",
    "fork-receiver-owned",
}

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
        persona = f.get("persona", "??")
        preview = (f.get("finding", "") or "").split(":", 1)[0][:60]
        loc = f"{ledger_yaml}: finding[{idx}] persona={persona} ({preview}…)"

        method = f.get("closure_test_method")
        sha = f.get("closure_test_commit_sha")
        path = f.get("closure_test_path")
        ver_ref = f.get("closure_verification_ref")
        ver_reason = f.get("closure_verification_reason")

        # Count populated executable shapes (a, b, c).
        populated = 0
        if method is not None and str(method).strip():
            populated += 1
            if not METHOD_RE.match(str(method).strip()):
                violations.append(
                    f"{loc} — closure_test_method "
                    f"{method!r} does not match identifier shape "
                    f"(^[A-Za-z_][A-Za-z0-9_]*(#…)?$)"
                )
        if sha is not None and str(sha).strip():
            populated += 1
            if not SHA_RE.match(str(sha).strip()):
                violations.append(
                    f"{loc} — closure_test_commit_sha "
                    f"{sha!r} does not match ^[0-9a-f]{{7,40}}$"
                )
        if path is not None and str(path).strip():
            populated += 1
            p = pathlib.Path(str(path).strip())
            if not p.exists():
                violations.append(
                    f"{loc} — closure_test_path {path!r} "
                    f"does not exist on disk"
                )

        has_ver = ver_ref is not None and str(ver_ref).strip()
        # Verification_ref requires a reason; reason without ref is an error.
        if ver_reason is not None and str(ver_reason).strip() and not has_ver:
            violations.append(
                f"{loc} — closure_verification_reason set without "
                f"closure_verification_ref"
            )
        if has_ver:
            if not ver_reason or not str(ver_reason).strip():
                violations.append(
                    f"{loc} — closure_verification_ref set without "
                    f"closure_verification_reason"
                )
            elif str(ver_reason).strip() not in ALLOWED_REASONS:
                violations.append(
                    f"{loc} — closure_verification_reason "
                    f"{ver_reason!r} not in allowed set "
                    f"{sorted(ALLOWED_REASONS)}"
                )

        # Rule: must have at least one shape populated.
        if populated == 0 and not has_ver:
            violations.append(
                f"{loc} — no closure_test_method / closure_test_commit_sha / "
                f"closure_test_path / closure_verification_ref present"
            )

if violations:
    print(
        "VIOLATION: dogfood-ledger real_bug entries missing or malformed "
        "test-coverage evidence (R87):",
        file=sys.stderr,
    )
    for v in violations:
        print(f"  {v}", file=sys.stderr)
    print("", file=sys.stderr)
    print(
        f"dogfood_finding_real_bug_test_coverage_guard: {len(violations)} "
        f"shape violation(s) — merge BLOCKED",
        file=sys.stderr,
    )
    sys.exit(1)
PY
then
    exit 1
fi

# Phase 2 — git resolution for closure_test_commit_sha values.
phase2_pairs=$(python3 - "$LEDGER_DIR" <<'PY'
import pathlib, re, sys, yaml
SHA_RE = re.compile(r"^[0-9a-f]{7,40}$")
ledger_dir = pathlib.Path(sys.argv[1])
for ledger_yaml in sorted(ledger_dir.glob("*.yaml")):
    doc = yaml.safe_load(ledger_yaml.read_text()) or {}
    for idx, f in enumerate(doc.get("findings") or []):
        if not isinstance(f, dict) or f.get("classification") != "real_bug":
            continue
        sha = f.get("closure_test_commit_sha")
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

phase2_violations=0
phase2_lines=""
while IFS=$'\t' read -r sha file_path locator persona preview; do
    [ -z "$sha" ] && continue
    if ! git -C "$REPO_ROOT" cat-file -e "${sha}^{commit}" 2>/dev/null; then
        phase2_violations=$((phase2_violations + 1))
        phase2_lines="${phase2_lines}
$file_path: $locator persona=$persona ($preview…) — closure_test_commit_sha '$sha' does not resolve to a local commit"
    fi
done <<EOF
$phase2_pairs
EOF

if [ "$phase2_violations" -gt 0 ]; then
    echo "VIOLATION: dogfood-ledger real_bug closure_test_commit_sha values do not resolve to local commits (R87):" >&2
    echo "$phase2_lines" >&2
    echo "" >&2
    echo "dogfood_finding_real_bug_test_coverage_guard: $phase2_violations resolution violation(s) — merge BLOCKED" >&2
    exit 1
fi

echo "dogfood_finding_real_bug_test_coverage_guard: every real_bug has at least one valid test-coverage shape"
exit 0
