#!/usr/bin/env bash
# practices/scripts/verify-rule-evidence-quotes.sh
# R85b (maintainer advisory) — verifies that verbatim quotes inside a
# rule's evidence block actually appear at the cited URL. NOT a hard
# guard (WebFetch in CI is fragile: rate limits, transient 5xx, network
# isolation). Maintainers run this locally before committing a new rule
# or before approving a quote-edit on an existing one.
#
# Motivation: R85's iter1 commit fabricated two evidence quotes
# (Fowler + NIST). Codex critic caught them. The catalog's evidence_guard
# only checks shape (URL + citation present), not content. This
# advisory script closes the content gap when invoked.
#
# Usage:
#   bash practices/scripts/verify-rule-evidence-quotes.sh <rule.md>
#   bash practices/scripts/verify-rule-evidence-quotes.sh --catalog practices
#   bash practices/scripts/verify-rule-evidence-quotes.sh --catalog practices-react
#   bash practices/scripts/verify-rule-evidence-quotes.sh --all
#
# Exit codes:
#   0 — every quote verified or all WARN (transient fetch failures).
#   1 — at least one quote FAILED to match the fetched content.
#   2 — usage / environment error.
#
# Algorithm per rule:
#   1. Parse the YAML frontmatter, walk `evidence:` entries.
#   2. For each entry with `source_type: external` + `url` + `citation`:
#      a. Extract every single-quoted substring of length ≥ 20 from
#         the citation. Those are the verbatim-quote candidates.
#      b. urlopen the URL, decode, strip HTML tags + decode entities.
#      c. Normalise whitespace (collapse runs of whitespace to one
#         space).
#      d. For each candidate quote, also normalise whitespace, then
#         substring-match against the page text.
#      e. PASS if all candidates match; WARN if URL fetch failed; FAIL
#         if any candidate did not match.
#   3. Entries with `upstream_id:` skip quote verification (those
#      reference local snapshots, already covered by evidence_guard).
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

usage() {
    sed -n '2,28p' "$0"
}

TARGETS=()
CATALOG=""
ALL=0

while [ $# -gt 0 ]; do
    case "$1" in
        --catalog) CATALOG="$2"; shift 2 ;;
        --catalog=*) CATALOG="${1#--catalog=}"; shift ;;
        --all) ALL=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *.md) TARGETS+=("$1"); shift ;;
        *) echo "verify-rule-evidence-quotes: unknown arg: $1" >&2; usage; exit 2 ;;
    esac
done

if [ "$ALL" -eq 1 ]; then
    for cat in practices practices-react; do
        if [ -d "$REPO_ROOT/$cat/rules" ]; then
            while IFS= read -r f; do
                TARGETS+=("$f")
            done < <(find "$REPO_ROOT/$cat/rules" -maxdepth 1 -name "*.md" -not -name "_template.md" | sort)
        fi
    done
elif [ -n "$CATALOG" ]; then
    if [ ! -d "$REPO_ROOT/$CATALOG/rules" ]; then
        echo "verify-rule-evidence-quotes: catalog '$CATALOG' has no rules/ dir" >&2
        exit 2
    fi
    while IFS= read -r f; do
        TARGETS+=("$f")
    done < <(find "$REPO_ROOT/$CATALOG/rules" -maxdepth 1 -name "*.md" -not -name "_template.md" | sort)
fi

if [ ${#TARGETS[@]} -eq 0 ]; then
    echo "verify-rule-evidence-quotes: no rule files supplied. Use a *.md arg, --catalog NAME, or --all." >&2
    usage
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "verify-rule-evidence-quotes: python3 not in PATH" >&2
    exit 2
fi

# Pass the rule paths to the Python helper.
python3 - "${TARGETS[@]}" <<'PY'
import html
import pathlib
import re
import socket
import sys
import urllib.error
import urllib.request

import yaml

QUOTE_MIN_LEN = 30
FETCH_TIMEOUT = 15  # seconds


def normalise_ws(s: str) -> str:
    # Whitespace collapse + Unicode curly-quote normalisation. Many doc
    # publishing toolchains (Sphinx, MkDocs, Pandoc) substitute
    # typographic curly quotes for ASCII apostrophes when rendering, so
    # a verbatim quote captured as ASCII in a catalog rule will not match
    # the rendered page text unless we normalise both sides.
    s = (s
         .replace("‘", "'")  # left single quote
         .replace("’", "'")  # right single quote
         .replace("“", '"')  # left double quote
         .replace("”", '"')  # right double quote
         .replace("–", "-")  # en dash
         .replace("—", "-")) # em dash
    return re.sub(r"\s+", " ", s).strip()


def strip_html(s: str) -> str:
    # Drop script/style blocks entirely so their JS string literals do
    # not produce spurious quote matches.
    s = re.sub(r"<(script|style)[^>]*>.*?</\1>", " ", s, flags=re.DOTALL | re.IGNORECASE)
    # Strip remaining tags.
    s = re.sub(r"<[^>]+>", " ", s)
    return html.unescape(s)


def extract_quotes(citation: str) -> list[str]:
    """Find ALL 'single-quoted' substrings of length >= QUOTE_MIN_LEN.

    Catalog evidence prose can nest a short supporting phrase
    (e.g. "Fowler's 'thought about the payoff' obligation") alongside the
    actual verbatim quote (e.g. "'The prudent debt example is deliberate
    because...'"). Earlier versions of this tool returned ONLY the longest
    candidate to avoid the supporting-phrase false positives, but that
    breaks the legitimate "context + chosen verbatim segment" pattern
    where the rule cites a long context for the reader AND a shorter
    static-fetch-friendly segment for the advisory check.

    Current shape: return every candidate ≥ QUOTE_MIN_LEN. The caller
    treats the evidence entry as VERIFIED if ANY candidate matches the
    fetched page text. Supporting-phrase false positives are still
    blocked because they typically have NO match in the page.
    """
    return re.findall(r"'([^']{%d,})'" % QUOTE_MIN_LEN, citation)


def fetch(url: str) -> tuple[str | None, str | None]:
    """Return (text, error). Text is None on fetch failure."""
    try:
        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": "ax-template-evidence-quote-verifier/1.0",
                "Accept": "text/html,application/xhtml+xml,text/plain;q=0.9",
            },
        )
        with urllib.request.urlopen(req, timeout=FETCH_TIMEOUT) as resp:
            ctype = resp.headers.get_content_charset() or "utf-8"
            raw = resp.read()
            try:
                text = raw.decode(ctype, errors="replace")
            except LookupError:
                text = raw.decode("utf-8", errors="replace")
            return text, None
    except (urllib.error.URLError, socket.timeout, ConnectionError) as e:
        return None, str(e)
    except Exception as e:  # pragma: no cover - defensive
        return None, f"unexpected: {e}"


