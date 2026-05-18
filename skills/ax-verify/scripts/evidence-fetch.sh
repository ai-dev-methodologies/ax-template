#!/usr/bin/env bash
# skills/ax-verify/scripts/evidence-fetch.sh — F14: evidence freshness check.
#
# Scans catalog rules for evidence quality issues:
#   - Rules with empty or missing evidence blocks (stale/invalid)
#   - Rules whose upstream_id references a snapshot ID not in _MANIFEST.yaml
#   - Rules with source_type=external but missing url or citation
#   - (optional) HTTP HEAD check on external URLs to detect 404/5xx
#
# Usage:
#   evidence-fetch.sh --all                    # check all rules
#   evidence-fetch.sh --rule <id-or-file>      # check one rule
#   evidence-fetch.sh --all --http-check       # also probe URLs (slow, network required)
#   evidence-fetch.sh --catalog practices-react
#
# Exit codes:
#   0 — all checked rules have valid evidence
#   1 — one or more evidence issues found (details on stderr)
#   2 — invalid arguments or missing directories

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# ── Argument parsing ──────────────────────────────────────────────────────────
MODE=""
QUERY=""
CATALOG="practices"
HTTP_CHECK=false

while [ $# -gt 0 ]; do
    case "$1" in
        --all)          MODE="all"; shift ;;
        --rule)         MODE="rule"; QUERY="$2"; shift 2 ;;
        --rule=*)       MODE="rule"; QUERY="${1#--rule=}"; shift ;;
        --catalog)      CATALOG="$2"; shift 2 ;;
        --catalog=*)    CATALOG="${1#--catalog=}"; shift ;;
        --http-check)   HTTP_CHECK=true; shift ;;
        -h|--help)
            sed -n '2,25p' "$0" | grep '^#' | sed 's/^# *//'
            exit 0 ;;
        *) echo "evidence-fetch: unknown argument: $1" >&2; exit 2 ;;
    esac
done

if [ -z "$MODE" ]; then
    echo "evidence-fetch: --all or --rule <id> required" >&2
    exit 2
fi

CATALOG_DIR="$REPO_ROOT/$CATALOG"
RULES_DIR="$CATALOG_DIR/rules"
MANIFEST="$CATALOG_DIR/upstream/_MANIFEST.yaml"

if [ ! -d "$RULES_DIR" ]; then
    echo "evidence-fetch: rules directory not found: $RULES_DIR" >&2
    exit 2
fi

# ── Build manifest ID set ─────────────────────────────────────────────────────
MANIFEST_IDS=""
if [ -f "$MANIFEST" ]; then
    MANIFEST_IDS="$(python3 -c "
import yaml, pathlib
d = yaml.safe_load(pathlib.Path('$MANIFEST').read_text()) or {}
ids = [s.get('id','') for s in d.get('snapshots', [])]
print('\n'.join(filter(None, ids)))
" 2>/dev/null || echo "")"
fi

# ── Python evidence checker ───────────────────────────────────────────────────
check_evidence() {
    local rules_pattern="$1"
    python3 - "$rules_pattern" "$MANIFEST_IDS" <<'PY'
import sys, pathlib, json, re, glob

pattern      = sys.argv[1]
manifest_ids = set(filter(None, sys.argv[2].splitlines()))

issues = []
ok     = []

for rule_path_str in sorted(glob.glob(pattern)):
    rule_path = pathlib.Path(rule_path_str)
    if rule_path.name.startswith("_") or rule_path.name == ".gitkeep":
        continue

    text = rule_path.read_text(encoding="utf-8")
    m    = re.match(r"^---\n(.*?)\n---", text, re.DOTALL)
    if not m:
        issues.append({"file": rule_path.name, "issue": "missing_frontmatter"})
        continue

    fm = m.group(1)

    # Extract evidence block list
    evidence_section = re.search(r"^evidence:\n((?:  .*\n?)*)", fm, re.MULTILINE)
    if not evidence_section:
        issues.append({"file": rule_path.name, "issue": "no_evidence_block"})
        continue

    evidence_text = evidence_section.group(1)

    # Parse evidence entries (simplified — look for upstream_id and source_type)
    if not evidence_text.strip():
        issues.append({"file": rule_path.name, "issue": "empty_evidence_block"})
        continue

    # Check upstream_id references exist in manifest
    for uid in re.findall(r"upstream_id:\s*(\S+)", evidence_text):
        if manifest_ids and uid not in manifest_ids:
            issues.append({
                "file": rule_path.name,
                "issue": "unknown_upstream_id",
                "detail": f"upstream_id='{uid}' not in _MANIFEST.yaml"
            })

    # Check external sources have non-empty url and citation
    if re.search(r"source_type:\s*external", evidence_text):
        # url: may be bare or quoted: url: https://... or url: "https://..."
        has_url      = bool(re.search(r'url:\s*["\']?https?://', evidence_text))
        has_citation = bool(re.search(r"citation:\s*\S", evidence_text))
        if not has_url:
            issues.append({"file": rule_path.name, "issue": "external_missing_url"})
        if not has_citation:
            issues.append({"file": rule_path.name, "issue": "external_missing_citation"})

    if not any(i["file"] == rule_path.name for i in issues):
        ok.append(rule_path.name)

result = {"ok": ok, "issues": issues}
print(json.dumps(result))
PY
}

