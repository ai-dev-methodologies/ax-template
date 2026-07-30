#!/usr/bin/env bash
# practices/scripts/snapshot-extract.sh — PRD-final-4 W1 (Lane A, single-writer).
#
# Deterministic HTML → plain-text extractor for authoring upstream/*.snapshot.md bodies.
# No model in the loop: curl fetches raw bytes, a fixed pipeline turns them into text.
#
# Pipeline (mirrors the DISK half of evidence_quote_spotcheck_guard.sh's own
# strip_html()+normalize() — read those two functions before changing this one, the two
# must never drift apart):
#   1. strip <script>...</script> and <style>...</style> blocks (case-insensitive, DOTALL)
#   2. strip all remaining <tag> markup
#   3. HTML-unescape entities (&amp; &#39; &nbsp; ...)
#   4. collapse all whitespace runs to a single space, trim
#
# This is intentionally NARROWER than the guard's normalize(): it does not touch smart
# quotes/em-dash/ellipsis/backtick/blockquote-marker — those are QUOTE-vs-SNAPSHOT
# comparison relaxations the guard applies at verification time to BOTH sides of a
# citation check, not part of turning HTML into committed snapshot prose. Duplicating
# them here would let two different code paths silently diverge; the guard's normalize()
# is later applied on top of whatever this script writes to disk, same as any other
# snapshot body.
#
# Usage:
#   practices/scripts/snapshot-extract.sh <URL>
#       Fetches the URL, prints the extracted BODY text to stdout, and prints fetch
#       metadata (url/curl_exit/http_status/raw_bytes/bytes/sha256) as `key=value`
#       lines to stderr — `bytes`/`sha256` describe the EXTRACTED output (the same
#       artifact), not the raw HTML download (`raw_bytes` is a separate diagnostic
#       field), so a caller can build a _FETCH-RECEIPTS.yaml row directly from
#       stderr without re-deriving anything. A dead/unreachable URL is NOT a script
#       failure: curl
#       succeeding against a 404 (recharts.org) still produces a valid receipt row
#       with http_status=404 and a (possibly short) extracted body — the caller
#       decides whether that counts as usable content.
#
#   practices/scripts/snapshot-extract.sh --self-test
#       Replays every committed fixture pair under
#       practices/scripts/fixtures/snapshot-extract/*.html against its matching
#       *.expected.txt through the SAME extraction function used for live fetches
#       (no network). Deterministic regression proof for the extractor itself, not
#       the operator or the network. Exit 0 all match, 1 any mismatch, 2 no fixtures
#       found (a self-test that silently checks nothing is a self-test that lies).
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FIXTURES_DIR="$SCRIPT_DIR/fixtures/snapshot-extract"
EXTRACT_PY="$SCRIPT_DIR/lib/snapshot_extract.py"

# Single code path for both live fetches and --self-test — the whole point of a replay
# fixture is that it exercises the ACTUAL extractor, not a hand-copied description of it.
# Implemented as a real file (not a `python3 - <<PY` heredoc): feeding the script body
# via stdin would consume the same fd the script needs to read the HTML from, leaving
# sys.stdin.read() empty inside the heredoc form — a real file avoids that conflict.
extract_stdin() {
    python3 "$EXTRACT_PY"
}

self_test() {
    local pass=1
    local n=0
    local html_file base expected_file actual expected
    for html_file in "$FIXTURES_DIR"/*.html; do
        [ -e "$html_file" ] || continue
        n=$((n + 1))
        base="${html_file%.html}"
        expected_file="${base}.expected.txt"
        if [ ! -f "$expected_file" ]; then
            echo "snapshot-extract: SELF_TEST_MISSING_EXPECTED — $expected_file" >&2
            pass=0
            continue
        fi
        actual="$(extract_stdin < "$html_file")"
        expected="$(cat "$expected_file")"
        if [ "$actual" != "$expected" ]; then
            echo "snapshot-extract: SELF_TEST_MISMATCH — $(basename "$html_file")" >&2
            echo "  expected: $expected" >&2
            echo "  actual:   $actual" >&2
            pass=0
        fi
    done
    if [ "$n" -eq 0 ]; then
        echo "snapshot-extract: SELF_TEST_ZERO_FIXTURES — no *.html fixtures found under $FIXTURES_DIR" >&2
        exit 2
    fi
    if [ "$pass" -eq 1 ]; then
        echo "snapshot-extract: self-test PASS ($n fixture(s))"
        exit 0
    fi
    exit 1
}

fetch_and_extract() {
    local url="$1"
    local tmp_body http_status curl_exit raw_bytes extracted extracted_bytes sha
    tmp_body="$(mktemp)"
    http_status="$(curl -sS -L --max-time 30 -o "$tmp_body" -w '%{http_code}' "$url")"
    curl_exit=$?
    raw_bytes=$(wc -c < "$tmp_body" | tr -d ' ')
    extracted="$(extract_stdin < "$tmp_body")"
    # bytes/sha256 below describe the SAME artifact (the extracted output) — a receipt's
    # byte count must pair with what its sha256 actually hashes, not the raw download size.
    extracted_bytes=$(printf '%s' "$extracted" | wc -c | tr -d ' ')
    sha="$(printf '%s' "$extracted" | shasum -a 256 | cut -d' ' -f1)"
    rm -f "$tmp_body"
    printf '%s' "$extracted"
    {
        echo "url=$url"
        echo "curl_exit=$curl_exit"
        echo "http_status=$http_status"
        echo "raw_bytes=$raw_bytes"
        echo "bytes=$extracted_bytes"
        echo "sha256=$sha"
    } >&2
}

case "${1:-}" in
    --self-test)
        self_test
        ;;
    "")
        echo "usage: snapshot-extract.sh <URL> | snapshot-extract.sh --self-test" >&2
        exit 2
        ;;
    *)
        fetch_and_extract "$1"
        ;;
esac
