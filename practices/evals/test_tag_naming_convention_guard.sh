#!/usr/bin/env bash
# practices/evals/test_tag_naming_convention_guard.sh — R24 36th hard guard.
#
# Closes the catalog enforcement loop gap identified in R24 root-cause-fix mode:
# every backend JUnit @Tag("...") value MUST follow the catalog UPPERCASE
# convention — pattern [A-Z][A-Z0-9_.-]* . The per-domain test tasks in
# backend/build.gradle.kts pivot on these tag values (`includeTags("PAYMENT")`,
# `includeTags("ASVS")`, etc.) and tag drift breaks the "single command binary
# pass/fail" promise silently.
#
# Why this guard exists: R23 verification surfaced that
# backend/src/test/java/com/ax/template/authblueprint/search/*.java uses
# `@Tag("search")` (lowercase) while every other domain uses UPPERCASE. Without
# a mechanical check, this drift can propagate to new domains, silently
# excluding tests from `./gradlew test{Domain}` runs.
#
# Convention enforced: tag values MUST match ^[A-Z][A-Z0-9_.-]*$
#   • Allowed:    ASVS, PAYMENT, ECOMMERCE, ASVS-V2.1.1, AUDIT-RECORD-001
#   • Forbidden:  search, payment, Asvs (must be SEARCH, PAYMENT, ASVS)
#
# Usage:
#   bash practices/evals/test_tag_naming_convention_guard.sh           # live repo
#   bash practices/evals/test_tag_naming_convention_guard.sh --fixtures
#   bash practices/evals/test_tag_naming_convention_guard.sh --root DIR
#
# Exit codes: 0 PASS · 1 violation · 2 usage error.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

FIXTURES_MODE=0
ROOT_OVERRIDE=""

while [ $# -gt 0 ]; do
    case "$1" in
        --fixtures) FIXTURES_MODE=1; shift ;;
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "test_tag_naming_convention_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

# ── Fixture mode ─────────────────────────────────────────────────────────────
if [ "$FIXTURES_MODE" -eq 1 ]; then
    FIXTURES_DIR="$SCRIPT_DIR/fixtures/test_tag_naming_convention"
    if [ ! -d "$FIXTURES_DIR" ]; then
        echo "test_tag_naming_convention_guard: fixtures directory missing: $FIXTURES_DIR" >&2
        exit 2
    fi

    pass=0
    fail=0

    for sub in "$FIXTURES_DIR"/pass_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "PASS [test_tag_naming_convention/$(basename "$sub")]"
            pass=$((pass + 1))
        else
            echo "FAIL [test_tag_naming_convention/$(basename "$sub")] — expected exit 0 on PASS fixture"
            fail=$((fail + 1))
        fi
    done

    for sub in "$FIXTURES_DIR"/fail_*; do
        [ -d "$sub" ] || continue
        if bash "$0" --root "$sub" >/dev/null 2>&1; then
            echo "FAIL [test_tag_naming_convention/$(basename "$sub")] — expected exit 1 on FAIL fixture"
            fail=$((fail + 1))
        else
            echo "PASS [test_tag_naming_convention/$(basename "$sub")]"
            pass=$((pass + 1))
        fi
    done

    echo ""
    echo "test_tag_naming_convention_guard: fixtures $pass PASS / $fail FAIL"
    if [ "$fail" -gt 0 ]; then exit 1; fi
    exit 0
fi

# ── Live mode (or --root override) ────────────────────────────────────────────
SCAN_ROOT="${ROOT_OVERRIDE:-$REPO_ROOT}"
if [ ! -d "$SCAN_ROOT" ]; then
    echo "test_tag_naming_convention_guard: root not found: $SCAN_ROOT" >&2
    exit 2
fi

python3 - "$SCAN_ROOT" <<'PYEOF'
import sys
import pathlib
import re
import datetime
import warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)

root = pathlib.Path(sys.argv[1])

# Scan all .java files under backend/src/test for @Tag("...") annotations.
backend_test = root / "backend" / "src" / "test"
fixture_test = root / "src" / "test"  # for fixture mode where root IS a synthetic backend

if backend_test.is_dir():
    scan_root = backend_test
elif fixture_test.is_dir():
    scan_root = fixture_test
else:
    print(f"test_tag_naming_convention_guard: no backend test sources under {root} — nothing to check")
    sys.exit(0)

# Pattern: @Tag("VALUE") — capture VALUE.
tag_re = re.compile(r'@Tag\s*\(\s*"([^"]+)"\s*\)')
# Convention: starts with uppercase letter, followed by uppercase/digit/_/-/.
ok_re = re.compile(r'^[A-Z][A-Z0-9_.-]*$')

violations = []
scanned_files = 0
scanned_tags = 0

for java_file in scan_root.rglob("*.java"):
    scanned_files += 1
    text = java_file.read_text(errors='replace')
    for m in tag_re.finditer(text):
        scanned_tags += 1
        val = m.group(1)
        if not ok_re.match(val):
            # Compute line number for the offending @Tag.
            line_no = text.count('\n', 0, m.start()) + 1
            violations.append((str(java_file.relative_to(root)), line_no, val))

ts = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

if not violations:
    print(f'{{"signal":"test.tag.naming_convention_pass","files":{scanned_files},"tags":{scanned_tags},"ts":"{ts}"}}')
    sys.exit(0)
else:
    for (path, line, val) in violations:
        print(f'VIOLATION [test_tag_naming_convention]: {path}:{line} @Tag("{val}") — expected ^[A-Z][A-Z0-9_.-]*$ (UPPERCASE)')
    print(f'{{"signal":"test.tag.naming_convention_violations","value":{len(violations)},"ts":"{ts}"}}')
    sys.exit(1)
PYEOF