# ── HTTP URL probe (optional) ─────────────────────────────────────────────────
probe_urls() {
    local rules_pattern="$1"
    python3 - "$rules_pattern" <<'PY'
import sys, pathlib, re, glob
try:
    import urllib.request, urllib.error
    has_urllib = True
except ImportError:
    has_urllib = False

pattern = sys.argv[1]
broken  = []

for rule_path_str in sorted(glob.glob(pattern)):
    rule_path = pathlib.Path(rule_path_str)
    if rule_path.name.startswith("_"):
        continue
    text = rule_path.read_text(encoding="utf-8")
    m    = re.match(r"^---\n(.*?)\n---", text, re.DOTALL)
    if not m:
        continue
    fm = m.group(1)
    urls = re.findall(r"url:\s*(https?://\S+)", fm)
    for url in urls:
        url = url.rstrip('"').rstrip("'")
        if not has_urllib:
            break
        try:
            req = urllib.request.Request(url, method="HEAD",
                                         headers={"User-Agent": "ax-verify/evidence-fetch"})
            resp = urllib.request.urlopen(req, timeout=8)
            if resp.status >= 400:
                broken.append({"file": rule_path.name, "url": url, "status": resp.status})
        except Exception as e:
            broken.append({"file": rule_path.name, "url": url, "error": str(e)[:80]})

import json
print(json.dumps(broken))
PY
}

# ── Build file pattern ─────────────────────────────────────────────────────────
if [ "$MODE" = "all" ]; then
    PATTERN="$RULES_DIR/*.md"
elif [ "$MODE" = "rule" ]; then
    # Handle absolute paths first
    if [ -f "$QUERY" ]; then
        PATTERN="$QUERY"
    else
        # Try relative to RULES_DIR
        exact="$RULES_DIR/$QUERY"
        if [ -f "$exact" ]; then
            PATTERN="$exact"
        else
            # Find by spec_id or title substring within catalog rules
            matched="$(find "$RULES_DIR" -name "*.md" | \
                python3 -c "
import sys, re, pathlib
query = '$QUERY'.lower()
for line in sys.stdin:
    p = pathlib.Path(line.strip())
    try:
        text = p.read_text()
        m = re.match(r'^---\n(.*?)\n---', text, re.DOTALL)
        if m and query in m.group(1).lower():
            print(str(p))
    except Exception:
        pass
" | head -1)"
            if [ -z "$matched" ]; then
                echo "evidence-fetch: no rule matching '$QUERY'" >&2
                exit 2
            fi
            PATTERN="$matched"
        fi
    fi
fi

echo "=== evidence-fetch: catalog=$CATALOG mode=$MODE ==="
echo ""

# Run check
RESULT="$(check_evidence "$PATTERN")"
OK_COUNT="$(echo "$RESULT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d['ok']))")"
ISSUE_COUNT="$(echo "$RESULT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d['issues']))")"

# Print issues
echo "$RESULT" | python3 -c "
import sys, json
d     = json.load(sys.stdin)
ok    = d['ok']
issues = d['issues']

if not issues:
    print('  All rules have valid evidence.')
else:
    print(f'  ISSUES ({len(issues)}):')
    for issue in issues:
        detail = issue.get('detail', '')
        print(f'    WARN [{issue[\"file\"]}] {issue[\"issue\"]}' + (f' — {detail}' if detail else ''))

print()
print(f'  OK: {len(ok)} rule(s)   ISSUES: {len(issues)} rule(s)')
"

# Optional HTTP probe
if [ "$HTTP_CHECK" = "true" ]; then
    echo ""
    echo "=== HTTP URL probe (--http-check) ==="
    BROKEN="$(probe_urls "$PATTERN")"
    BROKEN_COUNT="$(echo "$BROKEN" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")"
    if [ "$BROKEN_COUNT" -eq 0 ]; then
        echo "  All URLs reachable."
    else
        echo "$BROKEN" | python3 -c "
import sys, json
for b in json.load(sys.stdin):
    detail = b.get('error') or f'HTTP {b.get(\"status\")}'
    print(f'  BROKEN [{b[\"file\"]}] {b[\"url\"]} — {detail}')
"
    fi
fi

echo ""
if [ "$ISSUE_COUNT" -eq 0 ]; then
    echo "evidence-fetch: all rules PASS"
    exit 0
else
    echo "evidence-fetch: $ISSUE_COUNT rule(s) have evidence issues" >&2
    exit 1
fi
