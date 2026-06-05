#!/usr/bin/env bash
# practices/scripts/ax-ledger-review.sh
# ───────────────────────────────────────────────────────────────────────────────
# ax-ledger REVIEW (복기) — read the per-project usage ledger and surface what happened plus the
# improvement directions it implies. This is the retrospective half of the capture→review→improve→
# feedback loop: it does NOT change the catalog; it tells you WHERE to.
#
# Usage:
#   bash practices/scripts/ax-ledger-review.sh [--since YYYY-MM-DD] [--user EMAIL] [--unreviewed]
#
#   --since       only events on/after this UTC date (use for "this session" by passing today)
#   --user        only events from this user
#   --unreviewed  only events not yet marked reviewed=true
#
# Output: counts by kind, the top recurring violated rules/gates, open bypass attempts, and — for any
# rule violated ≥ AX_LEDGER_IMPROVE_THRESHOLD (default 3) times — a concrete improvement candidate.
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LEDGER_DIR="${AX_LEDGER_DIR:-$REPO_ROOT/.ax-ledger}"
THRESHOLD="${AX_LEDGER_IMPROVE_THRESHOLD:-3}"

SINCE=""; USER_FILTER=""; UNREVIEWED=0
while [ $# -gt 0 ]; do
    case "$1" in
        --since) SINCE="$2"; shift 2 ;;
        --since=*) SINCE="${1#--since=}"; shift ;;
        --user) USER_FILTER="$2"; shift 2 ;;
        --user=*) USER_FILTER="${1#--user=}"; shift ;;
        --unreviewed) UNREVIEWED=1; shift ;;
        *) echo "ax-ledger-review: unknown arg: $1" >&2; exit 2 ;;
    esac
done

EVENTS="$LEDGER_DIR/events.jsonl"
if [ ! -f "$EVENTS" ]; then
    echo "[ax-ledger] no ledger yet at $EVENTS — nothing to review (clean slate)."
    exit 0
fi

python3 - "$EVENTS" "$SINCE" "$USER_FILTER" "$UNREVIEWED" "$THRESHOLD" <<'PY'
import sys, json
from collections import Counter, defaultdict

path, since, user_filter, unreviewed, threshold = sys.argv[1:]
unreviewed = unreviewed == "1"
threshold = int(threshold)

rows = []
with open(path, encoding="utf-8") as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        try:
            ev = json.loads(line)
        except json.JSONDecodeError:
            continue
        if not isinstance(ev, dict):   # tolerate a stray non-object line without bricking the pipeline
            continue
        if since and ev.get("ts", "") < since:
            continue
        if user_filter and ev.get("user") != user_filter:
            continue
        if unreviewed and ev.get("reviewed"):
            continue
        rows.append(ev)

if not rows:
    print("[ax-ledger] no events match the filter.")
    sys.exit(0)

projects = sorted({e.get("project", "?") for e in rows})
users = sorted({e.get("user", "?") for e in rows})
print(f"=== ax-ledger review — {len(rows)} events ===")
print(f"projects: {', '.join(projects)}")
print(f"users:    {', '.join(users)}")

by_kind = Counter(e.get("kind", "?") for e in rows)
print("\n# by kind")
for k, n in by_kind.most_common():
    print(f"  {k:18s} {n}")

# recurring friction by (gate|rule) — gate FAILs, refused requests, bypass attempts, AND dogfood
# findings all feed improvement (a dogfood finding is exactly a catalog gap to answer)
viol = [e for e in rows if e.get("kind") in
        ("violation", "request_rejected", "bypass_attempt", "dogfood_finding")]
hot = Counter((e.get("gate") or e.get("rule") or "?") for e in viol)
if hot:
    print("\n# recurring rule/gate friction (violation + rejected + bypass)")
    for key, n in hot.most_common(15):
        print(f"  {n:3d}×  {key}")

open_bypass = [e for e in rows if e.get("kind") == "bypass_attempt" and not e.get("reviewed")]
if open_bypass:
    print(f"\n# !! open bypass attempts (Iron Law): {len(open_bypass)}")
    for e in open_bypass[:8]:
        print(f"  {e.get('ts')}  {e.get('user')}  {e.get('detail','')[:80]}")

# improvement directions — anything crossing the threshold is a catalog-improvement candidate
candidates = [(k, n) for k, n in hot.most_common() if n >= threshold]
print(f"\n# 개선 방향 (improvement candidates, ≥{threshold} occurrences)")
if not candidates:
    print(f"  (none yet — no rule/gate hit {threshold}× of friction)")
else:
    for key, n in candidates:
        print(f"  ▸ '{key}' caused friction {n}× — classify + feed back:")
        print(f"      (a) real catalog gap   → add/fix a rule or guard, record in practices/DECISIONS.md")
        print(f"      (b) over-strict rule   → relax with external evidence (RFC/OWASP/vendor), update the rule")
        print(f"      (c) doc / UX gap       → clarify the rule's rationale + the fix_playbook")
        print(f"      (d) legitimate one-off → note it; mark the events reviewed=true")

unrev = sum(1 for e in rows if not e.get("reviewed"))
print(f"\nunreviewed: {unrev}/{len(rows)}  (resolve by classifying above, then "
      f"bash practices/scripts/ax-ledger-resolve.sh marks them reviewed)")
PY
