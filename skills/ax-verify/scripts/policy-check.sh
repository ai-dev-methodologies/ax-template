#!/usr/bin/env bash
# skills/ax-verify/scripts/policy-check.sh — F13: pre-execution policy gate.
#
# AI agents invoke this BEFORE editing files to discover which catalog rules
# apply to the target domain, file path, or rule ID. Outputs a human-readable
# (or JSON) summary of applicable rules so the agent can code-to-policy.
#
# Usage:
#   policy-check.sh --domain <tag>         # match by tag (e.g. persistence, error, cache)
#   policy-check.sh --file <path>          # infer domain from file path
#   policy-check.sh --rule <PRACTICES-ID>  # show single rule summary
#   policy-check.sh --domain <tag> --format json
#   policy-check.sh --list-tags            # list all known tags
#
# Exit codes:
#   0 — lookup complete (results printed; may be 0 matches)
#   1 — invalid arguments or rules dir not found
#
# Output (text, default):
#   RULE  <id>          <impact>   <title>
#   spec_ref: <ref>
#   tags: <t1>, <t2>
#   ---
#
# Output (json):
#   [{"id":"...","title":"...","impact":"...","spec_ref":"...","tags":[...]}]

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
RULES_DIR="$REPO_ROOT/practices/rules"

# ── Argument parsing ──────────────────────────────────────────────────────────
MODE=""
QUERY=""
FORMAT="text"

while [ $# -gt 0 ]; do
    case "$1" in
        --domain)    MODE="domain";  QUERY="$2"; shift 2 ;;
        --domain=*)  MODE="domain";  QUERY="${1#--domain=}"; shift ;;
        --file)      MODE="file";    QUERY="$2"; shift 2 ;;
        --file=*)    MODE="file";    QUERY="${1#--file=}"; shift ;;
        --rule)      MODE="rule";    QUERY="$2"; shift 2 ;;
        --rule=*)    MODE="rule";    QUERY="${1#--rule=}"; shift ;;
        --list-tags) MODE="list-tags"; shift ;;
        --format)    FORMAT="$2"; shift 2 ;;
        --format=*)  FORMAT="${1#--format=}"; shift ;;
        -h|--help)
            sed -n '2,30p' "$0" | grep '^#' | sed 's/^# *//'
            exit 0 ;;
        *) echo "policy-check: unknown argument: $1" >&2; exit 1 ;;
    esac
done

if [ -z "$MODE" ]; then
    echo "policy-check: --domain, --file, --rule, or --list-tags required" >&2
    exit 1
fi

if [ ! -d "$RULES_DIR" ]; then
    echo "policy-check: rules directory not found: $RULES_DIR" >&2
    exit 1
fi

# ── Python helper: parse front-matter and emit JSON ───────────────────────────
# Generates a JSON array of all rule objects from rules/*.md front-matter.
parse_rules_json() {
python3 - "$RULES_DIR" <<'PY'
import sys, pathlib, json, re

rules_dir = pathlib.Path(sys.argv[1])
results = []

for rule_path in sorted(rules_dir.glob("*.md")):
    if rule_path.name.startswith("_") or rule_path.name == ".gitkeep":
        continue
    text = rule_path.read_text(encoding="utf-8")
    # Extract YAML front-matter between first --- delimiters
    m = re.match(r"^---\n(.*?)\n---", text, re.DOTALL)
    if not m:
        continue
    fm = m.group(1)

    def extract(key, default=""):
        pattern = rf"^{key}:\s*(.+)$"
        found = re.search(pattern, fm, re.MULTILINE)
        return found.group(1).strip() if found else default

    def extract_list(key):
        # Matches YAML block list under a key
        pattern = rf"^{key}:\n((?:  - .+\n)*)"
        found = re.search(pattern, fm, re.MULTILINE)
        if not found:
            return []
        return [line.strip().lstrip("- ") for line in found.group(1).strip().split("\n") if line.strip()]

    # Extract spec_ref ID (e.g. PRACTICES-PERS-005 from "specs/...#PRACTICES-PERS-005")
    raw_spec = extract("spec_ref").strip('"\'')
    spec_id = raw_spec.split("#")[-1].strip() if "#" in raw_spec else raw_spec

    obj = {
        "file": rule_path.name,
        "title": extract("title").strip('"\''),
        "impact": extract("impact").strip('"\''),
        "spec_ref": raw_spec,
        "spec_id": spec_id,
        "tags": extract_list("tags"),
        "upstream": extract_list("upstream"),
    }
    results.append(obj)

print(json.dumps(results))
PY
}

