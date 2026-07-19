#!/usr/bin/env bash
# practices/evals/locale_aware_format_guard.sh
#
# Closes the confirmed catalog gap surfaced by consumer-proof cell
# S3.e-commerce (practices/consumer-proof/engine/canary-gaps.yaml CANARY-001):
# there was no practices-react rule, no shipped ax/* ESLint rule, and no
# standalone shell guard enforcing locale-aware number/date formatting on the
# frontend. See practices-react/rules/locale-aware-number-date-format.md for
# the rule this guard mechanically enforces (spec_ref:
# specs/i18n-policy-l0.yaml#I18N-FORMATTING-001 — the backend item already
# requires NumberFormat/DateTimeFormatter over hard-coded format strings; this
# guard is the FE-side symmetric enforcement via Intl.NumberFormat /
# Intl.DateTimeFormat).
#
# WHAT IT FORBIDS (money/date display that does NOT go through the Intl API).
# Broadened in wave-1 (codex gpt-5.6-sol finding: the prior denylist let
# `"$" + total.toFixed(2)`, multiline manual date assembly, and .ts formatter
# utilities slip through — it was too narrow and .tsx-only):
#   D1  bare `.toLocaleString()` with NO locale/options argument (locale-blind:
#       silently follows the server/runtime default locale, not the caller's).
#   D2  manual date-part assembly — getMonth()/getDate()/getFullYear() joined
#       with `+` (single-line OR multiline). Hard-codes a display order that
#       cannot switch per locale (ko-KR yyyy.MM.dd vs en-US MM/dd/yyyy).
#   D3  `.toFixed(` on a money-named value (total/price/amount/fee/...) — emits
#       a raw fixed-decimal string with no locale grouping/currency symbol.
#   D4  string-concatenated currency symbol (`"$" + amount`) — hard-codes the
#       symbol and its position instead of Intl currency style.
# Correct code routes money through Intl.NumberFormat and dates through
# Intl.DateTimeFormat.
#
# This guard forbids the anti-patterns above (a repo-wide scan cannot REQUIRE
# every file to contain Intl.* formatting, since most files format neither a
# number nor a date). Comments are stripped before scanning so a descriptive
# comment cannot trigger a match (matches are on real code only). It scans
# BOTH *.tsx AND *.ts (formatter utilities commonly live in .ts). A directory
# with zero *.ts/*.tsx files is a SKIP, not a silent pass.
#
# Exit codes: 0 — no forbidden pattern (or nothing to scan)
#             1 — violation (signature: LOCALE_FORMAT_VIOLATION)
#             2 — usage/env error (python3 missing).
#
# Usage:
#   bash practices/evals/locale_aware_format_guard.sh                # default root=frontend/src
#   bash practices/evals/locale_aware_format_guard.sh --root DIR

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ROOT_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --root) ROOT_OVERRIDE="$2"; shift 2 ;;
        --root=*) ROOT_OVERRIDE="${1#--root=}"; shift ;;
        *) echo "locale_aware_format_guard: unknown arg: $1" >&2; exit 2 ;;
    esac
done

SRC="${ROOT_OVERRIDE:-$REPO_ROOT/frontend/src}"

if [ ! -d "$SRC" ]; then
    echo "locale_aware_format_guard: no such dir $SRC — SKIP"
    exit 0
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "locale_aware_format_guard: python3 not in PATH (required for multiline scan)" >&2
    exit 2
fi

SRC="$SRC" python3 - <<'PY'
import os
import re
import sys

src = os.environ["SRC"]

# ── comment strip (so a descriptive comment can't trigger a match) ─────────
BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT = re.compile(r"(?<!:)//.*$", re.MULTILINE)  # keep https:// intact


def strip_comments(text: str) -> str:
    # replace block comments with equal newline count to preserve line numbers
    text = BLOCK_COMMENT.sub(lambda m: "\n" * m.group(0).count("\n"), text)
    text = LINE_COMMENT.sub("", text)
    return text


# ── detectors ──────────────────────────────────────────────────────────────
# Each is independently falsifiable: deleting one entry greens exactly the
# fixture that isolates it (see practices/evals/fixtures/locale-aware-format/).
D_TOLOCALE = ("bare_toLocaleString",
              re.compile(r"\.toLocaleString\(\s*\)"))
D_MONEY_TOFIXED = ("money_toFixed", re.compile(
    r"\b(?:total|subtotal|grand_?total|price|amount|amt|cost|fee|balance|"
    r"charge|payment|due|paid|money|sum|revenue|refund)\w*\s*\.\s*toFixed\s*\(",
    re.IGNORECASE))
D_CURRENCY_CONCAT = ("currency_symbol_concat", re.compile(
    r"""["'][^"']*[$€£¥₩][^"']*["']\s*\+"""
    r"""|\+\s*["'][^"']*[$€£¥₩]"""))

SINGLE_LINE_DETECTORS = [D_TOLOCALE, D_MONEY_TOFIXED, D_CURRENCY_CONCAT]

# Multiline-aware: getMonth()/getDate()/getFullYear() joined by `+`, across
# lines. `[^;{}]` (which includes newlines) bounds the span to a single
# statement so unrelated getX() calls in different statements don't match.
D_DATE_CONCAT_NAME = "manual_date_concat"
D_DATE_CONCAT = re.compile(
    r"get(?:Month|Date|FullYear)\(\)[^;{}]{0,160}\+[^;{}]{0,160}"
    r"get(?:Month|Date|FullYear)\(\)")

files = []
for root, _dirs, fns in os.walk(src):
    for fn in sorted(fns):
        if fn.endswith(".ts") or fn.endswith(".tsx"):
            files.append(os.path.join(root, fn))
files.sort()

if not files:
    print(f"locale_aware_format_guard: 0 *.ts/*.tsx files under {src} — nothing to check")
    sys.exit(0)

print(f"locale_aware_format_guard: scanned {len(files)} *.ts/*.tsx file(s) under {src}")

hits = []
for path in files:
    with open(path, encoding="utf-8") as fh:
        raw = fh.read()
    code = strip_comments(raw)

    for lineno, line in enumerate(code.splitlines(), start=1):
        for name, rx in SINGLE_LINE_DETECTORS:
            if rx.search(line):
                hits.append(f"{path}:{lineno}: [{name}] {line.strip()[:100]}")

    # multiline date concat — report the line of the first getX() in the match
    m = D_DATE_CONCAT.search(code)
    if m:
        lineno = code.count("\n", 0, m.start()) + 1
        snippet = re.sub(r"\s+", " ", m.group(0))[:100]
        hits.append(f"{path}:{lineno}: [{D_DATE_CONCAT_NAME}] {snippet}")

if hits:
    print("VIOLATION: locale-blind money/date formatting — not routed through "
          "Intl.NumberFormat/Intl.DateTimeFormat:", file=sys.stderr)
    for h in sorted(hits):
        print(f"  {h}", file=sys.stderr)
    print("locale_aware_format_guard: LOCALE_FORMAT_VIOLATION — BLOCKED", file=sys.stderr)
    sys.exit(1)

print("locale_aware_format_guard: no locale-blind formatting found")
sys.exit(0)
PY
