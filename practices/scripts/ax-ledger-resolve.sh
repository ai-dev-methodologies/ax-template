#!/usr/bin/env bash
# practices/scripts/ax-ledger-resolve.sh
# ───────────────────────────────────────────────────────────────────────────────
# ax-ledger IMPROVE→FEEDBACK — close the loop: after a review classifies a recurring friction and a
# catalog change is filed (a rule/guard added/relaxed/clarified + recorded in practices/DECISIONS.md),
# mark the matching ledger events reviewed=true with the resolution. This is what makes the next fork
# stronger — the friction is now answered by the catalog, and the ledger records WHY.
#
# Usage:
#   bash practices/scripts/ax-ledger-resolve.sh --match <gate-or-rule> --resolution "<text>" \
#        [--classification gap|relax|doc|oneoff] [--decision <DECISIONS anchor or commit>]
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LEDGER_DIR="${AX_LEDGER_DIR:-$REPO_ROOT/.ax-ledger}"

MATCH=""; RESOLUTION=""; CLASSIFICATION="oneoff"; DECISION=""
while [ $# -gt 0 ]; do
    case "$1" in
        --match) MATCH="$2"; shift 2 ;;
        --resolution) RESOLUTION="$2"; shift 2 ;;
        --classification) CLASSIFICATION="$2"; shift 2 ;;
        --decision) DECISION="$2"; shift 2 ;;
        *) echo "ax-ledger-resolve: unknown arg: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$MATCH" ] || [ -z "$RESOLUTION" ]; then
    echo "usage: ax-ledger-resolve.sh --match <gate|rule> --resolution \"<text>\" [--classification gap|relax|doc|oneoff] [--decision <ref>]" >&2
    exit 2
fi
EVENTS="$LEDGER_DIR/events.jsonl"
[ -f "$EVENTS" ] || { echo "[ax-ledger] no ledger at $EVENTS"; exit 0; }

ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
python3 - "$EVENTS" "$MATCH" "$RESOLUTION" "$CLASSIFICATION" "$DECISION" "$ts" <<'PY'
import sys, json, os
path, match, resolution, classification, decision, ts = sys.argv[1:]
out, n = [], 0
with open(path, encoding="utf-8") as f:
    for line in f:
        line = line.rstrip("\n")
        if not line.strip():
            continue
        try:
            ev = json.loads(line)
        except json.JSONDecodeError:
            out.append(line); continue
        if not isinstance(ev, dict):
            out.append(line); continue
        # EXACT field match (gate OR rule) — substring containment would resolve unrelated events
        if not ev.get("reviewed") and match in ((ev.get("gate") or ""), (ev.get("rule") or "")):
            ev["reviewed"] = True
            ev["resolution"] = resolution
            ev["classification"] = classification
            if decision:
                ev["decision_ref"] = decision
            ev["resolved_at"] = ts
            n += 1
        out.append(json.dumps(ev, ensure_ascii=False))
# atomic rewrite: tmp + rename, so a crash/concurrent-append cannot corrupt or truncate the ledger
tmp = path + ".tmp"
with open(tmp, "w", encoding="utf-8") as f:
    f.write("\n".join(out) + ("\n" if out else ""))
os.replace(tmp, path)
print(f"[ax-ledger] resolved {n} event(s) matching '{match}' → {classification}: {resolution}")
PY
