"""practices/scripts/lib/snapshot_extract.py — PRD-final-4 W1 (Lane A).

The single extraction function used by BOTH live curl fetches and --self-test replay
in practices/scripts/snapshot-extract.sh. Kept as its own file (not an inline bash
heredoc) because a `python3 - <<'PY' ... PY` heredoc feeds the script text itself via
stdin, which leaves no stdin left over for the HTML the script is supposed to read —
this file is invoked as `python3 snapshot_extract.py < input.html`, an ordinary stdin
redirect with no such conflict.

Mirrors the DISK half of evidence_quote_spotcheck_guard.sh's strip_html()+normalize()
(script/style strip, tag strip, entity unescape, whitespace collapse) — deliberately
NOT the smart-quote/backtick/blockquote relaxations, which are quote-vs-snapshot
comparison rules the guard applies at verification time, not part of turning raw HTML
into committed snapshot prose. See snapshot-extract.sh's header for the full rationale.
"""
import html
import re
import sys


def extract(raw: str) -> str:
    data = re.sub(r'<script\b.*?</script>', ' ', raw, flags=re.S | re.I)
    data = re.sub(r'<style\b.*?</style>', ' ', data, flags=re.S | re.I)
    data = re.sub(r'<[^>]*>', ' ', data)
    data = html.unescape(data)
    return re.sub(r'\s+', ' ', data).strip()


def main() -> None:
    sys.stdout.write(extract(sys.stdin.read()))


if __name__ == "__main__":
    main()