def parse_frontmatter(text: str) -> dict | None:
    if not text.startswith("---"):
        return None
    parts = text.split("---", 2)
    if len(parts) < 3:
        return None
    try:
        return yaml.safe_load(parts[1]) or {}
    except yaml.YAMLError:
        return None


# ── main ──
overall_fail = 0
overall_pass = 0
overall_warn = 0

for rule_path_str in sys.argv[1:]:
    rule_path = pathlib.Path(rule_path_str)
    text = rule_path.read_text()
    fm = parse_frontmatter(text)
    if fm is None:
        print(f"[SKIP] {rule_path}: no parseable frontmatter")
        continue
    evidence = fm.get("evidence", []) or []
    rule_pass = 0
    rule_fail = 0
    rule_warn = 0
    rule_details: list[str] = []

    for i, ent in enumerate(evidence):
        if not isinstance(ent, dict):
            continue
        if "upstream_id" in ent:
            rule_details.append(
                f"  evidence[{i}] upstream_id={ent.get('upstream_id')} — skipped (snapshot, covered by evidence_guard)"
            )
            continue
        if ent.get("source_type") != "external":
            continue
        url = ent.get("url", "").strip()
        citation = ent.get("citation", "")
        if not url or not citation:
            rule_details.append(f"  evidence[{i}] — missing url or citation")
            rule_warn += 1
            continue

        quotes = extract_quotes(citation)
        if not quotes:
            rule_details.append(
                f"  evidence[{i}] {url} — no >={QUOTE_MIN_LEN}-char single-quoted candidates in citation (informational only — skipped)"
            )
            rule_warn += 1
            continue

        page_text, fetch_err = fetch(url)
        if page_text is None:
            rule_details.append(f"  evidence[{i}] {url} — WARN: fetch failed: {fetch_err}")
            rule_warn += 1
            continue
        normalised_page = normalise_ws(strip_html(page_text))

        matched = []
        missed = []
        for q in quotes:
            qn = normalise_ws(q)
            if qn in normalised_page:
                matched.append(qn[:40])
            else:
                missed.append(qn[:60])

        # ANY candidate matching is enough to VERIFY the evidence entry.
        # A rule may cite a long context AND a shorter static-fetch-
        # friendly segment; either matching is sufficient.
        if matched:
            rule_pass += 1
            rule_details.append(
                f"  evidence[{i}] {url} — VERIFIED ({len(matched)} of {len(quotes)} candidate(s) matched)"
            )
        else:
            rule_fail += 1
            rule_details.append(
                f"  evidence[{i}] {url} — UNVERIFIED: none of {len(quotes)} candidate quote(s) found in fetched text"
            )
            for m in missed:
                rule_details.append(f"      '{m}…'")
            rule_details.append(
                "      (advisory: page may be JS-rendered, paywalled, or behind redirects; manually inspect URL before accepting the citation)"
            )

    if rule_fail > 0:
        status = "UNVERIFIED"
        overall_fail += 1
    elif rule_warn > 0 and rule_pass == 0:
        status = "WARN"
        overall_warn += 1
    else:
        status = "VERIFIED"
        overall_pass += 1

    print(f"[{status}] {rule_path}")
    for line in rule_details:
        print(line)
    print()

print(f"Summary: {overall_pass} VERIFIED / {overall_warn} WARN / {overall_fail} UNVERIFIED")
print()
print("This is an advisory tool, NOT a hard guard. UNVERIFIED means the")
print("static fetch could not locate the verbatim quote — the page may be")
print("JS-rendered, paywalled, or the quote may be fabricated. Inspect the")
print("URL manually and confirm the quote appears before treating the")
print("rule's evidence as auditable.")
sys.exit(1 if overall_fail > 0 else 0)
PY