# ── Infer tags from file path ─────────────────────────────────────────────────
infer_tags_from_path() {
    local path="$1"
    local tags=()

    # Persistence / JPA patterns
    [[ "$path" == *"Repository"* || "$path" == *".jpa."* || "$path" == *"Entity"* ]] && tags+=("persistence" "jpa")
    [[ "$path" == *"soft"* || "$path" == *"Soft"* ]] && tags+=("soft-delete")
    # Transaction patterns
    [[ "$path" == *"Transactional"* || "$path" == *"Service"* ]] && tags+=("transaction" "core")
    # Error handling
    [[ "$path" == *"Exception"* || "$path" == *"Advice"* || "$path" == *"Handler"* ]] && tags+=("error")
    # Observability
    [[ "$path" == *"Log"* || "$path" == *"Mdc"* || "$path" == *"Trace"* ]] && tags+=("observability")
    # Validation
    [[ "$path" == *"Valid"* || "$path" == *"Dto"* || "$path" == *"Request"* ]] && tags+=("validation")
    # Cache
    [[ "$path" == *"Cache"* || "$path" == *"caffeine"* ]] && tags+=("cache")
    # Security
    [[ "$path" == *"Security"* || "$path" == *"Auth"* || "$path" == *"Jwt"* ]] && tags+=("security")
    # Migration
    [[ "$path" == *"/migration/"* || "$path" == *.sql ]] && tags+=("migration" "flyway")
    # Testing
    [[ "$path" == *"Test"* || "$path" == *"IT.java"* ]] && tags+=("testing")
    # Web / REST
    [[ "$path" == *"Controller"* || "$path" == *"RestController"* ]] && tags+=("web" "api")
    # Config
    [[ "$path" == *"Config"* || "$path" == *"Properties"* ]] && tags+=("config" "configuration-properties")
    # Async
    [[ "$path" == *"Async"* || "$path" == *"Scheduler"* || "$path" == *"Executor"* ]] && tags+=("async")
    # Messaging
    [[ "$path" == *"Message"* || "$path" == *"Event"* || "$path" == *"Publisher"* ]] && tags+=("messaging")
    # Build
    [[ "$path" == *.gradle* || "$path" == *pom.xml ]] && tags+=("build")

    # Fall back: infer from directory segments
    local dirpart
    dirpart="$(dirname "$path")"
    for segment in $(echo "$dirpart" | tr '/' '\n'); do
        case "$segment" in
            persistence|jpa) tags+=("persistence" "jpa") ;;
            transaction|tx)  tags+=("transaction") ;;
            service)         tags+=("transaction" "core") ;;
            error|exception) tags+=("error") ;;
            cache)           tags+=("cache") ;;
            security|auth)   tags+=("security") ;;
            config)          tags+=("config") ;;
            migration|db)    tags+=("migration") ;;
            test*)           tags+=("testing") ;;
            web|controller)  tags+=("web") ;;
            async|scheduler) tags+=("async") ;;
        esac
    done

    # De-duplicate and output
    printf '%s\n' "${tags[@]}" | sort -u | tr '\n' ' '
}

# ── List all known tags ───────────────────────────────────────────────────────
if [ "$MODE" = "list-tags" ]; then
    echo "=== Known tags in practices/rules/ ==="
    echo ""
    parse_rules_json | python3 -c "
import sys, json
rules = json.load(sys.stdin)
tags = set()
for r in rules:
    tags.update(r.get('tags', []))
for t in sorted(tags):
    print(' ', t)
"
    exit 0
fi

# ── Resolve domain tag for --file mode ────────────────────────────────────────
if [ "$MODE" = "file" ]; then
    inferred="$(infer_tags_from_path "$QUERY")"
    if [ -z "$inferred" ]; then
        echo "policy-check: could not infer domain tags from path: $QUERY" >&2
        echo "Hint: use --domain <tag> for explicit lookup" >&2
        exit 1
    fi
    echo "policy-check: inferred tags from '$QUERY': $inferred"
    echo ""
    # Use first inferred tag for lookup; TODO: multi-tag support
    QUERY="${inferred%% *}"
    MODE="domain"
fi

# ── Execute lookup ────────────────────────────────────────────────────────────
ALL_RULES_JSON="$(parse_rules_json)"

if [ "$MODE" = "rule" ]; then
    # Match by spec_id or filename keyword
    query_escaped="${QUERY//\'/\'\\\'\'}"
    MATCHES="$(echo "$ALL_RULES_JSON" | python3 -c "
import sys, json
rules = json.load(sys.stdin)
query = '''$query_escaped'''.upper()
matches = [r for r in rules
           if query in r.get('spec_id','').upper()
           or query in r.get('file','').upper()
           or query in r.get('title','').upper()]
print(json.dumps(matches))
")"
else
    # MODE=domain — match by tag substring
    MATCHES="$(echo "$ALL_RULES_JSON" | python3 -c "
import sys, json
rules = json.load(sys.stdin)
query_tag = '$QUERY'.lower()
matches = [r for r in rules
           if any(query_tag == t.lower() for t in r.get('tags', []))]
print(json.dumps(matches))
")"
fi

MATCH_COUNT="$(echo "$MATCHES" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")"

# ── Format output ─────────────────────────────────────────────────────────────
if [ "$FORMAT" = "json" ]; then
    echo "$MATCHES"
    exit 0
fi

# Text format
echo "=== policy-check: mode=$MODE query='$QUERY' ==="
echo ""

if [ "$MATCH_COUNT" -eq 0 ]; then
    echo "  (no rules matched)"
    echo ""
    echo "policy-check: 0 rules applicable"
    exit 0
fi

echo "$MATCHES" | python3 -c "
import sys, json
rules = json.load(sys.stdin)
for r in rules:
    impact_label = r.get('impact', 'UNKNOWN')
    print(f'  RULE  {r.get(\"spec_id\", r[\"file\"]):30s}  [{impact_label:6s}]  {r[\"title\"]}')
    if r.get('spec_ref'):
        print(f'        spec_ref: {r[\"spec_ref\"]}')
    if r.get('tags'):
        print(f'        tags:     {', '.join(r[\"tags\"])}')
    print()
"

echo "policy-check: $MATCH_COUNT rule(s) applicable to '$QUERY'"
exit 0
